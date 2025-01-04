package com.lkacz.pola

import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation

object SlideTransitionHelper {

    /**
     * IN LEFT
     * Equivalent to translating from -100% to 0, with alpha 0 to 1.
     */
    fun inLeftAnimation(duration: Long = 300L): Animation {
        val translate = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, -1.0f, // fromXDelta = -100%p
            Animation.RELATIVE_TO_PARENT, 0.0f,  // toXDelta = 0
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f
        ).apply { this.duration = duration }

        val alpha = AlphaAnimation(0f, 1f).apply {
            this.duration = duration
        }

        return AnimationSet(false).apply {
            addAnimation(translate)
            addAnimation(alpha)
        }
    }

    /**
     * IN RIGHT
     * Equivalent to translating from 100% to 0, with alpha 0 to 1.
     */
    fun inRightAnimation(duration: Long = 300L): Animation {
        val translate = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 1.0f,  // fromXDelta = 100%p
            Animation.RELATIVE_TO_PARENT, 0.0f,  // toXDelta = 0
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f
        ).apply { this.duration = duration }

        val alpha = AlphaAnimation(0f, 1f).apply {
            this.duration = duration
        }

        return AnimationSet(false).apply {
            addAnimation(translate)
            addAnimation(alpha)
        }
    }

    /**
     * OUT LEFT
     * Equivalent to translating from 0 to -100%p, with alpha 1 to 0.
     */
    fun outLeftAnimation(duration: Long = 300L): Animation {
        val translate = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0.0f,   // fromXDelta = 0
            Animation.RELATIVE_TO_PARENT, -1.0f,  // toXDelta = -100%p
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f
        ).apply { this.duration = duration }

        val alpha = AlphaAnimation(1f, 0f).apply {
            this.duration = duration
        }

        return AnimationSet(false).apply {
            addAnimation(translate)
            addAnimation(alpha)
        }
    }

    /**
     * OUT RIGHT
     * Equivalent to translating from 0 to 100%p, with alpha 1 to 0.
     */
    fun outRightAnimation(duration: Long = 300L): Animation {
        val translate = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0.0f,  // fromXDelta = 0
            Animation.RELATIVE_TO_PARENT, 1.0f,  // toXDelta = 100%p
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0.0f
        ).apply { this.duration = duration }

        val alpha = AlphaAnimation(1f, 0f).apply {
            this.duration = duration
        }

        return AnimationSet(false).apply {
            addAnimation(translate)
            addAnimation(alpha)
        }
    }
}
