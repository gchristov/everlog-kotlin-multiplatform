package com.everlog.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import com.everlog.R
import rx.Observable
import rx.subjects.PublishSubject

class CheckBoxTriStates : AppCompatCheckBox {

    enum class State {
        UNKNOWN,
        UNCHECKED,
        CHECKED
    }

    private var state = State.UNKNOWN

    private val mCheckChange = PublishSubject.create<Boolean>()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setupState()
    }

    fun observeCheckChange(): Observable<Boolean> {
        return mCheckChange
    }

    fun getState(): State {
        return state
    }

    fun setChecked(boolean: Boolean?) {
        this.state = when {
            boolean == null -> {
                State.UNKNOWN
            }
            boolean -> {
                State.CHECKED
            }
            else -> {
                State.UNCHECKED
            }
        }
        updateBtn()
    }

    private fun updateBtn() {
        val btnDrawable: Int = when (state) {
            State.UNKNOWN -> R.drawable.ic_checkbox_unknown
            State.UNCHECKED -> R.drawable.ic_checkbox_unchecked
            State.CHECKED -> R.drawable.ic_checkbox_checked
        }
        setButtonDrawable(btnDrawable)
    }

    // Setup

    private fun setupState() {
        updateBtn()
        setOnCheckedChangeListener { _, _ ->
            state = when (state) {
                State.UNKNOWN -> State.CHECKED
                State.CHECKED -> State.UNCHECKED
                State.UNCHECKED -> State.CHECKED
            }
            updateBtn()
            mCheckChange.onNext(getState() == State.CHECKED)
        }
    }
}