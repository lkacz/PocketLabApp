package com.lkacz.pola

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment

class InputFieldFragment : Fragment() {
    private var heading: String? = null
    private var body: String? = null
    private var buttonName: String? = null
    private var inputFields: List<String>? = null
    private lateinit var logger: Logger
    private val fieldValues = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            heading = it.getString("HEADING")
            body = it.getString("TEXT") // Retaining arg key "TEXT" for backward compatibility
            buttonName = it.getString("BUTTON")
            inputFields = it.getStringArrayList("INPUTFIELDS")
        }
        logger = Logger.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_input_field, container, false)

        val headingTextView: TextView = view.findViewById(R.id.headingTextView)
        val bodyTextView: TextView = view.findViewById(R.id.textTextView)
        val containerLayout: LinearLayout = view.findViewById(R.id.inputFieldContainer)

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        headingTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, heading ?: "Default Heading")
        headingTextView.textSize = FontSizeManager.getHeaderSize(requireContext())

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, body ?: "Default Body")
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())

        inputFields?.forEach { field ->
            val editText = EditText(context).apply {
                hint = field
                // This could be considered an "item," but applying body-size for consistency
                textSize = FontSizeManager.getBodySize(requireContext())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            editText.addTextChangedListener { fieldValues[field] = it.toString() }
            fieldValues[field] = ""
            containerLayout.addView(editText)
        }

        val nextButton = Button(context).apply {
            text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, buttonName ?: "Next")
            textSize = FontSizeManager.getButtonSize(requireContext())
            setOnClickListener {
                fieldValues.forEach { (field, value) ->
                    val isNumeric = value.toDoubleOrNull() != null
                    logger.logInputFieldFragment(
                        heading ?: "Default Heading",
                        body ?: "Default Body",
                        field,
                        value,
                        isNumeric
                    )
                }
                (activity as MainActivity).loadNextFragment()
            }
        }
        containerLayout.addView(nextButton)

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
