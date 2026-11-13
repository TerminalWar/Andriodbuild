package com.example.streaming

import com.example.data.model.ChatPlatform
import com.example.data.model.LiveAlertEvent
import com.example.data.model.StreamChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

enum class ChatConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class TwitchIrcChatClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentChannel: String = ""

    private val _status = MutableStateFlow(ChatConnectionStatus.DISCONNECTED)
    val status: StateFlow<ChatConnectionStatus> = _status.asStateFlow()

    private val _messages = MutableSharedFlow<StreamChatMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<StreamChatMessage> = _messages.asSharedFlow()

    private val _alerts = MutableSharedFlow<LiveAlertEvent>(extraBufferCapacity = 16)
    val alerts: SharedFlow<LiveAlertEvent> = _alerts.asSharedFlow()

    fun connect(channelName: String) {
        val cleanChannel = channelName.trim().lowercase().removePrefix("#")
        if (cleanChannel.isEmpty()) {
            disconnect()
            return
        }
        if (cleanChannel == currentChannel && _status.value == ChatConnectionStatus.CONNECTED) {
            return
        }
        disconnect()
        currentChannel = cleanChannel
        _status.value = ChatConnectionStatus.CONNECTING

        val request = Request.Builder()
            .url("wss://irc-ws.chat.twitch.tv:443")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                val anonNick = "justinfan${Random.nextInt(10000, 99999)}"
                ws.send("CAP REQ :twitch.tv/tags twitch.tv/commands")
                ws.send("PASS SCHMOOPIIE")
                ws.send("NICK $anonNick")
                ws.send("JOIN #$cleanChannel")
                _status.value = ChatConnectionStatus.CONNECTED
                scope.launch {
                    _messages.emit(
                        StreamChatMessage(
                            id = UUID.randomUUID().toString(),
                            platform = ChatPlatform.SYSTEM,
                            username = "mobileLabs IRC",
                            message = "Connected to #$cleanChannel live chat (read-only)",
                            nameColorHex = 0xFF31C3A2
                        )
                    )
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                text.split("\r\n").forEach { line ->
                    if (line.isBlank()) return@forEach
                    if (line.startsWith("PING")) {
                        ws.send(line.replaceFirst("PING", "PONG"))
                    } else {
                        parseIrcLine(line)
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _status.value = ChatConnectionStatus.ERROR
                scope.launch {
                    _messages.emit(
                        StreamChatMessage(
                            id = UUID.randomUUID().toString(),
                            platform = ChatPlatform.SYSTEM,
                            username = "Chat Error",
                            message = t.message ?: "Unable to connect to Twitch IRC",
                            nameColorHex = 0xFFFF4557
                        )
                    )
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _status.value = ChatConnectionStatus.DISCONNECTED
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnected")
        webSocket = null
        currentChannel = ""
        _status.value = ChatConnectionStatus.DISCONNECTED
    }

    private fun parseIrcLine(line: String) {
        // Format: @tags :user!user@user.tmi.twitch.tv PRIVMSG #channel :message
        var rest = line
        val tags = mutableMapOf<String, String>()
        if (rest.startsWith("@")) {
            val spaceIdx = rest.indexOf(' ')
            if (spaceIdx > 1) {
                val rawTags = rest.substring(1, spaceIdx)
                rawTags.split(';').forEach { pair ->
                    val eq = pair.indexOf('=')
                    if (eq > 0) {
                        tags[pair.substring(0, eq)] = pair.substring(eq + 1)
                    }
                }
                rest = rest.substring(spaceIdx + 1)
            }
        }

        if (rest.contains(" PRIVMSG ")) {
            val msgSplit = rest.split(" PRIVMSG ", limit = 2)
            val prefix = msgSplit[0]
            val trailingIdx = msgSplit[1].indexOf(" :")
            if (trailingIdx == -1) return
            val body = msgSplit[1].substring(trailingIdx + 2)
            val fallbackUser = prefix.removePrefix(":").substringBefore('!')
            val displayName = tags["display-name"]?.takeIf { it.isNotBlank() } ?: fallbackUser
            val colorHex = parseColorHex(tags["color"])
            val badges = tags["badges"].orEmpty()
            val badgeLabel = when {
                badges.contains("broadcaster") -> "HOST"
                badges.contains("moderator") -> "MOD"
                badges.contains("subscriber") -> "SUB"
                badges.contains("vip") -> "VIP"
                else -> null
            }

            scope.launch {
                _messages.emit(
                    StreamChatMessage(
                        id = tags["id"] ?: UUID.randomUUID().toString(),
                        platform = ChatPlatform.TWITCH,
                        username = displayName,
                        message = body,
                        nameColorHex = colorHex,
                        badge = badgeLabel
                    )
                )
            }
        } else if (rest.contains(" USERNOTICE ")) {
            val msgId = tags["msg-id"].orEmpty()
            val displayName = tags["display-name"]?.takeIf { it.isNotBlank() } ?: "Viewer"
            if (msgId == "sub" || msgId == "resub" || msgId == "subgift") {
                scope.launch {
                    _alerts.emit(
                        LiveAlertEvent(
                            id = System.currentTimeMillis(),
                            title = "$displayName subscribed!",
                            subtitle = tags["system-msg"]?.replace("\\s", " ") ?: "Welcome to the stream!",
                            kind = "SUB",
                            accentHex = 0xFF9146FF
                        )
                    )
                }
            } else if (msgId == "raid") {
                val viewers = tags["msg-param-viewerCount"] ?: "1"
                scope.launch {
                    _alerts.emit(
                        LiveAlertEvent(
                            id = System.currentTimeMillis(),
                            title = "$displayName raided with $viewers viewers!",
                            subtitle = "Incoming Twitch Raid",
                            kind = "RAID",
                            accentHex = 0xFF31C3A2
                        )
                    )
                }
            }
        }
    }

    private fun parseColorHex(raw: String?): Long {
        if (raw.isNullOrBlank() || !raw.startsWith("#")) return 0xFF31C3A2
        return runCatching {
            val rgb = raw.substring(1).toLong(16)
            0xFF000000L or rgb
        }.getOrDefault(0xFF31C3A2)
    }
}
