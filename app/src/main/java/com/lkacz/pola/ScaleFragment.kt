package com.lkacz.pola

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class ScaleFragment : Fragment() {
    private var header: String? = null
    private var introduction: String? = null
    private var item: String? = null
    private var responses: List<String>? = null
    private lateinit var logger: Logger
    private val selectedResponse = MutableLiveData<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString("HEADER")
            introduction = it.getString("INTRODUCTION")
            item = it.getString("ITEM")
            responses = it.getStringArrayList("RESPONSES")
        }
        logger = Logger.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scale, container, false)

        val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        val introductionTextView: TextView = view.findViewById(R.id.introductionTextView)
        val itemTextView: TextView = view.findViewById(R.id.itemTextView)

        // Enable HTML formatting for header, introduction, and item
        headerTextView.text = HtmlCompat.fromHtml(
            header ?: "Default Header",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        introductionTextView.text = HtmlCompat.fromHtml(
            introduction ?: "Default Introduction",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        itemTextView.text = HtmlCompat.fromHtml(
            item ?: "Default Item",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        responses?.forEachIndexed { index, response ->
            val button = Button(context).apply {
                // Preserve HTML formatting for response text
                text = HtmlCompat.fromHtml(response, HtmlCompat.FROM_HTML_MODE_LEGACY)
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 0)
                }
                setOnClickListener {
                    selectedResponse.value = response
                    logger.logScaleFragment(
                        header ?: "Default Header",
                        introduction ?: "Default Introduction",
                        item ?: "Default Item",
                        index + 1,
                        response
                    )
                    (activity as MainActivity).loadNextFragment()
                }
            }

            // Insert the button at the top of the container
            view.findViewById<LinearLayout>(R.id.buttonContainer).addView(button, 0)
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(
            header: String?,
            introduction: String?,
            item: String?,
            responses: List<String>?
        ) = ScaleFragment().apply {
            arguments = Bundle().apply {
                putString("HEADER", header)
                putString("INTRODUCTION", introduction)
                putString("ITEM", item)
                putStringArrayList("RESPONSES", ArrayList(responses ?: emptyList()))
            }
        }
    }
}
