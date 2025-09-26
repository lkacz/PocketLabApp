// Filename: HtmlDialogHelper.kt
package com.lkacz.pola

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog

object HtmlDialogHelper {
    @SuppressLint("SetJavaScriptEnabled")
    fun showHtmlContent(
        context: Context,
        title: String,
        htmlContent: String,
    ) {
        val webView =
            WebView(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                setBackgroundColor(Color.TRANSPARENT)
                isHorizontalScrollBarEnabled = false
                settings.apply {
                    javaScriptEnabled = false
                    domStorageEnabled = false
                    builtInZoomControls = true
                    displayZoomControls = false
                    loadsImagesAutomatically = true
                }
            }

        webView.webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val uri = request.url
                    return handleExternalNavigation(context, uri)
                }

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    return handleExternalNavigation(context, Uri.parse(url))
                }

                private fun handleExternalNavigation(
                    context: Context,
                    uri: Uri,
                ): Boolean {
                    return when (uri.scheme?.lowercase()) {
                        "http", "https", "mailto" -> {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            true
                        }
                        else -> false
                    }
                }
            }

        webView.loadDataWithBaseURL(
            "file:///android_asset/",
            htmlContent,
            "text/html",
            "utf-8",
            null,
        )

        AlertDialog.Builder(context).apply {
            setTitle(title)
            setView(webView)
            setPositiveButton("OK", null)
            show()
        }
    }
}
