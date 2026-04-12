package com.everlog.ui.fragments.home.exercise.info

import com.everlog.ui.fragments.home.exercise.BasePresenterExerciseTab

class PresenterExerciseInfo : BasePresenterExerciseTab<MvpViewExerciseInfo>() {

    override fun onReady() {
        super.onReady()
        observeYoutubeClick()
    }

    // Observers

    private fun observeYoutubeClick() {
        subscriptions.add(mvpView.onClickYoutube()
                .compose(applyUISchedulers())
                .subscribe({ navigator.openYoutubeSearch(exercise?.name) }, { throwable -> handleError(throwable) }))
    }
}