package com.everlog.ui.fragments.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.databinding.FragmentOnboardingPageBinding
import com.everlog.ui.fragments.base.BaseFragment
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import com.everlog.ui.fragments.base.BaseFragmentPresenter
import com.everlog.utils.ActivityUtils

class OnboardingPageFragment(titleResId: Int,
                             subtitleResId: Int,
                             imageResId: Int) : BaseFragment(), MvpViewOnboardingPage {

    private var mPresenter: PresenterOnboardingPage? = null
    private var _binding: FragmentOnboardingPageBinding? = null
    private val binding get() = _binding!!

    init {
        val bundle = Bundle()
        bundle.putSerializable(ELConstants.EXTRA_TITLE, titleResId)
        bundle.putSerializable(ELConstants.EXTRA_SUBTITLE, subtitleResId)
        bundle.putSerializable(ELConstants.EXTRA_IMAGE, imageResId)
        arguments = bundle
    }

    override fun onFragmentCreated() {
        loadData()
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_onboarding_page
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? {
        _binding = FragmentOnboardingPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        return mPresenter as? BaseFragmentPresenter<T>
    }

    // Loading

    private fun loadData() {
        binding.titleLbl.setText(arguments?.getInt(ELConstants.EXTRA_TITLE) ?: -1)
        binding.subtitleLbl.setText(arguments?.getInt(ELConstants.EXTRA_SUBTITLE) ?: -1)
        ActivityUtils.setupBackgroundImage(view, arguments?.getInt(ELConstants.EXTRA_IMAGE) ?: -1, R.id.imageView)
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterOnboardingPage()
    }
}