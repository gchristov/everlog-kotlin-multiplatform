package com.everlog.ui.onboarding

import android.app.Activity
import android.graphics.Point
import android.graphics.Rect
import android.view.View
import com.everlog.R
import com.everlog.utils.Utils
import com.everlog.utils.ViewUtils
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetView
import java.lang.ref.WeakReference
import java.util.*

abstract class BaseOnboardingController<T : Activity> constructor(context: T) {

    private var mActivity: WeakReference<T>? = null

    init {
        mActivity = WeakReference(context)
    }

    private var mCurrentTarget: TapTarget? = null
    private val mCurrentSequenceSteps: MutableList<TapTarget> = ArrayList()

    protected abstract fun doCheckOnboarding()

    protected fun getActivity(): T {
        return mActivity?.get()!!
    }

    fun checkOnboarding() {
        Utils.runWithDelay({
            if (Utils.isValidContext(mActivity?.get())) {
                doCheckOnboarding()
            }
        }, 500)
    }

    fun buildTapTarget(view: View,
                       title: String,
                       description: String,
                       id: Int): TapTarget {
        return TapTarget.forView(view, title, description)
                        .id(id)
    }

    fun buildTapTarget(rect: Rect,
                       title: String,
                       description: String,
                       id: Int): TapTarget {
        return TapTarget.forBounds(rect, title, description)
                .id(id)
    }

    fun showTarget(target: TapTarget, listener: TapTargetView.Listener? = null): Boolean {
        // Make sure there aren't any ongoing walkthroughs
        if (mCurrentTarget == null && mCurrentSequenceSteps.isEmpty()) {
            // Keep track of current steps
            mCurrentTarget = target
            TapTargetView.showFor(getActivity(), styleTarget(target), object : TapTargetView.Listener() {

                override fun onTargetCancel(view: TapTargetView?) {
                    super.onTargetCancel(view)
                    listener?.onTargetCancel(view)
                    mCurrentTarget = null
                }

                override fun onTargetDismissed(view: TapTargetView?, userInitiated: Boolean) {
                    super.onTargetDismissed(view, userInitiated)
                    listener?.onTargetDismissed(view, userInitiated)
                    mCurrentTarget = null
                }

                override fun onOuterCircleClick(view: TapTargetView?) {
                    super.onOuterCircleClick(view)
                    listener?.onOuterCircleClick(view)
                }

                override fun onTargetClick(view: TapTargetView?) {
                    super.onTargetClick(view)
                    listener?.onTargetClick(view)
                }

                override fun onTargetLongClick(view: TapTargetView?) {
                    super.onTargetLongClick(view)
                    listener?.onTargetLongClick(view)
                }
            })
            return true
        }
        return false
    }

    fun showSequence(steps: List<TapTarget>): Boolean {
        val targets: MutableList<TapTarget> = ArrayList()
        for (step in steps) {
            // Make sure there aren't any repeating steps
            if (!alreadyContainsStep(step, mCurrentSequenceSteps) && mCurrentTarget == null) {
                // Keep track of current steps
                mCurrentSequenceSteps.add(step)
                targets.add(styleTarget(step))
            }
        }
        if (targets.size == steps.size) {
            TapTargetSequence(getActivity())
                    .targets(targets)
                    .listener(object : TapTargetSequence.Listener {

                        override fun onSequenceFinish() {
                            mCurrentSequenceSteps.clear()
                        }

                        override fun onSequenceCanceled(lastTarget: TapTarget) {
                            mCurrentSequenceSteps.clear()
//                            val step = steps[targets.indexOf(lastTarget)]
//                            if (step != null) {
//                                if (step.cancelAction != null) {
//                                    step.cancelAction.run()
//                                }
//                            }
                        }

                        override fun onSequenceStep(lastTarget: TapTarget, targetClicked: Boolean) {
//                            val step = steps[targets.indexOf(lastTarget)]
//                            if (step != null) {
//                                if (step.target != null) {
//                                    step.target.performClick()
//                                }
//                                if (step.action != null) {
//                                    step.action.run()
//                                }
//                            }
                        }
                    }).start()
            return true
        }
        return false
    }

    private fun alreadyContainsStep(step: TapTarget, steps: List<TapTarget>): Boolean {
        for (s in steps) {
            if (s.id() == step.id()) {
                return true
            }
        }
        return false
    }

    private fun styleTarget(target: TapTarget): TapTarget {
        return target
                .outerCircleColor(R.color.main_accent)
                .titleTextSize(24)
                .titleTextColor(R.color.background_base)
                .descriptionTextColor(R.color.background_base)
                .dimColor(R.color.black_lighter)
                .transparentTarget(true)
    }

    internal fun getViewRect(view: View, radiusDp: Int): Rect {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val center = Point(location[0] + view.width/2, location[1] + view.height/2)
        val r = ViewUtils.dpToPx(radiusDp/2)
        return Rect(center.x - r, center.y - r, center.x + r, center.y + r)
    }
}