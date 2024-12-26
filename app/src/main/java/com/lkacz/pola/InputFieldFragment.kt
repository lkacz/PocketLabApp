package com.lkacz.pola

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment

class InputFieldFragment : Fragment() {
    private var heading: String? = null
    private var text: String? = null
    private var buttonName: String? = null
    private var inputFields: List<String>? = null
    private lateinit var logger: Logger
    private val fieldValues = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            heading = it.getString("HEADING")
            text = it.getString("TEXT")
            buttonName = it.getString("BUTTON")
            inputFields = it.getStringArrayList("INPUTFIELDS")
        }
        logger = Logger.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_input_field, container, false)

        val headingTextView: TextView = view.findViewById(R.id.headingTextView)
        val textTextView: TextView = view.findViewById(R.id.textTextView)

        // Enable HTML formatting for heading and body texts
        headingTextView.text = HtmlCompat.fromHtml(
            heading ?: "Default Heading",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        textTextView.text = HtmlCompat.fromHtml(
            text ?: "Default Text",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        // Dynamically create input fields
        inputFields?.forEach { field ->
            val editText = EditText(context).apply {
                hint = field
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                // Track text changes
                addTextChangedListener { fieldValues[field] = it.toString() }
            }

            fieldValues[field] = ""
            view.findViewById<LinearLayout>(R.id.inputFieldContainer).addView(editText)
        }

        // Create Next button with HTML text support
        val nextButton = Button(context).apply {
            text = HtmlCompat.fromHtml(buttonName ?: "Next", HtmlCompat.FROM_HTML_MODE_LEGACY)
            textSize = 16f
            setOnClickListener {
                fieldValues.forEach { (field, value) ->
                    val isNumeric = value.toDoubleOrNull() != null
                    logger.logInputFieldFragment(
                        heading ?: "Default Heading",
                        (text ?: "Default Text").toString(),
                        field,
                        value,
                        isNumeric
                    )
                }
                (activity as MainActivity).loadNextFragment()
            }
        }
        view.findViewById<LinearLayout>(R.id.inputFieldContainer).addView(nextButton)

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(
            heading: String?,
            text: String?,
            buttonName: String?,
            inputFields: List<String>?
        ) = InputFieldFragment().apply {
            arguments = Bundle().apply {
                putString("HEADING", heading)
                putString("TEXT", text)
                putString("BUTTON", buttonName)
                putStringArrayList("INPUTFIELDS", ArrayList(inputFields ?: emptyList()))
            }
        }
    }
}
