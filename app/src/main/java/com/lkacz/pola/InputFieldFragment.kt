// Filename: InputFieldFragment.kt
package com.lkacz.pola

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment

class InputFieldFragment : Fragment() {
    private var heading: String? = null
    private var body: String? = null
    private var buttonName: String? = null
    private var inputFields: List<String>? = null
    private lateinit var logger: Logger
    private val fieldValues = mutableMapOf<String, String>()

    private val mediaPlayers = mutableListOf<MediaPlayer>()
    private lateinit var videoView: VideoView
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            heading = it.getString("HEADING")
            body = it.getString("TEXT")
            buttonName = it.getString("BUTTON")
            inputFields = it.getStringArrayList("INPUTFIELDS")
        }
        logger = Logger.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate our revised layout
        val view = inflater.inflate(R.layout.fragment_input_field, container, false)

        // Screen background color
        view.setBackgroundColor(ColorManager.getScreenBackgroundColor(requireContext()))

        val headingTextView: TextView = view.findViewById(R.id.headingTextView)
        webView = view.findViewById(R.id.htmlSnippetWebView)
        // Hide WebView by default
        webView.visibility = View.GONE
        val bodyTextView: TextView = view.findViewById(R.id.bodyTextView)
        videoView = view.findViewById(R.id.videoView2)
        val containerLayout: LinearLayout = view.findViewById(R.id.inputFieldContainer)
        val continueButton: Button = view.findViewById(R.id.continueButton)

        setupWebView()

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        // Heading
        val cleanHeading = parseAndPlayAudioIfAny(heading.orEmpty(), mediaFolderUri)
        val refinedHeading = checkAndLoadHtml(cleanHeading, mediaFolderUri)
        checkAndPlayMp4(heading.orEmpty(), mediaFolderUri)
        headingTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, refinedHeading)
        headingTextView.textSize = FontSizeManager.getHeaderSize(requireContext())
        headingTextView.setTextColor(ColorManager.getHeaderTextColor(requireContext()))

        // Body
        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), mediaFolderUri)
        val refinedBody = checkAndLoadHtml(cleanBody, mediaFolderUri)
        checkAndPlayMp4(body.orEmpty(), mediaFolderUri)
        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, refinedBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())
        bodyTextView.setTextColor(ColorManager.getBodyTextColor(requireContext()))

        // Input fields
        inputFields?.forEach { fieldHint ->
            val cleanHint = parseAndPlayAudioIfAny(fieldHint, mediaFolderUri)
            val refinedHint = checkAndLoadHtml(cleanHint, mediaFolderUri)
            checkAndPlayMp4(fieldHint, mediaFolderUri)

            val editText = EditText(context).apply {
                hint = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, refinedHint)
                textSize = FontSizeManager.getBodySize(requireContext())
                setTextColor(ColorManager.getBodyTextColor(requireContext()))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            editText.addTextChangedListener {
                fieldValues[fieldHint] = it.toString()
            }
            fieldValues[fieldHint] = ""
            containerLayout.addView(editText)
        }

        // Continue button
        val cleanButtonText = parseAndPlayAudioIfAny(buttonName.orEmpty(), mediaFolderUri)
        val refinedButtonText = checkAndLoadHtml(cleanButtonText, mediaFolderUri)
        checkAndPlayMp4(buttonName.orEmpty(), mediaFolderUri)

        continueButton.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, refinedButtonText)
        continueButton.textSize = FontSizeManager.getButtonSize(requireContext())
        continueButton.setTextColor(ColorManager.getButtonTextColor(requireContext()))
        continueButton.setBackgroundColor(ColorManager.getButtonBackgroundColor(requireContext()))
        continueButton.setOnClickListener {
            fieldValues.forEach { (hint, value) ->
                val isNumeric = value.toDoubleOrNull() != null
                logger.logInputFieldFragment(
                    heading.orEmpty(),
                    body.orEmpty(),
                    hint,
                    value,
                    isNumeric
                )
            }
            (activity as MainActivity).loadNextFragment()
        }

        return view
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
                    // Make WebView visible only if we can load HTML
                    webView.visibility = View.VISIBLE
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

    companion object {
        @JvmStatic
        fun newInstance(
            heading: String?,
            text: String?,
            buttonName: String?,
            inputFields: List<String>?
        ) = InputFieldFragment().apply {
            arguments = Bundle().apply {
                putString("HEADING", heading)
                putString("TEXT", text)
                putString("BUTTON", buttonName)
                putStringArrayList("INPUTFIELDS", ArrayList(inputFields ?: emptyList()))
            }
        }
    }
}
