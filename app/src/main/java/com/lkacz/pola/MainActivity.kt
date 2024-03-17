package com.lkacz.pola

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import android.net.Uri

class MainActivity : AppCompatActivity(), StartFragment.OnProtocolSelectedListener {

    private val channelId = "ForegroundServiceChannel"
    private lateinit var fragmentLoader: FragmentLoader
    private var logger: Logger? = null // Make logger nullable
    private lateinit var protocolManager: ProtocolManager

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager(this).applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        supportActionBar?.hide()

        Logger.resetInstance()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, StartFragment())
                .commit()
        }

        createNotificationChannel()

        val serviceIntent = Intent(this, MyForegroundService::class.java)
        startForegroundService(serviceIntent)
    }

    override fun onProtocolSelected(protocolUri: Uri?) {
        protocolManager = ProtocolManager(this)
        protocolManager.readOriginalProtocol(protocolUri)
        val manipulatedProtocol: BufferedReader = protocolManager.getManipulatedProtocol()

        // Ensure logger is initialized here
        initializeLogger()
        logger?.setupLogger() // Setup logger with the current context and preferences

        // Now that logger is initialized and set up, create the fragmentLoader
        logger?.let {
            fragmentLoader = FragmentLoader(manipulatedProtocol, it)
            loadNextFragment()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        logger?.backupLogFile()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun loadNextFragment() {
        val fragment = fragmentLoader.loadNextFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun initializeLogger() {
        logger = Logger.getInstance(this)
    }
}