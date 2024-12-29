package com.lkacz.pola

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

class TimerFragment : BaseTouchAwareFragment(5000, 20) {

    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null
    private var timeInSeconds: Int? = null
    private lateinit var alarmHelper: AlarmHelper
    private lateinit var logger: Logger
    private var timer: CountDownTimer? = null

    // Holds MediaPlayer references for any played sounds in this fragment
    private val mediaPlayers = mutableListOf<MediaPlayer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString("HEADER")
            body = it.getString("BODY")
            nextButtonText = it.getString("NEXT_BUTTON_TEXT")
            timeInSeconds = it.getInt("TIME_IN_SECONDS")
        }
        if (timeInSeconds == null || timeInSeconds!! < 0) {
            timeInSeconds = 0
        }

        alarmHelper = AlarmHelper(requireContext())
        logger = Logger.getInstance(requireContext())
        logger.logTimerFragment(header ?: "Default Header", body ?: "Default Body", timeInSeconds ?: 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timer, container, false)

        val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        val bodyTextView: TextView = view.findViewById(R.id.bodyTextView)
        val nextButton: Button = view.findViewById(R.id.nextButton)
        val timerTextView: TextView = view.findViewById(R.id.timerTextView)

        val mediaFolderUri = MediaFolderManager(requireContext()).getMediaFolderUri()

        // Parse & play any audio references in header/body/nextButton text
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

        // Apply text to UI
        headerTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanHeader)
        headerTextView.textSize = FontSizeManager.getHeaderSize(requireContext())

        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())

        nextButton.text = HtmlMediaHelper.toSpannedHtml(requireContext(), mediaFolderUri, cleanNextButton)
        nextButton.textSize = FontSizeManager.getButtonSize(requireContext())
        nextButton.visibility = View.INVISIBLE

        // Timer text styling
        timerTextView.textSize = FontSizeManager.getBodySize(requireContext())

        // Start countdown
        val totalTimeMillis = (timeInSeconds ?: 0) * 1000L
        timer = object : CountDownTimer(totalTimeMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val minutesLeft = millisUntilFinished / 60000
                val secondsLeft = (millisUntilFinished % 60000) / 1000
                timerTextView.text = String.format("Time left: %02d:%02d", minutesLeft, secondsLeft)
            }

            override fun onFinish() {
                timerTextView.text = "Continue."
                nextButton.visibility = View.VISIBLE
                alarmHelper.startAlarm()
                logger.logTimerFragment(header ?: "Default Header", "Timer Finished", timeInSeconds ?: 0)
            }
        }.start()

        nextButton.setOnClickListener {
            alarmHelper.stopAlarm()
            (activity as MainActivity).loadNextFragment()
            logger.logTimerFragment(header ?: "Default Header", "Next Button Clicked", 0)
        }

        return view
    }

    override fun onTouchThresholdReached() {
        timer?.cancel()
        logger.logTimerFragment(header ?: "Default Header", "Timer forcibly ended by user", timeInSeconds ?: 0)
        view?.findViewById<TextView>(R.id.timerTextView)?.text = "Continue."
        view?.findViewById<Button>(R.id.nextButton)?.visibility = View.VISIBLE
        alarmHelper.startAlarm()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release all MediaPlayers for this fragment
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmHelper.release()
        logger.logTimerFragment(header ?: "Default Header", "Destroyed", timeInSeconds ?: 0)
    }

    companion object {
        @JvmStatic
        fun newInstance(
            header: String?,
            body: String?,
            nextButtonText: String?,
            timeInSeconds: Int?
        ) = TimerFragment().apply {
            arguments = Bundle().apply {
                putString("HEADER", header)
                putString("BODY", body)
                putString("NEXT_BUTTON_TEXT", nextButtonText)
                putInt("TIME_IN_SECONDS", timeInSeconds ?: 0)
            }
        }
    }
}
