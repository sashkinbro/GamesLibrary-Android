package com.sbro.gameslibrary.auth

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.sbro.gameslibrary.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthManager(
    private val onLoggedIn: (Context?) -> Unit = {},
    private val onLoggedOut: () -> Unit = {}
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)

    private var appContext: Context? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { fa ->
        _currentUser.value = fa.currentUser
        _isLoggedIn.value = fa.currentUser != null

        if (fa.currentUser != null) onLoggedIn(appContext)
        else onLoggedOut()
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun setAppContext(ctx: Context) {
        appContext = ctx.applicationContext
    }

    fun getGoogleSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(context, gso)
        return client.signInIntent
    }

    fun handleGoogleResult(
        context: Context,
        data: Intent?,
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val credential =
                    GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential).await()

                Toast.makeText(
                    context,
                    context.getString(R.string.auth_success),
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                e.printStackTrace()
                val msg = mapAuthError(context, e)
                onError(msg)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun signInWithEmail(
        context: Context,
        email: String,
        password: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                Toast.makeText(
                    context,
                    context.getString(R.string.auth_email_sign_in_success),
                    Toast.LENGTH_SHORT
                ).show()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = mapAuthError(context, e)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onError(msg)
            }
        }
    }

    fun registerWithEmail(
        context: Context,
        email: String,
        password: String,
        displayName: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val user = result.user
                if (!displayName.isNullOrBlank() && user != null) {
                    val request = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName.trim())
                        .build()
                    user.updateProfile(request).await()
                }
                sendEmailVerificationIfNeeded(context)

                Toast.makeText(
                    context,
                    context.getString(R.string.auth_register_success),
                    Toast.LENGTH_SHORT
                ).show()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = mapAuthError(context, e)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onError(msg)
            }
        }
    }

    fun sendPasswordReset(
        context: Context,
        email: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                Toast.makeText(
                    context,
                    context.getString(R.string.auth_reset_sent),
                    Toast.LENGTH_SHORT
                ).show()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = mapAuthError(context, e)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onError(msg)
            }
        }
    }

    private suspend fun sendEmailVerificationIfNeeded(context: Context) {
        val user = auth.currentUser ?: return
        if (!user.isEmailVerified) {
            try {
                user.sendEmailVerification().await()
                Toast.makeText(
                    context,
                    context.getString(R.string.auth_verify_sent),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Exception) {
            }
        }
    }

    fun signOut(context: Context) {
        scope.launch {
            auth.signOut()
            GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()

            Toast.makeText(
                context,
                context.getString(R.string.auth_signed_out),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mapAuthError(context: Context, e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> {
                when (e.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> context.getString(R.string.auth_error_user_not_found)
                    "ERROR_USER_DISABLED" -> context.getString(R.string.auth_error_user_disabled)
                    else -> context.getString(R.string.auth_error_generic)
                }
            }

            is FirebaseAuthInvalidCredentialsException -> {
                when (e.errorCode) {
                    "ERROR_WRONG_PASSWORD" -> context.getString(R.string.auth_error_wrong_password)
                    "ERROR_INVALID_EMAIL" -> context.getString(R.string.auth_error_bad_email)
                    "ERROR_INVALID_CREDENTIAL" -> context.getString(R.string.auth_error_wrong_password)
                    else -> context.getString(R.string.auth_error_generic)
                }
            }

            is FirebaseAuthUserCollisionException -> {
                context.getString(R.string.auth_error_email_in_use)
            }

            is FirebaseTooManyRequestsException -> {
                context.getString(R.string.auth_error_too_many_requests)
            }

            is FirebaseAuthException -> {
                context.getString(R.string.auth_error_generic)
            }
            else -> context.getString(R.string.auth_error_generic)
        }
    }

    fun updateDisplayName(
        context: Context,
        newName: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            try {
                val user = auth.currentUser
                if (user == null) {
                    onError(context.getString(R.string.auth_error_generic))
                    return@launch
                }

                val trimmed = newName.trim()
                val req = UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmed)
                    .build()

                user.updateProfile(req).await()
                _currentUser.value = auth.currentUser

                Toast.makeText(
                    context,
                    context.getString(R.string.profile_name_updated),
                    Toast.LENGTH_SHORT
                ).show()

                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = mapAuthError(context, e)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onError(msg)
            }
        }
    }
}


