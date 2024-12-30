// Filename: ScaleFragment.kt
package com.lkacz.pola

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class ScaleFragment : Fragment() {

    private var header: String? = null
    private var body: String? = null
    private var item: String? = null
    private var responses: List<String>? = null
    private lateinit var logger: Logger
    private val selectedResponse = MutableLiveData<String>()

    private val mediaPlayers = mutableListOf<MediaPlayer>()
    private lateinit var videoView: VideoView

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_scale, container, false)

        // Apply background color
        view.setBackgroundColor(ColorManager.getScreenBackgroundColor(requireContext()))

        val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        val bodyTextView: TextView = view.findViewById(R.id.introductionTextView)
        val itemTextView: TextView = view.findViewById(R.id.itemTextView)
        val buttonContainer: LinearLayout = view.findViewById(R.id.buttonContainer)
        videoView = view.findViewById(R.id.videoView2)

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        val cleanHeader = parseAndPlayAudioIfAny(header.orEmpty(), mediaFolderUri)
        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), mediaFolderUri)
        val cleanItem = parseAndPlayAudioIfAny(item.orEmpty(), mediaFolderUri)

        checkAndPlayMp4(header.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(body.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(item.orEmpty(), mediaFolderUri)

        headerTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanHeader)
        headerTextView.textSize = FontSizeManager.getHeaderSize(requireContext())
        headerTextView.setTextColor(ColorManager.getHeaderTextColor(requireContext()))

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())
        bodyTextView.setTextColor(ColorManager.getBodyTextColor(requireContext()))

        itemTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanItem)
        itemTextView.textSize = FontSizeManager.getItemSize(requireContext())
        itemTextView.setTextColor(ColorManager.getItemTextColor(requireContext()))

        // Retrieve user-selected padding (dp), convert to px
        val userPaddingDp = SpacingManager.getResponseButtonPadding(requireContext())
        val scale = resources.displayMetrics.density
        val userPaddingPx = (userPaddingDp * scale + 0.5f).toInt()

        responses?.forEachIndexed { index, response ->
            val buttonText = parseAndPlayAudioIfAny(response, mediaFolderUri)
            checkAndPlayMp4(response, mediaFolderUri)

            val button = Button(context).apply {
                text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, buttonText)
                textSize = FontSizeManager.getResponseSize(requireContext())
                setTextColor(ColorManager.getResponseTextColor(requireContext()))
                setBackgroundColor(ColorManager.getButtonBackgroundColor(requireContext()))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(userPaddingPx, userPaddingPx, userPaddingPx, userPaddingPx)
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()
        if (this::videoView.isInitialized && videoView.isPlaying) {
            videoView.stopPlayback()
        }
    }

    private fun parseAndPlayAudioIfAny(text: String, mediaFolderUri: Uri?): String {
        return AudioPlaybackHelper.parseAndPlayAudio(
            context = requireContext(),
            rawText = text,
            mediaFolderUri = mediaFolderUri,
            mediaPlayers = mediaPlayers
        )
    }

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
