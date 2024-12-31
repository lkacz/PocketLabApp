// Filename: TimerFragment.kt
package com.lkacz.pola

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
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
        val bodyTextView: TextView = view.findViewById(R.id.bodyTextView)
        videoView = view.findViewById(R.id.videoView2)
        val nextButton: Button = view.findViewById(R.id.nextButton)
        val timerTextView: TextView = view.findViewById(R.id.timerTextView)

        setupWebView()

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        val cleanHeader = parseAndPlayAudioIfAny(header.orEmpty(), mediaFolderUri)
        val refinedHeader = checkAndLoadHtml(cleanHeader, mediaFolderUri)

        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), mediaFolderUri)
        val refinedBody = checkAndLoadHtml(cleanBody, mediaFolderUri)

        val cleanNextText = parseAndPlayAudioIfAny(nextButtonText.orEmpty(), mediaFolderUri)
        val refinedNextText = checkAndLoadHtml(cleanNextText, mediaFolderUri)

        checkAndPlayMp4(header.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(body.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(nextButtonText.orEmpty(), mediaFolderUri)

        headerTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, refinedHeader)
        headerTextView.textSize = FontSizeManager.getHeaderSize(requireContext())
        headerTextView.setTextColor(ColorManager.getHeaderTextColor(requireContext()))

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, refinedBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())
        bodyTextView.setTextColor(ColorManager.getBodyTextColor(requireContext()))

        nextButton.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, refinedNextText)
        nextButton.textSize = FontSizeManager.getButtonSize(requireContext())
        nextButton.setTextColor(ColorManager.getButtonTextColor(requireContext()))
        nextButton.setBackgroundColor(ColorManager.getButtonBackgroundColor(requireContext()))
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

    private fun parseAndPlayAudioIfAny(text: String, mediaFolderUri: Uri?): String {
        return AudioPlaybackHelper.parseAndPlayAudio(
            context = requireContext(),
            rawText = text,
            mediaFolderUri = mediaFolderUri,
            mediaPlayers = mediaPlayers
        )
    }

    private fun checkAndPlayMp4(text: String, mediaFolderUri: Uri?) {
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
        playVideoFile(fileName, volume, mediaFolderUri)
    }

    private fun playVideoFile(fileName: String, volume: Float, mediaFolderUri: Uri?) {
        if (mediaFolderUri == null) return
        val parentFolder = DocumentFile.fromTreeUri(requireContext(), mediaFolderUri) ?: return
        val videoFile = parentFolder.findFile(fileName) ?: return
        if (!videoFile.exists() || !videoFile.isFile) return
        videoView.setVideoURI(videoFile.uri)
        videoView.setOnPreparedListener { mp ->
            mp.start()
        }
    }

    private fun checkAndLoadHtml(text: String, mediaFolderUri: Uri?): String {
        if (text.isBlank() || mediaFolderUri == null) return text
        val pattern = Regex("<([^>]+\\.html)>", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return text
        val matchedFull = match.value
        val fileName = match.groupValues[1].trim()

        val parentFolder = DocumentFile.fromTreeUri(requireContext(), mediaFolderUri) ?: return text
        val htmlFile = parentFolder.findFile(fileName)
        if (htmlFile != null && htmlFile.exists() && htmlFile.isFile) {
            try {
                requireContext().contentResolver.openInputStream(htmlFile.uri)?.use { inputStream ->
                    val htmlContent = inputStream.bufferedReader().readText()
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return text.replace(matchedFull, "")
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
