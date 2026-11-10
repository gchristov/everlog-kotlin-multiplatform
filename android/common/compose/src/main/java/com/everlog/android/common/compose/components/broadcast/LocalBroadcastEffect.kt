package com.everlog.android.common.compose.components.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Calls [onReceive] with each local broadcast of [actions] while this is in the composition, e.g. the app saying the
 * user's settings changed.
 */
@Composable
fun LocalBroadcastEffect(vararg actions: String, onReceive: (Intent) -> Unit) {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onReceive)
    DisposableEffect(context, *actions) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = latest(intent)
        }
        val manager = LocalBroadcastManager.getInstance(context)
        manager.registerReceiver(receiver, IntentFilter().apply { actions.forEach(::addAction) })
        onDispose { manager.unregisterReceiver(receiver) }
    }
}
