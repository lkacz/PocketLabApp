package com.lkacz.pola

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment

/**
 * Revised to handle both audio (.mp3) and video (.mp4) markers similarly to InstructionFragment.
 * Changes Made:
 * 1) Added a VideoView (videoView) to play .mp4 references from heading/body/button text and hints.
 * 2) Implemented checkAndPlayMp4() and playVideoFile() to detect and play .mp4.
 * 3) Ensured that any <filename.mp3,volume> markers are still parsed and played via AudioPlaybackHelper.
 * 4) Stopped/released video playback in onDestroyView() to prevent leaks.
 * Reasoning: This makes InputFieldFragment consistent with InstructionFragment for media playback.
 */
class InputFieldFragment : Fragment() {
    private var heading: String? = null
    private var body: String? = null
    private var buttonName: String? = null
    private var inputFields: List<String>? = null
    private lateinit var logger: Logger
    private val fieldValues = mutableMapOf<String, String>()

    private val mediaPlayers = mutableListOf<MediaPlayer>()
    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            heading = it.getString("HEADING")
            body = it.getString("TEXT")
            buttonName = it.getString("BUTTON")
            inputFields = it.getStringArrayList("INPUTFIELDS")
        }
        logger = Logger.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_input_field, container, false)

        val headingTextView: TextView = view.findViewById(R.id.headingTextView)
        val bodyTextView: TextView = view.findViewById(R.id.textTextView)
        val containerLayout: LinearLayout = view.findViewById(R.id.inputFieldContainer)

        // Newly added VideoView (must exist in fragment_input_field.xml with id=videoView2)
        videoView = view.findViewById(R.id.videoView2)

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        // Parse and play audio references in heading/body/button text
        // Then check and play .mp4 references
        val cleanHeading = parseAndPlayAudioIfAny(heading.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(heading.orEmpty(), mediaFolderUri)

        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(body.orEmpty(), mediaFolderUri)

        // Display heading/body text
        headingTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanHeading)
        headingTextView.textSize = FontSizeManager.getHeaderSize(requireContext())

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())

        // Populate input fields; also check & play any .mp3 / .mp4 in each field's hint
        inputFields?.forEach { field ->
            val cleanHint = parseAndPlayAudioIfAny(field, mediaFolderUri)
            checkAndPlayMp4(field, mediaFolderUri)

            val editText = EditText(context).apply {
                hint = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanHint)
                textSize = FontSizeManager.getBodySize(requireContext())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            editText.addTextChangedListener {
                fieldValues[field] = it.toString()
            }
            fieldValues[field] = ""
            containerLayout.addView(editText)
        }

        // Next button
        val cleanButtonText = parseAndPlayAudioIfAny(buttonName.orEmpty(), mediaFolderUri)
        checkAndPlayMp4(buttonName.orEmpty(), mediaFolderUri)

        val nextButton = Button(context).apply {
            text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanButtonText)
            textSize = FontSizeManager.getButtonSize(requireContext())
            setOnClickListener {
                fieldValues.forEach { (field, value) ->
                    val isNumeric = value.toDoubleOrNull() != null
                    logger.logInputFieldFragment(
                        heading.orEmpty(),
                        body.orEmpty(),
                        field,
                        value,
                        isNumeric
                    )
                }
                (activity as MainActivity).loadNextFragment()
            }
        }
        containerLayout.addView(nextButton)

        return view
    }

    /**
     * Detects and plays any <something.mp3[,volume]> placeholders
     * using AudioPlaybackHelper. Returns the string with those markers removed.
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
     * Checks for an <something.mp4[,volume]> marker and plays the video in the fragment's VideoView.
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
     * Locates [fileName] under [mediaFolderUri] and plays it on videoView.
     * For advanced volume control, custom MediaPlayer logic would be required.
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
            // Volume not settable directly via VideoView
        }
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

    companion object {
        @JvmStatic
        fun newInstance(
            heading: String?,
            text: String?,
            buttonName: String?,
            inputFields: List<String>?
        ) = InputFieldFragment().apply {
            arguments = Bundle().apply {
                putString("HEADING", heading)
                putString("TEXT", text)
                putString("BUTTON", buttonName)
                putStringArrayList("INPUTFIELDS", ArrayList(inputFields ?: emptyList()))
            }
        }
    }
}
