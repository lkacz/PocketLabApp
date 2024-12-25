package com.lkacz.pola

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment

/**
 * A base fragment that handles repeated touch-count logic (Tap-to-unlock behavior).
 * Children override [onTouchThresholdReached] to define their own actions.
 */
abstract class BaseTouchAwareFragment(
    private val resetTime: Long,
    private val threshold: Int
) : Fragment() {

    private val touchCounter by lazy { TouchCounter(resetTime, threshold) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up a universal touch listener that resets after [resetTime] ms
        // and triggers [onTouchThresholdReached] after [threshold] touches.
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (touchCounter.onTouch()) {
                    onTouchThresholdReached()
                }
                v.performClick()
            }
            true
        }
    }

    /**
     * Invoked when the user has tapped [threshold] times within [resetTime] ms.
     */
    protected open fun onTouchThresholdReached() {
        // Default is no action; child fragments override as needed.
    }
}