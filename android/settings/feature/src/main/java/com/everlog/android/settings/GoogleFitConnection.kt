package com.everlog.android.settings

import android.content.Intent
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.settings.data.model.ELIntegration

interface GoogleFitConnection {
    // Asks for Google Fit access, and calls [onGranted] with the new integration once given
    fun connect(view: BaseFragmentMvpView, onGranted: (ELIntegration) -> Unit)

    // Google sign-in's answer for connect, from onActivityResult
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    fun disconnect(integration: ELIntegration)
}
