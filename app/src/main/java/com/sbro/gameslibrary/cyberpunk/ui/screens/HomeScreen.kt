package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.os.SystemClock
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbro.gameslibrary.R

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onBack: () -> Unit,
    onOpenAllGames: () -> Unit,
    onOpenPs3Games: () -> Unit,
    onOpenPcGames: () -> Unit,
    onOpenSwitchGames: () -> Unit,
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
                CyberTopBar(
                    title = stringResource(R.string.home_platforms_title),
                    onBack = { safeClick(onBack) },
                    onProfile = { safeClick(onOpenProfile) }
                )
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 16.dp)
                        ) {
                            HomeHeroCard()
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState)
                                .padding(top = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            CyberPlatformButtons(
                                onOpenAllGames = { safeClick(onOpenAllGames) },
                                onOpenPs3Games = { safeClick(onOpenPs3Games) },
                                onOpenPcGames = { safeClick(onOpenPcGames) },
                                onOpenSwitchGames = { safeClick(onOpenSwitchGames) }
                            )
                            Spacer(
                                modifier = Modifier.windowInsetsBottomHeight(
                                    WindowInsets.navigationBars
                                )
                            )
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
                            HomeHeroCard()

                            Spacer(modifier = Modifier.height(24.dp))

                            CyberPlatformButtons(
                                onOpenAllGames = { safeClick(onOpenAllGames) },
                                onOpenPs3Games = { safeClick(onOpenPs3Games) },
                                onOpenPcGames = { safeClick(onOpenPcGames) },
                                onOpenSwitchGames = { safeClick(onOpenSwitchGames) }
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
fun CyberPlatformButtons(
    onOpenAllGames: () -> Unit,
    onOpenPs3Games: () -> Unit,
    onOpenPcGames: () -> Unit,
    onOpenSwitchGames: () -> Unit
) {
    CyberMenuButton(
        title = stringResource(R.string.home_button_all_games),
        techSubtitle = "DB_ACCESS // FULL",
        icon = Icons.Filled.Games,
        accentColor = CyberYellow,
        techId = "ALL-00",
        onClick = onOpenAllGames
    )

    Spacer(modifier = Modifier.height(16.dp))

    CyberMenuButton(
        title = stringResource(R.string.home_button_ps3),
        techSubtitle = "EMULATION // CELL_BE",
        icon = Icons.Filled.SportsEsports,
        accentColor = CyberBlue,
        techId = "PS3-01",
        onClick = onOpenPs3Games
    )

    Spacer(modifier = Modifier.height(16.dp))

    CyberMenuButton(
        title = stringResource(R.string.home_button_pc),
        techSubtitle = "x86_64 // WINDOWS",
        icon = Icons.Filled.Computer,
        accentColor = CyberRed,
        techId = "PC-02",
        onClick = onOpenPcGames
    )

    Spacer(modifier = Modifier.height(16.dp))

    CyberMenuButton(
        title = stringResource(R.string.home_button_switch),
        techSubtitle = "N.TEGRA // PORTABLE",
        icon = Icons.Filled.SportsEsports,
        accentColor = CyberYellow,
        techId = "NS-03",
        onClick = onOpenSwitchGames
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CyberTopBar(
    title: String,
    onBack: () -> Unit,
    onProfile: () -> Unit
) {
    Column {
        TopAppBar(modifier = Modifier.statusBarsPadding(),
            navigationIcon = {
                IconButton(
                    onClick = onBack,
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
            title = {
                GlitchText(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                )
            },
            actions = {
                IconButton(onClick = onProfile) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Profile",
                        tint = CyberYellow
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(CyberRed, CyberYellow, Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun HomeHeroCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_anim")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(topStart = 24.dp, bottomEnd = 24.dp))
            .background(CyberDark)
            .border(
                1.dp,
                CyberBlue.copy(alpha = 0.5f),
                CutCornerShape(topStart = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            val len = 20.dp.toPx()
            drawLine(CyberBlue, Offset(0f, 0f), Offset(len, 0f), stroke)
            drawLine(CyberBlue, Offset(0f, 0f), Offset(0f, len), stroke)
            drawLine(
                CyberBlue,
                Offset(size.width, size.height),
                Offset(size.width - len, size.height),
                stroke
            )
            drawLine(
                CyberBlue,
                Offset(size.width, size.height),
                Offset(size.width, size.height - len),
                stroke
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = CyberBlue.copy(alpha = alphaAnim),
                modifier = Modifier
                    .size(32.dp)
                    .padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = stringResource(R.string.home_hero_title).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CyberBlue,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_hero_body),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun CyberMenuButton(
    title: String,
    techSubtitle: String,
    icon: ImageVector,
    accentColor: Color,
    techId: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cutSize = 15.dp.toPx()
            val strokeWidth = 1.dp.toPx()

            val path = Path().apply {
                moveTo(cutSize, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - cutSize)
                lineTo(size.width - cutSize, size.height)
                lineTo(0f, size.height)
                lineTo(0f, cutSize)
                close()
            }

            drawPath(path, color = CyberDark)
            drawPath(
                path,
                color = accentColor.copy(alpha = if (isPressed) 1f else 0.4f),
                style = Stroke(strokeWidth)
            )

            if (isPressed) {
                drawPath(path, color = accentColor.copy(alpha = 0.1f))
            }
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
                    .background(accentColor.copy(alpha = 0.1f), CutCornerShape(10.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), CutCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White
                )
                Text(
                    text = techSubtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = accentColor.copy(alpha = 0.6f)
                )
            }

            Text(
                text = techId,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                ),
                color = Color.Gray
            )
        }
    }
}
