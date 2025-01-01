// Filename: CustomHtmlFragment.kt
package com.lkacz.pola

import android.media.MediaPlayer
import android.os.Bundle
import android.view.*
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import android.widget.LinearLayout
import android.widget.Toast

/**
 * Fragment that loads a custom HTML/JS file from the selected resources folder.
 * Once done, user can click a "Continue" button to proceed.
 */
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
        // Use a FrameLayout to position the button bottom-right elegantly
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

        // Create a 'continue' button with styling similar to other fragments
        val continueButton = Button(requireContext()).apply {
            text = "Continue"
            textSize = FontSizeManager.getButtonSize(requireContext())
            setTextColor(ColorManager.getButtonTextColor(requireContext()))
            setBackgroundColor(ColorManager.getButtonBackgroundColor(requireContext()))

            val density = resources.displayMetrics.density
            val marginPx = (16 * density + 0.5f).toInt()

            // Position bottom-right with some padding
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply {
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
            layoutParams = params

            setOnClickListener {
                (activity as? MainActivity)?.loadNextFragment()
            }
        }
        frameLayout.addView(continueButton)
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
        // Enable JavaScript, DOM storage, allow mixed content, etc.
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Provide a WebViewClient so that navigation/iframe loads happen inside this WebView
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
