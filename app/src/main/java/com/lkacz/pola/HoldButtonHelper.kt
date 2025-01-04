// Filename: HoldButtonHelper.kt
package com.lkacz.pola

import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout

/**
 * Helper class to add "press and hold to confirm" functionality to a Button.
 * Includes a semi-transparent overlay box that increases in width over the hold duration.
 */
object HoldButtonHelper {

    /**
     * Sets up a button with a press-and-hold action that includes a visual feedback overlay.
     *
     * @param button The button to which the hold functionality is added.
     * @param holdDurationMs The duration (in milliseconds) the button needs to be held.
     * @param onHoldComplete The action to perform when the hold is completed successfully.
     */
    fun setupHoldToConfirm(
        button: Button,
        holdDurationMs: Long = 1000L,
        onHoldComplete: () -> Unit
    ) {
        val context = button.context
        val parentLayout = ensureParentIsFrameLayout(button)

        // Prepare an overlay view that will expand over the button
        val overlayView = View(context).apply {
            setBackgroundColor(0x550000FF) // Semi-transparent blue
            alpha = 1f
            visibility = View.INVISIBLE
        }

        // Match the button's position and size in its FrameLayout parent
        // so that the overlay appears exactly above the button.
        // Initially set width=0 for the "expanding" animation effect.
        val buttonLayoutParams = button.layoutParams as? FrameLayout.LayoutParams
        val overlayLayoutParams = FrameLayout.LayoutParams(
            0,
            button.height,
            buttonLayoutParams?.gravity ?: (Gravity.START or Gravity.TOP)
        ).apply {
            // If the button layout has margins, replicate them
            leftMargin = buttonLayoutParams?.leftMargin ?: 0
            topMargin = buttonLayoutParams?.topMargin ?: 0
            rightMargin = buttonLayoutParams?.rightMargin ?: 0
            bottomMargin = buttonLayoutParams?.bottomMargin ?: 0
        }
        overlayView.layoutParams = overlayLayoutParams
        parentLayout.addView(overlayView)

        val holdHandler = Handler(Looper.getMainLooper())
        val holdRunnable = Runnable { onHoldComplete() }

        val animator = ValueAnimator.ofInt(0, button.width).apply {
            duration = holdDurationMs
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val currentWidth = animation.animatedValue as Int
                overlayLayoutParams.width = currentWidth
                overlayView.layoutParams = overlayLayoutParams
            }
            addListener(onEnd = {
                resetOverlay(overlayView, overlayLayoutParams)
            }, onCancel = {
                resetOverlay(overlayView, overlayLayoutParams)
            })
        }

        // Touch logic to start/stop the overlay animation
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    overlayView.visibility = View.VISIBLE
                    animator.start()
                    holdHandler.postDelayed(holdRunnable, holdDurationMs)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                    animator.cancel()
                    holdHandler.removeCallbacks(holdRunnable)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Ensures the button's parent is a FrameLayout, wrapping it if necessary.
     */
    private fun ensureParentIsFrameLayout(button: Button): FrameLayout {
        val parent = button.parent
        return if (parent is FrameLayout) {
            parent
        } else {
            val context = button.context
            val originalParent = parent as? ViewGroup
            val index = originalParent?.indexOfChild(button) ?: -1

            val frameLayout = FrameLayout(context).apply {
                layoutParams = button.layoutParams
            }

            originalParent?.apply {
                removeView(button)
                if (index >= 0) addView(frameLayout, index)
            }

            frameLayout.addView(button)
            frameLayout
        }
    }

    /**
     * Resets the overlay after hold finishes or is canceled.
     */
    private fun resetOverlay(overlayView: View, layoutParams: FrameLayout.LayoutParams) {
        overlayView.visibility = View.INVISIBLE
        layoutParams.width = 0
        overlayView.layoutParams = layoutParams
    }

    /**
     * Extension to simplify adding AnimatorListener to ValueAnimator.
     */
    private inline fun ValueAnimator.addListener(
        crossinline onStart: (animator: ValueAnimator) -> Unit = {},
        crossinline onEnd: (animator: ValueAnimator) -> Unit = {},
        crossinline onCancel: (animator: ValueAnimator) -> Unit = {},
        crossinline onRepeat: (animator: ValueAnimator) -> Unit = {}
    ) {
        addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {
                onStart(this@addListener)
            }

            override fun onAnimationEnd(animation: android.animation.Animator) {
                onEnd(this@addListener)
            }

            override fun onAnimationCancel(animation: android.animation.Animator) {
                onCancel(this@addListener)
            }

            override fun onAnimationRepeat(animation: android.animation.Animator) {
                onRepeat(this@addListener)
            }
        })
    }
}
