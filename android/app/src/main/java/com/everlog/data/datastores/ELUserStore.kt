package com.everlog.data.datastores

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.android.auth.data.model.ELUser
import com.everlog.android.auth.data.store.ELDocStoreUserLoadedEvent
import com.everlog.android.common.kotlin.AppContext
import com.everlog.android.common.mvp.MvpBroadcasts
import com.everlog.android.settings.data.SettingsManager
import com.everlog.android.common.data.store.ELDocumentStore
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent
import com.everlog.android.auth.data.LocalUserManager
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query

class ELUserStore : ELDocumentStore<ELUser>() {

    override fun getType(): Class<ELUser> {
        return ELUser::class.java
    }

    override fun getQuery(itemId: String): Query {
        return parentCollection
                .whereEqualTo(FieldPath.documentId(), itemId)
    }

    override fun getParentCollection(): CollectionReference {
        return FirestorePathManager.usersCollection
    }

    override fun getTag(): String {
        return "ELUserStore"
    }

    override fun itemReady(item: ELUser) {
        super.itemReady(item)
        val proChanged = LocalUserManager.getUser()?.isPro() != item.isPro()
        LocalUserManager.updateUser(item)
        if (proChanged) {
            if (!item.isPro()) {
                checkProDependentFeatures()
            }
            // Notify interested listeners of subscription change
            LocalBroadcastManager.getInstance(AppContext).sendBroadcast(Intent(MvpBroadcasts.PRO_CHANGED))
        }
    }

    private fun checkProDependentFeatures() {
        // Make sure to downgrade the user's local muscle goals, if they have a pro-based one set
        if (SettingsManager.manager.muscleGoal().proLocked()) {
            SettingsManager.manager.setMuscleGoal(SettingsManager.MuscleGoal.HISTORY)
        }
    }

    // Events

    override fun getDocumentStoreItemLoadedEvent(item: ELUser?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELUser?> {
        return ELDocStoreUserLoadedEvent(item, hasPendingWrites, fromCache, error)
    }

}
