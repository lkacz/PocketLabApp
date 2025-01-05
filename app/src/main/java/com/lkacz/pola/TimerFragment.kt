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
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment

class TimerFragment : Fragment() {

    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null
    private var timeSeconds: Int = 0

    private lateinit var logger: Logger

    private lateinit var headerTextView: TextView
    private lateinit var bodyTextView: TextView
    private lateinit var videoView: VideoView
    private lateinit var nextButton: Button
    private lateinit var timerTextView: TextView
    private lateinit var webView: WebView

    private val mediaPlayers = mutableListOf<MediaPlayer>()
    private var holdEnabled = false

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString("HEADER")
            body = it.getString("BODY")
            nextButtonText = it.getString("NEXT_BUTTON_TEXT")
            timeSeconds = it.getInt("TIME_SECONDS", 0)
        }
        logger = Logger.getInstance(requireContext())
        logger.logTimerFragment(header ?: "Default Header", body ?: "Default Body", timeSeconds, nextButtonText)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_timer, container, false)

        headerTextView = rootView.findViewById(R.id.headerTextView)
        webView = rootView.findViewById(R.id.htmlSnippetWebView)
        bodyTextView = rootView.findViewById(R.id.bodyTextView)
        videoView = rootView.findViewById(R.id.videoView2)
        timerTextView = rootView.findViewById(R.id.timerTextView)
        nextButton = rootView.findViewById(R.id.nextButton)

        // Button should remain hidden until time is up
        nextButton.visibility = View.INVISIBLE

        return rootView
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
        val cleanNext = parseAndPlayAudioIfAny(nextButtonText.orEmpty(), resourcesFolderUri)
        val refinedNext = checkAndLoadHtml(cleanNext, resourcesFolderUri)

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

        timerTextView.textSize = FontSizeManager.getTimerSize(requireContext())
        timerTextView.setTextColor(ColorManager.getTimerTextColor(requireContext()))
        applyTimerAlignment(timerTextView)

        // Parse [HOLD] from nextButtonText
        val (buttonTextNoHold, isHold) = parseHoldAttribute(refinedNext)
        holdEnabled = isHold

        nextButton.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, buttonTextNoHold)
        nextButton.textSize = FontSizeManager.getContinueSize(requireContext())
        nextButton.setTextColor(ColorManager.getContinueTextColor(requireContext()))
        nextButton.setBackgroundColor(ColorManager.getContinueBackgroundColor(requireContext()))
        applyContinueButtonPadding(nextButton)
        applyContinueAlignment(nextButton)

        // Once timer finishes, the button is shown. If [HOLD], we set up hold logic, otherwise direct click.
        startCountdown(timeSeconds)

        // We do not handle [TAP] in TimerFragment.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()
        countDownTimer?.cancel()

        if (this::videoView.isInitialized && videoView.isPlaying) {
            videoView.stopPlayback()
        }
        if (this::webView.isInitialized) {
            webView.destroy()
        }
    }

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
    }

    private fun startCountdown(seconds: Int) {
        if (seconds <= 0) {
            // If invalid or zero, instantly show button
            enableButtonPostTimer()
            return
        }

        countDownTimer = object : CountDownTimer(seconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secLeft = millisUntilFinished / 1000
                timerTextView.text = "Time remaining: $secLeft seconds"
            }

            override fun onFinish() {
                timerTextView.text = "Time remaining: 0 seconds"
                enableButtonPostTimer()
            }
        }.start()
    }

    private fun enableButtonPostTimer() {
        // Make the button visible now that the timer is over
        nextButton.visibility = View.VISIBLE

        if (holdEnabled) {
            HoldButtonHelper.setupHoldToConfirm(nextButton) {
                (activity as MainActivity).loadNextFragment()
            }
        } else {
            nextButton.setOnClickListener {
                (activity as MainActivity).loadNextFragment()
            }
        }
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

        val parentFolder = DocumentFile.fromTreeUri(requireContext(), resourcesFolderUri)
            ?: return text
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

    private fun applyTimerAlignment(textView: TextView) {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        when (prefs.getString("TIMER_ALIGNMENT", "CENTER")?.uppercase()) {
            "LEFT" -> textView.gravity = Gravity.START
            "RIGHT" -> textView.gravity = Gravity.END
            else -> textView.gravity = Gravity.CENTER
        }
    }

    private fun applyContinueAlignment(button: Button) {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        val horiz = prefs.getString("CONTINUE_ALIGNMENT_HORIZONTAL", "RIGHT")?.uppercase()
        val vert = prefs.getString("CONTINUE_ALIGNMENT_VERTICAL", "BOTTOM")?.uppercase()
        val lp = button.layoutParams as? FrameLayout.LayoutParams
            ?: (button.layoutParams as? LinearLayout.LayoutParams)?.let {
                // If it's a linear layout, convert it to a FrameLayout
                val newParams = FrameLayout.LayoutParams(it.width, it.height)
                button.layoutParams = newParams
                newParams
            }
            ?: return

        val hGravity = when (horiz) {
            "LEFT" -> Gravity.START
            "CENTER" -> Gravity.CENTER_HORIZONTAL
            else -> Gravity.END
        }
        val vGravity = when (vert) {
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

    companion object {
        @JvmStatic
        fun newInstance(header: String?, body: String?, nextButtonText: String?, timeSeconds: Int) =
            TimerFragment().apply {
                arguments = Bundle().apply {
                    putString("HEADER", header)
                    putString("BODY", body)
                    putString("NEXT_BUTTON_TEXT", nextButtonText)
                    putInt("TIME_SECONDS", timeSeconds)
                }
            }
    }
}
