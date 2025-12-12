package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.annotation.SuppressLint
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.WorkStatus
import com.sbro.gameslibrary.cyberpunk.components.WorkStatusBadge
import com.sbro.gameslibrary.viewmodel.GameDetailViewModel

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)
private val CyberGray = Color(0xFF202020)

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestHistoryDetailScreen(
    viewModel: GameDetailViewModel,
    gameId: String,
    testMillis: Long,
    onBack: () -> Unit,
    onEditTest: (Long) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scroll = rememberScrollState()

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 450L) return
        lastClickTime.longValue = now
        action()
    }

    LaunchedEffect(gameId) {
        viewModel.init(context, gameId)
    }

    val game by viewModel.game.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val canonicalTestId = remember(gameId, testMillis) { "${gameId}_${testMillis}" }

    val test = remember(game, testMillis, canonicalTestId) {
        game?.testResults?.firstOrNull { it.updatedAtMillis == testMillis }
            ?: game?.testResults?.firstOrNull { it.testId == canonicalTestId }
    }

    val canEdit = remember(test, currentUser) {
        val userUid = currentUser?.uid
        userUid != null && test != null && test.fromAccount && test.authorUid == userUid
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
            contentWindowInsets = WindowInsets(0),
            topBar = {
                Column {
                    TopAppBar(modifier = Modifier.statusBarsPadding(),
                        title = {
                            GlitchText(
                                text = stringResource(R.string.test_history_detail_title).uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
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
                        actions = {
                            if (canEdit) {
                                IconButton(
                                    onClick = { safeClick { onEditTest(testMillis) } },
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .clip(CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                                        .background(CyberDark)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit test",
                                        tint = CyberYellow
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = CyberYellow
                        )
                    )
                    HorizontalDivider(color = CyberYellow.copy(alpha = 0.12f))
                }
            }
        ) { pv ->

            when {
                isLoading -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(pv),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CyberYellow,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    return@Scaffold
                }

                game == null || test == null -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(pv),
                        contentAlignment = Alignment.Center
                    ) {
                        CyberEmptyPanel(text = stringResource(R.string.error_test_not_found))
                    }
                    return@Scaffold
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgBrush)
                    .padding(pv)
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {

                val panelShape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)
                Surface(
                    shape = panelShape,
                    color = CyberDark,
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                CyberRed.copy(alpha = 0.7f),
                                CyberYellow.copy(alpha = 0.7f),
                                CyberBlue.copy(alpha = 0.7f)
                            )
                        )
                    ),
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
                                WorkStatusBadge(status = test.status)
                            }

                            Column(horizontalAlignment = Alignment.End) {

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = test.testedDateFormatted.ifBlank { "—" },
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        ),
                                        color = CyberBlue.copy(alpha = 0.9f)
                                    )

                                    if (test.fromAccount) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = "From account",
                                            tint = CyberBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                val authorName = test.authorName?.trim().orEmpty()
                                if (test.fromAccount && authorName.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = authorName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = CyberYellow
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = CyberYellow.copy(alpha = 0.12f)
                        )

                        InfoRowCyber(stringResource(R.string.label_android_version), test.testedAndroidVersion)
                        InfoRowCyber(stringResource(R.string.label_device_model), test.testedDeviceModel)
                        InfoRowCyber(stringResource(R.string.label_gpu_model), test.testedGpuModel)
                        InfoRowCyber(stringResource(R.string.label_ram), test.testedRam)
                        InfoRowCyber(stringResource(R.string.label_wrapper), test.testedWrapper)
                        InfoRowCyber(stringResource(R.string.label_performance_mode), test.testedPerformanceMode)
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = CyberYellow.copy(alpha = 0.12f))
                Spacer(Modifier.height(14.dp))

                SectionTitleCyber(stringResource(R.string.test_history_section_app_game))
                InfoRowCyber(stringResource(R.string.test_history_label_app), test.testedApp)
                InfoRowCyber(stringResource(R.string.test_history_label_app_version), test.testedAppVersion)
                InfoRowCyber(stringResource(R.string.label_game_version_build), test.testedGameVersionOrBuild)

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = CyberYellow.copy(alpha = 0.12f))
                Spacer(Modifier.height(14.dp))

                if (test.status == WorkStatus.NOT_WORKING) {
                    SectionTitleCyber(stringResource(R.string.section_issue_details))
                    InfoRowCyber(
                        stringResource(R.string.label_issue_type),
                        stringResource(issueTypeToLabel(test.issueType))
                    )
                    InfoRowCyber(
                        stringResource(R.string.label_reproducibility),
                        stringResource(reproToLabel(test.reproducibility))
                    )
                    InfoRowCyber(stringResource(R.string.label_workaround), test.workaround)

                    if (test.issueNote.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            test.issueNote,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = CyberYellow.copy(alpha = 0.12f))
                    Spacer(Modifier.height(14.dp))
                }

                SectionTitleCyber(stringResource(R.string.section_emulator_settings))
                InfoRowCyber(
                    stringResource(R.string.label_emulator_build_type),
                    stringResource(emuBuildToLabel(test.emulatorBuildType))
                )
                InfoRowCyber(stringResource(R.string.label_accuracy_level), test.accuracyLevel)
                InfoRowCyber(stringResource(R.string.label_resolution_scale), test.resolutionScale)
                InfoRowCyber(
                    stringResource(R.string.label_async_shader),
                    if (test.asyncShaderEnabled) stringResource(R.string.value_on)
                    else stringResource(R.string.value_off)
                )
                InfoRowCyber(stringResource(R.string.label_frame_skip), test.frameSkip)

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = CyberYellow.copy(alpha = 0.12f))
                Spacer(Modifier.height(14.dp))

                SectionTitleCyber(stringResource(R.string.section_result_metrics))
                if (test.resolutionWidth.isNotBlank() && test.resolutionHeight.isNotBlank()) {
                    InfoRowCyber(
                        stringResource(R.string.test_history_label_resolution),
                        stringResource(
                            R.string.test_history_value_resolution,
                            test.resolutionWidth,
                            test.resolutionHeight
                        )
                    )
                }
                if (test.fpsMin.isNotBlank() && test.fpsMax.isNotBlank()) {
                    InfoRowCyber(
                        stringResource(R.string.test_history_label_fps),
                        stringResource(
                            R.string.test_history_value_fps,
                            test.fpsMin,
                            test.fpsMax
                        )
                    )
                }

                if (test.mediaLink.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    SectionTitleCyber(stringResource(R.string.label_media_link))

                    MediaBlockCyber(
                        url = test.mediaLink,
                        onOpenExternal = { safeClick { runCatching { uriHandler.openUri(test.mediaLink) } } }
                    )
                }

                Spacer(Modifier.height(24.dp))
                Spacer(
                    modifier = Modifier.windowInsetsBottomHeight(
                        WindowInsets.navigationBars
                    )
                )
            }
        }
    }
}


@Composable
private fun MediaBlockCyber(
    url: String,
    onOpenExternal: () -> Unit
) {
    val youtubeId = remember(url) { extractYouTubeId(url) }
    val isImage = remember(url) { isImageUrl(url) }
    val isDirectVideo = remember(url) { isDirectVideoUrl(url) }

    when {
        youtubeId != null -> {
            MediaPreviewCardCyber(
                thumbOverride = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg",
                label = stringResource(R.string.media_type_youtube),
                onOpenExternal = onOpenExternal
            )
        }
        isDirectVideo -> {
            InlineVideoPlayerCyber(url = url)
        }
        isImage -> {
            ImagePreviewCyber(url = url, onOpenExternal = onOpenExternal)
        }
        else -> {
            MediaPreviewCardCyber(
                thumbOverride = null,
                label = stringResource(R.string.media_type_video),
                onOpenExternal = onOpenExternal
            )
        }
    }
}

@Composable
private fun InlineVideoPlayerCyber(url: String) {
    val context = LocalContext.current

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    val shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp)

    Surface(
        shape = shape,
        color = CyberDark,
        border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.25f)),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = true
                }
            }
        )
    }
}

@Composable
private fun ImagePreviewCyber(url: String, onOpenExternal: () -> Unit) {
    val context = LocalContext.current
    val shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp)

    Surface(
        shape = shape,
        color = CyberDark,
        border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.35f)),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .clickable { onOpenExternal() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

@Composable
private fun MediaPreviewCardCyber(
    thumbOverride: String?,
    label: String,
    onOpenExternal: () -> Unit
) {
    val context = LocalContext.current
    val shape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp)

    Surface(
        shape = shape,
        color = CyberDark,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(CyberRed, CyberYellow, CyberBlue))
        ),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onOpenExternal() }
    ) {
        Box(Modifier.fillMaxSize()) {

            if (thumbOverride != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbOverride)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, CyberBlack.copy(alpha = 0.65f))
                            )
                        )
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(CyberGray)
                )
            }

            Icon(
                Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
            )

            Text(
                text = label.uppercase(),
                color = CyberYellow,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}


@Composable
private fun SectionTitleCyber(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        ),
        color = CyberYellow
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun InfoRowCyber(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            ),
            color = CyberBlue.copy(alpha = 0.9f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.92f)
        )
    }
}

@Composable
private fun CyberEmptyPanel(text: String) {
    Surface(
        shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
        color = CyberDark,
        border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.18f)),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = CyberYellow.copy(alpha = 0.9f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}


private fun isImageUrl(url: String): Boolean {
    val u = url.lowercase()
    return u.endsWith(".jpg") || u.endsWith(".jpeg") ||
            u.endsWith(".png") || u.endsWith(".webp") ||
            u.endsWith(".gif")
}

private fun isDirectVideoUrl(url: String): Boolean {
    val u = url.lowercase()
    return u.endsWith(".mp4") || u.endsWith(".webm") ||
            u.endsWith(".m3u8") || u.endsWith(".mov")
}

private fun extractYouTubeId(url: String): String? {
    val u = url.trim()
    return when {
        u.contains("youtu.be/") ->
            u.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
        u.contains("youtube.com/watch") && u.contains("v=") ->
            u.substringAfter("v=").substringBefore("&").substringBefore("?")
        u.contains("youtube.com/shorts/") ->
            u.substringAfter("shorts/").substringBefore("?").substringBefore("&")
        else -> null
    }?.takeIf { it.isNotBlank() }
}
