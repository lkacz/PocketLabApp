package com.lkacz.pola

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.text.HtmlCompat

object InstructionUiHelper {

    /**
     * Sets up the instruction fragment UI elements: header, body, and next button.
     *
     * @param view The inflated layout's root View.
     * @param header The text for the header TextView.
     * @param body The text for the body TextView, which may contain HTML formatting (e.g. <b></b>).
     * @param nextButtonText Optional custom text for the button. Defaults to "Next" if null.
     * @param onNextClick Action to be performed when the next button is clicked.
     *
     * @return The nextButton for further manipulation if needed (e.g., changing visibility).
     */
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

        headerTextView.text = header
        // CHANGED: Use HtmlCompat.fromHtml to display HTML-formatted text without sanitizing.
        bodyTextView.text = HtmlCompat.fromHtml(body, HtmlCompat.FROM_HTML_MODE_LEGACY)

        nextButton.text = nextButtonText ?: "Next"
        nextButton.setOnClickListener { onNextClick() }
        return nextButton
    }
}
