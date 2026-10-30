package com.everlog.android.exercise.data.model.picker

/**
 * One filter of a search the exercise picker remembers, as stored on the device with Gson. Stored by
 * name, so a key the app no longer has is dropped rather than breaking the rest. It lives with the
 * models because Gson maps it by reflection, which proguard-rules.pro keeps for data.model.
 */
data class StoredFilterToken(
    val key: String? = null,
    val values: List<String>? = null,
    val not: Boolean = false,
)
