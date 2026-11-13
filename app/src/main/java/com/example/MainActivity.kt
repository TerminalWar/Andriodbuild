package com.example

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import com.example.streaming.MediaProjectionHolder
import com.example.ui.StudioScreen
import com.example.ui.theme.MobileLabsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            MobileLabsTheme {
                StudioScreen()
            }
        }
        updateAutoPipParams(false)
    }

    override fun onResume() {
        super.onResume()
        val app = application as? MobileLabsApplication
        val active = MediaProjectionHolder.isScreenCapturing.value ||
            app?.encoderPipeline?.isActivelyStreamingOrRecording() == true
        updateAutoPipParams(active)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            val app = application as? MobileLabsApplication
            val isCapturingOrStreaming = MediaProjectionHolder.isScreenCapturing.value ||
                app?.encoderPipeline?.isActivelyStreamingOrRecording() == true
            updateAutoPipParams(isCapturingOrStreaming)
            if (isCapturingOrStreaming && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                runCatching {
                    enterPictureInPictureMode(buildPipParams(autoEnter = false))
                }
            }
        }
    }

    private fun updateAutoPipParams(autoEnter: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            runCatching {
                setPictureInPictureParams(buildPipParams(autoEnter))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(autoEnter: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val rootView = window?.decorView
            if (rootView != null && rootView.width > 0 && rootView.height > 0) {
                val hintRect = Rect()
                rootView.getGlobalVisibleRect(hintRect)
                builder.setSourceRectHint(hintRect)
            }
            builder.setAutoEnterEnabled(autoEnter)
        }
        return builder.build()
    }
}
