package com.lkacz.pola

import android.view.View
import android.widget.Button
import android.widget.TextView

object InstructionUiHelper {

    /**
     * Sets up the instruction fragment UI elements: header, body, and next button.
     *
     * @param view The inflated layout's root View.
     * @param header The text for the header TextView.
     * @param body The text for the body TextView.
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
        bodyTextView.text = body
        nextButton.text = nextButtonText ?: "Next"

        nextButton.setOnClickListener { onNextClick() }
        return nextButton
    }
}
