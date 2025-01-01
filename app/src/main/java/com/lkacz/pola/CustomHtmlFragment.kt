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
    private lateinit var webView: WebView
    private val mediaPlayers = mutableListOf<MediaPlayer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileName = arguments?.getString(ARG_HTML_FILE)
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

        val continueButton = Button(requireContext()).apply {
            text = "Continue"
            textSize = FontSizeManager.getContinueSize(requireContext())
            setTextColor(ColorManager.getContinueTextColor(requireContext()))
            setBackgroundColor(ColorManager.getContinueBackgroundColor(requireContext()))

            val density = resources.displayMetrics.density
            val ch = SpacingManager.getContinueButtonPaddingHorizontal(requireContext())
            val cv = SpacingManager.getContinueButtonPaddingVertical(requireContext())
            val chPx = (ch * density + 0.5f).toInt()
            val cvPx = (cv * density + 0.5f).toInt()
            setPadding(chPx, cvPx, chPx, cvPx)

            // Initially place in bottom-end (we will further adjust alignment below).
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

        // Now apply final alignment preference from ProtocolPrefs
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
        val alignment = prefs.getString("CONTINUE_ALIGNMENT", "CENTER")?.uppercase()

        val layoutParams = button.layoutParams
        if (layoutParams is FrameLayout.LayoutParams) {
            layoutParams.gravity = when (alignment) {
                "LEFT" -> Gravity.BOTTOM or Gravity.START
                "RIGHT" -> Gravity.BOTTOM or Gravity.END
                else -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            button.layoutParams = layoutParams
        }
    }

    companion object {
        private const val ARG_HTML_FILE = "ARG_HTML_FILE"

        fun newInstance(fileName: String): CustomHtmlFragment {
            val fragment = CustomHtmlFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_HTML_FILE, fileName)
            }
            return fragment
        }
    }
}
