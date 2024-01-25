package com.example.frags

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
    private val touchCounter = TouchCounter(2000, 5)  // Initialize TouchCounter with a 500ms reset time and 3 touch threshold

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
