// File: app/src/main/java/com/lkacz/pola/TapInstructionFragment.kt
package com.lkacz.pola

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.VideoView

/**
 * A fragment where the user must tap the screen a certain number of times (threshold)
 * to reveal the "Next" button. Revised to also support .mp4 playback.
 */
class TapInstructionFragment : BaseTouchAwareFragment(1000, 3) {

    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null
    private lateinit var logger: Logger
    private var nextButton: Button? = null

    // For audio playback
    private val mediaPlayers = mutableListOf<MediaPlayer>()
    // For video playback
    private lateinit var videoView: VideoView

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
        val view = inflater.inflate(R.layout.fragment_instruction, container, false)

        // Retrieve any user-selected media folder
        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        // For video playback
        videoView = view.findViewById(R.id.videoView2)

        // Parse & play audio in header/body/next button text, then check & play .mp4 references
        val cleanHeader = parseAndPlayAudioIfAny(header.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(header.orEmpty(), mediaFolderUri)

        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(body.orEmpty(), mediaFolderUri)

        val cleanNextButton = parseAndPlayAudioIfAny(nextButtonText.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(nextButtonText.orEmpty(), mediaFolderUri)

        // Use a helper to set up UI text and next button
        nextButton = InstructionUiHelper.setupInstructionViews(
            view,
            cleanHeader,
            cleanBody,
            cleanNextButton
        ) {
            // Next fragment loads only after user taps enough times
            (activity as MainActivity).loadNextFragment()
        }
        nextButton?.visibility = View.INVISIBLE
        return view
    }

    override fun onTouchThresholdReached() {
        logger.logOther("Tap threshold reached in TapInstructionFragment")
        // Reveal the Next button once threshold taps occur
        nextButton?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release audio players
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()

        // Stop video if playing
        if (this::videoView.isInitialized && videoView.isPlaying) {
            videoView.stopPlayback()
        }
    }

    /**
     * Detects and plays any <filename.mp3[,volume]> placeholders,
     * returning the original text with these markers removed.
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
     * Checks for <filename.mp4[,volume]> markers and plays them in [videoView] if found.
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
     * Full volume control would require a custom MediaPlayer.
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
            // Volume not directly adjustable via VideoView
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(header: String?, body: String?, nextButtonText: String?) =
            TapInstructionFragment().apply {
                arguments = Bundle().apply {
                    putString("HEADER", header)
                    putString("BODY", body)
                    putString("NEXT_BUTTON_TEXT", nextButtonText)
                }
            }
    }
}
