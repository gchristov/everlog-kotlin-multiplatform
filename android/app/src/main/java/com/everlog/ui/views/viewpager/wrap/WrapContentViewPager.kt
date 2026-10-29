package com.everlog.ui.views.viewpager.wrap

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import timber.log.Timber

/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Raanan Nevet
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
class WrapContentViewPager : ViewPager {
    private var mHeight = 0
    private var decorHeight = 0
    private var widthMeasuredSpec = 0
    private var animateHeight = false
    private var rightHeight = 0
    private var leftHeight = 0
    private var scrollingPosition = -1

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        addOnPageChangeListener(object : OnPageChangeListener {
            var state = 0
            override fun onPageScrolled(position: Int, offset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                if (state == SCROLL_STATE_IDLE) {
                    mHeight = 0 // measure the selected page in-case it's a change without scrolling
                    Timber.tag(TAG).d("onPageSelected:$position")
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                this.state = state
            }
        })
    }

    fun refresh() {
        mHeight = 0
        rightHeight = 0
        leftHeight = 0
        scrollingPosition = -1
        requestLayout()
        invalidate()
    }

    override fun setAdapter(adapter: PagerAdapter?) {
        require(adapter is ObjectAtPositionInterface) { "WrapContentViewPage requires that PagerAdapter will implement ObjectAtPositionInterface" }
        mHeight = 0 // so we measure the new content in onMeasure
        super.setAdapter(adapter)
    }

    /**
     * Allows to redraw the view size to wrap the content of the bigger child.
     *
     * @param widthMeasureSpec  with measured
     * @param heightMeasureSpec height measured
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasureSpec = heightMeasureSpec
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        widthMeasuredSpec = widthMeasureSpec
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        if (mode == MeasureSpec.UNSPECIFIED || mode == MeasureSpec.AT_MOST) {
            if (mHeight == 0) {
                // measure vertical decor (i.e. PagerTitleStrip) based on ViewPager implementation
                decorHeight = 0
                for (i in 0 until childCount) {
                    val child = getChildAt(i)
                    val lp = child.layoutParams as LayoutParams
                    if (lp.isDecor) {
                        val vgrav = lp.gravity and Gravity.VERTICAL_GRAVITY_MASK
                        val consumeVertical = vgrav == Gravity.TOP || vgrav == Gravity.BOTTOM
                        if (consumeVertical) {
                            decorHeight += child.measuredHeight
                        }
                    }
                }

                // make sure that we have an height (not sure if this is necessary because it seems that onPageScrolled is called right after
                val position = currentItem
                val child = getViewAtPosition(position)
                if (child != null) {
                    mHeight = measureViewHeight(child)
                }
                Timber.tag(TAG).d("onMeasure height:$mHeight decor:$decorHeight")
            }
            val totalHeight = mHeight + decorHeight + paddingBottom + paddingTop
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(totalHeight, MeasureSpec.EXACTLY)
            Timber.tag(TAG).d("onMeasure total height:$totalHeight")
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    public override fun onPageScrolled(position: Int, offset: Float, positionOffsetPixels: Int) {
        super.onPageScrolled(position, offset, positionOffsetPixels)
        // cache scrolled view heights
        if (scrollingPosition != position) {
            scrollingPosition = position
            // scrolled position is always the left scrolled page
            val leftView = getViewAtPosition(position)
            val rightView = getViewAtPosition(position + 1)
            if (leftView != null && rightView != null) {
                leftHeight = measureViewHeight(leftView)
                rightHeight = measureViewHeight(rightView)
                animateHeight = true
                Timber.tag(TAG).d("onPageScrolled heights left:$leftHeight right:$rightHeight")
            } else {
                animateHeight = false
            }
        }
        if (animateHeight) {
            val newHeight = (leftHeight * (1 - offset) + rightHeight * offset).toInt()
            if (mHeight != newHeight) {
                Timber.tag(TAG).d("onPageScrolled height change:$newHeight")
                mHeight = newHeight
                requestLayout()
                invalidate()
            }
        }
    }

    private fun measureViewHeight(view: View): Int {
        view.measure(ViewGroup.getChildMeasureSpec(widthMeasuredSpec, paddingLeft + paddingRight, view.layoutParams.width), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        return view.measuredHeight
    }

    private fun getViewAtPosition(position: Int): View? {
        if (adapter != null) {
            val objectAtPosition = (adapter as ObjectAtPositionInterface?)!!.getObjectAtPosition(position)
            if (objectAtPosition != null) {
                for (i in 0 until childCount) {
                    val child = getChildAt(i)
                    if (child != null && adapter!!.isViewFromObject(child, objectAtPosition)) {
                        return child
                    }
                }
            }
        }
        return null
    }

    companion object {
        private val TAG = WrapContentViewPager::class.java.simpleName
    }
}