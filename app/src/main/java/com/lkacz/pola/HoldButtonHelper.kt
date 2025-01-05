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
                progressDrawable.setLevel(level)

                if (fraction >= 1f) {
                    onHoldComplete()
                } else {
                    holdHandler.postDelayed(this, updateInterval)
                }
            }
        }

        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    button.background = progressDrawable
                    accumulatedTime = 0L
                    progressDrawable.setLevel(0)
                    holdHandler.postDelayed(holdRunnable, updateInterval)
                    true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_OUTSIDE -> {
                    holdHandler.removeCallbacks(holdRunnable)
                    progressDrawable.setLevel(0)
                    button.background = originalBg
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

        // If originalBg is null, use an empty ColorDrawable as the bottom layer.
        val bottomDrawable = originalBg ?: ColorDrawable(Color.TRANSPARENT)
        val layers = arrayOf(bottomDrawable, clipDrawable)
        return LayerDrawable(layers).apply {
            // Ensure the clip layer is above the original background
            setId(0, 0)
            setId(1, 1)
        }
    }
}
