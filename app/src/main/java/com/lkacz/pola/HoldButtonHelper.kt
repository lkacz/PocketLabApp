// Filename: HoldButtonHelper.kt
package com.lkacz.pola

import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import kotlin.math.min

object HoldButtonHelper {

    fun setupHoldToConfirm(
        button: Button,
        holdDurationMs: Long = 1000L,
        onHoldComplete: () -> Unit
    ) {
        val originalBg = button.background
        val progressDrawable = createLayeredDrawable(originalBg)

        val holdHandler = Handler(Looper.getMainLooper())
        var accumulatedTime = 0L
        val updateInterval = 25L
        val maxLevel = 10000

        val holdRunnable = object : Runnable {
            override fun run() {
                accumulatedTime += updateInterval
                val fraction = min(accumulatedTime.toFloat() / holdDurationMs.toFloat(), 1f)
                val level = (fraction * maxLevel).toInt()
                progressDrawable.level = level

                if (fraction >= 1f) {
                    // Fire the hold-complete callback
                    onHoldComplete()
                } else {
                    holdHandler.postDelayed(this, updateInterval)
                }
            }
        }

        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    button.background = progressDrawable
                    accumulatedTime = 0L
                    progressDrawable.level = 0
                    holdHandler.postDelayed(holdRunnable, updateInterval)
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_OUTSIDE -> {
                    // Stop counting the hold time
                    holdHandler.removeCallbacks(holdRunnable)
                    progressDrawable.level = 0
                    button.background = originalBg

                    // IMPORTANT: Call performClick() to signal a click action for accessibility
                    v.performClick()

                    true
                }

                else -> false
            }
        }
    }

    private fun createLayeredDrawable(originalBg: Drawable?): LayerDrawable {
        val transparentRect = ColorDrawable(Color.parseColor("#88FFFFFF"))
        val clipDrawable = ClipDrawable(
            transparentRect,
            Gravity.LEFT,
            ClipDrawable.HORIZONTAL
        ).apply {
            level = 0
        }

        // If originalBg is null, use a ColorDrawable as the bottom layer.
        val bottomDrawable = originalBg ?: ColorDrawable(Color.TRANSPARENT)
        val layers = arrayOf(bottomDrawable, clipDrawable)
        return LayerDrawable(layers).apply {
            // Ensure the clip layer is layered above the original background
            setId(0, 0)
            setId(1, 1)
        }
    }
}
