package com.everlog.ui.adapters

import android.animation.Animator
import android.animation.ObjectAnimator
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import com.ahamed.multiviewadapter.ItemBinder
import com.ahamed.multiviewadapter.ItemViewHolder
import com.everlog.R
import com.everlog.data.model.pro.ELProFeature
import com.everlog.data.model.pro.ELProQuestion
import com.everlog.databinding.RowProFeatureBinding
import com.everlog.databinding.RowProQuestionBinding
import com.everlog.utils.ViewUtils

class ProUpgradeAdapter {

    class FeatureBinder : ItemBinder<ELProFeature, FeatureViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): FeatureViewHolder {
            return FeatureViewHolder(inflater.inflate(R.layout.row_pro_feature, parent, false))
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELProFeature
        }

        override fun bind(holder: FeatureViewHolder, item: ELProFeature) {
            holder.render()
        }
    }

    class QuestionBinder : ItemBinder<ELProQuestion, QuestionViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): QuestionViewHolder {
            return QuestionViewHolder(inflater.inflate(R.layout.row_pro_question, parent, false))
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELProQuestion
        }

        override fun bind(holder: QuestionViewHolder, item: ELProQuestion) {
            holder.render()
        }
    }

    class FeatureViewHolder(itemView: View) : ItemViewHolder<ELProFeature>(itemView) {

        private val binding = RowProFeatureBinding.bind(itemView)

        // Render

        fun render() {
            renderChecks()
            renderText()
        }

        private fun renderChecks() {
            binding.proCheck.visibility = View.VISIBLE
            binding.freeCheck.visibility = if (item.free && item.noticeTextResId == null) View.VISIBLE else View.GONE
        }

        private fun renderText() {
            binding.descriptionLbl.setText(item.textResId)
            binding.noticeLbl.visibility = if (item.noticeTextResId != null) View.VISIBLE else View.GONE
            if (item.noticeTextResId != null) {
                binding.noticeLbl.setText(item.noticeTextResId!!)
            }
        }
    }

    class QuestionViewHolder(itemView: View) : ItemViewHolder<ELProQuestion>(itemView) {

        private val binding = RowProQuestionBinding.bind(itemView)

        private var mIsExpanded = false
        private var initialAnswerHeight = 0

        companion object {
            private const val ANIMATION_DURATION = 300
        }

        init {
            binding.cardContainer.setOnClickListener {
                handleExpand()
            }
        }

        // Render

        fun render() {
            renderQuestion()
        }

        private fun renderQuestion() {
            binding.questionTitle.setText(item.questionResId)
            binding.questionAnswer.setText(item.answerResId)
        }

        // Handlers

        private fun handleExpand() {
            mIsExpanded = !mIsExpanded
            expandOrCollapseAnswer()
            expandOrCollapseArrow()
        }

        private fun expandOrCollapseArrow() {
            val rotate = RotateAnimation(if (mIsExpanded) 0f else 180f, if (mIsExpanded) 180f else 0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            rotate.duration = ANIMATION_DURATION.toLong()
            rotate.interpolator = LinearInterpolator()
            rotate.fillAfter = true
            binding.collapseIndicator.startAnimation(rotate)
        }

        private fun expandOrCollapseAnswer() {
            val height = binding.questionAnswer.measuredHeight
            if (initialAnswerHeight <= 0) {
                initialAnswerHeight = height
            }
            binding.questionAnswer.maxLines = if (mIsExpanded) Int.MAX_VALUE else 1
            binding.questionAnswer.ellipsize = if (mIsExpanded) null else TextUtils.TruncateAt.END
            binding.questionAnswer.measure(View.MeasureSpec.makeMeasureSpec(binding.questionAnswer.measuredWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(if (mIsExpanded) ViewUtils.dpToPx(1000) else initialAnswerHeight, View.MeasureSpec.AT_MOST))
            val newHeight = binding.questionAnswer.measuredHeight
            val animation = ObjectAnimator.ofInt(binding.questionAnswer, "height", height, newHeight)
            animation.setDuration(ANIMATION_DURATION.toLong()).addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    binding.questionAnswer.maxLines = if (mIsExpanded) Int.MAX_VALUE else 1
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}
            })
            animation.start()
        }
    }
}