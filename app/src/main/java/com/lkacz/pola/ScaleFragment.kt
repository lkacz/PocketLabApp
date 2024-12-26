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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scale, container, false)

        val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        val introductionTextView: TextView = view.findViewById(R.id.introductionTextView)
        val itemTextView: TextView = view.findViewById(R.id.itemTextView)
        val buttonContainer: LinearLayout = view.findViewById(R.id.buttonContainer)

        // Retrieve the media folder URI to load images if <img> tags exist
        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        // Render HTML with image loading for header, intro, and item
        headerTextView.text = HtmlImageLoader.getSpannedFromHtml(
            requireContext(),
            mediaFolderUri,
            header ?: "Default Header"
        )

        introductionTextView.text = HtmlImageLoader.getSpannedFromHtml(
            requireContext(),
            mediaFolderUri,
            introduction ?: "Default Introduction"
        )

        itemTextView.text = HtmlImageLoader.getSpannedFromHtml(
            requireContext(),
            mediaFolderUri,
            item ?: "Default Item"
        )

        // Generate response buttons
        responses?.forEachIndexed { index, response ->
            val button = Button(context).apply {
                text = HtmlImageLoader.getSpannedFromHtml(
                    requireContext(),
                    mediaFolderUri,
                    response
                )
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
            buttonContainer.addView(button, 0)
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
