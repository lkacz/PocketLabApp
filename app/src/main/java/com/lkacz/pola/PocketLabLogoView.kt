// Filename: PocketLabLogoView.kt
package com.lkacz.pola

import android.animation.*
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.*
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin

class PocketLabLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val phoneDrawable = ContextCompat
        .getDrawable(context, R.drawable.ic_smartphone)
        ?.mutate()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 100f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // The phone’s bounding rect (in local phone coordinates)
    private val phoneRect = RectF()

    // Screen rect for shimmering effect
    private val screenRect = RectF()

    // Phone dimension ratios
    private val phoneWidthRatio = 0.12f
    private val phoneHeightRatio = 0.20f

    // Positions
    private var phoneCenterX = 0f
    private var phoneCenterY = 0f
    private var initialPhoneCenterX = 0f
    private var initialPhoneCenterY = 0f

    private var isDragging = false
    private var didUserMove = false
    private var touchOffsetX = 0f
    private var touchOffsetY = 0f

    private var phoneWidth = 0f
    private var phoneHeight = 0f

    // Rotation states
    private val baseRotation = -4f
    private var currentRotation = baseRotation

    // How far we let the phone float about
    private val floatRangeX = 6f
    private val floatRangeY = 10f

    // How much rotation from side to side
    private val rotationRange = 2f

    // Levitation animators
    private var translateXAnimator: ValueAnimator? = null
    private var translateYAnimator: ValueAnimator? = null
    private var rotationAnimator: ValueAnimator? = null
    private var levitateAnimatorSet: AnimatorSet? = null

    // Shimmer offset used to scroll the gradient
    private var shimmerOffset = 0f
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private var shimmerAnimator: ValueAnimator? = null

    // Stripe paint
    private val stripePaintA = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(120, 120, 120)
        style = Paint.Style.FILL
    }
    private val stripePaintB = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(200, 200, 200)
        style = Paint.Style.FILL
    }
    private val stripePaintC = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var stripeA_Y = 0f
    private var stripeB_Y = 0f
    private var stripeThickness = 0f

    //----------------------------------------------------------------------------------------------
    //  Lifecycle
    //----------------------------------------------------------------------------------------------
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val desiredSize = 600
        val w = resolveSize(desiredSize, widthMeasureSpec)
        val h = resolveSize(desiredSize, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        phoneWidth = w * phoneWidthRatio
        phoneHeight = w * phoneHeightRatio

        // Place the phone near the upper-middle portion
        phoneCenterX = w * 0.5f
        phoneCenterY = h * 0.4f

        // Remember these as "rest" positions
        initialPhoneCenterX = phoneCenterX
        initialPhoneCenterY = phoneCenterY

        currentRotation = baseRotation

        startLevitateAnimation()
        startShimmerAnimation()

        stripeThickness = h * 0.05f
        stripeA_Y = h * 0.57f
        stripeB_Y = h * 0.60f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Title text
        val textY = height * 0.2f
        canvas.drawText("Pocket Lab App", width * 0.5f, textY, textPaint)

        // Stripe A
        canvas.drawRect(0f, stripeA_Y, width.toFloat(), stripeA_Y + stripeThickness, stripePaintA)

        // Clip so the phone + screen are behind next stripes
        canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), stripeB_Y)

        // Translate + rotate phone
        canvas.translate(phoneCenterX, phoneCenterY)
        canvas.rotate(currentRotation)

        phoneRect.set(
            -phoneWidth / 2f,
            -phoneHeight / 2f,
            phoneWidth / 2f,
            phoneHeight / 2f
        )
        phoneDrawable?.setBounds(
            phoneRect.left.toInt(),
            phoneRect.top.toInt(),
            phoneRect.right.toInt(),
            phoneRect.bottom.toInt()
        )
        phoneDrawable?.draw(canvas)

        // Make the shimmering screen a bit smaller
        screenRect.set(phoneRect)
        val border = phoneWidth * 0.08f
        screenRect.inset(border, border)
        drawShimmeringScreen(canvas, screenRect)

        canvas.restore()

        // Stripe B
        canvas.drawRect(0f, stripeB_Y, width.toFloat(), stripeB_Y + stripeThickness, stripePaintB)

        // Stripe C
        val stripeC_Top = stripeB_Y + stripeThickness
        canvas.drawRect(0f, stripeC_Top, width.toFloat(), height.toFloat(), stripePaintC)
    }

    private fun drawShimmeringScreen(canvas: Canvas, screenBounds: RectF) {
        val gradientWidth = screenBounds.width() * 2
        val leftX = screenBounds.left - gradientWidth + shimmerOffset * gradientWidth * 2
        val rightX = leftX + gradientWidth

        val gradientColors = intArrayOf(
            Color.parseColor("#222222"),
            Color.parseColor("#222222"),
            Color.parseColor("#222222"),
            Color.parseColor("#333333")
        )
        val linearGradient = LinearGradient(
            leftX, screenBounds.top, rightX, screenBounds.bottom,
            gradientColors, null, Shader.TileMode.CLAMP
        )
        shimmerPaint.shader = linearGradient
        canvas.drawRoundRect(screenBounds, 8f, 8f, shimmerPaint)
    }

    //----------------------------------------------------------------------------------------------
    //  Touch / Drag
    //----------------------------------------------------------------------------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (hitTestPhone(event.x, event.y)) {
                    isDragging = true
                    didUserMove = false

                    // Pause anims so we can resume from same fraction
                    pauseLevitateAnimation()
                    pauseShimmerAnimation()

                    // Track offsets for both X and Y
                    touchOffsetX = event.x - phoneCenterX
                    touchOffsetY = event.y - phoneCenterY
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val newX = event.x - touchOffsetX
                    val newY = event.y - touchOffsetY

                    if (newX != phoneCenterX || newY != phoneCenterY) {
                        didUserMove = true
                    }
                    phoneCenterX = newX
                    phoneCenterY = newY
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    if (didUserMove) {
                        // Animate X and Y back to initial with a bounce/overshoot effect
                        animatePhoneBackToInitial()
                    } else {
                        // If user just tapped, resume from where we left off
                        resumeLevitateAnimation()
                        resumeShimmerAnimation()
                    }
                }
            }
        }
        return true
    }

    //----------------------------------------------------------------------------------------------
    //  Bouncing back (no dynamic library) with standard ValueAnimator
    //----------------------------------------------------------------------------------------------
    /**
     * Animate both X and Y to the “idle” positions using an Interpolator
     * that provides a bounce-like or overshoot effect.
     */
    private fun animatePhoneBackToInitial() {
        val animX = ValueAnimator.ofFloat(phoneCenterX, initialPhoneCenterX).apply {
            duration = 800
            interpolator = BounceInterpolator()
            // Or: interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { animator ->
                phoneCenterX = animator.animatedValue as Float
                invalidate()
            }
        }
        val animY = ValueAnimator.ofFloat(phoneCenterY, initialPhoneCenterY).apply {
            duration = 800
            interpolator = BounceInterpolator()
            // Or: interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { animator ->
                phoneCenterY = animator.animatedValue as Float
                invalidate()
            }
        }

        // Run them in parallel
        val bounceSet = AnimatorSet()
        bounceSet.playTogether(animX, animY)
        bounceSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Once done bouncing, re-sync the floating cycle so no jump
                smoothlyResumeLevitate()
                resumeShimmerAnimation()
            }
        })
        bounceSet.start()
    }

    /**
     * Sync phone’s (X, Y) *and* rotation with the levitation cycle so there's no jump.
     */
    private fun smoothlyResumeLevitate() {
        if (levitateAnimatorSet == null) {
            startLevitateAnimation()
            return
        }

        val tx = translateXAnimator
        val ty = translateYAnimator
        val rot = rotationAnimator
        if (tx == null || ty == null || rot == null ||
            !tx.isRunning || !ty.isRunning || !rot.isRunning) {
            // If something is not running, just (re)start
            startLevitateAnimation()
            return
        }

        // 1) Match X fraction
        val xDiff = phoneCenterX - initialPhoneCenterX
        val fracX = ((xDiff + floatRangeX / 2f) / floatRangeX).coerceIn(0f, 1f)
        tx.currentPlayTime = (fracX * tx.duration).toLong()
        updatePhoneXFromFraction(fracX)

        // 2) Match Y fraction
        val yDiff = phoneCenterY - initialPhoneCenterY
        val fracY = ((yDiff + floatRangeY / 2f) / floatRangeY).coerceIn(0f, 1f)
        ty.currentPlayTime = (fracY * ty.duration).toLong()
        updatePhoneYFromFraction(fracY)

        // 3) Match rotation fraction
        val rotDiff = currentRotation - baseRotation
        val fracRot = ((rotDiff + rotationRange) / (2f * rotationRange)).coerceIn(0f, 1f)
        rot.currentPlayTime = (fracRot * rot.duration).toLong()
        updateRotationFromFraction(fracRot)

        // Resume from the newly synced fraction
        resumeLevitateAnimation()
    }

    private fun updatePhoneXFromFraction(frac: Float) {
        val offset = floatRangeX * frac - (floatRangeX / 2f)
        phoneCenterX = initialPhoneCenterX + offset
        invalidate()
    }
    private fun updatePhoneYFromFraction(frac: Float) {
        val offset = floatRangeY * frac - (floatRangeY / 2f)
        phoneCenterY = initialPhoneCenterY + offset
        invalidate()
    }
    private fun updateRotationFromFraction(frac: Float) {
        val rangeVal = -rotationRange + (2f * rotationRange * frac)
        currentRotation = baseRotation + rangeVal
        invalidate()
    }

    //----------------------------------------------------------------------------------------------
    //  Levitation Animations (Idle float + rotation)
    //----------------------------------------------------------------------------------------------
    private fun startLevitateAnimation() {
        if (levitateAnimatorSet != null) return

        // phoneCenterX floats left-right
        translateXAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                val offset = floatRangeX * fraction - (floatRangeX / 2f)
                phoneCenterX = initialPhoneCenterX + offset
                invalidate()
            }
        }

        // phoneCenterY floats up-down
        translateYAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                val offset = floatRangeY * fraction - (floatRangeY / 2f)
                phoneCenterY = initialPhoneCenterY + offset
                invalidate()
            }
        }

        // Gentle rotation
        rotationAnimator = ValueAnimator.ofFloat(-rotationRange, rotationRange).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { anim ->
                currentRotation = baseRotation + (anim.animatedValue as Float)
                invalidate()
            }
        }

        levitateAnimatorSet = AnimatorSet().apply {
            playTogether(translateXAnimator, translateYAnimator, rotationAnimator)
            start()
        }
    }

    private fun pauseLevitateAnimation() {
        levitateAnimatorSet?.pause()
    }

    private fun resumeLevitateAnimation() {
        levitateAnimatorSet?.resume()
    }

    private fun stopLevitateAnimation() {
        levitateAnimatorSet?.cancel()
        levitateAnimatorSet = null
    }

    //----------------------------------------------------------------------------------------------
    //  Shimmer Animations
    //----------------------------------------------------------------------------------------------
    private fun startShimmerAnimation() {
        if (shimmerAnimator == null) {
            shimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 100000
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                addUpdateListener {
                    shimmerOffset = it.animatedFraction
                    invalidate()
                }
            }
        }
        if (shimmerAnimator!!.isPaused) {
            shimmerAnimator!!.resume()
        } else if (!shimmerAnimator!!.isRunning) {
            shimmerAnimator!!.start()
        }
    }

    private fun pauseShimmerAnimation() {
        shimmerAnimator?.pause()
    }

    private fun resumeShimmerAnimation() {
        shimmerAnimator?.resume()
    }

    private fun stopShimmerAnimation() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null
    }

    //----------------------------------------------------------------------------------------------
    //  Utility
    //----------------------------------------------------------------------------------------------
    private fun hitTestPhone(touchX: Float, touchY: Float): Boolean {
        // Convert the screen coords to phone coords, factoring in rotation.
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
