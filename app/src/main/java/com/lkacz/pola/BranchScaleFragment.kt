package com.lkacz.pola

import android.media.MediaPlayer
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
 * A specialized fragment for handling BRANCH_SCALE instructions.
 * Each response can optionally have a bracketed label, e.g. "Very negative[Part1]".
 * When clicked, if a bracketed label exists, we jump to that label; otherwise, move on.
 */
class BranchScaleFragment : Fragment() {

    private var header: String? = null
    private var body: String? = null
    private var item: String? = null
    private var branchResponses: List<Pair<String, String?>> = emptyList()
    private lateinit var logger: Logger
    private val selectedResponse = MutableLiveData<String>()

    private val mediaPlayers = mutableListOf<MediaPlayer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString(ARG_HEADER)
            body = it.getString(ARG_BODY)
            item = it.getString(ARG_ITEM)
            val rawList = it.getStringArrayList(ARG_BRANCH_RESPONSES)
            branchResponses = rawList?.map { raw ->
                val split = raw.split("||")
                val display = split.getOrNull(0) ?: ""
                val lbl = split.getOrNull(1)?.ifEmpty { null }
                display to lbl
            } ?: emptyList()
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
        val bodyTextView: TextView = view.findViewById(R.id.introductionTextView)
        val itemTextView: TextView = view.findViewById(R.id.itemTextView)
        val buttonContainer: LinearLayout = view.findViewById(R.id.buttonContainer)

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        // Parse & play audio references in header/body/item
        val cleanHeader = AudioPlaybackHelper.parseAndPlayAudio(
            context = requireContext(),
            rawText = header ?: "Default Header",
            mediaFolderUri = mediaFolderUri,
            mediaPlayers = mediaPlayers
        )
        val cleanBody = AudioPlaybackHelper.parseAndPlayAudio(
            context = requireContext(),
            rawText = body ?: "Default Body",
            mediaFolderUri = mediaFolderUri,
            mediaPlayers = mediaPlayers
        )
        val cleanItem = AudioPlaybackHelper.parseAndPlayAudio(
            context = requireContext(),
            rawText = item ?: "Default Item",
            mediaFolderUri = mediaFolderUri,
            mediaPlayers = mediaPlayers
        )

        headerTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanHeader)
        headerTextView.textSize = FontSizeManager.getHeaderSize(requireContext())

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())

        itemTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanItem)
        itemTextView.textSize = FontSizeManager.getItemSize(requireContext())

        // Dynamically add buttons
        branchResponses.forEachIndexed { index, (displayText, label) ->
            val cleanResponse = AudioPlaybackHelper.parseAndPlayAudio(
                context = requireContext(),
                rawText = displayText,
                mediaFolderUri = mediaFolderUri,
                mediaPlayers = mediaPlayers
            )
            val button = Button(context).apply {
                text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanResponse)
                textSize = FontSizeManager.getResponseSize(requireContext())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    selectedResponse.value = displayText
                    logger.logScaleFragment(
                        header ?: "Default Header",
                        body ?: "Default Body",
                        item ?: "Default Item",
                        index + 1,
                        displayText
                    )
                    val mainActivity = activity as? MainActivity
                    if (!label.isNullOrEmpty()) {
                        mainActivity?.loadFragmentByLabel(label)
                    } else {
                        mainActivity?.loadNextFragment()
                    }
                }
            }
            buttonContainer.addView(button, 0)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()
    }

    companion object {
        private const val ARG_HEADER = "branchScaleHeader"
        private const val ARG_BODY = "branchScaleBody"
        private const val ARG_ITEM = "branchScaleItem"
        private const val ARG_BRANCH_RESPONSES = "branchScaleResponses"

        @JvmStatic
        fun newInstance(
            header: String?,
            body: String?,
            item: String?,
            responses: List<Pair<String, String?>>
        ) = BranchScaleFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_HEADER, header)
                putString(ARG_BODY, body)
                putString(ARG_ITEM, item)
                val rawList = responses.map { "${it.first}||${it.second ?: ""}" }
                putStringArrayList(ARG_BRANCH_RESPONSES, ArrayList(rawList))
            }
        }
    }
}
