package com.sbro.gameslibrary.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.GameTestResult
import com.sbro.gameslibrary.components.WorkStatusBadge
import com.sbro.gameslibrary.viewmodel.GameDetailViewModel
import com.sbro.gameslibrary.viewmodel.TestComment
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameDetailScreen(
    viewModel: GameDetailViewModel,
    gameId: String,
    onBack: () -> Unit,
    onOpenEditStatus: (String) -> Unit,
    onOpenTestHistory: (String) -> Unit,
    testSaved: Boolean,
    onTestSavedConsumed: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(gameId) {
        viewModel.init(context, gameId)
    }
    LaunchedEffect(testSaved) {
        if (testSaved) {
            viewModel.refresh(context, gameId)
            onTestSavedConsumed()
        }
    }

    val game by viewModel.game.collectAsState()
    val commentsByTest by viewModel.commentsByTest.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMoreComments by viewModel.isLoadingMoreComments.collectAsState()

    var expandedDesc by remember { mutableStateOf(false) }

    if (isLoading && game == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (game == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Game not found", color = Color.Gray)
            Button(onClick = onBack, modifier = Modifier.padding(top = 40.dp)) {
                Text("Back")
            }
        }
        return
    }

    val g = game!!
    val latestTest = g.latestTestOrNull()

    var latestVisible by rememberSaveable(gameId) { mutableStateOf(false) }
    var lastAnimatedTestId by rememberSaveable(gameId) { mutableStateOf<String?>(null) }

    LaunchedEffect(latestTest?.testId) {
        val newId = latestTest?.testId

        if (newId == null) {
            latestVisible = false
            lastAnimatedTestId = null
            return@LaunchedEffect
        }

        if (newId != lastAnimatedTestId) {
            latestVisible = false
            delay(80)
            latestVisible = true
            lastAnimatedTestId = newId
        } else {
            latestVisible = true
        }
    }
    val latestStatus = g.overallStatus()
    val descText = g.description.ifBlank { stringResource(R.string.no_description) }

    val allCommentsForGame = remember(commentsByTest, gameId) {
        commentsByTest.values
            .flatten()
            .filter { it.gameId == gameId }
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
    }

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets(0)
    ) { paddingVals ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingVals.calculateBottomPadding())
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .height(500.dp)
                        .fillMaxWidth()
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(g.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(radius = 40.dp)
                            .background(cs.background.copy(alpha = 0.5f))
                    )

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(g.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to cs.background,
                                        0.15f to cs.background.copy(alpha = 0.8f),
                                        0.3f to Color.Transparent,
                                        0.7f to Color.Transparent,
                                        0.9f to cs.background.copy(alpha = 0.9f),
                                        1.0f to cs.background
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0.0f to cs.background,
                                        0.2f to Color.Transparent,
                                        0.8f to Color.Transparent,
                                        1.0f to cs.background
                                    )
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .offset(y = (-40).dp)
                ) {
                    if (g.platform.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = cs.primary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = g.platform,
                                color = cs.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = g.title,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (g.year.isNotBlank()) {

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = cs.surfaceVariant.copy(alpha = 0.7f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.CalendarToday,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = cs.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        g.year,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = cs.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (g.genre.isNotBlank()) {


                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = cs.surfaceVariant.copy(alpha = 0.7f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Category,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = cs.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        g.genre,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = cs.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (g.rating.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2E2512),
                                border = BorderStroke(
                                    1.dp,
                                    Color(0xFFE6B800).copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Star,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFFFFD700)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        g.rating,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFFFE082),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DataSourceBadgeMaterial(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )

                    Button(
                        onClick = { onOpenEditStatus(g.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                    ) {
                        Icon(Icons.Filled.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.button_edit_status))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = { onOpenTestHistory(g.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = g.testResults.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.History, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.button_tested_history))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.details_section_latest),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedVisibility(
                        visible = latestVisible,
                        enter = fadeIn(tween(250)) +
                                slideInVertically(
                                    initialOffsetY = { it / 4 },
                                    animationSpec = tween(300)
                                ) +
                                expandVertically(
                                    expandFrom = Alignment.Top,
                                    animationSpec = tween(300)
                                ),
                        exit = fadeOut(tween(150)) +
                                shrinkVertically(tween(200))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = cs.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    WorkStatusBadge(status = latestStatus)

                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            latestTest?.testedDateFormatted
                                                ?.takeIf { it.isNotBlank() }
                                                ?.let { date ->
                                                    Text(
                                                        text = date,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = cs.onSurfaceVariant
                                                    )
                                                }

                                            if (latestTest?.fromAccount == true) {
                                                Spacer(Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = "From account",
                                                    tint = cs.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        val authorName = latestTest?.authorName?.trim().orEmpty()
                                        if (latestTest?.fromAccount == true && authorName.isNotBlank()) {
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

                                if (latestTest != null) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = cs.onSurface.copy(alpha = 0.1f)
                                    )

                                    // Base Info
                                    InfoRowModern(Icons.Filled.Smartphone, latestTest.testedDeviceModel)
                                    InfoRowModern(
                                        Icons.Filled.Android,
                                        if (latestTest.testedAndroidVersion.isNotBlank())
                                            "Android ${latestTest.testedAndroidVersion}"
                                        else ""
                                    )
                                    InfoRowModern(Icons.Filled.Memory, latestTest.testedGpuModel)
                                    InfoRowModern(Icons.Filled.Storage, latestTest.testedRam)

                                    Spacer(Modifier.height(4.dp))
                                    InfoRowModern(Icons.Filled.Settings, latestTest.testedWrapper)
                                    InfoRowModern(Icons.Filled.Speed, latestTest.testedPerformanceMode)
                                    if (latestTest.winlatorFork.isNotBlank()) {
                                        InfoRowModern(Icons.Filled.Build, "Fork: ${latestTest.winlatorFork}")
                                    }
                                    if (latestTest.wineVersion.isNotBlank()) {
                                        InfoRowModern(Icons.Filled.Code, "Wine: ${latestTest.wineVersion}")
                                    }
                                    if (latestTest.box64Preset.isNotBlank()) {
                                        InfoRowModern(Icons.Filled.Memory, "Box64 Preset: ${latestTest.box64Preset}")
                                    }
                                    if (latestTest.audioDriver.isNotBlank()) {
                                        InfoRowModern(Icons.Filled.Audiotrack, "Audio: ${latestTest.audioDriver}")
                                    }
                                    if (latestTest.turnipVersion.isNotBlank()) {
                                        InfoRowModern(Icons.Filled.Settings, "Turnip: ${latestTest.turnipVersion}")
                                    }
                                    if (latestTest.downloadSize.isNotBlank()) {
                                        InfoRowModern(Icons.Filled.Download, "Size: ${latestTest.downloadSize}")
                                    }
                                    if (latestTest.dockedMode) {
                                        InfoRowModern(Icons.Filled.Tv, "Docked Mode")
                                    }
                                    if (latestTest.audioOutputEngine.isNotBlank()) {
                                        InfoRowModern(Icons.Filled.Audiotrack, "Audio: ${latestTest.audioOutputEngine}")
                                    }
                                    if (latestTest.cpuBackend.isNotBlank()) {
                                        InfoRowModern(Icons.Filled.Memory, "CPU Backend: ${latestTest.cpuBackend}")
                                    }
                                    if (latestTest.diskShaderCache) {
                                        InfoRowModern(Icons.Filled.Storage, "Disk Shader Cache")
                                    }
                                    if (latestTest.spuThreads.isNotBlank()) {
                                        InfoRowModern(Icons.Filled.DeveloperBoard, "SPU Threads: ${latestTest.spuThreads}")
                                    }
                                    if (latestTest.spuBlockSize.isNotBlank()) {
                                        InfoRowModern(Icons.Filled.DeveloperBoard, "SPU Block: ${latestTest.spuBlockSize}")
                                    }
                                    if (latestTest.vSync.isNotBlank()) {
                                        InfoRowModern(Icons.Filled.Settings, "VSync: ${latestTest.vSync}")
                                    }
                                    if (latestTest.anisotropicFilter.isNotBlank()) {
                                        InfoRowModern(Icons.Filled.Settings, "Anisotropic: ${latestTest.anisotropicFilter}")
                                    }

                                    Spacer(Modifier.height(4.dp))
                                    if (
                                        latestTest.resolutionWidth.isNotBlank() &&
                                        latestTest.resolutionHeight.isNotBlank()
                                    ) {
                                        InfoRowModern(
                                            Icons.Filled.AspectRatio,
                                            "${latestTest.resolutionWidth}×${latestTest.resolutionHeight}"
                                        )
                                    }
                                    if (
                                        latestTest.fpsMin.isNotBlank() &&
                                        latestTest.fpsMax.isNotBlank()
                                    ) {
                                        InfoRowModern(
                                            Icons.Filled.MonitorHeart,
                                            "FPS: ${latestTest.fpsMin} – ${latestTest.fpsMax}"
                                        )
                                    }
                                } else {
                                    Text(
                                        text = stringResource(R.string.details_no_tests),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = cs.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }

                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.details_section_overview),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground
                    )

                    Text(
                        text = descText,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp,
                        color = cs.onBackground.copy(alpha = 0.8f),
                        maxLines = if (expandedDesc) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .animateContentSize()
                            .clickable { expandedDesc = !expandedDesc }
                    )

                    if (descText.length > 150) {
                        Text(
                            text = if (expandedDesc)
                                stringResource(R.string.details_show_less)
                            else
                                stringResource(R.string.details_show_more),
                            color = cs.primary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { expandedDesc = !expandedDesc }
                        )
                    }

                    val videoTests = remember(g.testResults) {
                        g.testResults.filter { it.mediaLink.isNotBlank() }
                    }

                    if (videoTests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = stringResource(R.string.details_section_videos),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cs.onBackground
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            videoTests.forEach { test ->
                                TestVideoCard(
                                    test = test,
                                    onOpenVideo = { link ->
                                        runCatching { uriHandler.openUri(link) }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(
                            R.string.comments_section_title,
                            allCommentsForGame.size
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (allCommentsForGame.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = cs.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.comments_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allCommentsForGame.forEach { comment ->
                                CommentCard(comment)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadMoreCommentsForGame(g.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isLoadingMoreComments
                        ) {
                            if (isLoadingMoreComments) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(stringResource(R.string.comments_load_more))
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Spacer(
                        modifier = Modifier.windowInsetsBottomHeight(
                            WindowInsets.navigationBars
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .size(44.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleFavorite() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = if (g.isFavorite)
                            Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (g.isFavorite) Color(0xFFFF4081) else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRowModern(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    if (text.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun TestVideoCard(
    test: GameTestResult,
    onOpenVideo: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    val thumbUrl = remember(test.mediaLink) {
        youtubeThumbnailUrl(test.mediaLink)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cs.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))

                val header = buildString {
                    if (test.testedDateFormatted.isNotBlank()) append(test.testedDateFormatted)
                    if (test.testedDeviceModel.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(test.testedDeviceModel)
                    }
                    if (test.testedAndroidVersion.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append("Android ${test.testedAndroidVersion}")
                    }
                }.ifBlank { stringResource(R.string.test_history_detail_title) }

                Text(
                    text = header,
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cs.surface)
                    .clickable { onOpenVideo(test.mediaLink) },
                contentAlignment = Alignment.Center
            ) {
                if (thumbUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = if (thumbUrl == null) 0.35f else 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.open_video),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCard(comment: TestComment) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    val dateStr = remember(comment.createdAt) {
        val millis = comment.createdAt?.toDate()?.time ?: 0L
        if (millis == 0L) "" else {
            val fmt = SimpleDateFormat("d MMM yyyy • HH:mm", Locale.getDefault())
            fmt.format(Date(millis))
        }
    }

    val photoUrl = comment.authorPhotoUrl

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cs.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {

            if (comment.fromAccount && !photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(cs.surface)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(cs.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = cs.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val author = comment.authorName?.takeIf { it.isNotBlank() }
                        ?: comment.authorDevice.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.comments_unknown_author)

                    Text(
                        text = author,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )

                    if (dateStr.isNotBlank()) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface.copy(alpha = 0.9f)
                )
            }
        }
    }
}

private fun youtubeThumbnailUrl(url: String): String? {
    val u = url.trim()
    if (u.isBlank()) return null
    val shortMatch = Regex("youtu\\.be/([A-Za-z0-9_-]{6,})").find(u)
    val shortId = shortMatch?.groupValues?.getOrNull(1)
    val longMatch = Regex("[?&]v=([A-Za-z0-9_-]{6,})").find(u)
    val longId = longMatch?.groupValues?.getOrNull(1)
    val id = shortId ?: longId ?: return null
    return "https://img.youtube.com/vi/$id/hqdefault.jpg"
}

@Composable
private fun DataSourceBadgeMaterial(
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cs.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.10f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Android,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.details_data_source_igdb),
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}