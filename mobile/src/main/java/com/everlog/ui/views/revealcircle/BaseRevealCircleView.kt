package com.everlog.ui.views.revealcircle

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlin.math.max

abstract class BaseRevealCircleView(context: Context,
                                    layoutId: Int,
                                    layoutParams: ViewGroup.LayoutParams? = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)) : LinearLayout(context) {

    private val DURATION = 500L

    private var mRevealPoint: Point? = null
    private var mLayoutParams: ViewGroup.LayoutParams? = layoutParams

    internal abstract fun tag(): String

    internal abstract fun onReady()

    init {
        setupLayout(layoutId)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        onReady()
    }

    fun show(viewGroup: ViewGroup?, revealLocation: Point) {
        // Just in case remove any other timer
        val view = viewGroup?.findViewWithTag<View>(tag())
        if (view != null) {
            viewGroup.removeView(view)
        }
        mRevealPoint = revealLocation
        viewGroup?.addView(this, mLayoutParams)
        val cx: Int = revealLocation.x
        val cy: Int = revealLocation.y
        val radius = max(viewGroup?.width ?: 0, viewGroup?.height ?: 0)
        val anim: Animator = ViewAnimationUtils.createCircularReveal(this, cx, cy, 0f, radius.toFloat())
        anim.duration = DURATION
        anim.start()
    }

    fun hide(viewGroup: ViewGroup?) {
        if (isAttachedToWindow) {
            val cx: Int = mRevealPoint?.x ?: 0
            val cy: Int = mRevealPoint?.y ?: 0
            val radius = max(viewGroup?.width ?: 0, viewGroup?.height ?: 0)
            val anim: Animator = ViewAnimationUtils.createCircularReveal(this, cx, cy, radius.toFloat(), 0f)
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    viewGroup?.removeView(this@BaseRevealCircleView)
                }
            })
            anim.duration = DURATION
            anim.start()
        }
    }

    // Setup

    open fun setupLayout(layoutId: Int) {
        if (layoutId != 0) {
            View.inflate(context, layoutId, this)
        }
        this.tag = tag()
    }
}
