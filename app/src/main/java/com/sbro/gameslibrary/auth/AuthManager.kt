package com.sbro.gameslibrary.auth

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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

        if (fa.currentUser != null) {
            onLoggedIn(appContext)
        } else {
            onLoggedOut()
        }
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
                onError(e.message ?: "Auth failed")
                Toast.makeText(
                    context,
                    context.getString(R.string.auth_failed),
                    Toast.LENGTH_SHORT
                ).show()
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

}
