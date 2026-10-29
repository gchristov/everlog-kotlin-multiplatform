@file:JvmName("AppContext")

package com.everlog.android.common.kotlin

import android.content.Context

// The application context, for code that has no Context of its own. The app sets it first thing in Application.onCreate.
lateinit var AppContext: Context
