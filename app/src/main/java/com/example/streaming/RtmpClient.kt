package com.example.streaming

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLSocketFactory
import kotlin.random.Random

/**
 * Pure-Kotlin RTMP 1.0 & RTMPS (TLS) ingest client with connection error reporting
 * for seamless multi-hour auto-reconnects.
 */
class RtmpClient(
    val destinationId: Long,
    val destinationName: String,
    private val rtmpUrl: String,
    private val streamKey: String
) {
    private var socket: Socket? = null
    private var input: BufferedInputStream? = null
    private var output: BufferedOutputStream? = null

    private val isConnected = AtomicBoolean(false)
    private val writeLock = Any()
    private val bytesSent = AtomicLong(0L)
    private var chunkSizeOut = 4096

    @Volatile private var sentVideoConfig = false
    @Volatile private var sentAudioConfig = false
    @Volatile var audioSampleRateHz: Int = 44100
    @Volatile var audioBitrateKbps: Int = 160

    fun getAndResetBytesSent(): Long = bytesSent.getAndSet(0L)

    fun isActive(): Boolean = isConnected.get()

    companion object {
        /**
         * Performs a real TCP/TLS socket connect + C0/C1/C2 handshake test against the
         * configured RTMP/RTMPS server and returns the round-trip latency in milliseconds.
         */
        fun testHandshake(rtmpUrl: String, streamKey: String): Result<Long> {
            return runCatching {
                val startNs = System.nanoTime()
                val tester = RtmpClient(0L, "Preflight Test", rtmpUrl, streamKey)
                try {
                    val fullUrl = buildFullUrl(rtmpUrl, streamKey)
                    val parsed = parseRtmpUri(fullUrl, requireStreamKey = false)
                    val useTls = parsed.scheme.equals("rtmps", ignoreCase = true)
                    val port = if (parsed.port > 0) parsed.port else if (useTls) 443 else 1935
                    val rawSocket = Socket().apply {
                        tcpNoDelay = true
                        keepAlive = true
                        soTimeout = 5_000
                        connect(InetSocketAddress(parsed.host, port), 5_000)
                    }
                    val activeSocket = if (useTls) {
                        val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                        sslFactory.createSocket(rawSocket, parsed.host, port, true).apply {
                            soTimeout = 5_000
                        }
                    } else rawSocket
                    tester.socket = activeSocket
                    tester.input = BufferedInputStream(activeSocket.getInputStream(), 8192)
                    tester.output = BufferedOutputStream(activeSocket.getOutputStream(), 8192)
                    tester.performHandshake()
                    ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
                } finally {
                    tester.close()
                }
            }
        }

        internal data class ParsedRtmpUri(
            val scheme: String,
            val host: String,
            val port: Int,
            val app: String,
            val streamName: String,
            val tcUrl: String
        )

        internal fun buildFullUrl(base: String, key: String): String {
            val trimmedBase = base.trim()
            val trimmedKey = key.trim()
            if (trimmedKey.isEmpty()) return trimmedBase
            return if (trimmedBase.endsWith("/")) "$trimmedBase$trimmedKey" else "$trimmedBase/$trimmedKey"
        }

        internal fun parseRtmpUri(fullUrl: String, requireStreamKey: Boolean = true): ParsedRtmpUri {
            val uri = URI(fullUrl)
            val scheme = uri.scheme ?: "rtmp"
            val host = uri.host ?: throw IllegalArgumentException("Invalid RTMP host in URL: $fullUrl")
            val port = uri.port
            val rawPath = (uri.path ?: "").trimStart('/')
            val segments = rawPath.split('/', limit = 2)
            val app = segments.getOrNull(0)?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Missing RTMP app path (e.g. /app or /live2)")
            val streamName = segments.getOrNull(1)?.takeIf { it.isNotBlank() } ?: if (requireStreamKey) {
                throw IllegalArgumentException("Missing Stream Key")
            } else {
                ""
            }
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            val portSuffix = if (port > 0) ":$port" else ""
            val tcUrl = "$scheme://$host$portSuffix/$app"
            return ParsedRtmpUri(scheme, host, port, app, "$streamName$query", tcUrl)
        }
    }

    fun connect(width: Int, height: Int, fps: Int, videoBitrateKbps: Int): Result<Unit> {
        return runCatching {
            close()
            val fullUrl = buildFullUrl(rtmpUrl, streamKey)
            val parsed = parseRtmpUri(fullUrl, requireStreamKey = true)
            val useTls = parsed.scheme.equals("rtmps", ignoreCase = true)
            val port = if (parsed.port > 0) parsed.port else if (useTls) 443 else 1935

            val rawSocket = Socket().apply {
                tcpNoDelay = true
                keepAlive = true
                soTimeout = 10_000
                sendBufferSize = 256 * 1024
                connect(InetSocketAddress(parsed.host, port), 8_000)
            }
            val activeSocket = if (useTls) {
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                sslFactory.createSocket(rawSocket, parsed.host, port, true).apply {
                    soTimeout = 10_000
                }
            } else {
                rawSocket
            }

            socket = activeSocket
            input = BufferedInputStream(activeSocket.getInputStream(), 65536)
            output = BufferedOutputStream(activeSocket.getOutputStream(), 65536)

            performHandshake()
            if (!sendSetChunkSize(chunkSizeOut)) throw IllegalStateException("RTMP connection closed during SetChunkSize")
            if (!sendConnect(parsed.app, parsed.tcUrl)) throw IllegalStateException("RTMP connection closed during connect")
            drainServerResponses(timeoutMs = 1500)
            if (!sendReleaseStream(parsed.streamName)) throw IllegalStateException("RTMP connection closed during releaseStream")
            if (!sendFcPublish(parsed.streamName)) throw IllegalStateException("RTMP connection closed during FCPublish")
            if (!sendCreateStream()) throw IllegalStateException("RTMP connection closed during createStream")
            drainServerResponses(timeoutMs = 1000)
            if (!sendPublish(parsed.streamName)) throw IllegalStateException("RTMP server rejected publish or stream key")
            drainServerResponses(timeoutMs = 1000)
            if (!sendMetaData(width, height, fps, videoBitrateKbps)) throw IllegalStateException("RTMP connection closed during onMetaData")

            sentVideoConfig = false
            sentAudioConfig = false
            isConnected.set(true)
        }.onFailure {
            close()
        }
    }

    fun sendAudioConfig(aacConfig: ByteArray): Boolean {
        if (!isConnected.get() || sentAudioConfig) return true
        val payload = ByteArray(2 + aacConfig.size)
        payload[0] = 0xAF.toByte()
        payload[1] = 0x00
        System.arraycopy(aacConfig, 0, payload, 2, aacConfig.size)
        val ok = sendRtmpPacket(chunkStreamId = 4, timestampMs = 0, msgType = 8, streamId = 1, payload = payload)
        if (ok) sentAudioConfig = true
        return ok
    }

    fun sendAudioFrame(aacFrame: ByteArray, timestampMs: Int): Boolean {
        if (!isConnected.get()) return false
        val payload = ByteArray(2 + aacFrame.size)
        payload[0] = 0xAF.toByte()
        payload[1] = 0x01
        System.arraycopy(aacFrame, 0, payload, 2, aacFrame.size)
        return sendRtmpPacket(chunkStreamId = 4, timestampMs = timestampMs, msgType = 8, streamId = 1, payload = payload)
    }

    fun sendVideoConfig(sps: ByteArray, pps: ByteArray): Boolean {
        if (!isConnected.get() || sentVideoConfig) return true
        val out = ByteArrayOutputStream()
        out.write(0x17)
        out.write(0x00)
        out.write(0x00); out.write(0x00); out.write(0x00)
        out.write(0x01)
        out.write(if (sps.size > 1) sps[1].toInt() else 0x42)
        out.write(if (sps.size > 2) sps[2].toInt() else 0xC0)
        out.write(if (sps.size > 3) sps[3].toInt() else 0x1F)
        out.write(0xFF)
        out.write(0xE1)
        out.write((sps.size shr 8) and 0xFF)
        out.write(sps.size and 0xFF)
        out.write(sps)
        out.write(0x01)
        out.write((pps.size shr 8) and 0xFF)
        out.write(pps.size and 0xFF)
        out.write(pps)

        val ok = sendRtmpPacket(chunkStreamId = 6, timestampMs = 0, msgType = 9, streamId = 1, payload = out.toByteArray())
        if (ok) sentVideoConfig = true
        return ok
    }

    fun sendVideoFrame(avccNalus: ByteArray, isKeyFrame: Boolean, timestampMs: Int): Boolean {
        if (!isConnected.get() || !sentVideoConfig) return false
        val out = ByteArrayOutputStream(5 + avccNalus.size)
        out.write(if (isKeyFrame) 0x17 else 0x27)
        out.write(0x01)
        out.write(0x00); out.write(0x00); out.write(0x00)
        out.write(avccNalus)
        return sendRtmpPacket(chunkStreamId = 6, timestampMs = timestampMs, msgType = 9, streamId = 1, payload = out.toByteArray())
    }

    fun close() {
        isConnected.set(false)
        runCatching { input?.close() }
        runCatching { output?.close() }
        runCatching { socket?.close() }
        input = null
        output = null
        socket = null
    }

    private fun performHandshake() {
        val out = output!!
        val inp = input!!
        out.write(0x03)
        val c1 = ByteArray(1536)
        Random.nextBytes(c1)
        for (i in 0 until 8) c1[i] = 0
        out.write(c1)
        out.flush()

        val s0 = inp.read()
        if (s0 != 0x03) throw IllegalStateException("Unexpected RTMP server version: $s0")
        val s1 = readFully(inp, 1536)
        val s2 = readFully(inp, 1536)
        if (s2.size != 1536) throw IllegalStateException("Incomplete RTMP S2 handshake")

        out.write(s1)
        out.flush()
    }

    private fun sendSetChunkSize(size: Int): Boolean {
        val buf = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(size).array()
        return sendRtmpPacket(chunkStreamId = 2, timestampMs = 0, msgType = 1, streamId = 0, payload = buf)
    }

    private fun sendConnect(app: String, tcUrl: String): Boolean {
        val amf = Amf0Writer()
        amf.writeString("connect")
        amf.writeNumber(1.0)
        amf.writeObjectStart()
        amf.writeProperty("app", app)
        amf.writeProperty("type", "nonprivate")
        amf.writeProperty("flashVer", "FMLE/3.0 (compatible; mobileLabs/2.0)")
        amf.writeProperty("tcUrl", tcUrl)
        amf.writeProperty("fpad", false)
        amf.writeProperty("capabilities", 15.0)
        amf.writeProperty("audioCodecs", 1024.0)
        amf.writeProperty("videoCodecs", 128.0)
        amf.writeProperty("videoFunction", 1.0)
        amf.writeObjectEnd()
        return sendRtmpPacket(chunkStreamId = 3, timestampMs = 0, msgType = 20, streamId = 0, payload = amf.toByteArray())
    }

    private fun sendReleaseStream(streamName: String): Boolean {
        val amf = Amf0Writer()
        amf.writeString("releaseStream")
        amf.writeNumber(2.0)
        amf.writeNull()
        amf.writeString(streamName)
        return sendRtmpPacket(chunkStreamId = 3, timestampMs = 0, msgType = 20, streamId = 0, payload = amf.toByteArray())
    }

    private fun sendFcPublish(streamName: String): Boolean {
        val amf = Amf0Writer()
        amf.writeString("FCPublish")
        amf.writeNumber(3.0)
        amf.writeNull()
        amf.writeString(streamName)
        return sendRtmpPacket(chunkStreamId = 3, timestampMs = 0, msgType = 20, streamId = 0, payload = amf.toByteArray())
    }

    private fun sendCreateStream(): Boolean {
        val amf = Amf0Writer()
        amf.writeString("createStream")
        amf.writeNumber(4.0)
        amf.writeNull()
        return sendRtmpPacket(chunkStreamId = 3, timestampMs = 0, msgType = 20, streamId = 0, payload = amf.toByteArray())
    }

    private fun sendPublish(streamName: String): Boolean {
        val amf = Amf0Writer()
        amf.writeString("publish")
        amf.writeNumber(5.0)
        amf.writeNull()
        amf.writeString(streamName)
        amf.writeString("live")
        return sendRtmpPacket(chunkStreamId = 8, timestampMs = 0, msgType = 20, streamId = 1, payload = amf.toByteArray())
    }

    private fun sendMetaData(width: Int, height: Int, fps: Int, videoBitrateKbps: Int): Boolean {
        val amf = Amf0Writer()
        amf.writeString("@setDataFrame")
        amf.writeString("onMetaData")
        amf.writeEcmaArrayStart(10)
        amf.writeProperty("width", width.toDouble())
        amf.writeProperty("height", height.toDouble())
        amf.writeProperty("framerate", fps.toDouble())
        amf.writeProperty("videocodecid", 7.0)
        amf.writeProperty("videodatarate", videoBitrateKbps.toDouble())
        amf.writeProperty("audiocodecid", 10.0)
        amf.writeProperty("audiodatarate", audioBitrateKbps.toDouble())
        amf.writeProperty("audiosamplerate", audioSampleRateHz.toDouble())
        amf.writeProperty("audiosamplesize", 16.0)
        amf.writeProperty("stereo", false)
        amf.writeObjectEnd()
        return sendRtmpPacket(chunkStreamId = 4, timestampMs = 0, msgType = 18, streamId = 1, payload = amf.toByteArray())
    }

    private fun sendRtmpPacket(
        chunkStreamId: Int,
        timestampMs: Int,
        msgType: Int,
        streamId: Int,
        payload: ByteArray
    ): Boolean {
        return synchronized(writeLock) {
            val out = output ?: return@synchronized false
            try {
                var offset = 0
                val length = payload.size
                var firstChunk = true

                while (offset < length || (firstChunk && length == 0)) {
                    val toWrite = minOf(chunkSizeOut, length - offset)
                    if (firstChunk) {
                        out.write(chunkStreamId and 0x3F)
                        val ts = timestampMs.coerceAtLeast(0)
                        val tsField = if (ts >= 0xFFFFFF) 0xFFFFFF else ts
                        out.write((tsField shr 16) and 0xFF)
                        out.write((tsField shr 8) and 0xFF)
                        out.write(tsField and 0xFF)
                        out.write((length shr 16) and 0xFF)
                        out.write((length shr 8) and 0xFF)
                        out.write(length and 0xFF)
                        out.write(msgType and 0xFF)
                        out.write(streamId and 0xFF)
                        out.write((streamId shr 8) and 0xFF)
                        out.write((streamId shr 16) and 0xFF)
                        out.write((streamId shr 24) and 0xFF)
                        if (ts >= 0xFFFFFF) {
                            out.write((ts shr 24) and 0xFF)
                            out.write((ts shr 16) and 0xFF)
                            out.write((ts shr 8) and 0xFF)
                            out.write(ts and 0xFF)
                        }
                        firstChunk = false
                    } else {
                        out.write(0xC0 or (chunkStreamId and 0x3F))
                    }
                    if (toWrite > 0) {
                        out.write(payload, offset, toWrite)
                        offset += toWrite
                        bytesSent.addAndGet(toWrite.toLong())
                    } else {
                        break
                    }
                }
                out.flush()
                true
            } catch (_: Throwable) {
                close()
                false
            }
        }
    }

    private fun drainServerResponses(timeoutMs: Int) {
        val inp = input ?: return
        val sock = socket ?: return
        val prevTimeout = sock.soTimeout
        sock.soTimeout = timeoutMs.coerceAtLeast(250)
        try {
            val tmp = ByteArray(4096)
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                if (inp.available() > 0) {
                    inp.read(tmp)
                } else {
                    Thread.sleep(40)
                }
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { sock.soTimeout = prevTimeout }
        }
    }

    private fun readFully(inp: InputStream, size: Int): ByteArray {
        val buf = ByteArray(size)
        var read = 0
        while (read < size) {
            val n = inp.read(buf, read, size - read)
            if (n < 0) throw EOFException("RTMP stream closed during read")
            read += n
        }
        return buf
    }

    private class Amf0Writer {
        private val out = ByteArrayOutputStream()

        fun writeNumber(value: Double) {
            out.write(0x00)
            val bits = java.lang.Double.doubleToRawLongBits(value)
            for (shift in 56 downTo 0 step 8) {
                out.write(((bits ushr shift) and 0xFF).toInt())
            }
        }

        fun writeBoolean(value: Boolean) {
            out.write(0x01)
            out.write(if (value) 1 else 0)
        }

        fun writeString(value: String) {
            out.write(0x02)
            writeRawString(value)
        }

        fun writeNull() {
            out.write(0x05)
        }

        fun writeObjectStart() {
            out.write(0x03)
        }

        fun writeEcmaArrayStart(count: Int) {
            out.write(0x08)
            out.write((count shr 24) and 0xFF)
            out.write((count shr 16) and 0xFF)
            out.write((count shr 8) and 0xFF)
            out.write(count and 0xFF)
        }

        fun writeProperty(key: String, value: String) {
            writeRawString(key)
            writeString(value)
        }

        fun writeProperty(key: String, value: Double) {
            writeRawString(key)
            writeNumber(value)
        }

        fun writeProperty(key: String, value: Boolean) {
            writeRawString(key)
            writeBoolean(value)
        }

        fun writeObjectEnd() {
            out.write(0x00)
            out.write(0x00)
            out.write(0x09)
        }

        private fun writeRawString(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            out.write((bytes.size shr 8) and 0xFF)
            out.write(bytes.size and 0xFF)
            out.write(bytes)
        }

        fun toByteArray(): ByteArray = out.toByteArray()
    }
}
