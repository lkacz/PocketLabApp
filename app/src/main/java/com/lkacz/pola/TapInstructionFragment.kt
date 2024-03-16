package com.lkacz.pola

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class TapInstructionFragment : Fragment() {
    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null
    private lateinit var logger: Logger
    private val touchCounter = TouchCounter(1000, 3)  // Initialize TouchCounter with a 2s reset time and 5 touch threshold

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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_instruction, container, false)

        // Populate header and body text
        val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        val bodyTextView: TextView = view.findViewById(R.id.bodyTextView)
        val nextButton: Button = view.findViewById(R.id.nextButton)

        headerTextView.text = header ?: "Default Header"
        bodyTextView.text = body ?: "Default Body"
        nextButton.text = nextButtonText ?: "Next"
        nextButton.visibility = View.INVISIBLE // Hide the button initially

        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (touchCounter.onTouch()) {
                    nextButton.visibility = View.VISIBLE  // Show the "Next" button when touch threshold is reached
                }
                v.performClick()  // Indicate that the view was clicked
            }
            true
        }

        nextButton.setOnClickListener {
            // Add logic to move to the next fragment
            (activity as MainActivity).loadNextFragment()
        }

        return view
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
