package com.everlog.android.auth.data.store

import com.everlog.android.auth.data.model.ELUser
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent

// What the app's user store sends when the signed-in user's document loads or changes
class ELDocStoreUserLoadedEvent(
    item: ELUser?,
    hasPendingWrites: Boolean,
    fromCache: Boolean,
    error: Throwable?,
) : ELDocStoreItemLoadedEvent<ELUser?>(item, hasPendingWrites, fromCache, error)
