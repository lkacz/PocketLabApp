package com.lkacz.pola

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

/**
 * A fragment that displays instructions requiring taps before revealing the 'Next' button.
 * Taps are counted, and once threshold is reached, the button is displayed.
 * HTML formatting is preserved via HtmlUtils.parseHtml().
 */
class TapInstructionFragment : BaseTouchAwareFragment(1000, 3) {

    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null
    private lateinit var logger: Logger
    private var nextButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString("HEADER")
            body = it.getString("BODY")
            nextButtonText = it.getString("NEXT_BUTTON_TEXT")
        }
        logger = Logger.getInstance(requireContext())
        logger.logInstructionFragment(header ?: "Default Header", body ?: "Default Body")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_instruction, container, false)

        // Reuse the same UI setup helper, which applies HTML parsing.
        nextButton = InstructionUiHelper.setupInstructionViews(
            view,
            header ?: "Default Header",
            body ?: "Default Body",
            nextButtonText
        ) {
            (activity as MainActivity).loadNextFragment()
        }
        nextButton?.visibility = View.INVISIBLE
        return view
    }

    /**
     * Reveals the 'Next' button once the tap threshold is reached.
     */
    override fun onTouchThresholdReached() {
        logger.logOther("Tap threshold reached in TapInstructionFragment")
        nextButton?.visibility = View.VISIBLE
    }

    companion object {
        @JvmStatic
        fun newInstance(header: String?, body: String?, nextButtonText: String?) =
            TapInstructionFragment().apply {
                arguments = Bundle().apply {
                    putString("HEADER", header)
                    putString("BODY", body)
                    putString("NEXT_BUTTON_TEXT", nextButtonText)
                }
            }
    }
}
