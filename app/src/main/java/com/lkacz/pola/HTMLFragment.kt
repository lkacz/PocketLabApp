// Filename: HtmlFragment.kt
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

class HtmlFragment : Fragment() {

    private var fileName: String? = null
    private var nextButtonText: String? = null

    private lateinit var logger: Logger
    private lateinit var webView: WebView
    private lateinit var nextButton: Button
    private lateinit var containerLayout: FrameLayout

    private val mediaPlayers = mutableListOf<MediaPlayer>()

    private var tapEnabled = false
    private var holdEnabled = false
    private var tapCount = 0
    private val tapThreshold = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            fileName = it.getString("HTML_FILE")
            nextButtonText = it.getString("NEXT_BUTTON_TEXT")
        }
        logger = Logger.getInstance(requireContext())
        logger.logOther("Entered HtmlFragment: $fileName")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        containerLayout = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        webView = WebView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        containerLayout.addView(webView)

        nextButton = Button(requireContext()).apply {
            text = "Continue"
        }
        val buttonParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        )
        nextButton.layoutParams = buttonParams
        containerLayout.addView(nextButton)

        return containerLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(ColorManager.getScreenBackgroundColor(requireContext()))
        setupWebView()

        val resourcesFolderUri = ResourcesFolderManager(requireContext()).getResourcesFolderUri()

        val (buttonTextNoTap, isTap) = parseTapAttribute(
            parseAndPlayAudioIfAny(nextButtonText.orEmpty(), resourcesFolderUri)
        )
        tapEnabled = isTap

        val (buttonTextNoHold, isHold) = parseHoldAttribute(buttonTextNoTap)
        holdEnabled = isHold

        loadHtmlContentIfAvailable(fileName.orEmpty(), resourcesFolderUri)

        nextButton.text = buttonTextNoHold
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
                (activity as? MainActivity)?.loadNextFragment()
            }
        } else {
            nextButton.setOnClickListener {
                (activity as? MainActivity)?.loadNextFragment()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()
        if (this::webView.isInitialized) {
            webView.destroy()
        }
    }

    private fun loadHtmlContentIfAvailable(htmlFileName: String, resourcesFolderUri: Uri?) {
        if (htmlFileName.isBlank() || resourcesFolderUri == null) return
        val parentFolder = DocumentFile.fromTreeUri(requireContext(), resourcesFolderUri) ?: return
        val htmlFile = parentFolder.findFile(htmlFileName) ?: return
        if (!htmlFile.exists() || !htmlFile.isFile) return
        try {
            requireContext().contentResolver.openInputStream(htmlFile.uri)?.use { inputStream ->
                val htmlContent = inputStream.bufferedReader().readText()
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        } catch (_: Exception) {
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

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
    }

    private fun applyContinueAlignment(button: Button) {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        val horiz = prefs.getString("CONTINUE_ALIGNMENT_HORIZONTAL", "RIGHT")?.uppercase()
        val vert = prefs.getString("CONTINUE_ALIGNMENT_VERTICAL", "BOTTOM")?.uppercase()
        val lp = button.layoutParams as? FrameLayout.LayoutParams ?: return

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

    companion object {
        @JvmStatic
        fun newInstance(fileName: String, nextButtonText: String) =
            HtmlFragment().apply {
                arguments = Bundle().apply {
                    putString("HTML_FILE", fileName)
                    putString("NEXT_BUTTON_TEXT", nextButtonText)
                }
            }
    }
}
