package com.lkacz.pola

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.transition.Visibility
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
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

        val fragmentContainer = androidx.fragment.app.FragmentContainerView(this).apply {
            id = fragmentContainerId
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
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
        var mode = TransitionManager.getTransitionMode(this)
        val currentFragment = supportFragmentManager.findFragmentById(fragmentContainerId)

        newFragment.apply {
            allowEnterTransitionOverlap = true
            allowReturnTransitionOverlap = true
        }

        when (mode) {
            "off" -> {
                // No transition animations.
            }
            "slide" -> {
                // Slide out to the left, slide in from the right.
                // Here we properly use int constants for mode (Visibility.MODE_OUT / MODE_IN).
                val slideOut = Slide().apply {
                    slideEdge = Gravity.LEFT
                    duration = 350
                    mode = Visibility.MODE_OUT.toString()
                }
                val slideIn = Slide().apply {
                    slideEdge = Gravity.RIGHT
                    duration = 350
                    mode = Visibility.MODE_IN.toString()
                }
                val transitionSet = TransitionSet().apply {
                    addTransition(slideOut)
                    addTransition(slideIn)
                    ordering = TransitionSet.ORDERING_TOGETHER
                }
                currentFragment?.exitTransition = transitionSet
                newFragment.enterTransition = transitionSet
            }
            "dissolve" -> {
                val dissolveOut = Fade().apply { duration = 350 }
                val dissolveIn = Fade().apply { duration = 350 }
                currentFragment?.exitTransition = dissolveOut
                newFragment.enterTransition = dissolveIn
            }
            "fade" -> {
                val fadeOut = Fade().apply { duration = 350 }
                val fadeIn = Fade().apply {
                    duration = 350
                    startDelay = 350
                }
                currentFragment?.exitTransition = fadeOut
                newFragment.enterTransition = fadeIn
            }
        }

        supportFragmentManager.beginTransaction().apply {
            setReorderingAllowed(true)
            replace(fragmentContainerId, newFragment)
            commit()
        }
    }

    fun loadFragmentByLabel(label: String) {
        val newFragment = fragmentLoader.jumpToLabelAndLoad(label)
        var mode = TransitionManager.getTransitionMode(this)
        val currentFragment = supportFragmentManager.findFragmentById(fragmentContainerId)

        newFragment.apply {
            allowEnterTransitionOverlap = true
            allowReturnTransitionOverlap = true
        }

        when (mode) {
            "off" -> {
                // No transition animations.
            }
            "slide" -> {
                // Slide out to the left, slide in from the right.
                val slideOut = Slide().apply {
                    slideEdge = Gravity.LEFT
                    duration = 350
                    mode = Visibility.MODE_OUT.toString()
                }
                val slideIn = Slide().apply {
                    slideEdge = Gravity.RIGHT
                    duration = 350
                    mode = Visibility.MODE_IN.toString()
                }
                val transitionSet = TransitionSet().apply {
                    addTransition(slideOut)
                    addTransition(slideIn)
                    ordering = TransitionSet.ORDERING_TOGETHER
                }
                currentFragment?.exitTransition = transitionSet
                newFragment.enterTransition = transitionSet
            }
            "dissolve" -> {
                val dissolveOut = Fade().apply { duration = 350 }
                val dissolveIn = Fade().apply { duration = 350 }
                currentFragment?.exitTransition = dissolveOut
                newFragment.enterTransition = dissolveIn
            }
            "fade" -> {
                val fadeOut = Fade().apply { duration = 350 }
                val fadeIn = Fade().apply {
                    duration = 350
                    startDelay = 350
                }
                currentFragment?.exitTransition = fadeOut
                newFragment.enterTransition = fadeIn
            }
        }

        supportFragmentManager.beginTransaction().apply {
            setReorderingAllowed(true)
            replace(fragmentContainerId, newFragment)
            commit()
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
