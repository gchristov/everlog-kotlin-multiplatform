package com.everlog.android.plan.create

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.everlog.android.common.mvp.view.BaseView
import com.everlog.android.common.mvp.view.BaseViewMvpView
import com.everlog.android.common.mvp.view.BaseViewPresenter
import com.everlog.android.plan.R
import com.everlog.android.plan.adapter.MAX_FREE_WEEK_DAYS
import com.everlog.android.plan.databinding.ViewNotificationWarningBinding
import com.everlog.android.common.mvp.util.ViewUtils
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import com.everlog.android.common.design.R as DesignR

class WarningNotificationView(context: Context, attrs: AttributeSet?) : BaseView(context, attrs), MvpViewWarningNotification {

    enum class WarningType {
        PRO_PLAN_DAYS,
        PRO_MUSCLE_GOALS
    }

    private var _binding: ViewNotificationWarningBinding? = null
    private val binding get() = _binding!!

    private var mPresenter: PresenterWarningNotification? = null

    private lateinit var mType: WarningType

    override fun setupLayout(attrs: AttributeSet?, defStyleAttr: Int) {
        _binding = ViewNotificationWarningBinding.inflate(LayoutInflater.from(context), this, true)
    }

    public override fun onViewCreated() {
        // No-op
    }

    public override fun getLayoutResId(): Int {
        return 0
    }

    override fun <T : BaseViewMvpView> getPresenter(): BaseViewPresenter<T>? {
        return mPresenter as? BaseViewPresenter<T>
    }

    override fun onClickClose(): Observable<Void> {
        return RxView.clicks(binding.closeBtn)
    }

    override fun onClickAction(): Observable<Void> {
        return RxView.clicks(binding.containerLayout)
    }

    override fun getType(): WarningType {
        return mType
    }

    override fun hideNotification() {
        visibility = GONE
    }

    fun setType(type: WarningType) {
        this.mType = type
        render()
    }

    // Render

    private fun render() {
        when (mType) {
            WarningType.PRO_PLAN_DAYS,
            WarningType.PRO_MUSCLE_GOALS-> {
                val p = ViewUtils.dpToPx(6)
                binding.imageView.setPadding(p, p, p, p)
                binding.imageView.setImageResource(DesignR.drawable.ic_lock)
                binding.imageView.setBackgroundResource(DesignR.drawable.ic_hexagon)
                ImageViewCompat.setImageTintList(binding.imageView, ColorStateList.valueOf(ContextCompat.getColor(context, DesignR.color.background_card_lighter)));
                if (mType == WarningType.PRO_PLAN_DAYS) {
                    binding.titleLbl.text = context?.getString(R.string.pro_prompt_plan_title, MAX_FREE_WEEK_DAYS)
                    binding.descriptionLbl.text = context?.getString(R.string.pro_prompt_plan_subtitle)
                } else if (mType == WarningType.PRO_MUSCLE_GOALS) {
                    binding.titleLbl.text = context?.getString(R.string.pro_prompt_muscle_goals_title)
                    binding.descriptionLbl.text = context?.getString(R.string.pro_prompt_muscle_goals_subtitle)
                }
            }
        }
    }

    // Setup

    public override fun setupPresenter() {
        mPresenter = PresenterWarningNotification()
    }
}