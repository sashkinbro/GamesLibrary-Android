package com.sbro.gameslibrary.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.WorkStatus
import com.sbro.gameslibrary.components.WorkStatusBadge
import com.sbro.gameslibrary.viewmodel.GameViewModel

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestHistoryDetailScreen(
    viewModel: GameViewModel,
    gameId: String,
    testMillis: Long,
    onBack: () -> Unit,
    onEditGame: (String) -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) { viewModel.init(context) }

    val games by viewModel.games.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val game = remember(games, gameId) { games.firstOrNull { it.id == gameId } }
    val test = remember(game, testMillis) {
        game?.testResults?.firstOrNull { it.updatedAtMillis == testMillis }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.test_history_detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                )
                HorizontalDivider(color = cs.outline.copy(alpha = 0.4f))
            }
        }
    ) { pv ->
        if (game == null || test == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pv),
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.error_test_not_found)) }
            return@Scaffold
        }

        val userUid = currentUser?.uid
        val canEditTest =
            userUid != null &&
                    test.fromAccount &&
                    test.authorUid == userUid

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cs.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        WorkStatusBadge(status = test.status)

                        Column(horizontalAlignment = Alignment.End) {

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = test.testedDateFormatted.ifBlank { "—" },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = cs.onSurfaceVariant
                                )

                                if (test.fromAccount) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "From account",
                                        tint = cs.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                if (canEditTest) {
                                    Spacer(Modifier.width(10.dp))
                                    IconButton(onClick = { onEditGame(gameId) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.edit),
                                            tint = cs.primary
                                        )
                                    }
                                }
                            }
                            val authorName = test.authorName?.trim().orEmpty()
                            if (test.fromAccount && authorName.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = authorName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.onSurface.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = cs.onSurface.copy(alpha = 0.1f)
                    )

                    InfoRow(stringResource(R.string.label_android_version), test.testedAndroidVersion)
                    InfoRow(stringResource(R.string.label_device_model), test.testedDeviceModel)
                    InfoRow(stringResource(R.string.label_gpu_model), test.testedGpuModel)
                    InfoRow(stringResource(R.string.label_ram), test.testedRam)
                    InfoRow(stringResource(R.string.label_wrapper), test.testedWrapper)
                    InfoRow(stringResource(R.string.label_performance_mode), test.testedPerformanceMode)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = cs.outline.copy(alpha = 0.25f))
            Spacer(Modifier.height(14.dp))

            SectionTitle(stringResource(R.string.test_history_section_app_game))
            InfoRow(stringResource(R.string.test_history_label_app), test.testedApp)
            InfoRow(stringResource(R.string.test_history_label_app_version), test.testedAppVersion)
            InfoRow(stringResource(R.string.label_game_version_build), test.testedGameVersionOrBuild)

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = cs.outline.copy(alpha = 0.25f))
            Spacer(Modifier.height(14.dp))

            if (test.status == WorkStatus.NOT_WORKING) {
                SectionTitle(stringResource(R.string.section_issue_details))
                InfoRow(
                    stringResource(R.string.label_issue_type),
                    stringResource(issueTypeToLabel(test.issueType))
                )
                InfoRow(
                    stringResource(R.string.label_reproducibility),
                    stringResource(reproToLabel(test.reproducibility))
                )
                InfoRow(stringResource(R.string.label_workaround), test.workaround)

                if (test.issueNote.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(test.issueNote, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = cs.outline.copy(alpha = 0.25f))
                Spacer(Modifier.height(14.dp))
            }

            SectionTitle(stringResource(R.string.section_emulator_settings))
            InfoRow(
                stringResource(R.string.label_emulator_build_type),
                stringResource(emuBuildToLabel(test.emulatorBuildType))
            )
            InfoRow(stringResource(R.string.label_accuracy_level), test.accuracyLevel)
            InfoRow(stringResource(R.string.label_resolution_scale), test.resolutionScale)
            InfoRow(
                stringResource(R.string.label_async_shader),
                if (test.asyncShaderEnabled) stringResource(R.string.value_on)
                else stringResource(R.string.value_off)
            )
            InfoRow(stringResource(R.string.label_frame_skip), test.frameSkip)

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = cs.outline.copy(alpha = 0.25f))
            Spacer(Modifier.height(14.dp))

            SectionTitle(stringResource(R.string.section_result_metrics))
            if (test.resolutionWidth.isNotBlank() && test.resolutionHeight.isNotBlank()) {
                InfoRow(
                    stringResource(R.string.test_history_label_resolution),
                    stringResource(
                        R.string.test_history_value_resolution,
                        test.resolutionWidth,
                        test.resolutionHeight
                    )
                )
            }
            if (test.fpsMin.isNotBlank() && test.fpsMax.isNotBlank()) {
                InfoRow(
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
                SectionTitle(stringResource(R.string.label_media_link))

                MediaBlock(
                    url = test.mediaLink,
                    onOpenExternal = { runCatching { uriHandler.openUri(test.mediaLink) } }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MediaBlock(
    url: String,
    onOpenExternal: () -> Unit
) {
    val youtubeId = remember(url) { extractYouTubeId(url) }
    val isImage = remember(url) { isImageUrl(url) }
    val isDirectVideo = remember(url) { isDirectVideoUrl(url) }

    when {
        youtubeId != null -> {
            MediaPreviewCard(
                url = url,
                thumbOverride = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg",
                label = stringResource(R.string.media_type_youtube),
                onOpenExternal = onOpenExternal
            )
        }
        isDirectVideo -> {
            InlineVideoPlayer(url = url)
        }
        isImage -> {
            ImagePreview(url = url, onOpenExternal = onOpenExternal)
        }
        else -> {
            MediaPreviewCard(
                url = url,
                thumbOverride = null,
                label = stringResource(R.string.media_type_video),
                onOpenExternal = onOpenExternal
            )
        }
    }
}

@Composable
private fun InlineVideoPlayer(url: String) {
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

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp)),
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = true
            }
        }
    )
}

@Composable
private fun ImagePreview(url: String, onOpenExternal: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceVariant.copy(alpha = 0.55f),
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
private fun MediaPreviewCard(
    url: String,
    thumbOverride: String?,
    label: String,
    onOpenExternal: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceVariant.copy(alpha = 0.55f),
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
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                            )
                        )
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(cs.surfaceVariant)
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
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
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

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
