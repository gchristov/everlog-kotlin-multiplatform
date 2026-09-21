package com.everlog.ui.views.notification.home

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.everlog.config.HomeNotification
import com.everlog.databinding.ViewNotificationHomeBinding
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.ui.activities.home.HomeActivity
import com.everlog.ui.views.base.BaseView
import com.everlog.ui.views.base.BaseViewMvpView
import com.everlog.ui.views.base.BaseViewPresenter
import com.everlog.utils.glide.ELGlideModule
import com.jakewharton.rxbinding.view.RxView
import rx.Observable

class HomeNotificationView(context: Context, attrs: AttributeSet?) : BaseView(context, attrs), MvpViewHomeNotification {

    private var _binding: ViewNotificationHomeBinding? = null
    private val binding get() = _binding!!

    private var mPresenter: PresenterHomeNotification? = null
    private var mNotification: HomeNotification? = null

    override fun setupLayout(attrs: AttributeSet?, defStyleAttr: Int) {
        _binding = ViewNotificationHomeBinding.inflate(LayoutInflater.from(context), this, true)
    }

    public override fun onViewCreated() {
        // No-op
    }

    public override fun getLayoutResId(): Int {
        return 0 // Not used with ViewBinding
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

    override fun hideNotification() {
        visibility = GONE
    }

    override fun getNotification(): HomeNotification? {
        return mNotification
    }

    override fun showPlans() {
        (context as? HomeActivity)?.showPlans()
    }

    override fun showSettings() {
        (context as? HomeActivity)?.showSettings()
    }

    fun showHomeNotification(notification: HomeNotification?) {
        mNotification = notification
        val shouldShow = AppLaunchManager.manager.shouldShowHomeNotification(notification)
        visibility = if (shouldShow) VISIBLE else GONE
        if (shouldShow) {
            renderNotification(notification!!)
        }
    }

    // Render

    private fun renderNotification(notification: HomeNotification) {
        binding.titleLbl.text = notification.title
        binding.descriptionLbl.text = notification.description
        val hasImage = !TextUtils.isEmpty(notification.imageUrl)
        binding.imageView.visibility = if (hasImage) VISIBLE else GONE
        binding.updateLbl.visibility = if (notification.appUpdateRequired()) VISIBLE else GONE
        if (hasImage) {
            ELGlideModule.loadImage(notification.imageUrl, binding.imageView)
        }
    }

    // Setup

    public override fun setupPresenter() {
        mPresenter = PresenterHomeNotification()
    }
}