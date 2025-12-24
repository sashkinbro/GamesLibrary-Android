package com.sbro.gameslibrary.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbro.gameslibrary.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    hasSeenOnboarding: Boolean?,
    onNavigateNext: (goToOnboarding: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(hasSeenOnboarding) {
        if (hasSeenOnboarding != null) {
            delay(200)
            onNavigateNext(!hasSeenOnboarding)
        }
    }

    val bgBrush = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF07080C),
                Color(0xFF0A0B10),
                Color(0xFF040509)
            ),
            center = Offset(0.5f, 0.38f),
            radius = 1300f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFF7F8FB),
                Color(0xFFEFF1F6),
                Color(0xFFE6E9F0)
            ),
            center = Offset(0.5f, 0.38f),
            radius = 1300f
        )
    }

    val topGlow = if (isDark) Color(0x332196F3) else Color(0x221E88E5)

    val infinite = rememberInfiniteTransition(label = "splashInfinite")
    val iconAlpha by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconAlpha"
    )
    val iconScale by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    val shimmerX by infinite.animateFloat(
        initialValue = -700f,
        targetValue = 700f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF8E99A8),
            Color.White,
            Color(0xFF7E57C2),
            Color.White,
            Color(0xFF8E99A8)
        )
    } else {
        listOf(
            Color(0xFF6B7280),
            Color(0xFF111827),
            Color(0xFF1E88E5),
            Color(0xFF111827),
            Color(0xFF6B7280)
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = if (isDark) Color.Black else Color(0xFFF7F8FB)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(topGlow, Color.Transparent)
                        )
                    )
                    .align(Alignment.TopCenter)
            )

            Image(
                painter = painterResource(id = R.drawable.ic_icon),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
                    .scale(iconScale)
                    .alpha(iconAlpha)
            )

            val shimmerBrush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(shimmerX, 0f),
                end = Offset(shimmerX + 520f, 0f)
            )

            androidx.compose.material3.Text(
                text = "Game Library",
                style = TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF111827),
                    letterSpacing = 1.2.sp
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 98.dp)
                    .alpha(glowAlpha)
            )

            androidx.compose.material3.Text(
                text = "Game Library",
                style = TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    brush = shimmerBrush,
                    letterSpacing = 1.2.sp
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
            )
        }
    }
}
