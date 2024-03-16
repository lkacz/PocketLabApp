package com.lkacz.pola

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scale, container, false)

        val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        val introductionTextView: TextView = view.findViewById(R.id.introductionTextView)
        val itemTextView: TextView = view.findViewById(R.id.itemTextView)

        headerTextView.text = header ?: "Default Header"
        introductionTextView.text = introduction ?: "Default Introduction"
        itemTextView.text = item ?: "Default Item"

        // Dynamically populate response buttons
        responses?.forEachIndexed { index, response ->
            val button = Button(context)
            button.text = response
            button.textSize = 12f

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 0)  // Adjust margins as needed
            button.setPadding(0, 0, 0, 0)
            button.layoutParams = params

            button.setOnClickListener {
                selectedResponse.value = response
                // Log Button Click with selected response
                logger.logScaleFragment(
                    header ?: "Default Header",
                    introduction ?: "Default Introduction",
                    item ?: "Default Item",
                    index + 1,
                    response
                )
                (activity as MainActivity).loadNextFragment()
            }
            view.findViewById<LinearLayout>(R.id.buttonContainer).addView(button, 0)
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(header: String?, introduction: String?, item: String?, responses: List<String>?) =
            ScaleFragment().apply {
                arguments = Bundle().apply {
                    putString("HEADER", header)
                    putString("INTRODUCTION", introduction)
                    putString("ITEM", item)
                    putStringArrayList("RESPONSES", ArrayList(responses ?: emptyList()))
                }
            }
    }
}
