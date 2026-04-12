package com.everlog.utils

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.everlog.R
import java.util.concurrent.TimeUnit

class AnimationUtils {

    companion object {

        private val INTERVAL_BOUNCE = TimeUnit.SECONDS.toMillis(1)
        private val INTERVAL_PAGER = 300

        private var mPrevTriggerTimeBounce: Long? = null
        private var mPrevTriggerTimePager: Long? = null

        fun bounce(view: View?) {
            val now = System.currentTimeMillis()
            if (mPrevTriggerTimeBounce == null || (now - mPrevTriggerTimeBounce!! >= INTERVAL_BOUNCE)) {
                mPrevTriggerTimeBounce = now
                val animation: ObjectAnimator = ObjectAnimator.ofPropertyValuesHolder(
                        view,
                        PropertyValuesHolder.ofFloat("scaleX", 1.2f),
                        PropertyValuesHolder.ofFloat("scaleY", 1.2f))
                animation.duration = 200
                animation.repeatCount = 1
                animation.repeatMode = ObjectAnimator.REVERSE
                animation.start()
            }
        }

        @JvmStatic
        fun pagerAnimation(view: View?, reverse: Boolean) {
            val now = System.currentTimeMillis()
            if (mPrevTriggerTimePager == null || (now - mPrevTriggerTimePager!! >= INTERVAL_PAGER)) {
                mPrevTriggerTimePager = now
                val slideOut = AnimationUtils.loadAnimation(view!!.context, if (reverse) R.anim.slide_out_reverse else R.anim.slide_out)
                slideOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationRepeat(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        val slideIn = AnimationUtils.loadAnimation(view.context, if (reverse) R.anim.slide_in_reverse else R.anim.slide_in)
                        view.startAnimation(slideIn)
                    }
                })
                view.startAnimation(slideOut)
            }
        }
    }
}