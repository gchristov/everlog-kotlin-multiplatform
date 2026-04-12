package com.everlog.managers.auth

import android.content.Intent
import android.os.AsyncTask
import android.provider.Settings
import android.text.TextUtils
import com.everlog.BuildConfig
import com.everlog.R
import com.everlog.application.ELApplication
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.base.OnStoreItemListener
import com.everlog.data.model.ELConsent.Companion.newConsent
import com.everlog.data.model.ELUser
import com.everlog.managers.PlanManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.managers.auth.samples.CreateSampleRoutinesAsyncTask
import com.everlog.managers.integrations.GoogleFitIntegrationManager
import com.everlog.managers.preferences.PreferencesManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.navigator.Navigator
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.SetOptions
import io.customerly.Customerly
import timber.log.Timber

object AuthManager : PreferencesManager() {

    private val TAG = "AuthManager"

    private var mAuth: FirebaseAuth? = null
    private var mClient: GoogleSignInClient? = null
    private var mAuthListener: OnAuthActionListener? = null

    init {
        setupGoogleClient()
    }

    @JvmStatic
    fun isRunningInGoogleTestLab(): Boolean {
        return "true" == Settings.System.getString(ELApplication.getInstance().contentResolver, "firebase.test.lab")
    }

    fun initialize(listener: OnAuthActionListener) {
        when {
            isLoggedIn -> {
                Timber.tag(TAG).i("Initializing with logged in user")
                // Initialize LocalUserManager with latest local account
                ELDatastore.userStore().getItem(ELUser.buildUser(mAuth!!.currentUser!!).id, object : OnStoreItemListener<ELUser> {
                    override fun onItemLoaded(item: ELUser, fromCache: Boolean) {
                        listener.onSuccess(item)
                    }

                    override fun onItemLoadingError(throwable: Throwable) {
                        handleAuthError(throwable, object : OnAuthActionListener() {
                            override fun onError(throwable: Throwable) {
                                logout(listener)
                            }
                        })
                    }
                })
            }
            SettingsManager.manager.loggedIn() -> {
                handleAuthError(Exception("User previously logged in but not anymore => logging out"), object : OnAuthActionListener() {
                    override fun onError(throwable: Throwable) {
                        logout(listener)
                    }
                })
            }
            else -> {
                Timber.tag(TAG).i("Initializing with logged out user")
                listener.onLogout()
            }
        }
    }

    // Login management

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == ELActivityRequestCodes.REQUEST_LOGIN_GOOGLE) {
            handleGoogleLoginResult(data)
            return true
        }
        return false
    }

    val isLoggedIn: Boolean
        get() = SettingsManager.manager.loggedIn() && mAuth?.currentUser != null

    fun login(email: String,
              password: String,
              listener: OnAuthActionListener) {
        var fEmail = email
        var fPassword = password
        Timber.tag(TAG).i("Logging in using email")
        mAuthListener = listener
        if (isRunningInGoogleTestLab()) {
            // Force Google Test Lab user to use pre-existing account
            fEmail = ELConstants.CLOUD_TEST_EMAIL
            fPassword = ELConstants.CLOUD_TEST_PASSWORD
            disableFeaturesForGoogleTestLab()
        }
        mAuth
                ?.signInWithEmailAndPassword(fEmail, fPassword)
                ?.addOnCompleteListener(getLoginHandler())
    }

    fun loginWithGoogle(navigator: Navigator, listener: OnAuthActionListener) {
        Timber.tag(TAG).i("Logging in with Google")
        mAuthListener = listener
        navigator.openLoginWithGoogle(mClient)
    }

    fun register(fullName: String,
                 email: String,
                 password: String,
                 newsletterAccepted: Boolean,
                 listener: OnAuthActionListener) {
        mAuthListener = listener
        if (isRunningInGoogleTestLab()) {
            Timber.tag(TAG).i("Redirecting Google Test Lab registration to login")
            login(ELConstants.CLOUD_TEST_EMAIL, ELConstants.CLOUD_TEST_PASSWORD, listener)
        } else {
            Timber.tag(TAG).i("Registering using email")
            mAuth
                    ?.createUserWithEmailAndPassword(email, password)
                    ?.addOnCompleteListener(getRegistrationHandler(fullName, newsletterAccepted))
        }
    }

    fun requestPasswordReset(email: String, listener: OnAuthActionListener) {
        Timber.tag(TAG).i("Requesting password reset for $email")
        mAuthListener = listener
        mAuth
                ?.sendPasswordResetEmail(email)
                ?.addOnCompleteListener(requestPasswordResetHandler(email))
    }

    fun logout(listener: OnAuthActionListener) {
        Timber.tag(TAG).i("Logging out")
        // Stores
        ELDatastore.destroy()
        // Integrations
        GoogleFitIntegrationManager.signOut()
        // Local state
        SettingsManager.manager.setLoggedIn(false)
        LocalUserManager.clearUser()
        AnalyticsManager.manager.userLogout()
        AppLaunchManager.manager.clearAppUserData()
        SettingsManager.manager.clearUserPreferences()
        PlanManager.manager.clearOngoingPlan()
        mAuth?.signOut()
        mClient?.signOut()?.addOnCompleteListener {
            Customerly.logoutUser {
                listener.onLogout()
            }
        }
    }

    // Handlers

    private fun handleGoogleLoginResult(data: Intent?) {
        val completedTask = GoogleSignIn.getSignedInAccountFromIntent(data)
        if (completedTask.isSuccessful) {
            Timber.tag(TAG).i("Google login accepted")
            try {
                val account = completedTask.getResult(ApiException::class.java)
                if (account != null && account.idToken != null) {
                    if (isRunningInGoogleTestLab()) {
                        Timber.tag(TAG).i("Using Google Test Lab account")
                        // Force Google Test Lan to use pre-existing account
                        mAuth
                                ?.signInWithEmailAndPassword(ELConstants.CLOUD_TEST_EMAIL, ELConstants.CLOUD_TEST_PASSWORD)
                                ?.addOnCompleteListener(getLoginHandler())
                        disableFeaturesForGoogleTestLab()
                    } else {
                        Timber.tag(TAG).i("Logging in with Google credential")
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        mAuth
                                ?.signInWithCredential(credential)
                                ?.addOnCompleteListener(getLoginHandler())
                    }
                } else {
                    Timber.tag(TAG).i("Invalid Google account")
                    handleAuthError(Exception("Failed to login to Google: account is null or token missing"), mAuthListener)
                }
            } catch (e: ApiException) {
                e.printStackTrace()
                Timber.tag(TAG).e(e)
                handleAuthError(Exception("Failed to login to Google: " + e.message), mAuthListener)
            }
        } else {
            Timber.tag(TAG).i("Google login cancelled")
            handleAuthError(Exception("Failed to login to Google: " + completedTask.exception!!.message), mAuthListener)
        }
    }

    private fun handleAuthError(throwable: Throwable?, listener: OnAuthActionListener?) {
        if (throwable != null) {
            throwable.printStackTrace()
            Timber.tag(TAG).e(throwable)
            var msg = "Could not perform operation at this time. Please try again"
            if (!TextUtils.isEmpty(throwable.message)) {
                msg = throwable.message.toString()
            }
            listener?.onError(Exception(msg))
        }
    }

    private fun getLoginHandler(): OnCompleteListener<AuthResult> {
        return OnCompleteListener { task ->
            if (task.isSuccessful && mAuth?.currentUser != null) {
                Timber.tag(TAG).i("Login handler success")
                val user = ELUser.buildUser(mAuth!!.currentUser!!)
                val userJustRegistered = task.result?.additionalUserInfo?.isNewUser == true
                if (userJustRegistered) {
                    // This could happen when logging in with Google, so make sure we track it
                    AnalyticsManager.manager.userRegister(user.id, user.email, user.displayName)
                }
                finishLocalLogin(user, userJustRegistered)
            } else {
                Timber.tag(TAG).i("Login handler error")
                handleAuthError(task.exception ?: Exception("Couldn't login at this time. Please try again"), mAuthListener)
            }
        }
    }

    private fun getRegistrationHandler(fullName: String, newsletterAccepted: Boolean): OnCompleteListener<AuthResult> {
        return OnCompleteListener { task: Task<AuthResult> ->
            if (task.isSuccessful) {
                if (mAuthListener != null && mAuth!!.currentUser != null) {
                    Timber.tag(TAG).i("Register handler success")
                    // Set user's full name
                    val changeRequest = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()
                    // Update user profile
                    mAuth?.currentUser?.updateProfile(changeRequest)?.addOnCompleteListener {
                        val user = ELUser.buildUser(mAuth!!.currentUser!!)
                        AnalyticsManager.manager.userRegister(user.id, user.email, user.displayName)
                        // Update newsletter consent
                        val consent = newConsent(user.id, newsletterAccepted)
                        ELDatastore.consentStore().create(consent, SetOptions.merge())
                        if (newsletterAccepted) {
                            AnalyticsManager.manager.consentNewsletterGranted()
                        } else {
                            AnalyticsManager.manager.consentNewsletterDenied()
                        }
                        finishLocalLogin(user, task.result?.additionalUserInfo?.isNewUser == true)
                    }
                }
            } else {
                Timber.tag(TAG).i("Register handler error")
                handleAuthError(task.exception ?: Exception("Couldn't register at this time. Please try again"), mAuthListener)
            }
        }
    }

    private fun requestPasswordResetHandler(email: String): OnCompleteListener<Void> {
        return OnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.tag(TAG).i("Password reset email has been sent to $email")
                mAuthListener?.onResetPasswordSuccess()
            } else {
                mAuthListener?.onError(
                        task.exception ?:
                        Exception("Couldn't send email to $email -- try again later"))
            }
        }
    }

    private fun finishLocalLogin(user: ELUser, justRegistered: Boolean) {
        /*
         * Discovered something that looks like a bug in Firestore where set(..., merge=true)
         * overwrites the user subscription even if no subscription is provided. We are therefore
         * forced to reload the user profile when login/reg finishes to make sure we have the latest
         * version, even if it doesn't exist yet
         */
        val block = Runnable {
            saveUser(user)
            SettingsManager.manager.setLoggedIn(true)
            if (justRegistered) {
                addSampleRoutines(user)
            } else {
                mAuthListener?.onSuccess(user)
            }
        }
        // This is required here for the refresh to work
        LocalUserManager.updateUser(user)
        refreshLocalUser(object : OnStoreItemListener<ELUser> {
            override fun onItemLoaded(item: ELUser, fromCache: Boolean) {
                block.run()
            }

            override fun onItemLoadingError(throwable: Throwable) {
                block.run()
            }
        })
    }

    private fun addSampleRoutines(user: ELUser) {
        val task = CreateSampleRoutinesAsyncTask(ELApplication.getInstance()) { mAuthListener?.onSuccess(user) }
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun disableFeaturesForGoogleTestLab() {
        // Disable analytics
        AnalyticsManager.manager.toggleAnalytics(false)
    }

    // Firebase

    fun saveUser(user: ELUser) {
        if (BuildConfig.DEBUG) {
            Timber.tag(TAG).i("Saving user profile: user=%s", user)
        } else {
            Timber.tag(TAG).i("Saving user profile")
        }
        LocalUserManager.updateUser(user)
        ELDatastore.userStore().create(user, SetOptions.merge())
    }

    private fun refreshLocalUser(listener: OnStoreItemListener<ELUser>) {
        Timber.tag(TAG).i("Refreshing user profile")
        ELDatastore.userStore().getItem(LocalUserManager.getUser()?.id, object : OnStoreItemListener<ELUser> {
            override fun onItemLoaded(item: ELUser, fromCache: Boolean) {
                if (BuildConfig.DEBUG) {
                    Timber.tag(TAG).i("User profile refreshed: user=%s", item)
                } else {
                    Timber.tag(TAG).i("User profile refreshed")
                }
                listener.onItemLoaded(item, fromCache)
            }

            override fun onItemLoadingError(throwable: Throwable) {
                Timber.tag(TAG).e(throwable)
                listener.onItemLoadingError(throwable)
            }
        })
    }

    // Setup

    private fun setupGoogleClient() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(ELApplication.getInstance().getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        mClient = GoogleSignIn.getClient(ELApplication.getInstance(), options)
        mAuth = FirebaseAuth.getInstance()
    }

    open class OnAuthActionListener {
        open fun onLogout() {}
        open fun onSuccess(user: ELUser) {}
        open fun onResetPasswordSuccess() {}
        open fun onError(throwable: Throwable) {}
    }
}