package com.lkacz.pola

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * A custom view that draws:
 *  1. A "pocket" shape (round-rect) at the bottom.
 *  2. A "smartphone" shape (round-rect) partially hidden by the pocket.
 *  3. The text "Pocket Lab App" on the smartphone.
 *
 * The user can drag the phone downward. After release, the phone bounces back
 * up, revealing the text. No external images or resources are used.
 */
class PocketLabLogoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6200EE") // Purple
        style = Paint.Style.FILL
    }
    private val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 50f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // Phone bounding box (used for both drawing and hit detection)
    private val phoneRect = RectF()
    // Pocket bounding box
    private val pocketRect = RectF()

    // For drag and bounce
    private var phoneCenterX = 0f
    private var phoneCenterY = 0f
    private var initialPhoneCenterY = 0f
    private var phoneWidth = 0f
    private var phoneHeight = 0f
    private var isDragging = false
    private var touchOffsetY = 0f

    // Helps define how far we consider "into the pocket" before bouncing
    private val bounceThreshold = 50f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Make the view at least 600x600 if not constrained, or fill the parent
        val desiredSize = 600
        val w = resolveSize(desiredSize, widthMeasureSpec)
        val h = resolveSize(desiredSize, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Define pocket size as a wide round rect at the bottom
        val pocketHeight = h * 0.25f
        pocketRect.set(0f, h - pocketHeight, w.toFloat(), h.toFloat())

        // Define phone size
        phoneWidth = w * 0.65f
        phoneHeight = h * 0.45f
        phoneCenterX = w * 0.5f
        // Place phone so its bottom is initially hidden behind the pocket
        phoneCenterY = (h - pocketHeight) - phoneHeight * 0.5f + 20
        initialPhoneCenterY = phoneCenterY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the pocket
        val pocketRadius = height * 0.05f
        canvas.drawRoundRect(pocketRect, pocketRadius, pocketRadius, pocketPaint)

        // Draw the phone
        phoneRect.set(
            phoneCenterX - phoneWidth / 2f,
            phoneCenterY - phoneHeight / 2f,
            phoneCenterX + phoneWidth / 2f,
            phoneCenterY + phoneHeight / 2f
        )
        val phoneCornerRadius = phoneWidth * 0.05f
        canvas.drawRoundRect(phoneRect, phoneCornerRadius, phoneCornerRadius, phonePaint)

        // Draw the text "Pocket Lab App" on the phone
        val textX = phoneRect.centerX()
        val textY = phoneRect.centerY() + textPaint.textSize / 4
        canvas.drawText("Pocket Lab App", textX, textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if user touched the phone
                if (phoneRect.contains(event.x, event.y)) {
                    isDragging = true
                    // offset from phone center, so phone doesn't jump
                    touchOffsetY = event.y - phoneRect.centerY()
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    // Move the phone vertically with the finger
                    phoneCenterY = event.y - touchOffsetY
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    // Check if the phone is "inside" pocket enough to bounce
                    if (phoneRect.bottom > pocketRect.top - bounceThreshold) {
                        // Animate phone back up
                        bouncePhone()
                    } else {
                        // If not, just minor settle back
                        settlePhone()
                    }
                }
            }
        }
        return true
    }

    /**
     * Bounces the phone up to its original position with a fun overshoot.
     */
    private fun bouncePhone() {
        val bounceUp = ObjectAnimator.ofFloat(
            this,
            "phoneCenterY",
            phoneCenterY,
            initialPhoneCenterY - 20f // a bit above original
        )
        bounceUp.duration = 300

        val settleDown = ObjectAnimator.ofFloat(
            this,
            "phoneCenterY",
            initialPhoneCenterY - 20f,
            initialPhoneCenterY
        )
        settleDown.duration = 200

        AnimatorSet().apply {
            playSequentially(bounceUp, settleDown)
            start()
        }
    }

    /**
     * If the phone didn't quite make it to the pocket, just settle back.
     */
    private fun settlePhone() {
        val settleAnim = ObjectAnimator.ofFloat(
            this,
            "phoneCenterY",
            phoneCenterY,
            initialPhoneCenterY
        )
        settleAnim.duration = 200
        settleAnim.start()
    }

    /**
     * Used by ObjectAnimator to move the phone along Y while animating.
     */
    @Suppress("unused") // called reflectively by the Animator
    fun setPhoneCenterY(value: Float) {
        phoneCenterY = value
        invalidate()
    }

    fun getPhoneCenterY(): Float {
        return phoneCenterY
    }
}
