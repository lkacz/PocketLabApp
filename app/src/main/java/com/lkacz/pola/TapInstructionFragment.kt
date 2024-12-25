package com.lkacz.pola

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

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

        // Reuse the UI setup helper but initially hide the Next button.
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
     * Once the user has tapped enough times, reveal the 'Next' button
     * and log that the threshold was reached.
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
