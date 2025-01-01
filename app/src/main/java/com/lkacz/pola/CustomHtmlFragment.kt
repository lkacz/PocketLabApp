// Filename: CustomHtmlFragment.kt
package com.lkacz.pola

import android.media.MediaPlayer
import android.os.Bundle
import android.view.*
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment

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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        webView = WebView(requireContext())
        webView.visibility = View.GONE

        val scaleVal = resources.displayMetrics.density
        val marginPx = (16 * scaleVal + 0.5f).toInt()
        val webViewParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        webViewParams.setMargins(0, marginPx, 0, marginPx)
        webView.layoutParams = webViewParams

        layout.addView(webView)

        val continueButton = Button(requireContext()).apply {
            text = "Continue"
            setOnClickListener {
                (activity as? MainActivity)?.loadNextFragment()
            }
        }
        layout.addView(
            continueButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        return layout
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
        webView.webChromeClient = WebChromeClient()
    }

    private fun loadCustomHtml() {
        val resourcesUri = ResourcesFolderManager(requireContext()).getResourcesFolderUri()
        if (resourcesUri == null || fileName.isNullOrBlank()) {
            webView.loadData("<html><body><h2>File not found or invalid name.</h2></body></html>", "text/html", "UTF-8")
            return
        }

        val folder = DocumentFile.fromTreeUri(requireContext(), resourcesUri) ?: return
        val htmlFile = folder.findFile(fileName!!)
        if (htmlFile == null || !htmlFile.exists() || !htmlFile.isFile) {
            webView.loadData("<html><body><h2>HTML file not found in resources folder.</h2></body></html>", "text/html", "UTF-8")
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
            webView.loadData("<html><body><h2>Error loading file: ${e.message}</h2></body></html>", "text/html", "UTF-8")
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
