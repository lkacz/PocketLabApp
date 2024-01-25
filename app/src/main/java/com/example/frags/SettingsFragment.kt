package com.example.frags

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

class SettingsFragment : Fragment() {
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
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val headingTextView: TextView = view.findViewById(R.id.headingTextView)
        val textTextView: TextView = view.findViewById(R.id.textTextView)

        headingTextView.text = heading ?: "Default Heading"
        textTextView.text = text ?: "Default Text"

        inputFields?.forEach { field ->
            val editText = EditText(context)
            editText.hint = field
            editText.textSize = 14f

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 0)
            editText.layoutParams = params

            fieldValues[field] = ""

            editText.addTextChangedListener {
                fieldValues[field] = it.toString()
            }

            view.findViewById<LinearLayout>(R.id.inputFieldContainer).addView(editText)
        }

        val nextButton = Button(context)
        nextButton.text = buttonName ?: "Next"
        nextButton.textSize = 16f
        nextButton.setOnClickListener {
            fieldValues.forEach { (field, value) ->
                val isNumeric = value.toDoubleOrNull() != null
                logger.logSettingsFragment(
                    heading ?: "Default Heading",
                    text ?: "Default Text",
                    field,
                    value,
                    isNumeric
                )
            }
            (activity as MainActivity).loadNextFragment()
        }

        view.findViewById<LinearLayout>(R.id.inputFieldContainer).addView(nextButton)

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(heading: String?, text: String?, buttonName: String?, inputFields: List<String>?) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString("HEADING", heading)
                    putString("TEXT", text)
                    putString("BUTTON", buttonName)
                    putStringArrayList("INPUTFIELDS", ArrayList(inputFields ?: emptyList()))
                }
            }
    }
}
