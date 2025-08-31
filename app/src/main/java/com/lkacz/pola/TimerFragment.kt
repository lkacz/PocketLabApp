// Filename: TimerFragment.kt
package com.lkacz.pola

import android.content.Context
import android.graphics.Typeface
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
    private var timeInSeconds: Int? = null
    private var nextButtonText: String? = null

    private lateinit var alarmHelper: AlarmHelper
    private lateinit var logger: Logger
    private var timer: CountDownTimer? = null
    private val mediaPlayers = mutableListOf<MediaPlayer>()

    private lateinit var headerTextView: TextView
    private lateinit var webView: WebView
    private lateinit var bodyTextView: TextView
    private lateinit var videoView: VideoView
    private lateinit var timerTextView: TextView
    private lateinit var nextButton: Button

    private var holdEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString("HEADER")
            body = it.getString("BODY")
            timeInSeconds = it.getInt("TIME_IN_SECONDS")
            nextButtonText = it.getString("NEXT_BUTTON_TEXT")
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
        savedInstanceState: Bundle?,
    ): View {
        val rootFrame =
            FrameLayout(requireContext()).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                keepScreenOn = true
            }

        val scrollView =
            ScrollView(requireContext()).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                isFillViewport = true
            }
        val contentLayout =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16))
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        scrollView.addView(contentLayout)
        rootFrame.addView(scrollView)

        headerTextView =
            TextView(requireContext()).apply {
                text = "Default Header"
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin = dpToPx(16)
                    }
            }
        contentLayout.addView(headerTextView)

        webView =
            WebView(requireContext()).apply {
                visibility = View.GONE
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin = dpToPx(16)
                    }
            }
        contentLayout.addView(webView)

        bodyTextView =
            TextView(requireContext()).apply {
                text = "Default Body"
                textSize = 16f
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin = dpToPx(16)
                    }
            }
        contentLayout.addView(bodyTextView)

        videoView =
            VideoView(requireContext()).apply {
                visibility = View.GONE
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin = dpToPx(32)
                    }
            }
        contentLayout.addView(videoView)

        timerTextView =
            TextView(requireContext()).apply {
                text = ""
                textSize = 18f
                gravity = Gravity.CENTER
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        bottomMargin = dpToPx(16)
                    }
            }
        contentLayout.addView(timerTextView)

        nextButton =
            Button(requireContext()).apply {
                text = "Next"
            }
        val buttonParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END,
            )
        nextButton.layoutParams = buttonParams
        rootFrame.addView(nextButton)

        return rootFrame
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(ColorManager.getScreenBackgroundColor(requireContext()))
        setupWebView()

        val resourcesFolderUri = ResourcesFolderManager(requireContext()).getResourcesFolderUri()

        val cleanHeader = parseAndPlayAudioIfAny(header.orEmpty(), resourcesFolderUri)
        val refinedHeader = checkAndLoadHtml(cleanHeader, resourcesFolderUri)
        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), resourcesFolderUri)
        val refinedBody = checkAndLoadHtml(cleanBody, resourcesFolderUri)
        val (buttonTextNoHold, isHold) =
            parseHoldAttribute(
                parseAndPlayAudioIfAny(nextButtonText.orEmpty(), resourcesFolderUri),
            )
        holdEnabled = isHold
        val refinedNextText = checkAndLoadHtml(buttonTextNoHold, resourcesFolderUri)

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

        val timerSize = FontSizeManager.getTimerSize(requireContext())
        val timerColor = ColorManager.getTimerTextColor(requireContext())
        timerTextView.textSize = timerSize
        timerTextView.setTextColor(timerColor)
        applyTimerAlignment(timerTextView)
        applyTimerTextPadding(timerTextView)

        nextButton.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedNextText)
        nextButton.textSize = FontSizeManager.getContinueSize(requireContext())
        nextButton.setTextColor(ColorManager.getContinueTextColor(requireContext()))
        nextButton.setBackgroundColor(ColorManager.getContinueBackgroundColor(requireContext()))
        applyContinueButtonPadding(nextButton)
        applyContinueAlignment(nextButton)

        if ((timeInSeconds ?: 0) > 0) {
            nextButton.visibility = View.INVISIBLE
        }

        val totalTimeMillis = (timeInSeconds ?: 0) * 1000L
        timer =
            object : CountDownTimer(totalTimeMillis, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    val remainingSeconds = millisUntilFinished / 1000
                    if (remainingSeconds >= 3600) {
                        val hoursLeft = remainingSeconds / 3600
                        val minutesLeft = (remainingSeconds % 3600) / 60
                        val secondsLeft = remainingSeconds % 60
                        timerTextView.text = String.format("%02d:%02d:%02d", hoursLeft, minutesLeft, secondsLeft)
                    } else {
                        val minutesLeft = remainingSeconds / 60
                        val secondsLeft = remainingSeconds % 60
                        timerTextView.text = String.format("%02d:%02d", minutesLeft, secondsLeft)
                    }
                }

                override fun onFinish() {
                    nextButton.visibility = View.VISIBLE
                    alarmHelper.startAlarm()
                    logger.logTimerFragment(header ?: "Default Header", "Timer Finished", timeInSeconds ?: 0)
                }
            }

        if (timeInSeconds ?: 0 > 0) {
            timer?.start()
        } else {
            nextButton.visibility = View.VISIBLE
        }

        if (holdEnabled) {
            HoldButtonHelper.setupHoldToConfirm(nextButton) {
                alarmHelper.stopAlarm()
                (activity as MainActivity).loadNextFragment()
                logger.logTimerFragment(header ?: "Default Header", "Next Button Clicked", 0)
            }
        } else {
            nextButton.setOnClickListener {
                alarmHelper.stopAlarm()
                (activity as MainActivity).loadNextFragment()
                logger.logTimerFragment(header ?: "Default Header", "Next Button Clicked", 0)
            }
        }
    }

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

    private fun parseAndPlayAudioIfAny(
        text: String,
        resourcesFolderUri: Uri?,
    ): String {
        return AudioPlaybackHelper.parseAndPlayAudio(
            context = requireContext(),
            rawText = text,
            mediaFolderUri = resourcesFolderUri,
            mediaPlayers = mediaPlayers,
        )
    }

    private fun checkAndPlayMp4(
        text: String,
        resourcesFolderUri: Uri?,
    ) {
        val pattern = Regex("<([^>]+\\.mp4(?:,[^>]+)?)>", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return
        val group = match.groupValues[1]
        val segments = group.split(",")
        val fileName = segments[0].trim()
        val volume =
            if (segments.size > 1) {
                segments[1].trim().toFloatOrNull()?.coerceIn(0f, 100f)?.div(100f) ?: 1.0f
            } else {
                1.0f
            }

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

    private fun checkAndLoadHtml(
        text: String,
        resourcesFolderUri: Uri?,
    ): String {
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
            } catch (_: Exception) {
            }
        }
        return text.replace(matchedFull, "")
    }

    private fun applyHeaderAlignment(textView: TextView) {
    val prefs = requireContext().getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
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

    private fun applyTimerAlignment(textView: TextView) {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        when (prefs.getString("TIMER_ALIGNMENT", "CENTER")?.uppercase()) {
            "LEFT" -> textView.gravity = Gravity.START
            "RIGHT" -> textView.gravity = Gravity.END
            else -> textView.gravity = Gravity.CENTER
        }
    }

    private fun applyTimerTextPadding(textView: TextView) {
        val density = resources.displayMetrics.density
        val horizontalPadding = SpacingManager.getTimerPaddingHorizontal(requireContext())
        val verticalPadding = SpacingManager.getTimerPaddingVertical(requireContext())
        val hPx = (horizontalPadding * density + 0.5f).toInt()
        val vPx = (verticalPadding * density + 0.5f).toInt()
        textView.setPadding(hPx, vPx, hPx, vPx)
    }

    private fun applyContinueAlignment(button: Button) {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        val horiz = prefs.getString("CONTINUE_ALIGNMENT_HORIZONTAL", "RIGHT")?.uppercase()
        val vert = prefs.getString("CONTINUE_ALIGNMENT_VERTICAL", "BOTTOM")?.uppercase()

        val lp = button.layoutParams as? FrameLayout.LayoutParams ?: return

        val hGravity =
            when (horiz) {
                "LEFT" -> Gravity.START
                "CENTER" -> Gravity.CENTER_HORIZONTAL
                else -> Gravity.END
            }
        val vGravity =
            when (vert) {
                "TOP" -> Gravity.TOP
                else -> Gravity.BOTTOM
            }
        lp.gravity = hGravity or vGravity

        val density = resources.displayMetrics.density
        val marginPx = (32 * density + 0.5f).toInt()
        lp.setMargins(marginPx, marginPx, marginPx, marginPx)

        button.layoutParams = lp
    }

    private fun applyContinueButtonPadding(button: Button) {
        val density = resources.displayMetrics.density
        val ch = SpacingManager.getContinueButtonPaddingHorizontal(requireContext())
        val cv = SpacingManager.getContinueButtonPaddingVertical(requireContext())
        val chPx = (ch * density + 0.5f).toInt()
        val cvPx = (cv * density + 0.5f).toInt()
        button.setPadding(chPx, cvPx, chPx, cvPx)
    }

    private fun parseHoldAttribute(text: String): Pair<String, Boolean> {
        val regex = Regex("\\[HOLD\\]", RegexOption.IGNORE_CASE)
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
        /**
         * Revised order: header, body, timeInSeconds, nextButtonText
         */
        @JvmStatic
        fun newInstance(
            header: String?,
            body: String?,
            timeInSeconds: Int?,
            nextButtonText: String?,
        ) = TimerFragment().apply {
            arguments =
                Bundle().apply {
                    putString("HEADER", header)
                    putString("BODY", body)
                    putInt("TIME_IN_SECONDS", timeInSeconds ?: 0)
                    putString("NEXT_BUTTON_TEXT", nextButtonText)
                }
        }
    }
}
