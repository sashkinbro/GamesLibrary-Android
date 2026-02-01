package com.sbro.gameslibrary.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.WorkStatus
import com.sbro.gameslibrary.components.WorkStatusBadge
import com.sbro.gameslibrary.util.extractYouTubeId
import com.sbro.gameslibrary.util.isDirectVideoUrl
import com.sbro.gameslibrary.util.isImageUrl
import com.sbro.gameslibrary.util.isValidHttpUrl
import com.sbro.gameslibrary.viewmodel.GameDetailViewModel

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestHistoryDetailScreen(
    viewModel: GameDetailViewModel,
    gameId: String,
    testMillis: Long,
    onBack: () -> Unit,
    onEditTest: (Long) -> Unit,
    onOpenVideo: (String) -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    val scroll = rememberScrollState()

    LaunchedEffect(gameId) {
        viewModel.init(context, gameId)
    }

    val game by viewModel.game.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val canonicalTestId = remember(gameId, testMillis) {
        "${gameId}_${testMillis}"
    }

    val test = remember(game, testMillis, canonicalTestId) {
        game?.testResults?.firstOrNull { it.updatedAtMillis == testMillis }
            ?: game?.testResults?.firstOrNull { it.testId == canonicalTestId }
    }

    val canEdit = remember(test, currentUser) {
        val userUid = currentUser?.uid
        userUid != null &&
                test != null &&
                test.fromAccount &&
                test.authorUid == userUid
    }

    Scaffold(contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(modifier = Modifier.statusBarsPadding(),
                    title = { Text(stringResource(R.string.test_history_detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        if (canEdit) {
                            IconButton(onClick = { onEditTest(testMillis) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit test",
                                    tint = cs.primary
                                )
                            }
                        }
                    }
                )
                HorizontalDivider(color = cs.outline.copy(alpha = 0.4f))
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
                    CircularProgressIndicator()
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
                    Text(stringResource(R.string.error_test_not_found))
                }
                return@Scaffold
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = pv.calculateTopPadding())
                .verticalScroll(scroll)
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
                    InfoRow(stringResource(R.string.label_driver_version), test.testedDriverVersion)
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
            InfoRow(stringResource(R.string.label_download_size), test.downloadSize)
            InfoRow(stringResource(R.string.label_controller_support), test.controllerSupport)

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

            val hasPcFields = test.winlatorFork.isNotBlank() || test.wineVersion.isNotBlank() ||
                    test.box64Preset.isNotBlank() || test.vkd3dVersion.isNotBlank()

            if (hasPcFields) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.section_pc_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.primary
                )
                Spacer(Modifier.height(4.dp))
                InfoRow(stringResource(R.string.label_winlator_fork), test.winlatorFork)
                InfoRow(stringResource(R.string.label_wine_version), test.wineVersion)
                InfoRow(stringResource(R.string.label_box64_preset), test.box64Preset)
                InfoRow(stringResource(R.string.label_box64_version), test.box64Version)
                InfoRow(stringResource(R.string.label_turnip_version), test.turnipVersion)
                InfoRow(stringResource(R.string.label_audio_driver), test.audioDriver)
                InfoRow(stringResource(R.string.label_dxvk_version), test.dxvkVersion)
                InfoRow(stringResource(R.string.label_vkd3d_version), test.vkd3dVersion)
                InfoRow(stringResource(R.string.label_startup_selection), test.startupSelection)

                if (test.envVariables.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.label_env_variables), style = MaterialTheme.typography.bodySmall, color = cs.onSurface.copy(alpha = 0.7f))
                    Text(test.envVariables, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
            val hasSwitchFields = test.dockedMode || test.audioOutputEngine.isNotBlank() ||
                    test.cpuBackend.isNotBlank() || test.diskShaderCache

            if (hasSwitchFields) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.section_switch_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.primary
                )
                Spacer(Modifier.height(4.dp))
                InfoRow(stringResource(R.string.label_docked_mode), if(test.dockedMode) stringResource(R.string.value_on) else stringResource(R.string.value_off))
                InfoRow(stringResource(R.string.label_disk_shader_cache), if(test.diskShaderCache) stringResource(R.string.value_on) else stringResource(R.string.value_off))
                InfoRow(stringResource(R.string.label_reactive_flushing), if(test.reactiveFlushing) stringResource(R.string.value_on) else stringResource(R.string.value_off))
                InfoRow(stringResource(R.string.label_cpu_backend), test.cpuBackend)
                InfoRow(stringResource(R.string.label_audio_output_engine), test.audioOutputEngine)
            }
            if (test.spuThreads.isNotBlank() || test.spuBlockSize.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.section_ps3_settings),
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.primary
                )
                Spacer(Modifier.height(4.dp))
                InfoRow(stringResource(R.string.label_spu_threads), test.spuThreads)
                InfoRow(stringResource(R.string.label_spu_block_size), test.spuBlockSize)
            }

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
            val hasAdvancedGraphics = test.vSync.isNotBlank() || test.anisotropicFilter.isNotBlank() ||
                    test.antiAliasing.isNotBlank() || test.windowAdaptingFilter.isNotBlank()

            if (hasAdvancedGraphics) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.section_graphics_advanced),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                InfoRow(stringResource(R.string.label_vsync), test.vSync)
                InfoRow(stringResource(R.string.label_anisotropic_filtering), test.anisotropicFilter)
                InfoRow(stringResource(R.string.label_anti_aliasing), test.antiAliasing)
                InfoRow(stringResource(R.string.label_window_adapting_filter), test.windowAdaptingFilter)
            }

            if (isValidHttpUrl(test.mediaLink)) {
                Spacer(Modifier.height(14.dp))
                SectionTitle(stringResource(R.string.label_media_link))

                MediaBlock(
                    url = test.mediaLink,
                    onOpenVideo = onOpenVideo,
                    onOpenExternal = { runCatching { uriHandler.openUri(test.mediaLink) } }
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

@Composable
private fun MediaBlock(
    url: String,
    onOpenVideo: (String) -> Unit,
    onOpenExternal: () -> Unit
) {
    val youtubeId = remember(url) { extractYouTubeId(url) }
    val isImage = remember(url) { isImageUrl(url) }
    val isDirectVideo = remember(url) { isDirectVideoUrl(url) }

    when {
        youtubeId != null -> {
            MediaPreviewCard(
                thumbOverride = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg",
                label = stringResource(R.string.media_type_youtube),
                onOpen = { onOpenVideo(url) }
            )
        }
        isDirectVideo -> {
            MediaPreviewCard(
                thumbOverride = null,
                label = stringResource(R.string.media_type_video),
                onOpen = { onOpenVideo(url) }
            )
        }
        isImage -> {
            ImagePreview(url = url, onOpenExternal = onOpenExternal)
        }
    }
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
    thumbOverride: String?,
    label: String,
    onOpen: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cs.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clickable { onOpen() }
    ) {
        Box(Modifier.fillMaxSize()) {

            if (thumbOverride != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbOverride)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
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
