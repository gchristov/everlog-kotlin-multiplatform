package com.everlog.android.showcase.feature

import com.everlog.android.common.mvvm.CommonViewModel
import kotlinx.coroutines.CoroutineDispatcher

class DesignSystemViewModel(
    dispatcher: CoroutineDispatcher,
) : CommonViewModel<DesignSystemViewModel.State>(
    dispatcher = dispatcher,
    initialState = State()
) {
    fun onButtonClick() {
        setState { copy(buttonClicks = buttonClicks + 1) }
    }

    data class State(
        val buttonClicks: Int = 0,
    )
}
