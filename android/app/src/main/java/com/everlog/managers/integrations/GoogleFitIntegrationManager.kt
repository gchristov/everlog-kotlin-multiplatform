package com.everlog.managers.integrations

import android.Manifest
import android.app.Activity
import android.content.Intent
import com.everlog.R
import com.everlog.application.ELApplication
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.model.ELIntegration
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.auth.LocalUserManager
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import com.everlog.ui.navigator.ELNavigator
import com.everlog.utils.device.DeviceUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.firebase.firestore.SetOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import timber.log.Timber

object GoogleFitIntegrationManager {

    const val TAG = "GoogleFitManager"

    private var mListener: OnGoogleFitAccessListener? = null
    private val mFitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_WRITE)
            .build()

    // Access management

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == ELActivityRequestCodes.REQUEST_LOGIN_GOOGLE) {
            val completedTask = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (completedTask.isSuccessful) {
                val account = completedTask.getResult(ApiException::class.java)
                if (account != null && account.serverAuthCode != null) {
                    accessGranted(account.serverAuthCode!!)
                } else {
                    accessDenied()
                }
            } else {
                accessDenied()
            }
            return true
        }
        return false
    }

    fun connect(context: BaseFragmentMvpView, listener: OnGoogleFitAccessListener? = null) {
        AnalyticsManager.manager.integrationAccessRequested()
        Timber.tag(TAG).i("Connecting to Google Fit")
        mListener = listener
        // https://developers.google.com/fit/android/authorization#android-10
        if (DeviceUtils.isAndroidQ()) {
            // On Android 10 check the required permission specifically
            Dexter.withContext(context.getParentActivity())
                    .withPermissions(Manifest.permission.ACTIVITY_RECOGNITION)
                    .withListener(object : MultiplePermissionsListener {

                        override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                            if (report.areAllPermissionsGranted()) {
                                androidPermissionsGranted(context.getParentActivity()!!)
                            } else {
                                ELNavigator(context.context).promptForAppSettings(R.string.integrations_connect_permission_needed)
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>, token: PermissionToken) {
                            token.continuePermissionRequest()
                        }
                    }).check()
        } else {
            // On Android <= 9 the needed permission is granted specifically
            androidPermissionsGranted(context.getParentActivity()!!)
        }
    }

    private fun androidPermissionsGranted(activity: Activity) {
        ELNavigator(activity).openLoginWithGoogle(buildSignInClient())
    }

    private fun buildSignInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(ELApplication.getInstance().getString(R.string.default_web_client_id), true)
                .addExtension(mFitnessOptions)
                .build()
        return GoogleSignIn.getClient(ELApplication.getInstance(), options)
    }

    fun disconnect(integration: ELIntegration?) {
        AnalyticsManager.manager.integrationDisconnected()
        Timber.tag(TAG).i("Disconnecting from Google Fit")
        buildSignInClient().revokeAccess()
        accessDenied(integration)
    }

    fun signOut() {
        Timber.tag(TAG).i("Signing out from Google Fit")
        buildSignInClient().signOut()
    }

    private fun accessGranted(serverAuthCode: String) {
        AnalyticsManager.manager.integrationConnected()
        Timber.tag(TAG).i("Google Fit access granted")
        // Add integration
        val integration = ELIntegration.newIntegration(LocalUserManager.getUser()!!.id, ELIntegration.Type.GOOGLE_FIT, serverAuthCode)
        ELDatastore.integrationStore().create(integration, SetOptions.merge())
        Timber.tag(TAG).i("Added user integration")
        // Notify listener
        mListener?.onGranted(integration)
        mListener = null
    }

    private fun accessDenied(integration: ELIntegration? = null) {
        Timber.tag(TAG).i("Google Fit access denied")
        if (integration != null) {
            // Delete integration
            ELDatastore.integrationStore().delete(integration)
            Timber.tag(TAG).i("Deleted user integration")
        }
    }

    interface OnGoogleFitAccessListener {

        fun onGranted(integration: ELIntegration)
    }
}