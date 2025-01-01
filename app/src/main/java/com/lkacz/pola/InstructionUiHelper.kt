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

        // Changed from MediaFolderManager(...) to ResourcesFolderManager(...)
        val resourcesFolderUri = ResourcesFolderManager(view.context).getResourcesFolderUri()

        headerTextView.text = HtmlMediaHelper.toSpannedHtml(view.context, resourcesFolderUri, header)
        headerTextView.textSize = FontSizeManager.getHeaderSize(view.context)

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(view.context, resourcesFolderUri, body)
        bodyTextView.textSize = FontSizeManager.getBodySize(view.context)

        if (!nextButtonText.isNullOrEmpty()) {
            nextButton.text = HtmlMediaHelper.toSpannedHtml(view.context, resourcesFolderUri, nextButtonText)
        } else {
            nextButton.text = "Next"
        }
        nextButton.textSize = FontSizeManager.getButtonSize(view.context)

        nextButton.setOnClickListener { onNextClick() }
        return nextButton
    }
}
