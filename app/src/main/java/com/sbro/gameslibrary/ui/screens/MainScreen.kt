package com.sbro.gameslibrary.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
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
import com.sbro.gameslibrary.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GameViewModel,
    onOpenPlatforms: () -> Unit,
    onOpenLastTests: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

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
            MaterialTheme.colorScheme.surfaceContainer
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.home_title),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { safeClick(onOpenAbout) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "About",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                }
            }
        ) { padding ->

            if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        MainHeroCard()
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center
                    ) {
                        MainMenuButton(
                            title = stringResource(R.string.main_button_platforms),
                            subtitle = stringResource(R.string.main_button_platforms_sub),
                            icon = Icons.Filled.Widgets,
                            baseColor = MaterialTheme.colorScheme.primary,
                            onClick = { safeClick(onOpenPlatforms) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MainMenuButton(
                            title = stringResource(R.string.main_button_last_tests),
                            subtitle = stringResource(R.string.main_button_last_tests_sub_20),
                            icon = Icons.Filled.History,
                            baseColor = MaterialTheme.colorScheme.secondary,
                            onClick = { safeClick(onOpenLastTests) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MainMenuButton(
                            title = stringResource(R.string.main_button_profile),
                            subtitle = stringResource(R.string.main_button_profile_sub),
                            icon = Icons.Filled.Person,
                            baseColor = MaterialTheme.colorScheme.tertiary,
                            onClick = { safeClick(onOpenProfile) }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MainHeroCard()

                    Spacer(modifier = Modifier.height(32.dp))

                    MainMenuButton(
                        title = stringResource(R.string.main_button_platforms),
                        subtitle = stringResource(R.string.main_button_platforms_sub),
                        icon = Icons.Filled.Widgets,
                        baseColor = MaterialTheme.colorScheme.primary,
                        onClick = { safeClick(onOpenPlatforms) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MainMenuButton(
                        title = stringResource(R.string.main_button_last_tests),
                        subtitle = stringResource(R.string.main_button_last_tests_sub_20),
                        icon = Icons.Filled.History,
                        baseColor = MaterialTheme.colorScheme.secondary,
                        onClick = { safeClick(onOpenLastTests) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MainMenuButton(
                        title = stringResource(R.string.main_button_profile),
                        subtitle = stringResource(R.string.main_button_profile_sub),
                        icon = Icons.Filled.Person,
                        baseColor = MaterialTheme.colorScheme.tertiary,
                        onClick = { safeClick(onOpenProfile) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainHeroCard() {
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
            Column {
                Text(
                    text = stringResource(R.string.main_hero_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.main_hero_body),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = bodyColor,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun MainMenuButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    baseColor: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
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

            Column(modifier = Modifier.padding(start = 20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
