package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.os.SystemClock
import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
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
fun ResetPasswordScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
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

    val bgBrush = Brush.verticalGradient(listOf(CyberBlack, CyberDark, CyberBlack))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        CyberGridBackground()
        ScanlinesEffect()
        VignetteEffect()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            GlitchText(
                                text = stringResource(R.string.auth_reset_title).uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp,
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
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = CyberRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = CyberYellow
                        )
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberYellow.copy(alpha = 0.12f)
                    )
                }
            },
        ) { pv ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgBrush)
                    .padding(pv)
                    .navigationBarsPadding()
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp)
            ) {

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.auth_reset_body),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    ),
                    color = CyberBlue.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(Modifier.height(12.dp))

                val cardShape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)

                Surface(
                    shape = cardShape,
                    color = CyberDark,
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(CyberRed, CyberYellow, CyberBlue))
                    ),
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        CyberTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                error = null
                                sent = false
                            },
                            label = stringResource(R.string.auth_email_label),
                            placeholder = stringResource(R.string.auth_email_hint),
                            leading = Icons.Filled.MailOutline
                        )

                        if (error != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyberBlack,
                                border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.6f)),
                                tonalElevation = 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = error!!,
                                    color = CyberRed,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (sent) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyberBlack,
                                border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.7f)),
                                tonalElevation = 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_reset_sent_ui),
                                    color = CyberBlue,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        CyberPrimaryButton(
                            enabled = !isLoading,
                            loading = isLoading,
                            text = stringResource(R.string.auth_reset_button),
                            onClick = {
                                safeClick {
                                    val e = email.trim()
                                    when {
                                        e.isBlank() ->
                                            error = context.getString(R.string.auth_error_empty_email)
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
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CyberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leading: androidx.compose.ui.graphics.vector.ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                label,
                color = CyberBlue.copy(alpha = 0.9f),
                fontFamily = FontFamily.Monospace
            )
        },
        placeholder = {
            Text(
                placeholder,
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace
            )
        },
        leadingIcon = { Icon(leading, contentDescription = null, tint = CyberYellow) },
        singleLine = true,
        visualTransformation = VisualTransformation.None,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberYellow,
            unfocusedBorderColor = CyberYellow.copy(alpha = 0.35f),
            focusedTextColor = CyberYellow,
            unfocusedTextColor = CyberYellow,
            cursorColor = CyberYellow
        )
    )
}

@Composable
private fun CyberPrimaryButton(
    enabled: Boolean,
    loading: Boolean,
    text: String,
    onClick: () -> Unit
) {
    val shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)

    Surface(
        shape = shape,
        color = CyberBlack,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(CyberYellow, CyberBlue))
        ),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(listOf(CyberDark, CyberGray, CyberDark))
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = CyberYellow
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = CyberYellow
            )
        }
    }
}
