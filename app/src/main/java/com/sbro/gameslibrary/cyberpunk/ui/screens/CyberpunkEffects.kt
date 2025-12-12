package com.sbro.gameslibrary.cyberpunk.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberGray = Color(0xFF202020)



@Composable
fun GlitchText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val baseStyle = style.copy(color = CyberYellow)

    Box(modifier = modifier) {
        Text(
            text = text,
            style = baseStyle.copy(
                color = CyberBlue.copy(alpha = 0.25f),
                shadow = Shadow(
                    color = CyberBlue.copy(alpha = 0.9f),
                    blurRadius = 24f,
                    offset = Offset.Zero
                )
            )
        )

        Text(
            text = text,
            style = baseStyle.copy(
                color = CyberRed.copy(alpha = 0.18f),
                shadow = Shadow(
                    color = CyberRed.copy(alpha = 0.8f),
                    blurRadius = 14f,
                    offset = Offset.Zero
                )
            )
        )

        Text(
            text = text,
            style = baseStyle.copy(
                shadow = Shadow(
                    color = CyberYellow.copy(alpha = 0.9f),
                    blurRadius = 6f,
                    offset = Offset.Zero
                )
            )
        )
    }
}

@Composable
fun ScanlinesEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 50f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "offsetY"
    )
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.04f)) {
        val gap = 4.dp.toPx()
        for (y in 0 until size.height.toInt() step gap.toInt()) {
            drawLine(Color.White, Offset(0f, y.toFloat() + offsetY % gap), Offset(size.width, y.toFloat() + offsetY % gap))
        }
    }
}

@Composable
fun VignetteEffect() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                    center = Offset.Unspecified,
                    radius = 2000f
                )
            )
    )
}

@Composable
fun CyberGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 40.dp.toPx()
        val color = CyberGray.copy(alpha = 0.5f)
        for (x in 0..size.width.toInt() step gridSize.toInt()) {
            drawLine(color, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1f)
        }
        for (y in 0..size.height.toInt() step gridSize.toInt()) {
            drawLine(color, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
        }
    }
}