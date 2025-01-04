// Filename: MainActivity.kt

package com.lkacz.pola

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.Fade
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit

class MainActivity : AppCompatActivity(), StartFragment.OnProtocolSelectedListener {

    private val channelId = "ForegroundServiceChannel"
    private lateinit var fragmentLoader: FragmentLoader
    private lateinit var logger: Logger
    private lateinit var protocolManager: ProtocolManager

    private val fragmentContainerId = ViewGroup.generateViewId()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        supportActionBar?.hide()

        Logger.resetInstance()
        logger = Logger.getInstance(this)

        val fragmentContainer = FrameLayout(this).apply {
            id = fragmentContainerId
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(fragmentContainer)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(fragmentContainerId, StartFragment())
            }
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

    fun loadNextFragment() {
        val newFragment = fragmentLoader.loadNextFragment()
        val mode = TransitionManager.getTransitionMode(this)
        val currentFragment = supportFragmentManager.findFragmentById(fragmentContainerId)

        when (mode) {
            "off" -> {
                // No transition animations
            }
            "slide" -> {
                // Slide logic remains as is or can be handled via fragment animations
                // Example: setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                // or any existing logic you already have for sliding transitions
            }
            "dissolve" -> {
                val dissolveIn = Fade().apply { duration = 350 }
                val dissolveOut = Fade().apply { duration = 350 }
                newFragment.enterTransition = dissolveIn
                currentFragment?.exitTransition = dissolveOut
            }
            "fade" -> {
                // Old fragment fully fades out first, then the new fragment fades in
                val fadeOut = Fade().apply { duration = 350 }
                val fadeIn = Fade().apply { duration = 350; startDelay = 350 }
                newFragment.enterTransition = fadeIn
                currentFragment?.exitTransition = fadeOut
            }
        }

        supportFragmentManager.commit {
            replace(fragmentContainerId, newFragment)
        }
    }

    fun loadFragmentByLabel(label: String) {
        val newFragment = fragmentLoader.jumpToLabelAndLoad(label)
        val mode = TransitionManager.getTransitionMode(this)
        val currentFragment = supportFragmentManager.findFragmentById(fragmentContainerId)

        when (mode) {
            "off" -> {
                // No transition animations
            }
            "slide" -> {
                // Slide logic remains as is or can be handled via fragment animations
            }
            "dissolve" -> {
                val dissolveIn = Fade().apply { duration = 350 }
                val dissolveOut = Fade().apply { duration = 350 }
                newFragment.enterTransition = dissolveIn
                currentFragment?.exitTransition = dissolveOut
            }
            "fade" -> {
                val fadeOut = Fade().apply { duration = 350 }
                val fadeIn = Fade().apply { duration = 350; startDelay = 350 }
                newFragment.enterTransition = fadeIn
                currentFragment?.exitTransition = fadeOut
            }
        }

        supportFragmentManager.commit {
            replace(fragmentContainerId, newFragment)
        }
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
