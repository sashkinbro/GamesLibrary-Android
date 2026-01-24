package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.sbro.gameslibrary.R

// Import DataStore utilities for theme switching
import com.sbro.gameslibrary.util.CYBERPUNK_MODE
import com.sbro.gameslibrary.util.dataStore
import androidx.datastore.preferences.core.edit
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.material3.SwitchDefaults

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onShowRules: () -> Unit
) {
    val context = LocalContext.current

    // Observe the current theme preference and provide a coroutine scope for updates.
    val scope = rememberCoroutineScope()
    val isCyberpunkEnabled by context.dataStore.data
        .map { prefs -> prefs[CYBERPUNK_MODE] ?: false }
        .collectAsState(initial = false)

    val backLastClick = remember { mutableLongStateOf(0L) }
    fun safeBackClick() {
        val now = SystemClock.elapsedRealtime()
        if (now - backLastClick.longValue < 400L) return
        backLastClick.longValue = now
        onBack()
    }

    val siteUrl = stringResource(R.string.about_link_site_url)
    val githubUrl = stringResource(R.string.about_link_github_url)

    val versionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "N/A"
        } catch (_: Exception) {
            "N/A"
        }
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }

    val bgBrush = Brush.verticalGradient(listOf(CyberBlack, CyberDark, CyberBlack))
    val heroBrush = Brush.horizontalGradient(
        listOf(
            CyberRed.copy(alpha = 0.18f),
            CyberYellow.copy(alpha = 0.16f),
            CyberBlue.copy(alpha = 0.18f)
        )
    )

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
                Column {
                    TopAppBar(modifier = Modifier.statusBarsPadding(),
                        title = {
                            GlitchText(
                                text = stringResource(R.string.about_title).uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = ::safeBackClick,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .clip(CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                                    .background(CyberDark)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_navigate_back),
                                    tint = CyberRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = CyberYellow,
                            navigationIconContentColor = CyberYellow
                        )
                    )
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberYellow.copy(alpha = 0.12f)
                    )
                }
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgBrush)
                    .padding(top = padding.calculateTopPadding())
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val heroShape = CutCornerShape(topEnd = 22.dp, bottomStart = 22.dp)
                Surface(
                    shape = heroShape,
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
                            .background(heroBrush)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyberBlack.copy(alpha = 0.7f))
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_icon_splash),
                                    contentDescription = null,
                                    tint = CyberYellow,
                                    modifier = Modifier.size(44.dp)
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.app_name).uppercase(),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = CyberYellow
                                )
                                Spacer(Modifier.height(6.dp))
                                VersionChipCyber(text = "v$versionName")
                            }
                        }

                        Text(
                            text = stringResource(R.string.about_description),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.4.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Surface(
                    shape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
                    color = CyberDark,
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(CyberRed, CyberYellow, CyberBlue))
                    ),
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.about_toggle_cyberpunk).uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = CyberYellow,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isCyberpunkEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    context.dataStore.edit { prefs ->
                                        prefs[CYBERPUNK_MODE] = enabled
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberYellow,
                                checkedTrackColor = CyberYellow.copy(alpha = 0.3f),
                                uncheckedThumbColor = CyberBlue,
                                uncheckedTrackColor = CyberBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                SectionCardCyber {
                    LinkButtonCyber(
                        icon = Icons.Filled.Gavel,
                        title = stringResource(R.string.about_rules_button_title),
                        subtitle = stringResource(R.string.about_rules_button_subtitle),
                        onClick = onShowRules
                    )
                }

                SectionCardCyber {
                    AboutRowCyber(
                        icon = Icons.Filled.NewReleases,
                        title = stringResource(R.string.about_feature_fast_title),
                        body = stringResource(R.string.about_feature_fast_body)
                    )

                    DividerSoftCyber()

                    AboutRowCyber(
                        icon = Icons.Filled.Security,
                        title = stringResource(R.string.about_feature_anonymous_title),
                        body = stringResource(R.string.about_feature_anonymous_body)
                    )

                    DividerSoftCyber()

                    AboutRowCyber(
                        icon = Icons.Filled.Code,
                        title = stringResource(R.string.about_feature_open_title),
                        body = stringResource(R.string.about_feature_open_body)
                    )
                }

                SectionCardCyber {
                    LinkButtonCyber(
                        icon = Icons.Filled.Language,
                        title = stringResource(R.string.about_link_site_title),
                        subtitle = stringResource(R.string.about_link_site_subtitle),
                        onClick = { openUrl(siteUrl) }
                    )

                    Spacer(Modifier.height(10.dp))

                    LinkButtonCyber(
                        icon = Icons.Filled.Code,
                        title = stringResource(R.string.about_link_github_title),
                        subtitle = stringResource(R.string.about_link_github_subtitle),
                        onClick = { openUrl(githubUrl) }
                    )
                }

                Surface(
                    shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
                    color = CyberDark,
                    border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.18f)),
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = CyberBlue,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.size(10.dp))

                        Text(
                            text = stringResource(R.string.about_igdb_attribution),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.about_footer),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    ),
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
                Spacer(
                    modifier = Modifier
                        .height(4.dp)
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }
        }
    }
}

@Composable
private fun SectionCardCyber(
    content: @Composable () -> Unit
) {
    Surface(
        shape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
        color = CyberDark,
        border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.14f)),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun DividerSoftCyber() {
    Spacer(Modifier.height(6.dp))
    HorizontalDivider(
        color = CyberYellow.copy(alpha = 0.12f),
        thickness = 1.dp,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun VersionChipCyber(text: String) {
    Box(
        modifier = Modifier
            .clip(CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp))
            .background(CyberBlack.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = CyberYellow
        )
    }
}

@Composable
private fun AboutRowCyber(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
            color = CyberBlack,
            border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.35f)),
            modifier = Modifier.size(46.dp),
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyberBlue
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title.uppercase(),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                color = CyberYellow
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun LinkButtonCyber(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
        color = CyberBlack,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(CyberRed, CyberYellow, CyberBlue))
        ),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
                color = CyberDark,
                border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.18f)),
                modifier = Modifier.size(42.dp),
                tonalElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CyberYellow
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = CyberYellow
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
