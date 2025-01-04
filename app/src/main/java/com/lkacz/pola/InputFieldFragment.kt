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
    private var isRandom: Boolean = false

    private lateinit var logger: Logger
    private val fieldValues = mutableMapOf<String, String>()

    private val mediaPlayers = mutableListOf<MediaPlayer>()
    private lateinit var scrollView: ScrollView
    private lateinit var mainLayout: LinearLayout
    private lateinit var headingTextView: TextView
    private lateinit var webView: WebView
    private lateinit var bodyTextView: TextView
    private lateinit var videoView: VideoView
    private lateinit var containerLayout: LinearLayout
    private lateinit var continueButton: Button

    // [TAP] logic
    private var tapEnabled = false
    private var tapCount = 0
    private val tapThreshold = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            heading = it.getString("HEADING")
            body = it.getString("TEXT")
            buttonName = it.getString("BUTTON")
            inputFields = it.getStringArrayList("INPUTFIELDS")
            isRandom = it.getBoolean("IS_RANDOM", false)
        }
        logger = Logger.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Root container: a ScrollView to allow vertical scrolling
        scrollView = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
        }

        // Main vertical LinearLayout
        mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        scrollView.addView(mainLayout)

        // 1) Heading
        headingTextView = TextView(requireContext()).apply {
            text = "Default Heading"
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
        }
        mainLayout.addView(headingTextView)

        // 2) WebView (hidden by default)
        webView = WebView(requireContext()).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
        }
        mainLayout.addView(webView)

        // 3) Body text
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
        mainLayout.addView(bodyTextView)

        // 4) VideoView (hidden by default)
        videoView = VideoView(requireContext()).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(32)
            }
        }
        mainLayout.addView(videoView)

        // 5) Container for input fields
        containerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
        }
        mainLayout.addView(containerLayout)

        // 6) Continue Button
        continueButton = Button(requireContext()).apply {
            text = "Continue"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        mainLayout.addView(continueButton)

        return scrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(ColorManager.getScreenBackgroundColor(requireContext()))
        setupWebView()

        val resourcesFolderUri = ResourcesFolderManager(requireContext()).getResourcesFolderUri()

        // Heading
        val cleanHeading = parseAndPlayAudioIfAny(heading.orEmpty(), resourcesFolderUri)
        val refinedHeading = checkAndLoadHtml(cleanHeading, resourcesFolderUri)
        checkAndPlayMp4(heading.orEmpty(), resourcesFolderUri)
        headingTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedHeading)
        headingTextView.textSize = FontSizeManager.getHeaderSize(requireContext())
        headingTextView.setTextColor(ColorManager.getHeaderTextColor(requireContext()))
        applyHeaderAlignment(headingTextView)

        // Body
        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), resourcesFolderUri)
        val refinedBody = checkAndLoadHtml(cleanBody, resourcesFolderUri)
        checkAndPlayMp4(body.orEmpty(), resourcesFolderUri)
        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())
        bodyTextView.setTextColor(ColorManager.getBodyTextColor(requireContext()))
        applyBodyAlignment(bodyTextView)

        // Shuffle fields if isRandom
        val actualFields = if (isRandom) inputFields?.shuffled() ?: emptyList() else inputFields ?: emptyList()

        // Create EditTexts
        actualFields.forEach { fieldHint ->
            val cleanHint = parseAndPlayAudioIfAny(fieldHint, resourcesFolderUri)
            val refinedHint = checkAndLoadHtml(cleanHint, resourcesFolderUri)
            checkAndPlayMp4(fieldHint, resourcesFolderUri)

            val editText = EditText(requireContext()).apply {
                hint = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedHint)
                textSize = FontSizeManager.getBodySize(requireContext())
                setTextColor(ColorManager.getBodyTextColor(requireContext()))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            editText.addTextChangedListener { fieldValues[fieldHint] = it.toString() }
            fieldValues[fieldHint] = ""
            containerLayout.addView(editText)
        }

        // Continue button
        val (cleanButtonText, isTap) = parseTapAttribute(parseAndPlayAudioIfAny(buttonName.orEmpty(), resourcesFolderUri))
        tapEnabled = isTap
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
        applyContinueAlignment(continueButton)

        if (tapEnabled) {
            continueButton.visibility = View.INVISIBLE
            view.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    tapCount++
                    if (tapCount >= tapThreshold) {
                        continueButton.visibility = View.VISIBLE
                        tapCount = 0
                    }
                    true
                } else {
                    false
                }
            }
        }

        continueButton.setOnClickListener {
            // Log all fields
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
            segments[1].trim().toFloatOrNull()?.coerceIn(0f, 100f)?.div(100f) ?: 1.0f
        } else 1.0f

        if (resourcesFolderUri != null) {
            val parentFolder = DocumentFile.fromTreeUri(requireContext(), resourcesFolderUri)
            if (parentFolder != null) {
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
            heading: String?,
            text: String?,
            buttonName: String?,
            inputFields: List<String>?,
            isRandom: Boolean
        ) = InputFieldFragment().apply {
            arguments = Bundle().apply {
                putString("HEADING", heading)
                putString("TEXT", text)
                putString("BUTTON", buttonName)
                putStringArrayList("INPUTFIELDS", ArrayList(inputFields ?: emptyList()))
                putBoolean("IS_RANDOM", isRandom)
            }
        }
    }
}
