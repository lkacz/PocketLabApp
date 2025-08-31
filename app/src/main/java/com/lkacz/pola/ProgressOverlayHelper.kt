// Filename: ProgressOverlayHelper.kt
package com.lkacz.pola

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.FrameLayout

/**
 * Manages the creation, positioning, and reset of the semi-transparent
 * overlay used for [HOLD] functionality. By factoring this out, other
 * features can also reuse or adapt the approach without duplicating code.
 */
object ProgressOverlayHelper {
    /**
     * Creates and attaches a semi-transparent overlay view on top of [button].
     * Returns [OverlayInfo] containing the overlay and its parent frame.
     *
     * If [button]'s parent is not a [FrameLayout], we temporarily swap the
     * button into a new [FrameLayout] to avoid crashes.
     */
    fun createOverlay(
        context: Context,
        button: Button,
    ): OverlayInfo {
        val frameLayout = ensureFrameLayoutParent(button)
        val overlayView =
            View(context).apply {
                setBackgroundColor(Color.parseColor("#55000000")) // Semi-transparent overlay
                alpha = 1f
                visibility = View.INVISIBLE
                layoutParams =
                    FrameLayout.LayoutParams(0, button.height).also { lp ->
                        lp.leftMargin = button.left
                        lp.topMargin = button.top
                    }
            }
        frameLayout.addView(overlayView)
        return OverlayInfo(frameLayout, overlayView)
    }

    /**
     * Resets the overlay after hold finishes or is canceled.
     */
    fun resetOverlay(
        overlayView: View,
    ) {
        overlayView.visibility = View.INVISIBLE
        overlayView.layoutParams.width = 0
        overlayView.requestLayout()
        // Removed any extra call to animator.cancel() here to prevent recursion.
    }

    /**
     * Ensures [button] is in a [FrameLayout] so we can overlay [overlayView].
     */
    private fun ensureFrameLayoutParent(button: Button): FrameLayout {
        val parent = button.parent
        return if (parent is FrameLayout) {
            parent
        } else {
            val context = button.context
            val oldParentViewGroup = parent as? android.view.ViewGroup
            val index = oldParentViewGroup?.indexOfChild(button) ?: -1

            val frameLayout =
                FrameLayout(context).apply {
                    layoutParams = button.layoutParams
                }

            oldParentViewGroup?.apply {
                removeView(button)
                if (index >= 0) addView(frameLayout, index)
            }
            frameLayout.addView(
                button,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            frameLayout
        }
    }

    data class OverlayInfo(
        val parentFrame: FrameLayout,
        val overlayView: View,
    )
}
