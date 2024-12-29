// File: app/src/main/java/com/lkacz/pola/InstructionFragment.kt
package com.lkacz.pola

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import androidx.fragment.app.Fragment

class InstructionFragment : Fragment() {

    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null
    private lateinit var logger: Logger

    private lateinit var videoView: VideoView
    private val mediaPlayers = mutableListOf<MediaPlayer>() // For .mp3 playback

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

        val headerTextView = view.findViewById<android.widget.TextView>(R.id.headerTextView)
        val bodyTextView = view.findViewById<android.widget.TextView>(R.id.bodyTextView)
        val nextButton = view.findViewById<android.widget.Button>(R.id.nextButton)
        videoView = view.findViewById(R.id.videoView2)

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        // Parse out .mp3 and play with MediaPlayer
        val cleanHeader = parseAndPlayAudioIfAny(header.orEmpty(), mediaFolderUri)
        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), mediaFolderUri)
        val cleanNextText = parseAndPlayAudioIfAny(nextButtonText.orEmpty(), mediaFolderUri)

        // Check for .mp4 references and show them in VideoView
        checkAndPlayMp4(header.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(body.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(nextButtonText.orEmpty(), mediaFolderUri)

        // Apply HTML + user font sizes
        headerTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanHeader)
        headerTextView.textSize = FontSizeManager.getHeaderSize(requireContext())

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())

        nextButton.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanNextText)
        nextButton.textSize = FontSizeManager.getButtonSize(requireContext())

        nextButton.setOnClickListener {
            (activity as MainActivity).loadNextFragment()
        }

        return view
    }

    /**
     * Uses AudioPlaybackHelper to detect <sound.mp3[,volume]> placeholders,
     * play them, and strip them from the returned string for display.
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
     * If we detect <something.mp4[,volume]>, we show the video in VideoView and start playback.
     * The volume param (if present) is recognized but not directly modifiable via VideoView.
     */
    private fun checkAndPlayMp4(text: String, mediaFolderUri: Uri?) {
        val pattern = Regex("<([^>]+\\.mp4(?:,[^>]+)?)>", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return
        val group = match.groupValues[1]
        val segments = group.split(",")
        val fileName = segments[0].trim()
        // Volume is read but in standard VideoView usage, there's no direct .setVolume API.
        // Could implement custom MediaPlayer if precise volume control is required.
        // We keep the structure here for consistency with audio tags.
        val volume = if (segments.size > 1) {
            val vol = segments[1].trim().toFloatOrNull()
            if (vol != null && vol in 0f..100f) vol / 100f else 1.0f
        } else 1.0f

        videoView.visibility = View.VISIBLE
        playVideoFile(fileName, volume, mediaFolderUri)
    }

    /**
     * Locates [fileName] in [mediaFolderUri], sets up VideoView with that URI,
     * and starts playback. The layout can be set to match_parent or desired size.
     */
    private fun playVideoFile(fileName: String, volume: Float, mediaFolderUri: Uri?) {
        if (mediaFolderUri == null) return
        val parentFolder = androidx.documentfile.provider.DocumentFile.fromTreeUri(
            requireContext(),
            mediaFolderUri
        ) ?: return
        val videoFile = parentFolder.findFile(fileName) ?: return
        if (!videoFile.exists() || !videoFile.isFile) return

        val videoUri = videoFile.uri
        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mp ->
            // Standard VideoView lacks direct volume; custom MediaPlayer logic needed for advanced control
            mp.start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release any audio players used
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()

        // Stop the VideoView if it’s playing
        if (videoView.isPlaying) {
            videoView.stopPlayback()
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
