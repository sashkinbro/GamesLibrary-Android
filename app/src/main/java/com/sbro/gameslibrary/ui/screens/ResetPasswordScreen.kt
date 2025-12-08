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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scroll = rememberScrollState()

    var email by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 500L) return
        lastClickTime.longValue = now
        action()
    }

    val background = Brush.verticalGradient(
        listOf(cs.background, cs.surfaceContainerLow, cs.background)
    )

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.auth_reset_title),
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
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = cs.outline.copy(alpha = 0.4f)
                )
            }
        },
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(pv)
                .navigationBarsPadding()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.auth_reset_body),
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = cs.surfaceContainerHigh),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = null; sent = false },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.auth_email_label)) },
                        placeholder = { Text(stringResource(R.string.auth_email_hint)) },
                        leadingIcon = {
                            Icon(Icons.Filled.MailOutline, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (error != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text(error!!) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = cs.errorContainer,
                                labelColor = cs.onErrorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (sent) {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.auth_reset_sent_ui)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = cs.primaryContainer,
                                labelColor = cs.onPrimaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
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
                                    else -> {
                                        isLoading = true
                                        viewModel.sendPasswordReset(
                                            context = context,
                                            email = e,
                                            onSuccess = {
                                                isLoading = false
                                                sent = true
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
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            text = stringResource(R.string.auth_reset_button),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
