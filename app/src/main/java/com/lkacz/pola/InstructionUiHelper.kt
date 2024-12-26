package com.lkacz.pola

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.text.HtmlCompat

/**
 * Sets up the instruction fragment UI elements: header, body, and next button.
 * Uses [HtmlImageLoader] so that <img> tags in HTML can render from user-selected media folder.
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

        // Render possible <img> tags in header and body
        headerTextView.text = HtmlImageLoader.getSpannedFromHtml(
            view.context,
            mediaFolderUri,
            header
        )

        bodyTextView.text = HtmlImageLoader.getSpannedFromHtml(
            view.context,
            mediaFolderUri,
            body
        )

        // Apply HTML image loading to the button text if provided
        nextButton.text = if (!nextButtonText.isNullOrEmpty()) {
            HtmlImageLoader.getSpannedFromHtml(
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
