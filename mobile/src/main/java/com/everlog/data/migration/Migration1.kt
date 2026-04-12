package com.everlog.data.migration

import com.everlog.managers.firebase.FirestorePathManager

class Migration1 : Migration {

    override fun migrate(pathManager: FirestorePathManager) {
        println("MIGRATION 1 RUNNING")
    }
}