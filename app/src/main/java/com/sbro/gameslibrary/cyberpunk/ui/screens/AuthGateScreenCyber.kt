package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.sbro.gameslibrary.R

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)
private val CyberGray = Color(0xFF202020)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthGateScreenCyber(
    user: FirebaseUser?,
    onContinueAsGuest: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenRegister: () -> Unit
) {

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 500L) return
        lastClickTime.longValue = now
        action()
    }

    val scroll = rememberScrollState()
    val bgBrush = Brush.verticalGradient(listOf(CyberBlack, CyberDark, CyberBlack))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        CyberGridBackground()
        ScanlinesEffect()
        VignetteEffect()

        Scaffold(
            containerColor = Color.Transparent
        ) { pv ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pv)
                    .verticalScroll(scroll)
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val cardShape = CutCornerShape(0.dp, 24.dp, 0.dp, 24.dp)

                Surface(
                    shape = cardShape,
                    color = CyberDark.copy(alpha = 0.96f),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                CyberRed.copy(alpha = 0.65f),
                                CyberYellow.copy(alpha = 0.55f),
                                CyberBlue.copy(alpha = 0.65f)
                            )
                        )
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Surface(
                            shape = CircleShape,
                            color = CyberYellow.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null,
                                tint = CyberYellow,
                                modifier = Modifier
                                    .padding(14.dp)
                                    .size(42.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.auth_gate_title),
                            color = Color.White.copy(alpha = 0.92f),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            fontSize = 26.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = stringResource(R.string.auth_gate_subtitle),
                            color = Color.White.copy(alpha = 0.72f),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        Spacer(Modifier.height(4.dp))

                        CyberPrimaryButton(
                            text = stringResource(R.string.auth_gate_sign_in),
                            icon = Icons.AutoMirrored.Filled.Login,
                            accent = CyberYellow,
                            onClick = { safeClick(onOpenLogin) }
                        )

                        CyberSecondaryButton(
                            text = stringResource(R.string.auth_gate_register),
                            icon = Icons.Filled.PersonAdd,
                            accent = CyberBlue,
                            onClick = { safeClick(onOpenRegister) }
                        )

                        Spacer(Modifier.height(2.dp))

                        CyberTextAction(
                            text = stringResource(R.string.auth_gate_continue_guest),
                            icon = Icons.Filled.Visibility,
                            accent = CyberRed,
                            onClick = { safeClick(onContinueAsGuest) }
                        )

                        Text(
                            text = stringResource(R.string.auth_gate_guest_caption),
                            color = Color.White.copy(alpha = 0.56f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CyberPrimaryButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = CutCornerShape(0.dp, 16.dp, 0.dp, 16.dp)

    Surface(
        onClick = onClick,
        shape = shape,
        color = CyberBlack,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(accent.copy(alpha = 0.95f), CyberYellow.copy(alpha = 0.35f))
            )
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(CyberDark, CyberGray.copy(alpha = 0.65f), CyberDark)
                    )
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = text.uppercase(),
                color = accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun CyberSecondaryButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = CutCornerShape(0.dp, 16.dp, 0.dp, 16.dp)

    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(accent.copy(alpha = 0.75f), CyberYellow.copy(alpha = 0.18f), accent.copy(alpha = 0.75f))
            )
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = text.uppercase(),
                color = Color.White.copy(alpha = 0.88f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun CyberTextAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = CutCornerShape(0.dp, 12.dp, 0.dp, 12.dp)

    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(accent.copy(alpha = 0.55f), CyberYellow.copy(alpha = 0.18f), accent.copy(alpha = 0.55f))
            )
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = text.uppercase(),
                color = accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontSize = 13.sp
            )
        }
    }
}
