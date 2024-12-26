package com.lkacz.pola

import android.view.View
import android.widget.Button
import android.widget.TextView

object InstructionUiHelper {

    fun setupInstructionViews(
        view: View,
        header: String,
        body: String,
        nextButtonText: String?,
        onNextClick: () -> Unit
    ): Button {

        val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        val bodyTextView: TextView = view.findViewById(R.id.bodyTextView)
        val nextButton: Button = view.findViewById(R.id.nextButton)

        val mediaFolderUri = MediaFolderManager(view.context).getMediaFolderUri()

        headerTextView.text = HtmlMediaHelper.toSpannedHtml(view.context, mediaFolderUri, header)
        // Apply user-defined font sizes
        headerTextView.textSize = FontSizeManager.getHeaderSize(view.context)

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(view.context, mediaFolderUri, body)
        bodyTextView.textSize = FontSizeManager.getBodySize(view.context)

        nextButton.text = if (!nextButtonText.isNullOrEmpty()) {
            HtmlMediaHelper.toSpannedHtml(view.context, mediaFolderUri, nextButtonText)
        } else {
            "Next"
        }
        nextButton.textSize = FontSizeManager.getButtonSize(view.context)

        nextButton.setOnClickListener { onNextClick() }
        return nextButton
    }
}
