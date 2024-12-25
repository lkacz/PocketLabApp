package com.lkacz.pola

import android.view.View
import android.widget.Button
import android.widget.TextView

object InstructionUiHelper {

    /**
     * Sets up the instruction fragment UI elements: header, body, and next button.
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

        // Allow HTML rendering
        headerTextView.text = HtmlUtils.parseHtml(header)
        bodyTextView.text = HtmlUtils.parseHtml(body)
        nextButton.text = nextButtonText ?: "Next"

        nextButton.setOnClickListener { onNextClick() }
        return nextButton
    }
}
