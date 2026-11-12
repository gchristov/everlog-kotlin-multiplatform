package com.everlog.android.common.mvp.view

import com.everlog.android.common.design.R as DesignR
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox

class CheckBoxTriStates : AppCompatCheckBox {

    enum class State {
        UNKNOWN,
        UNCHECKED,
        CHECKED
    }

    private var state = State.UNKNOWN

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setupState()
    }

    fun getState(): State {
        return state
    }

    private fun updateBtn() {
        val btnDrawable: Int = when (state) {
            State.UNKNOWN -> DesignR.drawable.ic_checkbox_unknown
            State.UNCHECKED -> DesignR.drawable.ic_checkbox_unchecked
            State.CHECKED -> DesignR.drawable.ic_checkbox_checked
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
        }
    }
}