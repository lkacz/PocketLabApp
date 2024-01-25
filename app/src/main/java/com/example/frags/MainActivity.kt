package com.example.frags

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader

class MainActivity : AppCompatActivity() {

    private val channelId = "ForegroundServiceChannel"
    private lateinit var fragmentLoader: FragmentLoader
    private lateinit var logger: Logger
    private lateinit var protocolManager: ProtocolManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        supportActionBar?.hide()

        Logger.resetInstance()
        logger = Logger.getInstance(this)

        protocolManager = ProtocolManager(this)
        protocolManager.readOriginalProtocol()

        val manipulatedProtocol: BufferedReader = protocolManager.getManipulatedProtocol()

        fragmentLoader = FragmentLoader(manipulatedProtocol, logger)

        createNotificationChannel()

        val serviceIntent = Intent(this, MyForegroundService::class.java)
        startForegroundService(serviceIntent)

        loadNextFragment()
    }

override fun onDestroy() {
        super.onDestroy()
        logger.backupLogFile()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)  // Removed conditional check
    }

    fun loadNextFragment() {
        val fragment = fragmentLoader.loadNextFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
