package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbro.gameslibrary.R
import kotlinx.coroutines.delay

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenPlatforms: () -> Unit,
    onOpenLastTests: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation
    val scrollState = rememberScrollState()

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime < 500L) return
        lastClickTime = now
        action()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        CyberGridBackground()
        ScanlinesEffect()
        VignetteEffect()

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            containerColor = Color.Transparent,
            topBar = {
                CyberTopBar(onOpenAbout = { safeClick(onOpenAbout) })
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 16.dp)
                        ) {
                            CyberHeroCard()
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState)
                                    .padding(top = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                CyberButtonList(
                                    onOpenPlatforms,
                                    onOpenLastTests,
                                    onOpenProfile,
                                    ::safeClick
                                )
                                Spacer(
                                    modifier = Modifier.windowInsetsBottomHeight(
                                        WindowInsets.navigationBars
                                    )
                                )
                            }
                        }
                    }

                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(top = 12.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CyberHeroCard()
                            Spacer(modifier = Modifier.height(24.dp))
                            CyberButtonList(
                                onOpenPlatforms,
                                onOpenLastTests,
                                onOpenProfile,
                                ::safeClick
                            )
                            Spacer(
                                modifier = Modifier.windowInsetsBottomHeight(
                                    WindowInsets.navigationBars
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CyberButtonList(
    onOpenPlatforms: () -> Unit,
    onOpenLastTests: () -> Unit,
    onOpenProfile: () -> Unit,
    safeClick: (() -> Unit) -> Unit
) {
    CyberButton(
        text = stringResource(R.string.main_button_platforms),
        subText = stringResource(R.string.main_button_platforms_sub),
        icon = Icons.Filled.Widgets,
        accentColor = CyberBlue,
        techId = "A-01",
        onClick = { safeClick(onOpenPlatforms) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    CyberButton(
        text = stringResource(R.string.main_button_last_tests),
        subText = stringResource(R.string.main_button_last_tests_sub_20),
        icon = Icons.Filled.History,
        accentColor = CyberRed,
        techId = "B-02",
        onClick = { safeClick(onOpenLastTests) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    CyberButton(
        text = stringResource(R.string.main_button_profile),
        subText = stringResource(R.string.main_button_profile_sub),
        icon = Icons.Filled.Person,
        accentColor = CyberYellow,
        techId = "C-03",
        onClick = { safeClick(onOpenProfile) }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberTopBar(onOpenAbout: () -> Unit) {
    Column {
        TopAppBar( modifier = Modifier.statusBarsPadding(),
            title = {
                GlitchText(
                    text = stringResource(R.string.home_title).uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
            },
            actions = {
                IconButton(onClick = onOpenAbout) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "About",
                        tint = CyberBlue
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        Row(modifier = Modifier.fillMaxWidth().height(2.dp).padding(horizontal = 16.dp)) {
            Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(CyberYellow))
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.weight(0.5f).fillMaxHeight().background(CyberRed))
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(CyberBlue))
        }
    }
}

@Composable
fun CyberHeroCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(bottomEnd = 40.dp))
            .background(CyberYellow.copy(alpha = 0.05f))
            .border(1.dp, CyberYellow.copy(alpha = borderAlpha), CutCornerShape(bottomEnd = 40.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = CyberYellow.copy(alpha = 0.3f),
                start = Offset(0f, 0f),
                end = Offset(40.dp.toPx(), 0f),
                strokeWidth = 4.dp.toPx()
            )
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(CyberRed))
                Spacer(modifier = Modifier.width(8.dp))
                TypewriterText(
                    text = "SYSTEM_NOTIFICATION // PRIORITY_1",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberRed
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.main_hero_title).uppercase(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = CyberYellow,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.main_hero_body),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 22.sp
                )
            )
        }
    }
}

@Composable
fun CyberButton(
    text: String,
    subText: String,
    icon: ImageVector,
    accentColor: Color,
    techId: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")
    val containerColor = if (isPressed) accentColor.copy(alpha = 0.2f) else CyberDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.dp.toPx()
            val cutSize = 20.dp.toPx()

            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width - cutSize, 0f)
                lineTo(size.width, cutSize)
                lineTo(size.width, size.height)
                lineTo(cutSize, size.height)
                lineTo(0f, size.height - cutSize)
                close()
            }

            drawPath(path, color = containerColor)

            drawPath(path, color = accentColor.copy(alpha = if(isPressed) 1f else 0.5f), style = Stroke(strokeWidth))

            val cornerLength = 15.dp.toPx()
            drawLine(accentColor, Offset(0f, 0f), Offset(cornerLength, 0f), 4.dp.toPx())
            drawLine(accentColor, Offset(0f, 0f), Offset(0f, cornerLength), 4.dp.toPx())

            val trianglePath = Path().apply {
                moveTo(size.width, size.height)
                lineTo(size.width - cornerLength, size.height)
                lineTo(size.width, size.height - cornerLength)
                close()
            }
            drawPath(trianglePath, color = accentColor)
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, accentColor, CutCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.Gray
                )
            }

            Text(
                text = techId,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = accentColor.copy(alpha = 0.7f)
            )
        }
    }
}


@Composable
fun TypewriterText(text: String, style: TextStyle) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        displayedText = ""
        text.forEach { char ->
            displayedText += char
            delay(50)
        }
    }

    Text(text = displayedText + "_", style = style)
}
