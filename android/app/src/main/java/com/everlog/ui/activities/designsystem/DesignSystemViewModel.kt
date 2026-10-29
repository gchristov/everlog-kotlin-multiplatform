package com.everlog.ui.activities.designsystem

import com.everlog.ui.mvvm.CommonViewModel
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
