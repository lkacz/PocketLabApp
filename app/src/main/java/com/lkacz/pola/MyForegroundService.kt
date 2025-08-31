package com.lkacz.pola

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MyForegroundService : Service() {
    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val NOTIFICATION_ID = 1

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("PoLA Foreground Service")
                .setContentText("PoLA service is running...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

        startForeground(NOTIFICATION_ID, notification)

        // If you want the service to continue running until explicitly stopped
        return START_STICKY
    }
}
