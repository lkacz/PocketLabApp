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
import kotlin.random.Random

/**
 * A custom View featuring:
 * - A static two-color background:
 *   (1) Top half blue (#E3F2FD),
 *   (2) Bottom half green (#C8E6C9).
 * - Seven parallax layers (1–4 for blue “sky” + clouds, 5–7 for green “grass”),
 *   from slowest to fastest. White cloud layer is always above the blue layers.
 * - A phone graphic in the middle, levitating with a shimmer effect.
 */
class PocketLabLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    //----------------------------------------------------------------------------------------------
    //  Parallax Structures
    //----------------------------------------------------------------------------------------------

    /** A single “box” that scrolls from right to left. */
    data class ParallaxBox(
        var x: Float,
        var y: Float,
        var width: Float,
        var height: Float,
        var color: Int
    )

    /** A layer that has multiple ParallaxBoxes and a speed at which they move. */
    data class ParallaxLayer(
        val boxes: MutableList<ParallaxBox>,
        val speed: Float     // pixels per second
    )

    private val parallaxLayers = mutableListOf<ParallaxLayer>()
    private val parallaxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Parallax animation
    private var parallaxAnimator: ValueAnimator? = null
    private var lastFrameTimeNanos = 0L

    //----------------------------------------------------------------------------------------------
    //  Phone + Text + Stripes
    //----------------------------------------------------------------------------------------------

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

    // Stripe paints
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

        // Place the phone around 45% so it's visually between the top (blue) half and bottom (green) half
        phoneCenterX = w * 0.5f
        phoneCenterY = h * 0.45f

        // Remember these as "rest" positions
        initialPhoneCenterX = phoneCenterX
        initialPhoneCenterY = phoneCenterY

        currentRotation = baseRotation

        // Start phone levitation & shimmer
        startLevitateAnimation()
        startShimmerAnimation()

        stripeThickness = h * 0.05f
        stripeA_Y = h * 0.57f
        stripeB_Y = h * 0.60f

        // Initialize parallax layers
        initParallaxLayers(w, h)
        startParallaxAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1) Draw static top half (blue)
        val halfHeight = (height / 2f)
        val topRect = RectF(0f, 0f, width.toFloat(), halfHeight)
        canvas.drawRect(topRect, Paint().apply { color = Color.parseColor("#E3F2FD") })

        // 2) Draw static bottom half (green)
        val bottomRect = RectF(0f, halfHeight, width.toFloat(), height.toFloat())
        canvas.drawRect(bottomRect, Paint().apply { color = Color.parseColor("#C8E6C9") })

        // 3) Draw parallax layers on top
        drawParallaxBackground(canvas)

        // 4) Draw title text
        val textY = height * 0.2f
        canvas.drawText("Pocket Lab App", width * 0.5f, textY, textPaint)

        // 5) Stripe A
        canvas.drawRect(0f, stripeA_Y, width.toFloat(), stripeA_Y + stripeThickness, stripePaintA)

        // 6) Clip so the phone + screen are behind next stripes
        canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), stripeB_Y)

        // 7) Translate + rotate phone
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

        // 8) Shimmering screen
        screenRect.set(phoneRect)
        val border = phoneWidth * 0.08f
        screenRect.inset(border, border)
        drawShimmeringScreen(canvas, screenRect)

        canvas.restore()

        // 9) Stripe B
        canvas.drawRect(0f, stripeB_Y, width.toFloat(), stripeB_Y + stripeThickness, stripePaintB)

        // 10) Stripe C
        val stripeC_Top = stripeB_Y + stripeThickness
        canvas.drawRect(0f, stripeC_Top, width.toFloat(), height.toFloat(), stripePaintC)
    }

    //----------------------------------------------------------------------------------------------
    //  Parallax: Initialization + Drawing + Updates
    //----------------------------------------------------------------------------------------------

    /**
     * Create 7 layers from slowest to fastest.
     *  - parallax1: Blue (light)
     *  - parallax2: Blue (medium)
     *  - parallax3: Blue (darker)
     *  - parallax4: White clouds (always on top of the blues)
     *  - parallax5: Green (light)
     *  - parallax6: Green (medium)
     *  - parallax7: Green (darker, fastest)
     *
     * Each set is restricted to either top half (blue layers) or bottom half (green layers).
     */
    private fun initParallaxLayers(w: Int, h: Int) {
        parallaxLayers.clear()

        // define speeds (from slowest to fastest)
        val speed1 = 8f
        val speed2 = 11f
        val speed3 = 14f
        val speed4 = 17f
        val speed5 = 20f
        val speed6 = 23f
        val speed7 = 26f

        // define color ranges
        val topHalf = 0f..(h / 2f)
        val bottomHalf = (h / 2f)..h.toFloat()

        // Blue-ish layers
        // 1) Light blue
        val layer1 = ParallaxLayer(
            generateBoxes(
                count = 15,
                screenWidth = w,
                screenHeight = h,
                minY = topHalf.start,
                maxY = topHalf.endInclusive,
                colorRange = 0xFFBBDEFB.toInt()..0xFF90CAF9.toInt(),
                cloudChance = 0f
            ),
            speed1
        )
        // 2) Medium blue
        val layer2 = ParallaxLayer(
            generateBoxes(
                count = 15,
                screenWidth = w,
                screenHeight = h,
                minY = topHalf.start,
                maxY = topHalf.endInclusive,
                colorRange = 0xFF90CAF9.toInt()..0xFF64B5F6.toInt(),
                cloudChance = 0f
            ),
            speed2
        )
        // 3) Darker blue
        val layer3 = ParallaxLayer(
            generateBoxes(
                count = 15,
                screenWidth = w,
                screenHeight = h,
                minY = topHalf.start,
                maxY = topHalf.endInclusive,
                colorRange = 0xFF64B5F6.toInt()..0xFF2196F3.toInt(),
                cloudChance = 0f
            ),
            speed3
        )
        // 4) White clouds (slightly varying whiteness), near top as well
        val layer4 = ParallaxLayer(
            generateBoxes(
                count = 15,
                screenWidth = w,
                screenHeight = h,
                minY = topHalf.start,
                // If you want them fairly high, reduce the maxY (like 0.3f*h).
                // For now we put them anywhere in top half.
                maxY = topHalf.endInclusive,
                colorRange = 0xFFECECEC.toInt()..0xFFFFFFFF.toInt(),
                cloudChance = 1f  // effectively all white, with slight variations
            ),
            speed4
        )

        // Green-ish layers
        // 5) Light green
        val layer5 = ParallaxLayer(
            generateBoxes(
                count = 15,
                screenWidth = w,
                screenHeight = h,
                minY = bottomHalf.start,
                maxY = bottomHalf.endInclusive,
                colorRange = 0xFFC8E6C9.toInt()..0xFFA5D6A7.toInt(),
                cloudChance = 0f
            ),
            speed5
        )
        // 6) Medium green
        val layer6 = ParallaxLayer(
            generateBoxes(
                count = 15,
                screenWidth = w,
                screenHeight = h,
                minY = bottomHalf.start,
                maxY = bottomHalf.endInclusive,
                colorRange = 0xFFA5D6A7.toInt()..0xFF81C784.toInt(),
                cloudChance = 0f
            ),
            speed6
        )
        // 7) Darker green (fastest)
        val layer7 = ParallaxLayer(
            generateBoxes(
                count = 15,
                screenWidth = w,
                screenHeight = h,
                minY = bottomHalf.start,
                maxY = bottomHalf.endInclusive,
                colorRange = 0xFF81C784.toInt()..0xFF4CAF50.toInt(),
                cloudChance = 0f
            ),
            speed7
        )

        // Add them in order from far (slowest) to near (fastest)
        parallaxLayers.addAll(listOf(layer1, layer2, layer3, layer4, layer5, layer6, layer7))
    }

    private fun generateBoxes(
        count: Int,
        screenWidth: Int,
        screenHeight: Int,
        minY: Float,
        maxY: Float,
        colorRange: IntRange,
        cloudChance: Float
    ): MutableList<ParallaxBox> {
        val list = mutableListOf<ParallaxBox>()
        repeat(count) {
            val w = Random.nextFloat() * (screenWidth * 0.4f) + 50f
            val h = Random.nextFloat() * (screenHeight * 0.4f) + 20f

            val x = Random.nextFloat() * screenWidth
            val y = Random.nextFloat() * (maxY - minY) + minY

            // For clouds or near-white boxes, we just randomize in [minColor..maxColor].
            val color = if (Random.nextFloat() < cloudChance) {
                // But here cloudChance = 1f in layer4, so effectively we randomize whiteness
                randomColorInRange(colorRange.first, colorRange.last)
            } else {
                // Otherwise pick from the color range
                randomColorInRange(colorRange.first, colorRange.last)
            }
            list.add(ParallaxBox(x, y, w, h, color))
        }
        return list
    }

    /** Returns a random color between two ARGB values. */
    private fun randomColorInRange(minColor: Int, maxColor: Int): Int {
        val aMin = (minColor shr 24) and 0xFF
        val rMin = (minColor shr 16) and 0xFF
        val gMin = (minColor shr 8) and 0xFF
        val bMin = minColor and 0xFF

        val aMax = (maxColor shr 24) and 0xFF
        val rMax = (maxColor shr 16) and 0xFF
        val gMax = (maxColor shr 8) and 0xFF
        val bMax = maxColor and 0xFF

        val aLo = minOf(aMin, aMax)
        val aHi = maxOf(aMin, aMax)
        val rLo = minOf(rMin, rMax)
        val rHi = maxOf(rMin, rMax)
        val gLo = minOf(gMin, gMax)
        val gHi = maxOf(gMin, gMax)
        val bLo = minOf(bMin, bMax)
        val bHi = maxOf(bMin, bMax)

        val a = Random.nextInt(aLo, aHi + 1)
        val r = Random.nextInt(rLo, rHi + 1)
        val g = Random.nextInt(gLo, gHi + 1)
        val b = Random.nextInt(bLo, bHi + 1)

        return Color.argb(a, r, g, b)
    }

    private fun drawParallaxBackground(canvas: Canvas) {
        // Draw layers in the order they appear in parallaxLayers (slowest first -> fastest last).
        for (layer in parallaxLayers) {
            for (box in layer.boxes) {
                parallaxPaint.color = box.color
                canvas.drawRect(
                    box.x, box.y,
                    box.x + box.width,
                    box.y + box.height,
                    parallaxPaint
                )
            }
        }
    }

    /**
     * Shift each box left by (speed * deltaTime).
     * When a box goes off-screen on the left, re-randomize its x,y to re-enter on the right.
     */
    private fun updateParallaxBoxes(deltaTimeSec: Float) {
        val screenWidth = width.toFloat()
        for (layer in parallaxLayers) {
            val speed = layer.speed
            for (box in layer.boxes) {
                box.x -= speed * deltaTimeSec
                // If fully off the left side, respawn on the right
                if (box.x + box.width < 0f) {
                    box.x = screenWidth + Random.nextFloat() * screenWidth

                    // Identify which layer we are in
                    val index = parallaxLayers.indexOf(layer)
                    // Re-map the index to the correct vertical half
                    // 0..3 => top half, 4..6 => bottom half
                    if (index in 0..3) {
                        // top half
                        val minY = 0f
                        val maxY = height / 2f
                        box.y = Random.nextFloat() * (maxY - minY) + minY
                    } else {
                        // bottom half
                        val minY = height / 2f
                        val maxY = height.toFloat()
                        box.y = Random.nextFloat() * (maxY - minY) + minY
                    }
                }
            }
        }
    }

    private fun startParallaxAnimation() {
        if (parallaxAnimator == null) {
            parallaxAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 100000 // large duration
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                addUpdateListener {
                    val nowNanos = System.nanoTime()
                    if (lastFrameTimeNanos == 0L) {
                        lastFrameTimeNanos = nowNanos
                        return@addUpdateListener
                    }
                    val deltaTimeSec = (nowNanos - lastFrameTimeNanos) / 1_000_000_000f
                    lastFrameTimeNanos = nowNanos

                    updateParallaxBoxes(deltaTimeSec)
                    invalidate()
                }
            }
        }
        if (parallaxAnimator!!.isPaused) {
            parallaxAnimator!!.resume()
        } else if (!parallaxAnimator!!.isRunning) {
            parallaxAnimator!!.start()
        }
    }

    private fun pauseParallaxAnimation() {
        parallaxAnimator?.pause()
    }

    private fun resumeParallaxAnimation() {
        parallaxAnimator?.resume()
    }

    private fun stopParallaxAnimation() {
        parallaxAnimator?.cancel()
        parallaxAnimator = null
    }

    //----------------------------------------------------------------------------------------------
    //  Shimmering Screen
    //----------------------------------------------------------------------------------------------

    private fun drawShimmeringScreen(canvas: Canvas, screenBounds: RectF) {
        val gradientWidth = screenBounds.width() * 2
        val leftX = screenBounds.left - gradientWidth + shimmerOffset * gradientWidth * 2
        val rightX = leftX + gradientWidth

        val gradientColors = intArrayOf(
            Color.parseColor("#555555"),
            Color.parseColor("#555555"),
            Color.parseColor("#555555"),
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

                    pauseLevitateAnimation()
                    pauseShimmerAnimation()
                    pauseParallaxAnimation()

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
                        // Animate X/Y back with bounce
                        animatePhoneBackToInitial()
                    } else {
                        // If user just tapped, resume from where we left off
                        resumeLevitateAnimation()
                        resumeShimmerAnimation()
                        resumeParallaxAnimation()
                    }
                }
            }
        }
        return true
    }

    //----------------------------------------------------------------------------------------------
    //  Bouncing back with standard ValueAnimator
    //----------------------------------------------------------------------------------------------
    private fun animatePhoneBackToInitial() {
        val animX = ValueAnimator.ofFloat(phoneCenterX, initialPhoneCenterX).apply {
            duration = 800
            interpolator = BounceInterpolator()
            addUpdateListener { animator ->
                phoneCenterX = animator.animatedValue as Float
                invalidate()
            }
        }
        val animY = ValueAnimator.ofFloat(phoneCenterY, initialPhoneCenterY).apply {
            duration = 800
            interpolator = BounceInterpolator()
            addUpdateListener { animator ->
                phoneCenterY = animator.animatedValue as Float
                invalidate()
            }
        }

        val bounceSet = AnimatorSet()
        bounceSet.playTogether(animX, animY)
        bounceSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                smoothlyResumeLevitate()
                resumeShimmerAnimation()
                resumeParallaxAnimation()
            }
        })
        bounceSet.start()
    }

    private fun smoothlyResumeLevitate() {
        if (levitateAnimatorSet == null) {
            startLevitateAnimation()
            return
        }

        val tx = translateXAnimator
        val ty = translateYAnimator
        val rot = rotationAnimator
        if (tx == null || ty == null || rot == null ||
            !tx.isRunning || !ty.isRunning || !rot.isRunning
        ) {
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
        // Convert screen coords to phone coords, factoring in rotation.
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
