// Filename: CustomHtmlFragment.kt
package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Button
import android.widget.FrameLayout
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment

class CustomHtmlFragment : Fragment() {

    private var fileName: String? = null
    private var continueButtonText: String? = null
    private lateinit var webView: WebView
    private val mediaPlayers = mutableListOf<MediaPlayer>()

    private var tapEnabled = false
    private var tapCount = 0
    private val tapThreshold = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileName = arguments?.getString(ARG_HTML_FILE)
        continueButtonText = arguments?.getString(ARG_BUTTON_TEXT, "Continue")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val frameLayout = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        webView = WebView(requireContext()).apply {
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        frameLayout.addView(webView)

        val (cleanText, isTap) = parseTapAttribute(continueButtonText.orEmpty())
        tapEnabled = isTap

        val continueButton = Button(requireContext()).apply {
            text = cleanText.ifBlank { "Continue" }
            textSize = FontSizeManager.getContinueSize(requireContext())
            setTextColor(ColorManager.getContinueTextColor(requireContext()))
            setBackgroundColor(ColorManager.getContinueBackgroundColor(requireContext()))

            val density = resources.displayMetrics.density
            val ch = SpacingManager.getContinueButtonPaddingHorizontal(requireContext())
            val cv = SpacingManager.getContinueButtonPaddingVertical(requireContext())
            val chPx = (ch * density + 0.5f).toInt()
            val cvPx = (cv * density + 0.5f).toInt()
            setPadding(chPx, cvPx, chPx, cvPx)

            val marginPx = (16 * density + 0.5f).toInt()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply {
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }

            setOnClickListener {
                (activity as? MainActivity)?.loadNextFragment()
            }
        }
        frameLayout.addView(continueButton)

        if (tapEnabled) {
            continueButton.visibility = View.INVISIBLE
            frameLayout.setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
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

        applyContinueAlignment(continueButton)

        return frameLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWebView()
        loadCustomHtml()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()
    }

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
    }

    private fun loadCustomHtml() {
        val resourcesUri = ResourcesFolderManager(requireContext()).getResourcesFolderUri()
        if (resourcesUri == null || fileName.isNullOrBlank()) {
            webView.loadData(
                "<html><body><h2>File not found or invalid name.</h2></body></html>",
                "text/html",
                "UTF-8"
            )
            return
        }

        val folder = DocumentFile.fromTreeUri(requireContext(), resourcesUri) ?: return
        val htmlFile = folder.findFile(fileName!!)
        if (htmlFile == null || !htmlFile.exists() || !htmlFile.isFile) {
            webView.loadData(
                "<html><body><h2>HTML file not found in resources folder.</h2></body></html>",
                "text/html",
                "UTF-8"
            )
            return
        }

        try {
            requireContext().contentResolver.openInputStream(htmlFile.uri)?.use { inputStream ->
                val htmlContent = inputStream.bufferedReader().readText()
                webView.visibility = View.VISIBLE
                webView.loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        } catch (e: Exception) {
            webView.loadData(
                "<html><body><h2>Error loading file: ${e.message}</h2></body></html>",
                "text/html",
                "UTF-8"
            )
        }
    }

    private fun applyContinueAlignment(button: Button) {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        val horiz = prefs.getString("CONTINUE_ALIGNMENT_HORIZONTAL", "RIGHT")?.uppercase()
        val vert = prefs.getString("CONTINUE_ALIGNMENT_VERTICAL", "BOTTOM")?.uppercase()
        val lp = button.layoutParams as? FrameLayout.LayoutParams ?: return
        val (hGravity, vGravity) = when (horiz) {
            "LEFT" -> Gravity.START to when (vert) {
                "TOP" -> Gravity.TOP
                else -> Gravity.BOTTOM
            }
            "CENTER" -> Gravity.CENTER_HORIZONTAL to when (vert) {
                "TOP" -> Gravity.TOP
                else -> Gravity.BOTTOM
            }
            else -> Gravity.END to when (vert) {
                "TOP" -> Gravity.TOP
                else -> Gravity.BOTTOM
            }
        }
        lp.gravity = hGravity or vGravity
        button.layoutParams = lp
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

    companion object {
        private const val ARG_HTML_FILE = "ARG_HTML_FILE"
        private const val ARG_BUTTON_TEXT = "ARG_BUTTON_TEXT"

        fun newInstance(fileName: String, buttonText: String): CustomHtmlFragment {
            val fragment = CustomHtmlFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_HTML_FILE, fileName)
                putString(ARG_BUTTON_TEXT, buttonText)
            }
            return fragment
        }
    }
}
