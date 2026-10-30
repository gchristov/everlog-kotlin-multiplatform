@file:JvmName("PresenterNavigator")

package com.everlog.ui.navigator

import com.everlog.android.common.mvp.BasePresenter

// The app's screens open each other through the navigator. A feature's presenters use their feature's dependencies.
val BasePresenter<*>.navigator: Navigator
    get() = ELNavigator(context)
