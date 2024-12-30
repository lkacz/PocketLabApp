// Filename: TapInstructionFragment.kt
package com.lkacz.pola

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView

class TapInstructionFragment : BaseTouchAwareFragment(1000, 3) {

    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null
    private lateinit var logger: Logger
    private var nextButton: Button? = null
    private val mediaPlayers = mutableListOf<MediaPlayer>()
    private lateinit var videoView: VideoView
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString("HEADER")
            body = it.getString("BODY")
            nextButtonText = it.getString("NEXT_BUTTON_TEXT")
        }
        logger = Logger.getInstance(requireContext())
        logger.logInstructionFragment(header ?: "Default Header", body ?: "Default Body")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_instruction, container, false)

        // BG color
        view.setBackgroundColor(ColorManager.getScreenBackgroundColor(requireContext()))
        videoView = view.findViewById(R.id.videoView2)

        // Insert a WebView before the nextButton, similar to TimerFragment
        val containerLayout = view as LinearLayout
        val nextButtonView = containerLayout.findViewById<Button>(R.id.nextButton)
        webView = WebView(requireContext())

        // Add 16 dp top and bottom margin for the WebView
        val scale = resources.displayMetrics.density
        val marginPx = (16 * scale + 0.5f).toInt()
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, marginPx, 0, marginPx)
        webView.layoutParams = layoutParams

        containerLayout.addView(webView, containerLayout.indexOfChild(nextButtonView))
        setupWebView()

        val headerTextView = view.findViewById<TextView>(R.id.headerTextView)
        val bodyTextView = view.findViewById<TextView>(R.id.bodyTextView)
        nextButton = view.findViewById(R.id.nextButton)
        nextButton?.visibility = View.INVISIBLE

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        val cleanHeader = parseAndPlayAudioIfAny(header.orEmpty(), mediaFolderUri)
        val refinedHeader = checkAndLoadHtml(cleanHeader, mediaFolderUri)

        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), mediaFolderUri)
        val refinedBody = checkAndLoadHtml(cleanBody, mediaFolderUri)

        val cleanNextButton = parseAndPlayAudioIfAny(nextButtonText.orEmpty(), mediaFolderUri)
        val refinedNextButton = checkAndLoadHtml(cleanNextButton, mediaFolderUri)

        checkAndPlayMp4(header.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(body.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(nextButtonText.orEmpty(), mediaFolderUri)

        headerTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, refinedHeader)
        headerTextView.textSize = FontSizeManager.getHeaderSize(requireContext())
        headerTextView.setTextColor(ColorManager.getHeaderTextColor(requireContext()))

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, refinedBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())
        bodyTextView.setTextColor(ColorManager.getBodyTextColor(requireContext()))

        nextButton?.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, refinedNextButton)
        nextButton?.textSize = FontSizeManager.getButtonSize(requireContext())
        nextButton?.setTextColor(ColorManager.getButtonTextColor(requireContext()))
        nextButton?.setBackgroundColor(ColorManager.getButtonBackgroundColor(requireContext()))
        nextButton?.setOnClickListener {
            (activity as MainActivity).loadNextFragment()
        }

        return view
    }

    override fun onTouchThresholdReached() {
        logger.logOther("Tap threshold reached in TapInstructionFragment")
        nextButton?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()
        if (this::videoView.isInitialized && videoView.isPlaying) {
            videoView.stopPlayback()
        }
        if (this::webView.isInitialized) {
            webView.destroy()
        }
    }

    private fun checkAndLoadHtml(text: String, mediaFolderUri: Uri?): String {
        if (text.isBlank() || mediaFolderUri == null) return text
        val pattern = Regex("<([^>]+\\.html)>", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return text
        val matchedFull = match.value
        val fileName = match.groupValues[1].trim()

        val parentFolder = androidx.documentfile.provider.DocumentFile.fromTreeUri(requireContext(), mediaFolderUri)
            ?: return text
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
        val parentFolder = androidx.documentfile.provider.DocumentFile.fromTreeUri(requireContext(), mediaFolderUri)
            ?: return
        val videoFile = parentFolder.findFile(fileName) ?: return
        if (!videoFile.exists() || !videoFile.isFile) return
        videoView.setVideoURI(videoFile.uri)
        videoView.setOnPreparedListener { mp ->
            mp.start()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(header: String?, body: String?, nextButtonText: String?) =
            TapInstructionFragment().apply {
                arguments = Bundle().apply {
                    putString("HEADER", header)
                    putString("BODY", body)
                    putString("NEXT_BUTTON_TEXT", nextButtonText)
                }
            }
    }
}
