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

/**
 * Replaced 'introduction' with 'body' for naming consistency.
 */
class ScaleFragment : Fragment() {
    private var header: String? = null
    private var body: String? = null
    private var item: String? = null
    private var responses: List<String>? = null
    private lateinit var logger: Logger
    private val selectedResponse = MutableLiveData<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString("HEADER")
            body = it.getString("BODY")
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
        val bodyTextView: TextView = view.findViewById(R.id.introductionTextView) // Renamed in layout file to bodyTextView for clarity
        val itemTextView: TextView = view.findViewById(R.id.itemTextView)
        val buttonContainer: LinearLayout = view.findViewById(R.id.buttonContainer)

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        // Apply persisted font sizes
        headerTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, header ?: "Default Header")
        headerTextView.textSize = FontSizeManager.getHeaderSize(requireContext())

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, body ?: "Default Body")
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())

        itemTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, item ?: "Default Item")
        itemTextView.textSize = FontSizeManager.getItemSize(requireContext())

        responses?.forEachIndexed { index, response ->
            val button = Button(context).apply {
                text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, response)
                textSize = FontSizeManager.getResponseSize(requireContext())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    selectedResponse.value = response
                    logger.logScaleFragment(
                        header ?: "Default Header",
                        body ?: "Default Body",
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
            body: String?,
            item: String?,
            responses: List<String>?
        ) = ScaleFragment().apply {
            arguments = Bundle().apply {
                putString("HEADER", header)
                putString("BODY", body)
                putString("ITEM", item)
                putStringArrayList("RESPONSES", ArrayList(responses ?: emptyList()))
            }
        }
    }
}
