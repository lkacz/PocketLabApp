// Filename: MainActivity.kt
package com.lkacz.pola

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.transition.Fade
import android.os.StrictMode
// BuildConfig is generated in the same package; no explicit import needed
import timber.log.Timber
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), StartFragment.OnProtocolSelectedListener {
    private val channelId = "ForegroundServiceChannel"
    private lateinit var fragmentLoader: FragmentLoader
    private lateinit var logger: Logger
    private lateinit var protocolManager: ProtocolManager
    private lateinit var sharedPref: SharedPreferences
    private var pendingResumeIndex: Int? = null

    private val fragmentContainerId = ViewGroup.generateViewId()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        supportActionBar?.hide()

        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            // StrictMode policies to surface accidental disk/network on main thread and leaks
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
            if (Timber.forest().isEmpty()) {
                Timber.plant(Timber.DebugTree())
            }
            Timber.d("MainActivity onCreate - debug tools initialized")
        }

        Logger.resetInstance()
        logger = Logger.getInstance(this)
        sharedPref = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)

        val fragmentContainer =
            androidx.fragment.app.FragmentContainerView(this).apply {
                id = fragmentContainerId
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
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

        if (savedInstanceState == null) {
            maybeShowResumeProtocolPrompt(fragmentContainer)
        }
    }

    override fun onProtocolSelected(protocolUri: Uri?) {
        protocolManager = ProtocolManager(this)
        protocolManager.readOriginalProtocol(protocolUri)
        val manipulatedProtocol = protocolManager.getManipulatedProtocol()
        fragmentLoader = FragmentLoader(manipulatedProtocol, logger)
        val resumeIndex = pendingResumeIndex
        if (resumeIndex != null) {
            fragmentLoader.prepareForResume(resumeIndex)
        } else {
            clearProtocolProgress()
        }
        pendingResumeIndex = null
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

        newFragment.apply {
            allowEnterTransitionOverlap = true
            allowReturnTransitionOverlap = true
        }

        handleProgressFor(newFragment)

        when (mode) {
            "off" -> {
                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    replace(fragmentContainerId, newFragment)
                    commit()
                }
            }

            "slide" -> {
                currentFragment?.view?.startAnimation(SlideTransitionHelper.outLeftAnimation(350L))
                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    replace(fragmentContainerId, newFragment)
                    commit()
                }
                newFragment.lifecycleScope.launch {
                    delay(10)
                    newFragment.view?.startAnimation(SlideTransitionHelper.inRightAnimation(350L))
                }
            }

            "slideLeft" -> {
                currentFragment?.view?.startAnimation(SlideTransitionHelper.outRightAnimation(350L))
                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    replace(fragmentContainerId, newFragment)
                    commit()
                }
                newFragment.lifecycleScope.launch {
                    delay(10)
                    newFragment.view?.startAnimation(SlideTransitionHelper.inLeftAnimation(350L))
                }
            }

            "dissolve" -> {
                val dissolveOut = Fade().apply { duration = 350 }
                val dissolveIn = Fade().apply { duration = 350 }
                currentFragment?.exitTransition = dissolveOut
                newFragment.enterTransition = dissolveIn

                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    replace(fragmentContainerId, newFragment)
                    commit()
                }
            }

            "fade" -> {
                val fadeOut =
                    Fade().apply {
                        duration = 350
                    }
                val fadeIn =
                    Fade().apply {
                        duration = 350
                        startDelay = 350
                    }
                currentFragment?.exitTransition = fadeOut
                newFragment.enterTransition = fadeIn

                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    replace(fragmentContainerId, newFragment)
                    commit()
                }
            }

            else -> {
                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    replace(fragmentContainerId, newFragment)
                    commit()
                }
            }
        }
    }

    fun loadFragmentByLabel(label: String) {
        val newFragment = fragmentLoader.jumpToLabelAndLoad(label)
        val mode = TransitionManager.getTransitionMode(this)
        val currentFragment = supportFragmentManager.findFragmentById(fragmentContainerId)

        newFragment.apply {
            allowEnterTransitionOverlap = true
            allowReturnTransitionOverlap = true
        }

        handleProgressFor(newFragment)

        when (mode) {
            "off" -> {
                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    replace(fragmentContainerId, newFragment)
                    commit()
                }
            }

            "slide" -> {
                currentFragment?.view?.startAnimation(SlideTransitionHelper.outLeftAnimation(350L))
                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    replace(fragmentContainerId, newFragment)
                    commit()
                }
                newFragment.lifecycleScope.launch {
                    delay(10)
                    newFragment.view?.startAnimation(SlideTransitionHelper.inRightAnimation(350L))
                }
            }

            "slideLeft" -> {
                currentFragment?.view?.startAnimation(SlideTransitionHelper.outRightAnimation(350L))
                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    replace(fragmentContainerId, newFragment)
                    commit()
                }
                newFragment.lifecycleScope.launch {
                    delay(10)
                    newFragment.view?.startAnimation(SlideTransitionHelper.inLeftAnimation(350L))
                }
            }

            "dissolve" -> {
                val dissolveOut = Fade().apply { duration = 350 }
                val dissolveIn = Fade().apply { duration = 350 }
                currentFragment?.exitTransition = dissolveOut
                newFragment.enterTransition = dissolveIn

                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    replace(fragmentContainerId, newFragment)
                    commit()
                }
            }

            "fade" -> {
                val fadeOut =
                    Fade().apply {
                        duration = 350
                    }
                val fadeIn =
                    Fade().apply {
                        duration = 350
                        startDelay = 350
                    }
                currentFragment?.exitTransition = fadeOut
                newFragment.enterTransition = fadeIn

                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    replace(fragmentContainerId, newFragment)
                    commit()
                }
            }

            else -> {
                supportFragmentManager.beginTransaction().apply {
                    setReorderingAllowed(true)
                    replace(fragmentContainerId, newFragment)
                    commit()
                }
            }
        }
    }

    private fun maybeShowResumeProtocolPrompt(container: View) {
        val storedUri = sharedPref.getString(Prefs.KEY_PROTOCOL_URI, null)
        val storedIndex = sharedPref.getInt(Prefs.KEY_PROTOCOL_PROGRESS_INDEX, -1)
        val wasInProgress = sharedPref.getBoolean(Prefs.KEY_PROTOCOL_IN_PROGRESS, false)
        val shouldPrompt = !storedUri.isNullOrBlank() && storedIndex >= 0 && wasInProgress
        if (shouldPrompt) {
            container.post { showResumeProtocolDialog(storedIndex) }
        }
    }

    private fun showResumeProtocolDialog(resumeIndex: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_resume_protocol_title))
            .setMessage(getString(R.string.dialog_resume_protocol_message))
            .setPositiveButton(R.string.dialog_resume_protocol_continue) { _, _ ->
                resumeLastProtocolIfAvailable(resumeIndex)
            }
            .setNegativeButton(R.string.dialog_resume_protocol_start_over) { _, _ ->
                clearProtocolProgress()
            }
            .setCancelable(false)
            .show()
    }

    private fun handleProgressFor(fragment: Fragment) {
        if (!::fragmentLoader.isInitialized) return
        when (fragment) {
            is EndFragment -> clearProtocolProgress()
            else -> {
                val index = fragmentLoader.getCurrentCommandIndex()
                if (index >= 0) {
                    sharedPref.edit()
                        .putInt(Prefs.KEY_PROTOCOL_PROGRESS_INDEX, index)
                        .putBoolean(Prefs.KEY_PROTOCOL_IN_PROGRESS, true)
                        .apply()
                }
            }
        }
    }

    private fun clearProtocolProgress() {
        sharedPref.edit()
            .remove(Prefs.KEY_PROTOCOL_PROGRESS_INDEX)
            .putBoolean(Prefs.KEY_PROTOCOL_IN_PROGRESS, false)
            .apply()
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                channelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun resumeLastProtocolIfAvailable(resumeIndex: Int) {
        val uriString = sharedPref.getString(Prefs.KEY_PROTOCOL_URI, null) ?: return
        val parsedUri = runCatching { Uri.parse(uriString) }.getOrNull()
        if (parsedUri != null) {
            pendingResumeIndex = resumeIndex
            onProtocolSelected(parsedUri)
        } else {
            clearProtocolProgress()
        }
    }
}
