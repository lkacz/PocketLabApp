package com.lkacz.pola

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * Updated to allow fragments to jump to labels (e.g., from BranchScaleFragment).
 */
class MainActivity : AppCompatActivity(), StartFragment.OnProtocolSelectedListener {

    private val channelId = "ForegroundServiceChannel"
    private lateinit var fragmentLoader: FragmentLoader
    private lateinit var logger: Logger
    private lateinit var protocolManager: ProtocolManager

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager(this).applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        supportActionBar?.hide()

        Logger.resetInstance()
        logger = Logger.getInstance(this)

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
        val manipulatedProtocol = protocolManager.getManipulatedProtocol()

        fragmentLoader = FragmentLoader(manipulatedProtocol, logger)
        loadNextFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.backupLogFile()
    }

    /**
     * Loads the next instruction-based fragment.
     */
    fun loadNextFragment() {
        val fragment = fragmentLoader.loadNextFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * Public method that a BranchScaleFragment can call to jump to a label.
     */
    fun loadFragmentByLabel(label: String) {
        val fragment = fragmentLoader.jumpToLabelAndLoad(label)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
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
}
