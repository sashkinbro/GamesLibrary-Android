package com.sbro.gameslibrary.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbro.gameslibrary.R

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

    val lastClickTime = remember { mutableLongStateOf(0L) }

    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 500L) return
        lastClickTime.longValue = now
        action()
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceContainer,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                Column {
                    TopAppBar(modifier = Modifier.statusBarsPadding(),
                        navigationIcon = {
                            IconButton(onClick = { safeClick(onBack) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back)
                                )
                            }
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.home_platforms_title),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        },
                        actions = {
                            IconButton(onClick = { safeClick(onOpenProfile) }) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = stringResource(R.string.main_button_profile)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                }
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
                                .verticalScroll(rememberScrollState())
                                .padding(top = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            HomeMenuButton(
                                title = stringResource(R.string.home_button_all_games),
                                icon = Icons.Filled.Games,
                                baseColor = MaterialTheme.colorScheme.primary,
                                onClick = { safeClick(onOpenAllGames) }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            HomeMenuButton(
                                title = stringResource(R.string.home_button_ps3),
                                icon = Icons.Filled.SportsEsports,
                                baseColor = MaterialTheme.colorScheme.tertiary,
                                onClick = { safeClick(onOpenPs3Games) }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            HomeMenuButton(
                                title = stringResource(R.string.home_button_pc),
                                icon = Icons.Filled.Computer,
                                baseColor = MaterialTheme.colorScheme.secondary,
                                onClick = { safeClick(onOpenPcGames) }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            HomeMenuButton(
                                title = stringResource(R.string.home_button_switch),
                                icon = Icons.Filled.SportsEsports,
                                baseColor = MaterialTheme.colorScheme.primary,
                                onClick = { safeClick(onOpenSwitchGames) }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HomeHeroCard()

                        Spacer(modifier = Modifier.height(32.dp))

                        HomeMenuButton(
                            title = stringResource(R.string.home_button_all_games),
                            icon = Icons.Filled.Games,
                            baseColor = MaterialTheme.colorScheme.primary,
                            onClick = { safeClick(onOpenAllGames) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        HomeMenuButton(
                            title = stringResource(R.string.home_button_ps3),
                            icon = Icons.Filled.SportsEsports,
                            baseColor = MaterialTheme.colorScheme.tertiary,
                            onClick = { safeClick(onOpenPs3Games) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        HomeMenuButton(
                            title = stringResource(R.string.home_button_pc),
                            icon = Icons.Filled.Computer,
                            baseColor = MaterialTheme.colorScheme.secondary,
                            onClick = { safeClick(onOpenPcGames) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        HomeMenuButton(
                            title = stringResource(R.string.home_button_switch),
                            icon = Icons.Filled.SportsEsports,
                            baseColor = MaterialTheme.colorScheme.primary,
                            onClick = { safeClick(onOpenSwitchGames) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeroCard() {
    val isDark = isSystemInDarkTheme()

    val backgroundBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                MaterialTheme.colorScheme.surfaceContainerLow
            ),
            tileMode = TileMode.Clamp
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary
            ),
            tileMode = TileMode.Clamp
        )
    }

    val titleColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White
    val bodyColor =
        if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.9f)

    val iconTint = if (isDark) MaterialTheme.colorScheme.primary else Color.White

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = stringResource(R.string.home_hero_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.home_hero_body),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = bodyColor,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeMenuButton(
    title: String,
    icon: ImageVector,
    baseColor: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        baseColor.copy(alpha = 0.12f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = baseColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}
