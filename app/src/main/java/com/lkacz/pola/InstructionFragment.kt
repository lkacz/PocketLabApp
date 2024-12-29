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
    private val mediaPlayers = mutableListOf<MediaPlayer>() // For .mp3 playback if desired

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

        // 1) Possibly parse out .mp3 references and play them with a normal MediaPlayer
        //    (Only if you still want to handle <filename.mp3[,volume]> markers the old way).
        //    For example:
        val cleanHeader = parseAndPlayAudioIfAny(header.orEmpty(), mediaFolderUri)
        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), mediaFolderUri)
        val cleanNext = parseAndPlayAudioIfAny(nextButtonText.orEmpty(), mediaFolderUri)

        // 2) Possibly parse out .mp4 references and show them in the VideoView
        checkAndPlayMp4(header.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(body.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(nextButtonText.orEmpty(), mediaFolderUri)

        // 3) Set text with HTML rendering, if desired
        headerTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanHeader)
        headerTextView.textSize = FontSizeManager.getHeaderSize(requireContext())

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())

        nextButton.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanNext)
        nextButton.textSize = FontSizeManager.getButtonSize(requireContext())

        nextButton.setOnClickListener {
            (activity as MainActivity).loadNextFragment()
        }

        return view
    }

    private fun parseAndPlayAudioIfAny(text: String, mediaFolderUri: Uri?): String {
        // If you still want to reuse your existing "audio only" logic,
        // you can keep calling MediaPlaybackHelper or a custom method:
        // We'll do a quick example that only checks for .mp3:
        return AudioPlaybackHelper.parseAndPlayAudio(
            context = requireContext(),
            rawText = text,
            mediaFolderUri = mediaFolderUri,
            mediaPlayers = mediaPlayers
        )
    }

    private fun checkAndPlayMp4(text: String, mediaFolderUri: Uri?) {
        // Find a <filename.mp4[,volume]> pattern if it exists
        val pattern = Regex("<([^>]+\\.mp4(?:,[^>]+)?)>", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return

        // Example parse:
        val group = match.groupValues[1]
        val segments = group.split(",")
        val fileName = segments[0].trim()
        val volume = if (segments.size > 1) {
            val vol = segments[1].trim().toFloatOrNull()
            if (vol != null && vol in 0f..100f) vol / 100f else 1.0f
        } else 1.0f

        // Show the VideoView
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

        // If you need advanced volume control, you'd do so with a custom MediaPlayer
        // For the default VideoView approach:
        videoView.setOnPreparedListener { mp ->
            // mp.setVolume(leftVolume, rightVolume) is not directly available on a standard VideoView
            // This is possible only if you do something like:
            //   videoView.setOnPreparedListener { mediaPlayer ->
            //       // Force MediaPlayer usage or get the audio session.
            //   }
            mp.start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release any audio players used
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()

        // Optionally stop the VideoView if it’s playing
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
