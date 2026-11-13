package com.example.streaming

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MobileLabsApplication
import com.example.R

class StreamBroadcastService : Service() {

    companion object {
        const val CHANNEL_ID = "mobilelabs_broadcast_channel"
        const val NOTIFICATION_ID = 4102

        const val ACTION_START_PROJECTION = "com.example.action.START_PROJECTION"
        const val ACTION_UPDATE_STATUS = "com.example.action.UPDATE_STATUS"
        const val ACTION_STOP_ALL = "com.example.action.STOP_ALL"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_STATUS_TEXT = "extra_status_text"
        const val EXTRA_INCLUDE_MEDIA_PROJECTION = "extra_include_media_projection"

        fun startForegroundStudio(context: Context, statusText: String) {
            val intent = Intent(context, StreamBroadcastService::class.java).apply {
                action = ACTION_UPDATE_STATUS
                putExtra(EXTRA_STATUS_TEXT, statusText)
                putExtra(EXTRA_INCLUDE_MEDIA_PROJECTION, MediaProjectionHolder.isScreenCapturing.value)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startScreenProjection(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, StreamBroadcastService::class.java).apply {
                action = ACTION_START_PROJECTION
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
                putExtra(EXTRA_INCLUDE_MEDIA_PROJECTION, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, StreamBroadcastService::class.java))
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mobileLabs::BroadcastWakeLock").apply {
            acquire(6 * 60 * 60 * 1000L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ALL -> {
                MediaProjectionHolder.release()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START_PROJECTION -> {
                promoteForeground("Screen Capture & Studio Active", includeMediaProjection = true)
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                @Suppress("DEPRECATION")
                val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    val projection = mpm.getMediaProjection(resultCode, data)
                    if (projection != null) {
                        val metrics = resources.displayMetrics
                        MediaProjectionHolder.attachProjection(
                            projection = projection,
                            width = metrics.widthPixels,
                            height = metrics.heightPixels,
                            densityDpi = metrics.densityDpi
                        )
                        val app = application as MobileLabsApplication
                        app.audioMixerEngine.startCapture(hasMicPermission = true, mediaProjection = projection)
                    }
                }
            }

            else -> {
                val status = intent?.getStringExtra(EXTRA_STATUS_TEXT) ?: "Broadcast Studio Active"
                val withProj = intent?.getBooleanExtra(EXTRA_INCLUDE_MEDIA_PROJECTION, false) == true
                promoteForeground(status, withProj)
            }
        }
        return START_STICKY
    }

    private fun promoteForeground(statusText: String, includeMediaProjection: Boolean) {
        val notification = buildNotification(statusText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            if (includeMediaProjection) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, StreamBroadcastService::class.java).apply { action = ACTION_STOP_ALL },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("mobileLabs Studio")
            .setContentText(statusText)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, "Stop Capture", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_stream),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_stream_desc)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        MediaProjectionHolder.release()
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        super.onDestroy()
    }
}
