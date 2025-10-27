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
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment

class InputFieldFragment : Fragment() {
    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null
    private var fields: List<String> = emptyList()
    private var isRandom: Boolean = false

    private lateinit var logger: Logger
    private lateinit var headerTextView: TextView
    private lateinit var webView: WebView
    private lateinit var bodyTextView: TextView
    private lateinit var videoView: VideoView
    private lateinit var nextButton: Button

    private val mediaPlayers = mutableListOf<MediaPlayer>()
    private val editTexts = mutableListOf<EditText>()

    private var tapEnabled = false
    private var holdEnabled = false
    private var tapCount = 0
    private val tapThreshold = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString("HEADER")
            body = it.getString("BODY")
            nextButtonText = it.getString("NEXT_BUTTON_TEXT")
            fields = it.getStringArrayList("FIELDS")?.toList() ?: emptyList()
            isRandom = it.getBoolean("IS_RANDOM", false)
        }
        logger = Logger.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val frameLayout =
            FrameLayout(requireContext()).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
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
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        scrollView.addView(contentLayout)
        frameLayout.addView(scrollView)

        headerTextView =
            TextView(requireContext()).apply {
                text = "Default Header"
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
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

        for (field in fields) {
            val editText =
                EditText(requireContext()).apply {
                    hint = field
                    textSize = FontSizeManager.getBodySize(requireContext())
                    setTextColor(ColorManager.getBodyTextColor(requireContext()))
                }
            contentLayout.addView(editText)
            editTexts.add(editText)
        }

        nextButton =
            Button(requireContext()).apply {
                text = "Continue"
            }
        val buttonParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END,
            )
        nextButton.layoutParams = buttonParams
        frameLayout.addView(nextButton)

        return frameLayout
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

        val (buttonTextNoTap, isTap) =
            parseTapAttribute(
                parseAndPlayAudioIfAny(nextButtonText.orEmpty(), resourcesFolderUri),
            )
        tapEnabled = isTap

        val (buttonTextNoHold, isHold) = parseHoldAttribute(buttonTextNoTap)
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

        nextButton.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedNextText)
        nextButton.textSize = FontSizeManager.getContinueSize(requireContext())
        nextButton.setTextColor(ColorManager.getContinueTextColor(requireContext()))
        nextButton.setBackgroundColor(ColorManager.getContinueBackgroundColor(requireContext()))
        applyContinueButtonPadding(nextButton)
        applyContinueAlignment(nextButton)

        if (tapEnabled) {
            nextButton.visibility = View.INVISIBLE
            view.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    tapCount++
                    if (tapCount >= tapThreshold) {
                        nextButton.visibility = View.VISIBLE
                        tapCount = 0
                    }
                    v.performClick()
                    true
                } else {
                    false
                }
            }
        }

        if (holdEnabled) {
            HoldButtonHelper.setupHoldToConfirm(nextButton) {
                logResponsesAndContinue()
            }
        } else {
            nextButton.setOnClickListener {
                logResponsesAndContinue()
            }
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

    private fun logResponsesAndContinue() {
        for (editText in editTexts) {
            val response = editText.text.toString().trim()
            val numeric = response.toFloatOrNull() != null
            logger.logInputFieldFragment(
                header = header.orEmpty(),
                body = body.orEmpty(),
                item = editText.hint?.toString().orEmpty(),
                response = response,
                isNumeric = numeric,
            )
        }
        (activity as? MainActivity)?.loadNextFragment()
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

        // Try loading from resources folder if available
        if (resourcesFolderUri != null) {
            val parentFolder = DocumentFile.fromTreeUri(requireContext(), resourcesFolderUri)
            val videoFile = parentFolder?.findFile(fileName)
            if (videoFile != null && videoFile.exists() && videoFile.isFile) {
                videoView.visibility = View.VISIBLE
                videoView.setVideoURI(videoFile.uri)
                videoView.setOnPreparedListener { mp ->
                    mp.start()
                    mp.setVolume(volume, volume)
                }
                return
            }
        }

        // Fallback: try loading from assets
        try {
            val afd = requireContext().assets.openFd(fileName)
            videoView.visibility = View.VISIBLE
            videoView.setVideoURI(Uri.parse("file:///android_asset/$fileName"))
            videoView.setOnPreparedListener { mp ->
                mp.start()
                mp.setVolume(volume, volume)
            }
            afd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAndLoadHtml(
        text: String,
        resourcesFolderUri: Uri?,
    ): String {
        if (text.isBlank()) return text
        val pattern = Regex("<([^>]+\\.html)>", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return text
        val matchedFull = match.value
        val fileName = match.groupValues[1].trim()

        // Try loading from resources folder if available
        if (resourcesFolderUri != null) {
            val parentFolder = DocumentFile.fromTreeUri(requireContext(), resourcesFolderUri)
            val htmlFile = parentFolder?.findFile(fileName)
            if (htmlFile != null && htmlFile.exists() && htmlFile.isFile) {
                try {
                    requireContext().contentResolver.openInputStream(htmlFile.uri)?.use { inputStream ->
                        val htmlContent = inputStream.bufferedReader().readText()
                        webView.visibility = View.VISIBLE
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    }
                    return text.replace(matchedFull, "")
                } catch (_: Exception) {
                }
            }
        }

        // Fallback: try loading from assets
        try {
            requireContext().assets.open(fileName).use { inputStream ->
                val htmlContent = inputStream.bufferedReader().readText()
                webView.visibility = View.VISIBLE
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        } catch (_: Exception) {
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
        val prefs = requireContext().getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        when (prefs.getString("BODY_ALIGNMENT", "CENTER")?.uppercase()) {
            "LEFT" -> textView.gravity = Gravity.START
            "RIGHT" -> textView.gravity = Gravity.END
            else -> textView.gravity = Gravity.CENTER
        }
    }

    private fun applyContinueAlignment(button: Button) {
        val prefs = requireContext().getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
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

    private fun parseTapAttribute(text: String): Pair<String, Boolean> {
        val regex = Regex("\\[TAP\\]", RegexOption.IGNORE_CASE)
        return if (regex.containsMatchIn(text)) {
            val newText = text.replace(regex, "").trim()
            newText to true
        } else {
            text to false
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

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    companion object {
        @JvmStatic
        fun newInstance(
            heading: String?,
            body: String?,
            buttonName: String?,
            inputFields: List<String>,
            isRandom: Boolean,
        ) = InputFieldFragment().apply {
            arguments =
                Bundle().apply {
                    putString("HEADER", heading)
                    putString("BODY", body)
                    putString("NEXT_BUTTON_TEXT", buttonName)
                    putStringArrayList("FIELDS", ArrayList(inputFields))
                    putBoolean("IS_RANDOM", isRandom)
                }
        }
    }
}
