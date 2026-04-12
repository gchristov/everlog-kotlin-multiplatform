package com.everlog.ui.views.tab

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import com.everlog.R
import com.everlog.databinding.ViewTabsBinding
import com.everlog.ui.fragments.base.BaseTabFragment
import com.everlog.ui.views.viewpager.ELFragmentPagerAdapter
import com.google.android.material.tabs.TabLayout

class TabsPage : LinearLayout {

    private lateinit var binding: ViewTabsBinding
    private var mAdapter: ELFragmentPagerAdapter? = null

    constructor(context: Context?) : super(context) {
        init(null, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        setupLayout()
    }

    fun enableNewBadge(index: Int) {
        binding.tabLayout.getTabAt(index)?.customView?.findViewById<View>(R.id.badge)?.visibility = View.VISIBLE
    }

    fun setTabs(fragmentManager: FragmentManager,
                fragments: List<BaseTabFragment>,
                selectedIndex: Int? = 0,
                tabChangeListener: TabLayout.OnTabSelectedListener? = null) {
        if (mAdapter == null) {
            mAdapter = ELFragmentPagerAdapter(fragmentManager)
            binding.pager.adapter = mAdapter
        }
        mAdapter?.setItems(fragments)
        binding.pager.offscreenPageLimit = fragments.size
        binding.pager.clearOnPageChangeListeners()
        binding.pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
                // No-op
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
                // No-op
            }

            override fun onPageSelected(p0: Int) {
                if (binding.tabLayout.selectedTabPosition != p0) {
                    val tab = binding.tabLayout.getTabAt(p0)
                    tab?.select()
                }
            }
        })
        // Link tab bar
        binding.tabLayout.removeAllTabs()
        fragments.forEach {
            val tab = binding.tabLayout.newTab().setText(context.getString(it.getTitleResId())).setCustomView(R.layout.view_tab)
            binding.tabLayout.addTab(tab)
        }
        binding.tabLayout.clearOnTabSelectedListeners()
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {
                tabChangeListener?.onTabReselected(tab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tabChangeListener?.onTabUnselected(tab)
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                if (binding.pager.currentItem != tab.position) {
                    binding.pager.setCurrentItem(tab.position, true)
                }
                selectTab(tab.position)
                tabChangeListener?.onTabSelected(tab)
            }
        })
        binding.pager.setCurrentItem(selectedIndex ?: 0, false)
        selectTab(selectedIndex ?: 0)
    }

    private fun selectTab(position: Int) {
        for (i in 0 until binding.tabLayout.tabCount) {
            val tab = binding.tabLayout.getTabAt(i)
            val titleView = tab?.customView?.findViewById<TextView>(android.R.id.text1)
            titleView?.let {
                it.setTextColor(ContextCompat.getColor(it.context, if (i == position) R.color.white_base else R.color.gray_1))
                it.background = ContextCompat.getDrawable(context, if (i == position) R.drawable.rounded_corners_home_tab_selected else R.drawable.rounded_corners_home_tab)
            }
        }
    }

    // Setup

    private fun setupLayout() {
        binding = ViewTabsBinding.inflate(LayoutInflater.from(context), this, true)
    }
}