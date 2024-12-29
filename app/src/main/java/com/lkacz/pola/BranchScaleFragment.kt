// File: app/src/main/java/com/lkacz/pola/BranchScaleFragment.kt
package com.lkacz.pola

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

/**
 * A specialized fragment for handling BRANCH_SCALE instructions.
 * Each response can optionally have a bracketed label, e.g. "Very negative[Part1]".
 * When clicked, if a bracketed label exists, we jump to that label; otherwise, move on.
 *
 * Revised to also play .mp4 references, similarly to InstructionFragment and others.
 */
class BranchScaleFragment : Fragment() {

    private var header: String? = null
    private var body: String? = null
    private var item: String? = null
    private var branchResponses: List<Pair<String, String?>> = emptyList()
    private lateinit var logger: Logger
    private val selectedResponse = MutableLiveData<String>()

    // For audio playback
    private val mediaPlayers = mutableListOf<MediaPlayer>()
    // For video playback
    private lateinit var videoView: VideoView

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

        // New: VideoView to play .mp4 references if present
        videoView = view.findViewById(R.id.videoView2)

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        // Parse & play audio in header, then check & play .mp4
        val cleanHeader = parseAndPlayAudioIfAny(header ?: "Default Header", mediaFolderUri)
        checkAndPlayMp4(header ?: "Default Header", mediaFolderUri)

        // Parse & play audio in body, then check & play .mp4
        val cleanBody = parseAndPlayAudioIfAny(body ?: "Default Body", mediaFolderUri)
        checkAndPlayMp4(body ?: "Default Body", mediaFolderUri)

        // Parse & play audio in item, then check & play .mp4
        val cleanItem = parseAndPlayAudioIfAny(item ?: "Default Item", mediaFolderUri)
        checkAndPlayMp4(item ?: "Default Item", mediaFolderUri)

        // Set UI text with HTML + user-defined font sizes
        headerTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanHeader)
        headerTextView.textSize = FontSizeManager.getHeaderSize(requireContext())

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())

        itemTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanItem)
        itemTextView.textSize = FontSizeManager.getItemSize(requireContext())

        // Dynamically create buttons for each response
        branchResponses.forEachIndexed { index, (displayText, label) ->
            // Parse & play audio for the display text, then check & play .mp4
            val cleanResponse = parseAndPlayAudioIfAny(displayText, mediaFolderUri)
            checkAndPlayMp4(displayText, mediaFolderUri)

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
            // Add each new button at the top
            buttonContainer.addView(button, 0)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release audio players
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()

        // Stop video playback if playing
        if (this::videoView.isInitialized && videoView.isPlaying) {
            videoView.stopPlayback()
        }
    }

    /**
     * Detects and plays any <filename.mp3[,volume]> markers
     * and returns the cleaned text without those markers.
     */
    private fun parseAndPlayAudioIfAny(text: String, mediaFolderUri: Uri?): String {
        return AudioPlaybackHelper.parseAndPlayAudio(
            context = requireContext(),
            rawText = text,
            mediaFolderUri = mediaFolderUri,
            mediaPlayers = mediaPlayers
        )
    }

    /**
     * Checks for a <filename.mp4[,volume]> marker and plays
     * it in the fragment's [videoView] if found.
     */
    private fun checkAndPlayMp4(text: String, mediaFolderUri: Uri?) {
        val pattern = Regex("<([^>]+\\.mp4(?:,[^>]+)?)>", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return
        val group = match.groupValues[1]

        val segments = group.split(",")
        val fileName = segments[0].trim()
        val volume = if (segments.size > 1) {
            val vol = segments[1].trim().toFloatOrNull()
            if (vol != null && vol in 0f..100f) vol / 100f else 1.0f
        } else 1.0f

        videoView.visibility = View.VISIBLE
        playVideoFile(fileName, volume, mediaFolderUri)
    }

    /**
     * Locates [fileName] in [mediaFolderUri] and plays it in [videoView].
     * Volume control would require a custom MediaPlayer approach.
     */
    private fun playVideoFile(fileName: String, volume: Float, mediaFolderUri: Uri?) {
        if (mediaFolderUri == null) return
        val parentFolder = androidx.documentfile.provider.DocumentFile.fromTreeUri(requireContext(), mediaFolderUri)
            ?: return
        val videoFile = parentFolder.findFile(fileName) ?: return
        if (!videoFile.exists() || !videoFile.isFile) return

        val videoUri = videoFile.uri
        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mp ->
            mp.start()
        }
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
