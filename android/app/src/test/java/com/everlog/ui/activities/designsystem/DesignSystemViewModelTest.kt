package com.everlog.ui.activities.designsystem

import com.everlog.testutil.FakeCoroutineDispatcher
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DesignSystemViewModelTest {

    private fun viewModel() = DesignSystemViewModel(dispatcher = FakeCoroutineDispatcher)

    @Test
    fun `starts with no clicks`() {
        val viewModel = viewModel()

        assertThat(viewModel.state.value).isEqualTo(DesignSystemViewModel.State())
    }

    @Test
    fun `button clicks are counted`() {
        val viewModel = viewModel()

        viewModel.onButtonClick()
        viewModel.onButtonClick()

        assertThat(viewModel.state.value).isEqualTo(DesignSystemViewModel.State(buttonClicks = 2))
    }
}
