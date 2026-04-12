package com.everlog.ui.activities.home.search

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.PorterDuff
import android.os.Build
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.ui.activities.base.BaseActivity
import rx.subjects.PublishSubject

abstract class BaseSearchActivity : BaseActivity() {

    abstract fun getSearchToolbar(): Toolbar

    protected var mSearchMenu: MenuItem? = null

    private var mFirstSearchIgnored = false // First search clears the text view.
    private var mSearchExpanded = false

    protected var mOnSearchChanged = PublishSubject.create<String>()
    protected var mOnSearchShown = PublishSubject.create<Void>()
    protected var mOnSearchHidden = PublishSubject.create<Void>()

    override fun onActivityCreated() {
        setupToolbarSearch()
        setupSearchView()
    }

    override fun onBackPressed() {
        if (mSearchExpanded) {
            stopSearch()
        } else {
            super.onBackPressed()
        }
    }

    open fun stopSearch() {
        getSearchToolbar().collapseActionView()
    }

    protected fun showSearch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            circleReveal(getSearchToolbar().id, 1, true, true)
        } else {
            getSearchToolbar().visibility = View.VISIBLE
        }
        mSearchMenu?.expandActionView()
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun circleReveal(
        viewID: Int,
        posFromRight: Int,
        containsOverflow: Boolean,
        isShow: Boolean
    ) {
        val myView = findViewById<View>(viewID)
        val width = myView.width
        val cx = width
        val cy = myView.height / 2

        val anim: Animator = if (isShow)
            ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0f, width.toFloat())
        else
            ViewAnimationUtils.createCircularReveal(myView, cx, cy, width.toFloat(), 0f)

        anim.duration = 220L

        // make the view invisible when the animation is done
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!isShow) {
                    super.onAnimationEnd(animation)
                    myView.visibility = View.INVISIBLE
                }
            }
        })
        // make the view visible and start the animation
        if (isShow) {
            myView.visibility = View.VISIBLE
        }
        // start the animation
        anim.start()
    }

    // Setup

    private fun setupToolbarSearch() {
        val searchToolbar = getSearchToolbar()
        searchToolbar.inflateMenu(R.menu.menu_activity_exercises_search)
        searchToolbar.setNavigationOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                circleReveal(searchToolbar.id, 1, true, false)
            } else {
                searchToolbar.visibility = View.GONE
            }
        }

        mSearchMenu = searchToolbar.menu.findItem(R.id.action_search)
        mSearchMenu?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                mSearchExpanded = true
                mOnSearchShown.onNext(null)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                mSearchExpanded = false
                mOnSearchHidden.onNext(null)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    circleReveal(searchToolbar.id, 1, true, false)
                } else {
                    searchToolbar.visibility = View.GONE
                }
                return true
            }
        })
    }

    private fun setupSearchView() {
        val searchView = mSearchMenu?.actionView as? SearchView ?: return

        // Customise search close btn.
        val closeButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeButton?.setColorFilter(
            ContextCompat.getColor(this, R.color.background_base),
            PorterDuff.Mode.SRC_IN
        )
        closeButton?.setImageResource(R.drawable.ic_clear_white)

        // set hint and the text colors
        val txtSearch = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        txtSearch?.setHint(getString(R.string.exercises_search))
        txtSearch?.setHintTextColor(ContextCompat.getColor(this, R.color.gray_3))
        txtSearch?.setTextColor(ContextCompat.getColor(this, R.color.background_base))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (mFirstSearchIgnored && mSearchExpanded) {
                    mOnSearchChanged.onNext(newText)
                }
                mFirstSearchIgnored = true
                return false
            }
        })
    }
}
