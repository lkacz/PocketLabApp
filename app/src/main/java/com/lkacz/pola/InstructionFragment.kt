package com.lkacz.pola

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.fragment.app.Fragment

/**
 * Revised to play .mp4 video while retaining HTML logic for displaying images.
 * - Added a [VideoView] (videoView) for .mp4 playback.
 * - Implemented [checkAndPlayMp4] and [playVideoFile] to detect and play .mp4 files.
 * - Ensured HTML-based image loading remains intact via [HtmlMediaHelper].
 */
class InstructionFragment : Fragment() {

    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null

    private lateinit var logger: Logger

    // For video playback
    private lateinit var videoView: VideoView

    // For audio playback (if .mp3 references appear)
    private val mediaPlayers = mutableListOf<MediaPlayer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString("HEADER")
            body = it.getString("BODY")
            nextButtonText = it.getString("NEXT_BUTTON_TEXT")
        }
        logger = Logger.getInstance(requireContext())
        logger.logInstructionFragment(header ?: "Default Header", body ?: "Default Body")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Uses a layout that includes:
        //   - headerTextView (TextView)
        //   - bodyTextView (TextView)
        //   - nextButton (Button)
        //   - videoView2 (VideoView, initially GONE)
        val view = inflater.inflate(R.layout.fragment_instruction, container, false)

        // Reference the VideoView from layout
        videoView = view.findViewById(R.id.videoView2)

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        // Parse out any .mp3 markers and play them (removing from text to display)
        val cleanHeader = parseAndPlayAudioIfAny(header.orEmpty(), mediaFolderUri)
        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), mediaFolderUri)
        val cleanNextText = parseAndPlayAudioIfAny(nextButtonText.orEmpty(), mediaFolderUri)

        // Check for .mp4 references and play them in the VideoView
        checkAndPlayMp4(header.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(body.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(nextButtonText.orEmpty(), mediaFolderUri)

        // Set up the UI using InstructionUiHelper
        InstructionUiHelper.setupInstructionViews(
            view,
            cleanHeader,
            cleanBody,
            cleanNextText
        ) {
            // Move on to the next fragment
            (activity as MainActivity).loadNextFragment()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release any audio players used
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()

        // Stop the VideoView if it’s playing
        if (this::videoView.isInitialized && videoView.isPlaying) {
            videoView.stopPlayback()
        }
    }

    /**
     * Detects and plays <filename.mp3[,volume]> placeholders, returning
     * a text string with those placeholders removed.
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
     * Checks for <filename.mp4[,volume]> markers and plays them in videoView if found.
     * The text is not altered here; the placeholders remain for consistency but
     * will typically be stripped from the displayed text by the audio parse method.
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
     * Locates [fileName] under [mediaFolderUri] and plays it in [videoView].
     * Volume control requires a custom MediaPlayer approach if partial volume is needed.
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
            // Standard VideoView doesn't expose setVolume
            mp.start()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(header: String?, body: String?, nextButtonText: String?) =
            InstructionFragment().apply {
                arguments = Bundle().apply {
                    putString("HEADER", header)
                    putString("BODY", body)
                    putString("NEXT_BUTTON_TEXT", nextButtonText)
                }
            }
    }
}
