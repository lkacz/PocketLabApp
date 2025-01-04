// Filename: TimerFragment.kt
package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.*
import androidx.core.view.setPadding
import androidx.documentfile.provider.DocumentFile

class TimerFragment : BaseTouchAwareFragment(5000, 20) {

    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null
    private var timeInSeconds: Int? = null

    private lateinit var alarmHelper: AlarmHelper
    private lateinit var logger: Logger
    private var timer: CountDownTimer? = null

    private val mediaPlayers = mutableListOf<MediaPlayer>()

    // Views created programmatically
    private lateinit var headerTextView: TextView
    private lateinit var webView: WebView
    private lateinit var bodyTextView: TextView
    private lateinit var videoView: VideoView
    private lateinit var timerTextView: TextView
    private lateinit var nextButton: Button

    // [TAP] logic
    private var tapEnabled = false
    private var tapCount = 0
    private val tapThreshold = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString("HEADER")
            body = it.getString("BODY")
            nextButtonText = it.getString("NEXT_BUTTON_TEXT")
            timeInSeconds = it.getInt("TIME_IN_SECONDS")
        }
        if (timeInSeconds == null || timeInSeconds!! < 0) {
            timeInSeconds = 0
        }
        alarmHelper = AlarmHelper(requireContext())
        logger = Logger.getInstance(requireContext())
        logger.logTimerFragment(header ?: "Default Header", body ?: "Default Body", timeInSeconds ?: 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            keepScreenOn = true
        }

        headerTextView = TextView(requireContext()).apply {
            text = "Default Header"
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
        }
        rootLayout.addView(headerTextView)

        webView = WebView(requireContext()).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
        }
        rootLayout.addView(webView)

        bodyTextView = TextView(requireContext()).apply {
            text = "Default Body"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
        }
        rootLayout.addView(bodyTextView)

        videoView = VideoView(requireContext()).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(32)
            }
        }
        rootLayout.addView(videoView)

        timerTextView = TextView(requireContext()).apply {
            text = "Time remaining: XX seconds"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            // Center text within the TextView itself
            gravity = Gravity.CENTER
        }
        rootLayout.addView(timerTextView)

        nextButton = Button(requireContext()).apply {
            text = "Next"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
            }
        }
        rootLayout.addView(nextButton)

        return rootLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(ColorManager.getScreenBackgroundColor(requireContext()))
        setupWebView()

        val resourcesFolderUri = ResourcesFolderManager(requireContext()).getResourcesFolderUri()

        val cleanHeader = parseAndPlayAudioIfAny(header.orEmpty(), resourcesFolderUri)
        val refinedHeader = checkAndLoadHtml(cleanHeader, resourcesFolderUri)
        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), resourcesFolderUri)
        val refinedBody = checkAndLoadHtml(cleanBody, resourcesFolderUri)
        val (cleanNextText, isTap) = parseTapAttribute(
            parseAndPlayAudioIfAny(nextButtonText.orEmpty(), resourcesFolderUri)
        )
        tapEnabled = isTap
        val refinedNextText = checkAndLoadHtml(cleanNextText, resourcesFolderUri)

        checkAndPlayMp4(header.orEmpty(), resourcesFolderUri)
        checkAndPlayMp4(body.orEmpty(), resourcesFolderUri)
        checkAndPlayMp4(nextButtonText.orEmpty(), resourcesFolderUri)

        headerTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedHeader)
        headerTextView.textSize = FontSizeManager.getHeaderSize(requireContext())
        headerTextView.setTextColor(ColorManager.getHeaderTextColor(requireContext()))
        applyHeaderAlignment(headerTextView)

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())
        bodyTextView.setTextColor(ColorManager.getBodyTextColor(requireContext()))
        applyBodyAlignment(bodyTextView)

        nextButton.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedNextText)
        nextButton.textSize = FontSizeManager.getContinueSize(requireContext())
        nextButton.setTextColor(ColorManager.getContinueTextColor(requireContext()))
        nextButton.setBackgroundColor(ColorManager.getContinueBackgroundColor(requireContext()))
        applyContinueButtonPadding(nextButton)
        applyContinueAlignment(nextButton)

        // Define what a "click" should do (performClick will end up here):
        view.setOnClickListener {
            if (tapEnabled) {
                tapCount++
                if (tapCount >= tapThreshold) {
                    nextButton.visibility = View.VISIBLE
                    tapCount = 0
                }
            }
        }

        // Attach a touch listener that calls performClick() on ACTION_DOWN
        if (tapEnabled) {
            nextButton.visibility = View.INVISIBLE
            view.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    // Call performClick to handle accessibility
                    v.performClick()
                    // We return true to indicate we've consumed the event
                    true
                } else {
                    false
                }
            }
        }

        val totalTimeMillis = (timeInSeconds ?: 0) * 1000L
        timer = object : CountDownTimer(totalTimeMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val minutesLeft = millisUntilFinished / 60000
                val secondsLeft = (millisUntilFinished % 60000) / 1000
                timerTextView.text = String.format("%02d:%02d", minutesLeft, secondsLeft)
            }

            override fun onFinish() {
                timerTextView.text = "Continue."
                nextButton.visibility = View.VISIBLE
                alarmHelper.startAlarm()
                logger.logTimerFragment(header ?: "Default Header", "Timer Finished", timeInSeconds ?: 0)
            }
        }.start()

        nextButton.setOnClickListener {
            alarmHelper.stopAlarm()
            (activity as MainActivity).loadNextFragment()
            logger.logTimerFragment(header ?: "Default Header", "Next Button Clicked", 0)
        }
    }

    // Optionally override performClick on the fragment’s root view if you want custom logic
    // associated specifically with performClick. Usually, setting an OnClickListener is enough.

    override fun onTouchThresholdReached() {
        timer?.cancel()
        logger.logTimerFragment(header ?: "Default Header", "Timer forcibly ended by user", timeInSeconds ?: 0)
        timerTextView.text = "Continue."
        nextButton.visibility = View.VISIBLE
        alarmHelper.startAlarm()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()
        if (this::videoView.isInitialized && videoView.isPlaying) {
            videoView.stopPlayback()
        }
        webView.destroy()
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmHelper.release()
        logger.logTimerFragment(header ?: "Default Header", "Destroyed", timeInSeconds ?: 0)
    }

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
    }

    private fun parseAndPlayAudioIfAny(text: String, resourcesFolderUri: Uri?): String {
        return AudioPlaybackHelper.parseAndPlayAudio(
            context = requireContext(),
            rawText = text,
            mediaFolderUri = resourcesFolderUri,
            mediaPlayers = mediaPlayers
        )
    }

    private fun checkAndPlayMp4(text: String, resourcesFolderUri: Uri?) {
        val pattern = Regex("<([^>]+\\.mp4(?:,[^>]+)?)>", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return
        val group = match.groupValues[1]
        val segments = group.split(",")
        val fileName = segments[0].trim()
        val volume = if (segments.size > 1) {
            segments[1].trim().toFloatOrNull()?.coerceIn(0f, 100f)?.div(100f) ?: 1.0f
        } else 1.0f

        if (resourcesFolderUri != null) {
            val parentFolder = DocumentFile.fromTreeUri(requireContext(), resourcesFolderUri) ?: return
            val videoFile = parentFolder.findFile(fileName)
            if (videoFile != null && videoFile.exists() && videoFile.isFile) {
                videoView.visibility = View.VISIBLE
                videoView.setVideoURI(videoFile.uri)
                videoView.setOnPreparedListener { mp ->
                    mp.start()
                    mp.setVolume(volume, volume)
                }
            }
        }
    }

    private fun checkAndLoadHtml(text: String, resourcesFolderUri: Uri?): String {
        if (text.isBlank() || resourcesFolderUri == null) return text
        val pattern = Regex("<([^>]+\\.html)>", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return text
        val matchedFull = match.value
        val fileName = match.groupValues[1].trim()

        val parentFolder = DocumentFile.fromTreeUri(requireContext(), resourcesFolderUri) ?: return text
        val htmlFile = parentFolder.findFile(fileName)
        if (htmlFile != null && htmlFile.exists() && htmlFile.isFile) {
            try {
                requireContext().contentResolver.openInputStream(htmlFile.uri)?.use { inputStream ->
                    val htmlContent = inputStream.bufferedReader().readText()
                    webView.visibility = View.VISIBLE
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return text.replace(matchedFull, "")
    }

    private fun applyHeaderAlignment(textView: TextView) {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        when (prefs.getString("HEADER_ALIGNMENT", "CENTER")?.uppercase()) {
            "LEFT" -> textView.gravity = Gravity.START
            "RIGHT" -> textView.gravity = Gravity.END
            else -> textView.gravity = Gravity.CENTER
        }
    }

    private fun applyBodyAlignment(textView: TextView) {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        when (prefs.getString("BODY_ALIGNMENT", "CENTER")?.uppercase()) {
            "LEFT" -> textView.gravity = Gravity.START
            "RIGHT" -> textView.gravity = Gravity.END
            else -> textView.gravity = Gravity.CENTER
        }
    }

    private fun applyContinueAlignment(button: Button) {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        when (prefs.getString("CONTINUE_ALIGNMENT", "CENTER")?.uppercase()) {
            "LEFT" -> (button.layoutParams as? LinearLayout.LayoutParams)?.gravity = Gravity.START
            "RIGHT" -> (button.layoutParams as? LinearLayout.LayoutParams)?.gravity = Gravity.END
            else -> (button.layoutParams as? LinearLayout.LayoutParams)?.gravity = Gravity.CENTER_HORIZONTAL
        }
    }

    private fun applyContinueButtonPadding(button: Button) {
        val density = resources.displayMetrics.density
        val ch = SpacingManager.getContinueButtonPaddingHorizontal(requireContext())
        val cv = SpacingManager.getContinueButtonPaddingVertical(requireContext())
        val chPx = (ch * density + 0.5f).toInt()
        val cvPx = (cv * density + 0.5f).toInt()
        button.setPadding(chPx, cvPx, chPx, cvPx)
    }

    private fun parseTapAttribute(text: String): Pair<String, Boolean> {
        val regex = Regex("\\[TAP\\]", RegexOption.IGNORE_CASE)
        return if (regex.containsMatchIn(text)) {
            val newText = text.replace(regex, "").trim()
            newText to true
        } else {
            text to false
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    companion object {
        @JvmStatic
        fun newInstance(
            header: String?,
            body: String?,
            nextButtonText: String?,
            timeInSeconds: Int?
        ) = TimerFragment().apply {
            arguments = Bundle().apply {
                putString("HEADER", header)
                putString("BODY", body)
                putString("NEXT_BUTTON_TEXT", nextButtonText)
                putInt("TIME_IN_SECONDS", timeInSeconds ?: 0)
            }
        }
    }
}
