package com.everlog.application.features

import android.content.Intent
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.settings.GoogleFitConnection
import com.everlog.android.settings.data.model.ELIntegration
import com.everlog.managers.integrations.GoogleFitIntegrationManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppGoogleFitConnection : GoogleFitConnection {
    override fun connect(view: BaseFragmentMvpView, onGranted: (ELIntegration) -> Unit) =
        GoogleFitIntegrationManager.connect(view, object : GoogleFitIntegrationManager.OnGoogleFitAccessListener {
            override fun onGranted(integration: ELIntegration) = onGranted(integration)
        })

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        GoogleFitIntegrationManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun disconnect(integration: ELIntegration) = GoogleFitIntegrationManager.disconnect(integration)
}
