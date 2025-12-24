package com.sbro.gameslibrary.cyberpunk.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbro.gameslibrary.R
import kotlinx.coroutines.delay

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)

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

    val bgBrush = Brush.radialGradient(
        colors = listOf(CyberBlack, CyberDark, CyberBlack),
        center = Offset(0.5f, 0.38f),
        radius = 1300f
    )

    val infinite = rememberInfiniteTransition(label = "splashInfinite")

    val iconAlpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(820, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconAlpha"
    )
    val iconScale by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(980, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    val shimmerX by infinite.animateFloat(
        initialValue = -720f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    val titleGlowAlpha by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleGlowAlpha"
    )

    val shimmerColors = listOf(
        CyberYellow.copy(alpha = 0.35f),
        CyberYellow,
        CyberBlue,
        CyberYellow,
        CyberYellow.copy(alpha = 0.35f)
    )

    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 520f, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        CyberGridBackground()
        ScanlinesEffect()
        VignetteEffect()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
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
                                listOf(
                                    CyberBlue.copy(alpha = 0.20f),
                                    Color.Transparent
                                )
                            )
                        )
                        .align(Alignment.TopCenter)
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_icon_splash),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.Center)
                        .scale(iconScale)
                        .alpha(iconAlpha)
                )

                androidx.compose.material3.Text(
                    text = "GAME LIBRARY",
                    style = TextStyle(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberYellow,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 98.dp)
                        .alpha(titleGlowAlpha)
                )

                androidx.compose.material3.Text(
                    text = "GAME LIBRARY",
                    style = TextStyle(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        brush = shimmerBrush,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp)
                )

                androidx.compose.material3.Text(
                    text = if (isDark) "BOOT SEQUENCE" else "BOOT SEQUENCE",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 70.dp)
                )
            }
        }
    }
}
