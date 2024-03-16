package com.lkacz.pola

import android.os.Handler
import android.os.Looper

class TouchCounter(private val resetTime: Long, private val threshold: Int) {

    private var touchCount = 0
    private val touchResetHandler = Handler(Looper.getMainLooper())

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

