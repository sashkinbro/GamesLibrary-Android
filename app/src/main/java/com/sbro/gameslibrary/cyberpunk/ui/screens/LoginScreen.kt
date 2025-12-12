package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.app.Activity
import android.os.SystemClock
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.viewmodel.ProfileViewModel

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)
private val CyberGray = Color(0xFF202020)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onGoRegister: () -> Unit,
    onGoReset: () -> Unit,
    onLoggedIn: () -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 500L) return
        lastClickTime.longValue = now
        action()
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            viewModel.handleGoogleResult(context, res.data) {
                error = it
            }
        }
    }

    val bgBrush = Brush.verticalGradient(
        listOf(CyberBlack, CyberDark, CyberBlack)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        CyberGridBackground()
        ScanlinesEffect()
        VignetteEffect()

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            GlitchText(
                                text = stringResource(R.string.auth_login_title).uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 22.sp
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { safeClick(onBack) },
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .clip(CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                                    .background(CyberDark)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = CyberRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = CyberYellow,
                            navigationIconContentColor = CyberYellow
                        ),
                        modifier = Modifier.statusBarsPadding()
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(CyberRed, CyberYellow, Color.Transparent)
                                )
                            )
                    )
                }
            },
            bottomBar = {
                Surface(
                    color = CyberBlack,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.auth_no_account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.width(6.dp))
                        TextButton(
                            onClick = { safeClick(onGoRegister) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = CutCornerShape(0.dp, 10.dp, 0.dp, 10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.auth_go_register).uppercase(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = CyberBlue,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        ) { pv ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pv)
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.auth_sign_in_email),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberYellow.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(Modifier.height(12.dp))

                // MAIN CARD
                Surface(
                    shape = CutCornerShape(0.dp, 22.dp, 0.dp, 22.dp),
                    color = CyberDark.copy(alpha = 0.98f),
                    border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.12f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        CyberOutlinedTextField(
                            value = email,
                            onValueChange = { email = it; error = null },
                            label = stringResource(R.string.auth_email_label),
                            placeholder = stringResource(R.string.auth_email_hint),
                            leading = {
                                Icon(
                                    Icons.Filled.MailOutline,
                                    contentDescription = null,
                                    tint = CyberBlue
                                )
                            },
                            singleLine = true,
                            isError = error != null &&
                                    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
                        )

                        CyberOutlinedTextField(
                            value = pass,
                            onValueChange = { pass = it; error = null },
                            label = stringResource(R.string.auth_password_label),
                            placeholder = stringResource(R.string.auth_password_hint),
                            leading = {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = CyberBlue
                                )
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = error != null && pass.length < 6
                        )

                        if (error != null) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        error!!,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = CyberRed.copy(alpha = 0.15f),
                                    labelColor = CyberRed
                                ),
                                border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        CyberCutButton(
                            text = stringResource(R.string.auth_login_button),
                            onClick = {
                                safeClick {
                                    val e = email.trim()
                                    val p = pass
                                    when {
                                        e.isBlank() ->
                                            error = context.getString(R.string.auth_error_empty_email)
                                        !Patterns.EMAIL_ADDRESS.matcher(e).matches() ->
                                            error = context.getString(R.string.auth_error_bad_email)
                                        p.isBlank() ->
                                            error = context.getString(R.string.auth_error_empty_password)
                                        p.length < 6 ->
                                            error = context.getString(R.string.auth_error_short_password)
                                        else -> {
                                            isLoading = true
                                            viewModel.signInWithEmail(
                                                context = context,
                                                email = e,
                                                password = p,
                                                onSuccess = {
                                                    isLoading = false
                                                    onLoggedIn()
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
                            accent = CyberYellow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberYellow
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                        }

                        TextButton(
                            onClick = { safeClick(onGoReset) },
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                stringResource(R.string.auth_forgot_password).uppercase(),
                                color = CyberYellow.copy(alpha = 0.9f),
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = CyberYellow.copy(alpha = 0.25f)
                    )
                    Text(
                        text = stringResource(R.string.common_or).ifBlank { "or" }.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberYellow.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = CyberYellow.copy(alpha = 0.25f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        safeClick {
                            googleLauncher.launch(viewModel.getGoogleSignInIntent(context))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = CutCornerShape(0.dp, 14.dp, 0.dp, 14.dp),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(CyberRed.copy(alpha = 0.9f), CyberBlue.copy(alpha = 0.9f))
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CyberDark,
                        contentColor = CyberYellow
                    )
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.sign_in_google).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}


@Composable
private fun CyberOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leading: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    visualTransformation: PasswordVisualTransformation? = null,
    isError: Boolean = false
) {
    if (visualTransformation != null) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    label,
                    fontFamily = FontFamily.Monospace,
                    color = CyberYellow.copy(alpha = 0.8f)
                )
            },
            placeholder = {
                Text(
                    placeholder,
                    fontFamily = FontFamily.Monospace,
                    color = CyberYellow.copy(alpha = 0.5f)
                )
            },
            leadingIcon = leading,
            singleLine = singleLine,
            isError = isError,
            visualTransformation = visualTransformation,
            shape = CutCornerShape(0.dp, 12.dp, 0.dp, 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberYellow,
                unfocusedBorderColor = CyberYellow.copy(alpha = 0.25f),
                focusedContainerColor = CyberGray,
                unfocusedContainerColor = CyberGray,
                focusedTextColor = CyberYellow,
                unfocusedTextColor = CyberYellow,
                cursorColor = CyberBlue,
                errorBorderColor = CyberRed,
                errorContainerColor = CyberGray,
                errorTextColor = CyberYellow
            )
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    label,
                    fontFamily = FontFamily.Monospace,
                    color = CyberYellow.copy(alpha = 0.8f)
                )
            },
            placeholder = {
                Text(
                    placeholder,
                    fontFamily = FontFamily.Monospace,
                    color = CyberYellow.copy(alpha = 0.5f)
                )
            },
            leadingIcon = leading,
            singleLine = singleLine,
            isError = isError,
            shape = CutCornerShape(0.dp, 12.dp, 0.dp, 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberYellow,
                unfocusedBorderColor = CyberYellow.copy(alpha = 0.25f),
                focusedContainerColor = CyberGray,
                unfocusedContainerColor = CyberGray,
                focusedTextColor = CyberYellow,
                unfocusedTextColor = CyberYellow,
                cursorColor = CyberBlue,
                errorBorderColor = CyberRed,
                errorContainerColor = CyberGray,
                errorTextColor = CyberYellow
            )
        )
    }
}

@Composable
private fun CyberCutButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = CyberYellow,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val shape = CutCornerShape(0.dp, 14.dp, 0.dp, 14.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = CyberDark,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    CyberRed.copy(alpha = if (enabled) 0.9f else 0.3f),
                    accent.copy(alpha = if (enabled) 0.9f else 0.3f)
                )
            )
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.height(54.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            CyberGray.copy(alpha = if (enabled) 0.35f else 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.width(8.dp))
            }

            Text(
                text = text.uppercase(),
                color = accent.copy(alpha = if (enabled) 1f else 0.45f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 15.sp
            )

            if (trailingContent != null) {
                Spacer(Modifier.width(8.dp))
                trailingContent()
            }
        }
    }
}
