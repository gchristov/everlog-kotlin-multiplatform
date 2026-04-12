package com.everlog.data.migration

import com.everlog.managers.firebase.FirestorePathManager

interface Migration {

    fun migrate(pathManager: FirestorePathManager)
}