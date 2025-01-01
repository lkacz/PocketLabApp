// Filename: TimerFragment.kt
package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import android.widget.VideoView
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
    private lateinit var videoView: VideoView
    private lateinit var webView: WebView

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
    ): View? {
        return inflater.inflate(R.layout.fragment_timer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(ColorManager.getScreenBackgroundColor(requireContext()))

        val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        webView = view.findViewById(R.id.htmlSnippetWebView)
        webView.visibility = View.GONE
        val bodyTextView: TextView = view.findViewById(R.id.bodyTextView)
        videoView = view.findViewById(R.id.videoView2)
        val nextButton: Button = view.findViewById(R.id.nextButton)
        val timerTextView: TextView = view.findViewById(R.id.timerTextView)

        setupWebView()

        val resourcesFolderUri = ResourcesFolderManager(requireContext()).getResourcesFolderUri()

        val cleanHeader = parseAndPlayAudioIfAny(header.orEmpty(), resourcesFolderUri)
        val refinedHeader = checkAndLoadHtml(cleanHeader, resourcesFolderUri)
        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), resourcesFolderUri)
        val refinedBody = checkAndLoadHtml(cleanBody, resourcesFolderUri)
        val cleanNextText = parseAndPlayAudioIfAny(nextButtonText.orEmpty(), resourcesFolderUri)
        val refinedNextText = checkAndLoadHtml(cleanNextText, resourcesFolderUri)

        checkAndPlayMp4(header.orEmpty(), resourcesFolderUri)
        checkAndPlayMp4(body.orEmpty(), resourcesFolderUri)
        checkAndPlayMp4(nextButtonText.orEmpty(), resourcesFolderUri)

        // Header
        headerTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedHeader)
        headerTextView.textSize = FontSizeManager.getHeaderSize(requireContext())
        headerTextView.setTextColor(ColorManager.getHeaderTextColor(requireContext()))
        applyHeaderAlignment(headerTextView)

        // Body
        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())
        bodyTextView.setTextColor(ColorManager.getBodyTextColor(requireContext()))

        // CONTINUE button
        nextButton.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedNextText)
        nextButton.textSize = FontSizeManager.getContinueSize(requireContext())
        nextButton.setTextColor(ColorManager.getContinueTextColor(requireContext()))
        nextButton.setBackgroundColor(ColorManager.getContinueBackgroundColor(requireContext()))

        val density = resources.displayMetrics.density
        val ch = SpacingManager.getContinueButtonPaddingHorizontal(requireContext())
        val cv = SpacingManager.getContinueButtonPaddingVertical(requireContext())
        val chPx = (ch * density + 0.5f).toInt()
        val cvPx = (cv * density + 0.5f).toInt()
        nextButton.setPadding(chPx, cvPx, chPx, cvPx)
        nextButton.visibility = View.INVISIBLE

        timerTextView.textSize = FontSizeManager.getBodySize(requireContext())
        timerTextView.setTextColor(ColorManager.getBodyTextColor(requireContext()))

        val totalTimeMillis = (timeInSeconds ?: 0) * 1000L
        timer = object : CountDownTimer(totalTimeMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val minutesLeft = millisUntilFinished / 60000
                val secondsLeft = (millisUntilFinished % 60000) / 1000
                timerTextView.text = String.format("Time left: %02d:%02d", minutesLeft, secondsLeft)
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

    override fun onTouchThresholdReached() {
        timer?.cancel()
        logger.logTimerFragment(header ?: "Default Header", "Timer forcibly ended by user", timeInSeconds ?: 0)
        view?.findViewById<TextView>(R.id.timerTextView)?.text = "Continue."
        view?.findViewById<Button>(R.id.nextButton)?.visibility = View.VISIBLE
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
            val vol = segments[1].trim().toFloatOrNull()
            if (vol != null && vol in 0f..100f) vol / 100f else 1.0f
        } else 1.0f
        videoView.visibility = View.VISIBLE
        playVideoFile(fileName, volume, resourcesFolderUri)
    }

    private fun playVideoFile(fileName: String, volume: Float, resourcesFolderUri: Uri?) {
        if (resourcesFolderUri == null) return
        val parentFolder = DocumentFile.fromTreeUri(requireContext(), resourcesFolderUri) ?: return
        val videoFile = parentFolder.findFile(fileName) ?: return
        if (!videoFile.exists() || !videoFile.isFile) return
        videoView.setVideoURI(videoFile.uri)
        videoView.setOnPreparedListener { mp -> mp.start() }
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
        val alignment = prefs.getString("HEADER_ALIGNMENT", "CENTER")?.uppercase()
        when (alignment) {
            "LEFT" -> textView.gravity = Gravity.START
            "RIGHT" -> textView.gravity = Gravity.END
            else -> textView.gravity = Gravity.CENTER
        }
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
