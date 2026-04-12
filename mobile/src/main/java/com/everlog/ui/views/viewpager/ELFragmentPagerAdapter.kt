package com.everlog.ui.views.viewpager

import android.os.Parcelable
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.FragmentTransaction
import timber.log.Timber

open class ELFragmentPagerAdapter : FragmentStatePagerAdapter {

    private val TAG = "ELFragmentPagerAdapter"

    private var mFragments: List<Fragment> = ArrayList()
    private var mFragmentManager: FragmentManager? = null

    constructor(fm: FragmentManager) : super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        this.mFragmentManager = fm
    }

    constructor(fm: FragmentManager, fragments: List<Fragment>) : super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        this.mFragmentManager = fm
        this.mFragments = fragments
    }

    internal open fun setItems(items: List<Fragment>) {
        (mFragments as MutableList).clear()
        (mFragments as MutableList).addAll(items)
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): Fragment {
        return mFragments[position]
    }

    override fun getCount(): Int {
        return mFragments.size
    }

    override fun saveState(): Parcelable? {
        return null
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
        if (position <= count) {
            if (mFragmentManager != null) {
                try {
                    val trans: FragmentTransaction = mFragmentManager!!.beginTransaction()
                    trans.remove((`object` as Fragment))
                    trans.commit()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Timber.tag(TAG).e(e)
                }
            }
        }
    }

    override fun getItemPosition(`object`: Any): Int {
        val oneMapFragment: Fragment = `object` as Fragment
        val index = mFragments.indexOf(oneMapFragment)
        return if (index == -1) POSITION_NONE else index
    }
}