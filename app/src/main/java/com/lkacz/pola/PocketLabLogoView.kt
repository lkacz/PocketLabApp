package com.lkacz.pola

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin

/**
 * A custom view that draws:
 *   1. A "pocket" shape (round-rect) at the bottom (dark).
 *   2. A smartphone icon (vector drawable) partially hidden by the pocket.
 *   3. Light "Pocket Lab App" text on top of the icon.
 *
 * The user can drag the phone downward. After release, it bounces back.
 * The icon also gently floats (levitates) when not being dragged,
 * inviting the user to interact.
 *
 * Key Changes:
 * 1) Using a vector resource ic_smartphone.xml to represent the smartphone,
 *    rather than the old “call” icon or a custom drawn shape.
 * 2) Tinted the icon to match a dark theme if desired.
 * 3) Preserved slight tilt (baseRotation) and floating animation logic.
 */
class PocketLabLogoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Nearly black for the pocket
        color = Color.rgb(20, 20, 20)
        style = Paint.Style.FILL
    }

    // Smartphone icon from our custom vector drawable (see ic_smartphone.xml).
    // Tinted dark if desired; you can adjust the tint color or remove it if you prefer.
    private val phoneDrawable = ContextCompat.getDrawable(context, R.drawable.ic_smartphone)?.mutate()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Light text for contrast
        color = Color.LTGRAY
        textSize = 80f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // The bounding rect for the pocket at the bottom.
    private val pocketRect = RectF()

    // The phone's bounding rect for drag detection in local coords.
    private val phoneRect = RectF()

    // For drag + bounce
    private var phoneCenterX = 0f
    private var phoneCenterY = 0f
    private var initialPhoneCenterY = 0f
    private var isDragging = false
    private var touchOffsetY = 0f

    // We'll define an approximate "phone size" for bounding and hit detection.
    private var phoneWidth = 0f
    private var phoneHeight = 0f

    // Slight angle offset so icon isn’t fully vertical
    private var baseRotation = -10f
    private var currentRotation = baseRotation

    // How far the phone can enter the pocket area before we bounce back
    private val bounceThreshold = 50f

    // Animators for gentle levitation
    private var levitateAnimatorSet: AnimatorSet? = null

    init {
        // Optional: tint the smartphone icon darker if you prefer
        phoneDrawable?.setTint(Color.DKGRAY)
        phoneDrawable?.setTintMode(PorterDuff.Mode.SRC_IN)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val desiredSize = 600
        val w = resolveSize(desiredSize, widthMeasureSpec)
        val h = resolveSize(desiredSize, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Define the pocket region
        val pocketHeight = h * 0.10f
        pocketRect.set(0f, h - pocketHeight, w.toFloat(), h.toFloat())

        // We'll define a bounding box for the phone icon
        phoneWidth = w * 0.35f
        phoneHeight = w * 0.35f
        phoneCenterX = w * 0.5f
        phoneCenterY = (h - pocketHeight) - phoneHeight * 0.5f + 20f
        initialPhoneCenterY = phoneCenterY

        // Start the tilt + float
        currentRotation = baseRotation
        startLevitateAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the dark round-rect pocket at bottom
        val pocketRadius = height * 0.05f
        canvas.drawRoundRect(pocketRect, pocketRadius, pocketRadius, pocketPaint)

        // Save canvas state and rotate around phone center
        canvas.save()
        canvas.translate(phoneCenterX, phoneCenterY)
        canvas.rotate(currentRotation)

        // phoneRect is local coordinates for the icon
        phoneRect.set(-phoneWidth / 2f, -phoneHeight / 2f, phoneWidth / 2f, phoneHeight / 2f)

        // Lay out the icon
        phoneDrawable?.setBounds(
            phoneRect.left.toInt(),
            phoneRect.top.toInt(),
            phoneRect.right.toInt(),
            phoneRect.bottom.toInt()
        )
        phoneDrawable?.draw(canvas)

        // Draw the text “Pocket Lab App” on top
        val textY = phoneRect.centerY() + textPaint.textSize / 4
        canvas.drawText("Pocket Lab App", phoneRect.centerX(), textY, textPaint)

        // Restore
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (hitTestPhone(event.x, event.y)) {
                    isDragging = true
                    stopLevitateAnimation()
                    touchOffsetY = event.y - phoneCenterY
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    phoneCenterY = event.y - touchOffsetY
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    // If phone is low enough, bounce it
                    if ((phoneCenterY + phoneHeight / 2f) > (pocketRect.top - bounceThreshold)) {
                        bouncePhone()
                    } else {
                        settlePhone()
                    }
                    startLevitateAnimation()
                }
            }
        }
        return true
    }

    /**
     * Check if user touched inside the phone icon, taking rotation into account.
     */
    private fun hitTestPhone(touchX: Float, touchY: Float): Boolean {
        val relX = touchX - phoneCenterX
        val relY = touchY - phoneCenterY
        val rad = Math.toRadians(currentRotation.toDouble())

        val cosA = cos(-rad)
        val sinA = sin(-rad)

        // Reverse-rotate the point
        val rotatedX = (relX * cosA - relY * sinA).toFloat()
        val rotatedY = (relX * sinA + relY * cosA).toFloat()

        return phoneRect.contains(rotatedX, rotatedY)
    }

    private fun bouncePhone() {
        val bounceUp = ObjectAnimator.ofFloat(
            this, "phoneCenterY", phoneCenterY, initialPhoneCenterY - 20f
        ).apply { duration = 300 }

        val settleDown = ObjectAnimator.ofFloat(
            this, "phoneCenterY", initialPhoneCenterY - 20f, initialPhoneCenterY
        ).apply { duration = 200 }

        AnimatorSet().apply {
            playSequentially(bounceUp, settleDown)
            start()
        }
    }

    private fun settlePhone() {
        ObjectAnimator.ofFloat(
            this, "phoneCenterY", phoneCenterY, initialPhoneCenterY
        ).apply {
            duration = 200
            start()
        }
    }

    // Animator reflection usage
    @Suppress("unused")
    fun setPhoneCenterY(value: Float) {
        phoneCenterY = value
        invalidate()
    }

    fun getPhoneCenterY(): Float = phoneCenterY

    /**
     * Gently float the icon up/down and rotate ±2° around baseRotation.
     */
    private fun startLevitateAnimation() {
        if (levitateAnimatorSet != null) return

        val floatRange = 10f
        val rotationRange = 2f

        val levitateTranslate = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                val offset = floatRange * fraction - (floatRange / 2)
                phoneCenterY = initialPhoneCenterY + offset
                invalidate()
            }
        }
        val levitateRotation = ValueAnimator.ofFloat(-rotationRange, rotationRange).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { anim ->
                val delta = anim.animatedValue as Float
                currentRotation = baseRotation + delta
                invalidate()
            }
        }
        levitateAnimatorSet = AnimatorSet().apply {
            playTogether(levitateTranslate, levitateRotation)
            start()
        }
    }

    private fun stopLevitateAnimation() {
        levitateAnimatorSet?.cancel()
        levitateAnimatorSet = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isDragging) {
            startLevitateAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopLevitateAnimation()
    }
}
