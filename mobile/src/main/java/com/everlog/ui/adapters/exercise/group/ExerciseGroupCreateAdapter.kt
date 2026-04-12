package com.everlog.ui.adapters.exercise.group

import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.ItemBinder
import com.ahamed.multiviewadapter.ItemViewHolder
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.config.AppConfig
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.set.ELSetType
import com.everlog.databinding.RowExerciseGroupCreateBinding
import com.everlog.databinding.RowExerciseWithinSetCreateBinding
import com.everlog.databinding.RowSetCreateBinding
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.ui.dialog.DialogBuilder
import com.everlog.ui.dialog.ToastBuilder
import com.everlog.ui.navigator.ELNavigator
import com.everlog.ui.views.viewpager.wrap.ObjectAtPositionPagerAdapter
import com.everlog.utils.ArrayResourceTypeUtils
import com.everlog.utils.Throttler
import com.everlog.utils.Utils
import com.everlog.utils.ViewUtils
import com.everlog.utils.format.FormatUtils.Companion.formatDurationShort
import com.everlog.utils.format.FormatUtils.Companion.formatSetWeight
import rx.Observable
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import java.util.concurrent.TimeUnit


class ExerciseGroupCreateAdapter {

    private val CLICK_THROTTLE = 700

    private lateinit var builder: Builder
    private val mGroupPageIndex = HashMap<String, Int>() // Keep track of current set for group

    fun build(builder: Builder): GroupBinder {
        this.builder = builder
        return GroupBinder()
    }

    class Builder {

        internal var allowSetCompletion = false
        internal var allowSetTemplates = true
        internal var touchHelper: ItemTouchHelper? = null
        internal var listener: OnExerciseGroupListener? = null

        fun setAllowSetCompletion(allowSetCompletion: Boolean): Builder {
            this.allowSetCompletion = allowSetCompletion
            return this
        }

        fun setAllowSetTemplates(allowSetTemplates: Boolean): Builder {
            this.allowSetTemplates = allowSetTemplates
            return this
        }

        fun setTouchHelper(touchHelper: ItemTouchHelper?): Builder {
            this.touchHelper = touchHelper
            return this
        }

        fun setListener(listener: OnExerciseGroupListener?): Builder {
            this.listener = listener
            return this
        }

        fun saveWorkout() {
            listener?.onGroupEdited()
        }

        fun hasSelections(): Boolean {
            return listener?.hasSelections() == true
        }
    }

    inner class GroupBinder : ItemBinder<ELExerciseGroup, GroupViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): GroupViewHolder {
            return GroupViewHolder(inflater.inflate(R.layout.row_exercise_group_create, parent, false))
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELExerciseGroup
        }

        override fun bind(holder: GroupViewHolder, item: ELExerciseGroup) {
            holder.render()
        }
    }

    inner class GroupViewHolder internal constructor(itemView: View) : ItemViewHolder<ELExerciseGroup>(itemView) {

        val binding = RowExerciseGroupCreateBinding.bind(itemView)

        private val mAdapter = SetPagerAdapter()
        private val mAddThrottle = Throttler()

        init {
            binding.addBtn.setOnClickListener { onClickAdd() }
            setupPager()
        }

        fun onClickAdd() {
            if (!mAddThrottle.canPerformAction(CLICK_THROTTLE)) {
                return
            }
            if (item.getTotalSetsCount() >= AppConfig.configuration.maxExerciseSets) {
                ToastBuilder.showToast(itemView.context, itemView.context.getString(R.string.exercises_max_sets, AppConfig.configuration.maxExerciseSets))
                return
            }
            item.setAdd()
            render()
            scrollTo(mAdapter.count - 1) // Scroll to last item
            builder.saveWorkout()
            AnalyticsManager.manager.setAdded()
        }

        override fun getDragDirections(): Int {
            return ItemTouchHelper.UP or ItemTouchHelper.DOWN
        }

        internal fun render() {
            renderSets()
            renderSelection()
        }

        internal fun scrollTo(page: Int, delay: Int? = 500) {
            if (canScrollTo(page)) {
                Utils.runWithDelay({
                    binding.pager.setCurrentItem(page, true)
                }, delay!!)
            }
        }

        internal fun canScrollTo(page: Int): Boolean {
            return page >= 0 && page < mAdapter.count
        }

        // Render

        private fun renderSets() {
            val setCount = item.getTotalSetsCount()
            mAdapter.setItems(setCount, this)
            mAdapter.notifyDataSetChanged()
            if (binding.pager.currentItem != mGroupPageIndex[item.uuid] ?: 0) {
                binding.pager.setCurrentItem(mGroupPageIndex[item.uuid] ?: 0, false)
            }
            binding.pager.refresh()
        }

        private fun renderSelection() {
            binding.indicator.visibility = if (builder.hasSelections() || mAdapter.count < 2) View.GONE else View.VISIBLE
            binding.addBtn.visibility = if (builder.hasSelections()) View.INVISIBLE else View.VISIBLE
            binding.dragIcon.visibility = if (builder.hasSelections()) View.VISIBLE else View.INVISIBLE
            binding.dragIcon.setOnTouchListener { _: View?, event: MotionEvent ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    builder.touchHelper?.startDrag(this)
                }
                false
            }
        }

        // Setup

        private fun setupPager() {
            binding.pager.adapter = mAdapter
            binding.pager.pageMargin = ViewUtils.dpToPxFromRaw(itemView.context, R.dimen.activity_margin_triple)
            binding.pager.offscreenPageLimit = 2
            binding.pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    mGroupPageIndex[item.uuid!!] = position
                }
            })
            binding.indicator.attachToPager(binding.pager)
        }

        fun getPager(): ViewPager = binding.pager
    }

    inner class SetPagerAdapter : ObjectAtPositionPagerAdapter() {

        private val MENU_DUPLICATE = 1
        private val MENU_DELETE = 2

        private var setCount: Int = 0
        private lateinit var groupVH: GroupViewHolder
        private val mCompleteThrottle = Throttler()
        private val destroyedItems: Queue<SetViewHolder> = LinkedList()

        fun setItems(sets: Int, groupVH: GroupViewHolder) {
            this.setCount = sets
            this.groupVH = groupVH
        }

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getCount(): Int {
            return setCount
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return (`object` as SetViewHolder).view === view
        }

        override fun instantiateItemObject(container: ViewGroup, position: Int): Any {
            var viewHolder = destroyedItems.poll()
            if (viewHolder != null) {
                // Re-add existing view before rendering so that we can make change inside getView()
                container.addView(viewHolder.view)
            } else {
                val binding = RowSetCreateBinding.inflate(LayoutInflater.from(container.context), container, false)
                viewHolder = SetViewHolder(binding)
                container.addView(viewHolder.view)
            }
            // Bind views
            render(position, viewHolder)
            return viewHolder
        }

        override fun destroyItemObject(container: ViewGroup, position: Int, `object`: Any?) {
            container.removeView((`object` as SetViewHolder).view)
            destroyedItems.add(`object`)
        }

        // Render

        private fun render(set: Int, myVH: SetViewHolder) {
            // Clicks
            myVH.binding.completeBtn.setOnClickListener {
                handleComplete(set)
            }
            val menuListener = { _: View ->
                handleMenu(set, myVH)
            }
            myVH.binding.root.setOnClickListener(menuListener)
            myVH.binding.menuBtn.setOnClickListener(menuListener)
            // Render
            renderSet(set, myVH)
            renderExercises(set, myVH)
            renderCompleteButtons(set, myVH)
        }

        private fun renderSet(set: Int, myVH: SetViewHolder) {
            myVH.binding.setNumber.text = String.format("%s %s", if (groupVH.item.getSetType() === ELSetType.SINGLE) "Set" else ArrayResourceTypeUtils.withSetTypes().getTitle(groupVH.item.type!!, groupVH.item.type?.lowercase(Locale.getDefault())?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + " Set"), set + 1)
            myVH.binding.setNumber.visibility = if (builder.hasSelections()) View.INVISIBLE else View.VISIBLE
            myVH.binding.menuBtn.visibility = if (!builder.hasSelections()) View.VISIBLE else View.INVISIBLE
        }

        private fun renderExercises(set: Int, myVH: SetViewHolder) {
            val binder = ExerciseBinder(groupVH, set, myVH, this)
            val adapter = RecyclerAdapter()
            val dataListManager = DataListManager<ELRoutineExercise>(adapter)
            adapter.addDataManager(dataListManager)
            adapter.registerBinder(binder)
            dataListManager.set(groupVH.item.getExercisesForSetIndex(set))
            myVH.binding.recyclerView.layoutManager = LinearLayoutManager(myVH.view.context)
            myVH.binding.recyclerView.adapter = adapter
            if (!builder.allowSetCompletion) {
                myVH.binding.recyclerView.setBackgroundResource(R.drawable.rounded_corners_exercise_group_full)
            } else {
                myVH.binding.recyclerView.setBackgroundResource(if (builder.hasSelections()) R.drawable.rounded_corners_exercise_group_full else R.drawable.rounded_corners_exercise_group)
            }
        }

        private fun renderCompleteButtons(set: Int, myVH: SetViewHolder) {
            val complete = groupVH.item.setIsComplete(set)
            myVH.binding.completeBtn.text = String.format("%s Set %s", if (complete) "Edit" else "Complete", set + 1)
            myVH.binding.completeBtn.setBackgroundResource(if (complete) R.drawable.rounded_corners_btn_set_edit else R.drawable.rounded_corners_btn_set_complete)
            myVH.binding.completeBtn.setTextColor(ContextCompat.getColor(myVH.view.context, if (complete) R.color.main_accent else R.color.background_base))
            myVH.binding.completeBtn.visibility = if (builder.allowSetCompletion && !builder.hasSelections()) View.VISIBLE else View.GONE
        }

        // Handlers

        private fun handleComplete(set: Int) {
            if (!mCompleteThrottle.canPerformAction(CLICK_THROTTLE)) {
                return
            }
            val complete = groupVH.item.setIsComplete(set)
            if (complete) {
                // Make set incomplete
                groupVH.item.setClearCompletedDate(set)
                groupVH.render()
                builder.saveWorkout()
            } else {
                // Complete set
                groupVH.item.setComplete(set)
                groupVH.scrollTo(set + 1, 0)
                builder.listener?.onSetCompleted(groupVH.item)
                // Wait until scroll
                Utils.runWithDelay({
                    groupVH.render()
                    builder.saveWorkout()
                }, if (groupVH.canScrollTo(set + 1)) 500 else 0)
            }
        }

        internal fun handleMenu(set: Int, myVH: SetViewHolder) {
            if (builder.hasSelections()) {
                return
            }
            val menuBtn = myVH.binding.menuBtn
            val popup = PopupMenu(menuBtn.context, menuBtn)
            popup.menu.add(0, MENU_DUPLICATE, MENU_DUPLICATE, "Duplicate").setIcon(R.drawable.ic_copy)
            popup.menu.add(0, MENU_DELETE, MENU_DELETE, "Delete").setIcon(R.drawable.ic_delete_black)
            popup.setOnMenuItemClickListener { menuItem: MenuItem ->
                var scrollToLast = false
                when (menuItem.itemId) {
                    MENU_DUPLICATE -> {
                        if (groupVH.item.getTotalSetsCount() >= AppConfig.configuration.maxExerciseSets) {
                            ToastBuilder.showToast(myVH.view.context, myVH.view.context.getString(R.string.exercises_max_sets, AppConfig.configuration.maxExerciseSets))
                        } else {
                            groupVH.item.setDuplicate(set)
                            scrollToLast = true
                        }
                    }
                    MENU_DELETE -> handleDelete(set)
                }
                groupVH.render()
                if (scrollToLast) {
                    groupVH.scrollTo((groupVH.getPager().adapter?.count ?: 0) - 1)
                }
                builder.saveWorkout()
                false
            }
            val menuHelper = MenuPopupHelper(menuBtn.context, (popup.menu as MenuBuilder), menuBtn)
            menuHelper.setForceShowIcon(true)
            menuHelper.show()
        }

        private fun handleDelete(set: Int) {
            if (groupVH.item.setDelete(set)) {
                // If group has no more sets, remove it from the workout
                builder.listener?.onGroupDeleted(groupVH.item)
            }
            AnalyticsManager.manager.setDeleted()
            ToastBuilder.showToast(groupVH.itemView.context, "Set deleted")
        }

        inner class SetViewHolder constructor(val binding: RowSetCreateBinding) {
            val view: View = binding.root
        }
    }

    inner class ExerciseBinder(

            private val groupVH: GroupViewHolder,
            private val set: Int,
            private val setVH: SetPagerAdapter.SetViewHolder,
            private val setAdapter: SetPagerAdapter

    ) : ItemBinder<ELRoutineExercise, ExerciseViewHolder>() {

        override fun create(inflater: LayoutInflater, parent: ViewGroup): ExerciseViewHolder {
            return ExerciseViewHolder(inflater.inflate(R.layout.row_exercise_within_set_create, parent, false))
        }

        override fun canBindData(item: Any): Boolean {
            return item is ELRoutineExercise
        }

        override fun bind(holder: ExerciseViewHolder, item: ELRoutineExercise) {
            holder.setItems(groupVH, set, setVH, setAdapter)
            holder.render()
        }
    }

    inner class ExerciseViewHolder(itemView: View) : ItemViewHolder<ELRoutineExercise>(itemView) {

        val binding = RowExerciseWithinSetCreateBinding.bind(itemView)

        private lateinit var groupVH: GroupViewHolder
        private lateinit var setView: SetPagerAdapter.SetViewHolder
        private lateinit var setVH: SetPagerAdapter
        private var set = 0

        private val mWeightWatcher: TextWatcher = object : TextWatcher {
            private var prevVisibility: Int? = null
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                prevVisibility = binding.weightPanel.visibility
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // TODO: Optimisation
                groupVH.item.setUpdateWeightFromInput(item, set, binding.weightField.text.toString())
                render()
                if (prevVisibility != binding.weightPanel.visibility) {
                    groupVH.render()
                }
                builder.saveWorkout()
            }

            override fun afterTextChanged(s: Editable) {}
        }
        private val mRepsWatcher: TextWatcher = object : TextWatcher {
            private var prevVisibility: Int? = null
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                prevVisibility = binding.repsPanel.visibility
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // TODO: Optimisation
                groupVH.item.setUpdateRepsFromInput(item, set, binding.repsField.text.toString(), builder.allowSetTemplates)
                render()
                if (prevVisibility != binding.repsPanel.visibility) {
                    groupVH.render()
                }
                builder.saveWorkout()
            }

            override fun afterTextChanged(s: Editable) {}
        }
        private val mTimeWatcher: TextWatcher = object : TextWatcher {
            private var prevVisibility: Int? = null
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                prevVisibility = binding.timePanel.visibility
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // TODO: Optimisation
                val set = groupVH.item.setUpdateTimeFromInput(item, set, binding.timeField.text.toString(), builder.allowSetTemplates)
                render()
                if (prevVisibility != binding.timePanel.visibility) {
                    groupVH.render()
                }
                builder.saveWorkout()
                builder.listener?.onTimeChanged(item, set)
            }

            override fun afterTextChanged(s: Editable) {}
        }

        init {
            binding.historyBtn.setOnClickListener { onClickHistory() }
            binding.weightPanel.setOnClickListener { onClickWeightPanel(it) }
            binding.weightMenu.setOnClickListener { onClickWeightPanel(it) }
            binding.repsPanel.setOnClickListener { onClickRepsPanel(it) }
            binding.repsMenu.setOnClickListener { onClickRepsPanel(it) }
            binding.timePanel.setOnClickListener { onClickTimePanel(it) }
            binding.timeMenu.setOnClickListener { onClickTimePanel(it) }
            binding.reduceWeightBtn.setOnClickListener { onClickReduceWeight() }
            binding.increaseWeightBtn.setOnClickListener { onClickIncreaceWeight() }
            binding.reduceRepsBtn.setOnClickListener { onClickReduceReps() }
            binding.increaseRepsBtn.setOnClickListener { onClickIncreaceReps() }
            binding.startTimerBtn.setOnClickListener { onClickTimerStart() }
            binding.containerLayout.setOnClickListener { onClickBackground() }
        }

        fun onClickBackground() {
            setVH.handleMenu(set, setView)
        }

        fun onClickHistory() {
            ELNavigator(itemView.context).openExerciseDetails(ExerciseDetailsActivity.Companion.Properties()
                    .exercise(item.exercise!!)
                    .type(ExerciseDetailsActivity.Companion.Type.HISTORY))
        }

        fun onClickWeightPanel(view: View) {
            Observable.just(DialogBuilder.showInputNumberDialog(view.context,
                    if (getSet().isWeightEntered()) formatSetWeight(getSet().getWeight()) else null,
                    DialogBuilder.MetricDialogType.WEIGHT)
                    .subscribe { text: String? ->
                        binding.weightField.text = if (TextUtils.isEmpty(text)) "" else text
                    })
        }

        fun onClickRepsPanel(view: View) {
            val obs = if (!builder.allowSetTemplates) {
                DialogBuilder.showInputNumberDialog(view.context,
                        if (getSet().isRepsEntered()) getSet().getReps().toString() + "" else null,
                        DialogBuilder.MetricDialogType.REPS)
            } else {
                DialogBuilder.showInputNumberDialog(view.context,
                        if (getSet().isRequiredRepsEntered()) getSet().getRequiredReps().toString() + "" else null,
                        DialogBuilder.MetricDialogType.REPS_REQUIRED)
            }
            Observable.just(obs
                    .subscribe { text: String? ->
                        binding.repsField.text = if (TextUtils.isEmpty(text)) "" else text
                    })
        }

        fun onClickTimePanel(view: View) {
            val obs = if (!builder.allowSetTemplates) {
                DialogBuilder.showPickerDurationDialog(view.context,
                        if (getSet().isTimeEntered()) getSet().getTimeSeconds() else AppConfig.configuration.defaultExerciseTimeSeconds,
                        DialogBuilder.DurationPickerDialogType.EXERCISE_TIME)
            } else {
                DialogBuilder.showPickerDurationDialog(view.context,
                        if (getSet().isRequiredTimeEntered()) getSet().getRequiredTimeSeconds() else AppConfig.configuration.defaultExerciseTimeSeconds,
                        DialogBuilder.DurationPickerDialogType.EXERCISE_TIME_REQUIRED)
            }
            Observable.just(obs
                    .subscribe { value: Int ->
                        binding.timeField.text = if (value <= 0) "" else value.toString()
                    })
        }

        fun onClickReduceWeight() {
            getSet().modifyWeight(-SettingsManager.manager.weightIncrease())
            renderWeight()
        }

        fun onClickIncreaceWeight() {
            getSet().modifyWeight(SettingsManager.manager.weightIncrease())
            renderWeight()
        }

        fun onClickReduceReps() {
            getSet().modifyReps(-1)
            renderReps()
        }

        fun onClickIncreaceReps() {
            getSet().modifyReps(1)
            renderReps()
        }

        fun onClickTimerStart() {
            builder.listener?.onStartTimer(item, getSet())
        }

        internal fun setItems(group: GroupViewHolder,
                              set: Int,
                              setView: SetPagerAdapter.SetViewHolder,
                              setVH: SetPagerAdapter) {
            this.groupVH = group
            this.set = set
            this.setView = setView
            this.setVH = setVH
        }

        internal fun render() {
            // Avoid multiple invocations of the text watchers
            binding.weightField.removeTextChangedListener(mWeightWatcher)
            binding.repsField.removeTextChangedListener(mRepsWatcher)
            binding.timeField.removeTextChangedListener(mTimeWatcher)
            renderExercise()
            renderWeight()
            renderReps()
            renderTime()
            renderAddMenus()
            renderSelection()
            if (builder.allowSetCompletion && getSet().isComplete()) {
                renderCompleteSet()
            } else {
                renderIncompleteSet()
            }
            // Reattach the text watchers
            binding.weightField.addTextChangedListener(mWeightWatcher)
            binding.repsField.addTextChangedListener(mRepsWatcher)
            binding.timeField.addTextChangedListener(mTimeWatcher)
            // Clicks
            val click = {
                val wasAlreadyInSelectMode = builder.hasSelections()
                builder.listener?.onClickExercise(groupVH.item, item)
                if (wasAlreadyInSelectMode) {
                    render()
                }
            }
            binding.checkbox.setOnClickListener { click() }
            binding.exerciseImg.setOnClickListener { click() }
            binding.exerciseContainer.setOnClickListener { click() }
            binding.exerciseContainer.setOnLongClickListener {
                click()
                true
            }
        }

        // Render

        private fun renderSelection() {
            val selectMode = builder.hasSelections()
            binding.exerciseImg.visibility = if (selectMode) View.GONE else View.VISIBLE
            binding.checkbox.visibility = if (selectMode) View.VISIBLE else View.GONE
            binding.historyBtn.visibility = if (selectMode || !builder.allowSetCompletion) View.GONE else View.VISIBLE
            if (builder.listener?.isSelected(item) == true) {
                binding.checkbox.setImageResource(R.drawable.ic_check_green)
                binding.checkbox.setBackgroundResource(R.drawable.circle_accent)
            } else {
                binding.checkbox.setImageDrawable(null)
                binding.checkbox.setBackgroundResource(R.drawable.circle_border_accent)
            }
        }

        private fun renderExercise() {
            binding.exerciseName.text = item.getName()
            binding.exerciseImg.setExercise(item.exercise)
            binding.exerciseImg.applyMask(R.drawable.mask_circle)
        }

        private fun renderIncompleteSet() {
            val selectMode = builder.hasSelections()
            binding.completeContainer.visibility = View.GONE
            binding.incompleteContainer.visibility = if (selectMode) View.GONE else View.VISIBLE
        }

        private fun renderCompleteSet() {
            val selectMode = builder.hasSelections()
            binding.completeContainer.visibility = if (selectMode) View.GONE else View.VISIBLE
            binding.incompleteContainer.visibility = View.GONE
            ExerciseGroupSummaryAdapter.renderCompleteSetSummary(itemView.context, getSet(), binding.completeContainer)
        }

        private fun renderWeight() {
            binding.weightField.text = formatSetWeight(getSet().getWeight())
            binding.weightPanel.visibility = if (!builder.allowSetTemplates && getSet().isWeightEntered()) View.VISIBLE else View.GONE
            binding.weightUnit.text = String.format("(%s)", SettingsManager.weightUnitAbbreviation())
        }

        private fun renderReps() {
            if (!builder.allowSetTemplates) {
                binding.increaseRepsBtn.visibility = View.VISIBLE
                binding.reduceRepsBtn.visibility = View.VISIBLE
                binding.repsField.text = getSet().getReps().toString()
                binding.repsPanel.visibility = if (getSet().isRepsEntered() && getSet().canShowRepOptions()) View.VISIBLE else View.GONE
                binding.repsField.gravity = Gravity.CENTER
                binding.repsField.setPadding(0, 0, 0, 0)
            } else {
                binding.increaseRepsBtn.visibility = View.GONE
                binding.reduceRepsBtn.visibility = View.GONE
                binding.repsPanel.visibility = if (getSet().isRequiredRepsEntered() && getSet().canShowRepOptions()) View.VISIBLE else View.GONE
                binding.repsField.text = getSet().getRequiredReps().toString()
                binding.repsField.gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
                binding.repsField.setPadding(0, 0, ViewUtils.dpToPxFromRaw(itemView.context, R.dimen.margin_20), 0)
            }
        }

        private fun renderTime() {
            if (!builder.allowSetTemplates) {
                binding.timeField.text = formatDurationShort(TimeUnit.SECONDS.toMillis(getSet().getTimeSeconds().toLong()), "mm:ss")
                binding.timePanel.visibility = if (getSet().isTimeEntered() && getSet().canShowTimeOptions()) View.VISIBLE else View.GONE
            } else {
                binding.timeField.text = formatDurationShort(TimeUnit.SECONDS.toMillis(getSet().getRequiredTimeSeconds().toLong()), "mm:ss")
                binding.timePanel.visibility = if (getSet().isRequiredTimeEntered() && getSet().canShowTimeOptions()) View.VISIBLE else View.GONE
            }
            binding.startTimerBtn.visibility = if (!builder.allowSetCompletion) View.GONE else View.VISIBLE
        }

        private fun renderAddMenus() {
            // Weight
            binding.weightMenu.visibility = if (!builder.allowSetTemplates && !getSet().isWeightEntered()) View.VISIBLE else View.GONE
            // Reps
            if (!builder.allowSetTemplates) {
                binding.repsMenu.visibility = if (!getSet().isRepsEntered()) View.VISIBLE else View.GONE
            } else {
                binding.repsMenu.visibility = if (!getSet().isRequiredRepsEntered()) View.VISIBLE else View.GONE
            }
            // Time
            if (!builder.allowSetTemplates) {
                binding.timeMenu.visibility = if (!getSet().isTimeEntered()) View.VISIBLE else View.GONE
            } else {
                binding.timeMenu.visibility = if (!getSet().isRequiredTimeEntered()) View.VISIBLE else View.GONE
            }
            // Separator
            binding.separator.visibility = if (binding.timePanel.visibility != View.VISIBLE && binding.repsPanel.visibility != View.VISIBLE && binding.weightPanel.visibility != View.VISIBLE) View.GONE else View.VISIBLE
        }

        private fun getSet(): ELSet {
            return groupVH.item.setForExercise(item, set)
        }
    }

    interface OnExerciseGroupListener {
        fun onClickExercise(group: ELExerciseGroup, exercise: ELRoutineExercise)
        fun onGroupDeleted(group: ELExerciseGroup)
        fun onGroupEdited()
        fun hasSelections(): Boolean
        fun isSelected(exercise: ELRoutineExercise): Boolean
        fun onStartTimer(exercise: ELRoutineExercise, set: ELSet)
        fun onTimeChanged(exercise: ELRoutineExercise, set: ELSet)
        fun onSetCompleted(group: ELExerciseGroup)
    }
}