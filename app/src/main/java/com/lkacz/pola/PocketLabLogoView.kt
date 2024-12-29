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

/*
Changes Made:
1. Moved the text "Pocket Lab App" to the background so it does not rotate or move with the phone.
2. Reduced phone width to 20% of the screen (instead of 35%) to make the phone narrower.
3. Made the pocket narrower (25% of the screen width) and centered it at the bottom.
4. Preserved bounce-back logic for the phone.
5. Kept levitation/floating animations for the phone, while the text remains stationary in the background.
Reasoning:
- The user wants the text fixed in the background, the phone narrower, and the pocket less wide, with the phone still draggable and bouncing back.
- Retained all original functionalities (dragging, levitation, bounce) except for the text which is now drawn behind the phone and no longer moves/rotates.
*/

class PocketLabLogoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // Paint for the pocket
    private val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(20, 20, 20)
        style = Paint.Style.FILL
    }

    // Smartphone icon
    private val phoneDrawable = ContextCompat.getDrawable(context, R.drawable.ic_smartphone)?.mutate()

    // Paint for the background text that remains stationary
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 80f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // Pocket rect (now narrower and centered)
    private val pocketRect = RectF()

    // Phone rect for local coordinates
    private val phoneRect = RectF()

    // Phone center coords for dragging/floating
    private var phoneCenterX = 0f
    private var phoneCenterY = 0f
    private var initialPhoneCenterY = 0f

    private var isDragging = false
    private var touchOffsetY = 0f

    // Phone size
    private var phoneWidth = 0f
    private var phoneHeight = 0f

    // Slight rotation offset for phone
    private var baseRotation = -10f
    private var currentRotation = baseRotation

    // Bounce threshold
    private val bounceThreshold = 50f

    // Levitation animators
    private var levitateAnimatorSet: AnimatorSet? = null

    init {
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

        // Pocket is 25% of screen width, centered at bottom
        val pocketWidth = w * 0.25f
        val pocketHeight = h * 0.10f
        val pocketLeft = (w - pocketWidth) / 2f
        val pocketTop = h - pocketHeight
        pocketRect.set(pocketLeft, pocketTop, pocketLeft + pocketWidth, h.toFloat())

        // Phone narrower: 20% of screen width
        phoneWidth = w * 0.20f
        phoneHeight = w * 0.20f

        // Center phone above pocket
        phoneCenterX = w * 0.5f
        phoneCenterY = pocketTop - phoneHeight * 0.5f + 20f
        initialPhoneCenterY = phoneCenterY

        currentRotation = baseRotation
        startLevitateAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1) Draw the stationary background text near the center or top
        val textY = height * 0.4f
        canvas.drawText("Pocket Lab App", width * 0.5f, textY, textPaint)

        // 2) Draw pocket
        val pocketRadius = (pocketRect.height()) / 2f
        canvas.drawRoundRect(pocketRect, pocketRadius, pocketRadius, pocketPaint)

        // 3) Draw phone with rotation around center
        canvas.save()
        canvas.translate(phoneCenterX, phoneCenterY)
        canvas.rotate(currentRotation)

        phoneRect.set(-phoneWidth / 2f, -phoneHeight / 2f, phoneWidth / 2f, phoneHeight / 2f)

        phoneDrawable?.setBounds(
            phoneRect.left.toInt(),
            phoneRect.top.toInt(),
            phoneRect.right.toInt(),
            phoneRect.bottom.toInt()
        )
        phoneDrawable?.draw(canvas)

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
                    // Check if phone is lowered near the pocket
                    val phoneBottom = phoneCenterY + phoneHeight / 2f
                    if (phoneBottom > (pocketRect.top - bounceThreshold)) {
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

    private fun bouncePhone() {
        // Quick bounce up then settle
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

    // Reflection use for bounce anim
    @Suppress("unused")
    fun setPhoneCenterY(value: Float) {
        phoneCenterY = value
        invalidate()
    }

    fun getPhoneCenterY(): Float = phoneCenterY

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
                currentRotation = baseRotation + (anim.animatedValue as Float)
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

    /**
     * Checks if the user tapped on the phone, accounting for current rotation.
     */
    private fun hitTestPhone(touchX: Float, touchY: Float): Boolean {
        val relX = touchX - phoneCenterX
        val relY = touchY - phoneCenterY
        val rad = Math.toRadians(currentRotation.toDouble())
        val cosA = cos(-rad)
        val sinA = sin(-rad)
        val rotatedX = (relX * cosA - relY * sinA).toFloat()
        val rotatedY = (relX * sinA + relY * cosA).toFloat()
        return phoneRect.contains(rotatedX, rotatedY)
    }
}
