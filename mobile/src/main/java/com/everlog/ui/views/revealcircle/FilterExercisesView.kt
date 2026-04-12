package com.everlog.ui.views.revealcircle

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.databinding.ViewFilterExercisesBinding
import com.everlog.ui.adapters.exercise.ExerciseCategoryAdapter
import com.everlog.utils.ArrayResourceTypeUtils
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import java.io.Serializable

class FilterExercisesView(context: Context,
                          filters: ExerciseFilters? = null,
                          listener: OnFilterListener) : BaseRevealCircleView(context, R.layout.view_filter_exercises) {

    private val TAG = "FilterExercisesView"

    private lateinit var binding: ViewFilterExercisesBinding

    private var mFilterListener = listener
    private var mFilters = filters

     private val mAdapter = RecyclerAdapter()
     private val mDataListManager = DataListManager<ExerciseCategory>(mAdapter)

    override fun setupLayout(layoutId: Int) {
        binding = ViewFilterExercisesBinding.inflate(LayoutInflater.from(context), this, true)
        this.tag = tag()
    }

    override fun tag(): String {
        return TAG
    }

    override fun onReady() {
        setupTopBar()
        setupListView()
        loadCategories()
        binding.saveBtn.setOnClickListener {
            onClickSave()
        }
    }

    fun onClickSave() {
        mFilterListener.onSave(buildFilters())
    }

    // Loading

    private fun loadCategories() {
        val categoryTypes = ArrayResourceTypeUtils.withExerciseCategories().types
        val categories = ArrayList<ExerciseCategory>()
        for (type in categoryTypes) {
            val category = ExerciseCategory(type, ArrayResourceTypeUtils.withExerciseCategories().getTitle(type, type.lowercase().capitalize()))
            // Check existing filters
            mFilters?.categories?.forEach loop@ {
                if (it.type.equals(category.type)) {
                    category.selected = it.selected
                    return@loop
                }
            }
            categories.add(category)
        }
         mDataListManager.set(categories)
         mAdapter.notifyDataSetChanged()
    }

    private fun buildFilters(): ExerciseFilters {
        val filters = ExerciseFilters()
        for (i in 0 until mDataListManager.count) {
            val category = mDataListManager.get(i)
            if (category?.selected == true) {
                filters.categories.add(category)
            }
        }
        return filters
    }

    // Setup

    private fun setupTopBar() {
        binding.root.findViewById<Toolbar>(R.id.toolbar).setTitle(R.string.filter)
        binding.root.findViewById<Toolbar>(R.id.toolbar).setNavigationIcon(R.drawable.ic_clear_white)
        binding.root.findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            hide(parent as ViewGroup)
        }
    }

    private fun setupListView() {
        mAdapter.addDataManager(mDataListManager)
        mAdapter.registerBinder(ExerciseCategoryAdapter.Binder())
        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.FLEX_START
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = mAdapter
    }

    data class ExerciseCategory (

            var type: String? = null,
            val title: String? = null,

            // Aux

            var selected: Boolean = false

    ) : Serializable

    data class ExerciseFilters (

            var title: String? = null,
            var categories: ArrayList<ExerciseCategory> = ArrayList()

    ) {
        fun hasFilter(): Boolean {
            return !TextUtils.isEmpty(title) || categories.isNotEmpty()
        }
    }

    interface OnFilterListener {

        fun onSave(filter: ExerciseFilters)
    }
}
