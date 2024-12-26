package com.lkacz.pola.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lkacz.pola.Logger
import com.lkacz.pola.MyForegroundService
import com.lkacz.pola.ProtocolManager
import com.lkacz.pola.ThemeManager
import com.lkacz.pola.ui.screens.AppNavigation

/**
 * MainActivity revised to use Jetpack Compose.
 * Key changes:
 * - Removed XML-based setContentView and replaced it with setContent { ... } for composables.
 * - Retained the original logic for the foreground service and logger usage.
 * - Manages theming via [ThemeManager].
 */
class MainActivity : ComponentActivity() {

    private val channelId = "ForegroundServiceChannel"
    lateinit var logger: Logger
    lateinit var protocolManager: ProtocolManager
    lateinit var themeManager: ThemeManager

    // State of selected protocol Uri in Compose-based screens.
    var protocolUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme early
        themeManager = ThemeManager(this)
        themeManager.applyTheme()

        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Reinitialize logger for each session
        Logger.resetInstance()
        logger = Logger.getInstance(this)

        createNotificationChannel()
        startForegroundService(Intent(this, MyForegroundService::class.java))

        setContent {
            AppNavigation(
                mainActivity = this,
                logger = logger
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.backupLogFile()
    }

    /**
     * Creates the notification channel for the foreground service.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
