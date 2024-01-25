package com.example.frags

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class FixedInstructionFragment : Fragment() {
    private var header: String? = null
    private var body: String? = null
    private var nextButtonText: String? = null
    private var timeInSeconds: Int? = null
    private lateinit var alarmManager: AlarmManager  // Declare AlarmManager variable
    private lateinit var logger: Logger

    private val touchCounter = TouchCounter(5000, 20)  // Initialize TouchCounter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString("HEADER")
            body = it.getString("BODY")
            nextButtonText = it.getString("NEXT_BUTTON_TEXT")
            timeInSeconds = it.getInt("TIME_IN_SECONDS")
        }

        alarmManager = AlarmManager(requireContext())  // Initialize AlarmManager
        logger = Logger.getInstance(requireContext())
        logger.logFixedInstructionFragment(header ?: "Default Header", body ?: "Default Body", timeInSeconds ?: 0)  // Log Fragment start
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fixed_instruction, container, false)

        // UI element initialization
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
                timerTextView.text = String.format("Czas do kolejnej części: %02d:%02d", minutesLeft, secondsLeft)
            }

            override fun onFinish() {
                timerTextView.text = "Przejdź dalej."
                nextButton.visibility = View.VISIBLE
                alarmManager.startAlarm()
                logger.logFixedInstructionFragment(header ?: "Default Header", "Timer Finished", timeInSeconds ?: 0)
            }

        }.start()

        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (touchCounter.onTouch()) {
                    timer.cancel()
                    timer.onFinish()  // Trigger timer finish actions
                }
                v.performClick()  // Indicate that the view was clicked
            }
            true
        }

        nextButton.setOnClickListener {
            alarmManager.stopAlarm()  // Stop alarm using AlarmManager
            (activity as MainActivity).loadNextFragment()
            logger.logFixedInstructionFragment(
                header = header ?: "Default Header",
                body = "Next Button Clicked",
                timeInSeconds = 0,  // Assuming no waiting time for this event
                other = null  // Any additional information
            )
        }


        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmManager.release()  // Release resources using AlarmManager
        logger.logFixedInstructionFragment(header ?: "Default Header", "Destroyed", timeInSeconds ?: 0)  // Updated log statement
    }


    companion object {
        @JvmStatic
        fun newInstance(header: String?, body: String?, nextButtonText: String?, timeInSeconds: Int?) =
            FixedInstructionFragment().apply {
                arguments = Bundle().apply {
                    putString("HEADER", header)
                    putString("BODY", body)
                    putString("NEXT_BUTTON_TEXT", nextButtonText)
                    putInt("TIME_IN_SECONDS", timeInSeconds ?: 0)
                }
            }
    }
}
