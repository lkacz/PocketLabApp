package com.lkacz.pola

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class TapInstructionFragment : BaseTouchAwareFragment(1000, 3) {

    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null
    private lateinit var logger: Logger
    private var nextButton: Button? = null

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
        val view = inflater.inflate(R.layout.fragment_instruction, container, false)

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

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
        val cleanNextButton = AudioPlaybackHelper.parseAndPlayAudio(
            context = requireContext(),
            rawText = nextButtonText ?: "Next",
            mediaFolderUri = mediaFolderUri,
            mediaPlayers = mediaPlayers
        )

        nextButton = InstructionUiHelper.setupInstructionViews(
            view,
            cleanHeader,
            cleanBody,
            cleanNextButton
        ) {
            (activity as MainActivity).loadNextFragment()
        }
        nextButton?.visibility = View.INVISIBLE
        return view
    }

    override fun onTouchThresholdReached() {
        logger.logOther("Tap threshold reached in TapInstructionFragment")
        nextButton?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()
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
