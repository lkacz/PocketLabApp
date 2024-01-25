package com.example.frags

import android.os.Handler

class TouchCounter(private val resetTime: Long, private val threshold: Int) {

    private var touchCount = 0
    private val touchResetHandler = Handler()

    fun onTouch(): Boolean {
        touchCount++
        if (touchCount == 1) {
            touchResetHandler.postDelayed({
                touchCount = 0
            }, resetTime)
        }
        if (touchCount >= threshold) {
            touchCount = 0
            return true
        }
        return false
    }
}
