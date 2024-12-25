package com.lkacz.pola

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class TimerFragment : Fragment() {
    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null
    private var timeInSeconds: Int? = null
    private lateinit var alarmHelper: AlarmHelper
    private lateinit var logger: Logger

    private val touchCounter = TouchCounter(5000, 20)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString("HEADER")
            body = it.getString("BODY")
            nextButtonText = it.getString("NEXT_BUTTON_TEXT")
            timeInSeconds = it.getInt("TIME_IN_SECONDS")
        }

        alarmHelper = AlarmHelper(requireContext())
        logger = Logger.getInstance(requireContext())
        logger.logTimerFragment(header ?: "Default Header", body ?: "Default Body", timeInSeconds ?: 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timer, container, false)

        val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        val bodyTextView: TextView = view.findViewById(R.id.bodyTextView)
        val nextButton: Button = view.findViewById(R.id.nextButton)
        val timerTextView: TextView = view.findViewById(R.id.timerTextView)

        headerTextView.text = header ?: "Default Header"
        bodyTextView.text = body ?: "Default Body"
        nextButton.text = nextButtonText ?: "Next"
        nextButton.visibility = View.INVISIBLE

        val totalTimeMillis = (timeInSeconds ?: 0) * 1000L
        val countDownInterval = 1000L

        val timer = object : CountDownTimer(totalTimeMillis, countDownInterval) {
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

        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (touchCounter.onTouch()) {
                    timer.cancel()
                    timer.onFinish()
                }
                v.performClick()
            }
            true
        }

        nextButton.setOnClickListener {
            alarmHelper.stopAlarm()
            (activity as MainActivity).loadNextFragment()
            logger.logTimerFragment(
                header ?: "Default Header",
                body = "Next Button Clicked",
                timeInSeconds = 0
            )
        }

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmHelper.release()
        logger.logTimerFragment(header ?: "Default Header", "Destroyed", timeInSeconds ?: 0)
    }

    companion object {
        @JvmStatic
        fun newInstance(header: String?, body: String?, nextButtonText: String?, timeInSeconds: Int?) =
            TimerFragment().apply {
                arguments = Bundle().apply {
                    putString("HEADER", header)
                    putString("BODY", body)
                    putString("NEXT_BUTTON_TEXT", nextButtonText)
                    putInt("TIME_IN_SECONDS", timeInSeconds ?: 0)
                }
            }
    }
}
