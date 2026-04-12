package com.everlog.ui.activities.home.web

import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.databinding.ActivityWebviewBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter

class WebViewActivity : BaseActivity(), MvpViewWeb {

    private var mPresenter: PresenterWebView? = null
    private lateinit var binding: ActivityWebviewBinding

    override fun onActivityCreated() {
        setupTopBar()
        setupWebView()
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // Workaround appcompat-1.1.0 bug https://issuetracker.google.com/issues/141132133
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
        if (Build.VERSION.SDK_INT in 21..22) {
            return
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_WEB_VIEW
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_webview
    }

    override fun getBindingView(): View? {
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun getWebUrl(): String {
        return intent.getStringExtra(ELConstants.EXTRA_WEB_URL)!!
    }

    override fun getWebTitle(): String {
        return intent.getStringExtra(ELConstants.EXTRA_WEB_TITLE)!!
    }

    override fun loadWebsite(url: String) {
        binding.webView.loadUrl(url)
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterWebView()
    }

    private fun setupTopBar() {
//        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.title = getWebTitle()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        binding.toolbar.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupWebView() {
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                if (progress < 100 && binding.toolbarProgress.visibility != ProgressBar.VISIBLE) {
                    binding.toolbarProgress.visibility = ProgressBar.VISIBLE
                }
                if (progress >= 100) {
                    binding.toolbarProgress.visibility = ProgressBar.INVISIBLE
                }
            }
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return false
            }
        }
    }
}