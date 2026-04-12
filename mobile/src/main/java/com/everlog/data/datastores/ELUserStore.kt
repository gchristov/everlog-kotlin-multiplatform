package com.everlog.data.datastores

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.application.ELApplication
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.base.ELDocumentStore
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.model.ELUser
import com.everlog.managers.auth.LocalUserManager
import com.everlog.managers.firebase.FirestorePathManager
import com.everlog.managers.preferences.SettingsManager
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
            LocalBroadcastManager.getInstance(ELApplication.getInstance()).sendBroadcast(Intent(ELConstants.BROADCAST_PRO_CHANGED))
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

    class ELDocStoreUserLoadedEvent internal constructor(item: ELUser?,
                                                         hasPendingWrites: Boolean,
                                                         fromCache: Boolean,
                                                         error: Throwable?) : ELDocStoreItemLoadedEvent<ELUser?>(item, hasPendingWrites, fromCache, error)
}