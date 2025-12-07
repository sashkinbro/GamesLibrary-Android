package com.sbro.gameslibrary.ui.screens

import android.os.SystemClock
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onGoLogin: () -> Unit,
    onRegistered: () -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scroll = rememberScrollState()

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var pass2 by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 500L) return
        lastClickTime.longValue = now
        action()
    }

    val background = Brush.verticalGradient(listOf(cs.background, cs.surfaceContainer))

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.auth_register_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { safeClick(onBack) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                )
                HorizontalDivider(color = cs.outline.copy(alpha = 0.4f))
            }
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(pv)
                .navigationBarsPadding()
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.auth_name_label)) },
                        placeholder = { Text(stringResource(R.string.auth_name_hint)) },
                        leadingIcon = {
                            Icon(Icons.Filled.AccountCircle, contentDescription = null)
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.auth_email_label)) },
                        placeholder = { Text(stringResource(R.string.auth_email_hint)) },
                        leadingIcon = {
                            Icon(Icons.Filled.MailOutline, contentDescription = null)
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.auth_password_label)) },
                        placeholder = { Text(stringResource(R.string.auth_password_hint)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = pass2,
                        onValueChange = { pass2 = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.auth_password_repeat_label)) },
                        placeholder = { Text(stringResource(R.string.auth_password_repeat_hint)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    if (error != null) {
                        Text(
                            text = error!!,
                            color = cs.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            safeClick {
                                val e = email.trim()
                                when {
                                    e.isBlank() -> error = context.getString(R.string.auth_error_empty_email)
                                    !Patterns.EMAIL_ADDRESS.matcher(e).matches() ->
                                        error = context.getString(R.string.auth_error_bad_email)
                                    pass.isBlank() -> error = context.getString(R.string.auth_error_empty_password)
                                    pass.length < 6 -> error = context.getString(R.string.auth_error_short_password)
                                    pass != pass2 -> error = context.getString(R.string.auth_error_passwords_mismatch)
                                    else -> {
                                        isLoading = true
                                        viewModel.registerWithEmail(
                                            context = context,
                                            email = e,
                                            password = pass,
                                            displayName = name.trim().ifBlank { null },
                                            onSuccess = {
                                                isLoading = false
                                                onRegistered()
                                            },
                                            onError = {
                                                isLoading = false
                                                error = it
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.auth_register_button))
                    }

                    Text(
                        text = stringResource(R.string.auth_register_note_verify),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.auth_have_account),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(6.dp))
                TextButton(onClick = { safeClick(onGoLogin) }) {
                    Text(stringResource(R.string.auth_go_login))
                }
            }
        }
    }
}
