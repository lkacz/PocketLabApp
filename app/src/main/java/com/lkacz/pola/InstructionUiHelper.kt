package com.lkacz.pola

import android.view.View
import android.widget.Button
import android.widget.TextView

/**
 * Sets up the instruction fragment UI elements: header, body, and next button.
 * Uses [HtmlMediaHelper] so that <img> tags in HTML can render from user-selected media folder
 * and properly scale width and height if provided.
 */
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

        // Retrieve the URI of the user-selected media folder
        val mediaFolderUri = MediaFolderManager(view.context).getMediaFolderUri()

        // Render possible <img> tags in header and body using HtmlMediaHelper
        headerTextView.text = HtmlMediaHelper.toSpannedHtml(
            view.context,
            mediaFolderUri,
            header
        )

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(
            view.context,
            mediaFolderUri,
            body
        )

        // Apply HTML image loading to the button text if provided
        nextButton.text = if (!nextButtonText.isNullOrEmpty()) {
            HtmlMediaHelper.toSpannedHtml(
                view.context,
                mediaFolderUri,
                nextButtonText
            )
        } else {
            "Next"
        }

        nextButton.setOnClickListener { onNextClick() }
        return nextButton
    }
}
