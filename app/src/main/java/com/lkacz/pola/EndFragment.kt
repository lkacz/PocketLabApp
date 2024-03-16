package com.lkacz.pola

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class EndFragment : Fragment() {

    private lateinit var logger: Logger
    private val touchCounter = TouchCounter(5000, 20)  // Initialize TouchCounter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Obtain the Logger instance using the fragment's context
        logger = Logger.getInstance(requireContext())
        return inflater.inflate(R.layout.fragment_end, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Delay the backup operation for 1 second
        Handler(Looper.getMainLooper()).postDelayed({
            logger.backupLogFile()
        }, 500)
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (touchCounter.onTouch()) {
                    // Close the application logic goes here
                    activity?.finish()
                }
                v.performClick()  // Indicate that the view was clicked
            }
            true
        }
    }
}

