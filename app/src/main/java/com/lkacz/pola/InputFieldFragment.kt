// Filename: InputFieldFragment.kt
package com.lkacz.pola

import android.content.Context
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
        val view = inflater.inflate(R.layout.fragment_input_field, container, false)
        view.setBackgroundColor(ColorManager.getScreenBackgroundColor(requireContext()))

        val headingTextView: TextView = view.findViewById(R.id.headingTextView)
        webView = view.findViewById(R.id.htmlSnippetWebView)
        webView.visibility = View.GONE
        val bodyTextView: TextView = view.findViewById(R.id.bodyTextView)
        videoView = view.findViewById(R.id.videoView2)
        val containerLayout: LinearLayout = view.findViewById(R.id.inputFieldContainer)
        val continueButton: Button = view.findViewById(R.id.continueButton)

        setupWebView()

        val resourcesFolderUri = ResourcesFolderManager(requireContext()).getResourcesFolderUri()

        val cleanHeading = parseAndPlayAudioIfAny(heading.orEmpty(), resourcesFolderUri)
        val refinedHeading = checkAndLoadHtml(cleanHeading, resourcesFolderUri)
        checkAndPlayMp4(heading.orEmpty(), resourcesFolderUri)
        headingTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedHeading)
        headingTextView.textSize = FontSizeManager.getHeaderSize(requireContext())
        headingTextView.setTextColor(ColorManager.getHeaderTextColor(requireContext()))
        applyHeaderAlignment(headingTextView)

        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), resourcesFolderUri)
        val refinedBody = checkAndLoadHtml(cleanBody, resourcesFolderUri)
        checkAndPlayMp4(body.orEmpty(), resourcesFolderUri)
        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())
        bodyTextView.setTextColor(ColorManager.getBodyTextColor(requireContext()))

        inputFields?.forEach { fieldHint ->
            val cleanHint = parseAndPlayAudioIfAny(fieldHint, resourcesFolderUri)
            val refinedHint = checkAndLoadHtml(cleanHint, resourcesFolderUri)
            checkAndPlayMp4(fieldHint, resourcesFolderUri)

            val editText = EditText(context).apply {
                hint = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedHint)
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

        val cleanButtonText = parseAndPlayAudioIfAny(buttonName.orEmpty(), resourcesFolderUri)
        val refinedButtonText = checkAndLoadHtml(cleanButtonText, resourcesFolderUri)
        checkAndPlayMp4(buttonName.orEmpty(), resourcesFolderUri)

        continueButton.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedButtonText)
        continueButton.textSize = FontSizeManager.getContinueSize(requireContext())
        continueButton.setTextColor(ColorManager.getContinueTextColor(requireContext()))
        continueButton.setBackgroundColor(ColorManager.getContinueBackgroundColor(requireContext()))

        val density = resources.displayMetrics.density
        val ch = SpacingManager.getContinueButtonPaddingHorizontal(requireContext())
        val cv = SpacingManager.getContinueButtonPaddingVertical(requireContext())
        val chPx = (ch * density + 0.5f).toInt()
        val cvPx = (cv * density + 0.5f).toInt()
        continueButton.setPadding(chPx, cvPx, chPx, cvPx)

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

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
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
