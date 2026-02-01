package com.sbro.gameslibrary.cyberpunk.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.GameTestResult
import com.sbro.gameslibrary.cyberpunk.components.WorkStatusBadge
import com.sbro.gameslibrary.util.extractYouTubeId
import com.sbro.gameslibrary.util.isDirectVideoUrl
import com.sbro.gameslibrary.util.isValidHttpUrl
import com.sbro.gameslibrary.viewmodel.GameDetailViewModel
import com.sbro.gameslibrary.viewmodel.TestComment
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)
private val CyberGray = Color(0xFF202020)

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameDetailScreen(
    viewModel: GameDetailViewModel,
    gameId: String,
    onBack: () -> Unit,
    onOpenEditStatus: (String) -> Unit,
    onOpenTestHistory: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    testSaved: Boolean,
    onTestSavedConsumed: () -> Unit
) {
    val context = LocalContext.current

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
    val gameComments by viewModel.gameComments.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val hasMoreGameComments by viewModel.hasMoreGameComments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMoreComments by viewModel.isLoadingMoreComments.collectAsState()

    var expandedDesc by remember { mutableStateOf(false) }
    var commentInput by rememberSaveable(gameId) { mutableStateOf("") }
    var editingComment by remember { mutableStateOf<TestComment?>(null) }
    var editText by remember { mutableStateOf("") }

    if (isLoading && game == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBlack),
            contentAlignment = Alignment.Center
        ) {
            CyberGridBackground()
            ScanlinesEffect()
            VignetteEffect()
            CircularProgressIndicator(color = CyberYellow)
        }
        return
    }

    if (game == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBlack),
            contentAlignment = Alignment.Center
        ) {
            CyberGridBackground()
            ScanlinesEffect()
            VignetteEffect()

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "GAME NOT FOUND",
                    color = CyberYellow,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberDark,
                        contentColor = CyberYellow
                    ),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(CyberRed, CyberYellow))
                    )
                ) {
                    Text("BACK", fontFamily = FontFamily.Monospace)
                }
            }
        }
        return
    }

    val g = game!!
    val latestTest = g.latestTestOrNull()
    val latestStatus = g.overallStatus()
    val descText = g.description.ifBlank { stringResource(R.string.no_description) }

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

    val gameCommentsSorted = remember(gameComments, gameId) {
        gameComments
            .filter { it.gameId == gameId }
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
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
            containerColor = Color.Transparent,
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
                    var headerAspectRatio by rememberSaveable(gameId) { mutableStateOf(3f / 4f) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(g.imageUrl.replace("t_cover_big_2x", "t_1080p"))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            onSuccess = { result ->
                                val width = result.result.drawable.intrinsicWidth
                                val height = result.result.drawable.intrinsicHeight
                                if (width > 0 && height > 0) {
                                    headerAspectRatio = width.toFloat() / height.toFloat()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(headerAspectRatio)
                                .background(CyberDark)
                                .animateContentSize()
                        )
                    }

                    Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        GlitchText(
                            text = g.title.uppercase(),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (g.platform.isNotBlank()) {
                                CyberChip(
                                    icon = Icons.Filled.Tv,
                                    text = g.platform.uppercase(),
                                    tint = CyberYellow
                                )
                            }
                            if (g.year.isNotBlank()) {
                                CyberChip(
                                    icon = Icons.Filled.CalendarToday,
                                    text = g.year,
                                    tint = CyberYellow
                                )
                            }

                            if (g.rating.isNotBlank()) {
                                CyberChip(
                                    icon = Icons.Filled.Star,
                                    text = g.rating,
                                    tint = CyberYellow,
                                    highlight = true
                                )
                            }

                            if (g.genre.isNotBlank()) {
                                CyberChip(
                                    icon = Icons.Filled.Category,
                                    text = g.genre,
                                    tint = CyberBlue
                                )
                            }

                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        DataSourceBadge(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )

                        CyberCutButton(
                            text = stringResource(R.string.button_edit_status),
                            icon = Icons.Filled.Edit,
                            accent = CyberYellow,
                            onClick = { onOpenEditStatus(g.id) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        CyberCutButton(
                            text = stringResource(R.string.button_tested_history),
                            icon = Icons.Filled.History,
                            accent = CyberBlue,
                            enabled = g.testResults.isNotEmpty(),
                            onClick = { onOpenTestHistory(g.id) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.details_section_latest).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberYellow,
                            fontFamily = FontFamily.Monospace
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
                                shape = CutCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 18.dp,
                                    bottomEnd = 0.dp,
                                    bottomStart = 18.dp
                                ),
                                color = CyberDark.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f)),
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
                                                            color = CyberYellow.copy(alpha = 0.7f),
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }

                                                if (latestTest?.fromAccount == true) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Icon(
                                                        Icons.Filled.CheckCircle,
                                                        contentDescription = "From account",
                                                        tint = CyberBlue,
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
                                                    color = CyberYellow,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }

                                    if (latestTest != null) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = CyberYellow.copy(alpha = 0.12f)
                                        )
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
                                            InfoRowModern(Icons.Filled.Memory, "Box64: ${latestTest.box64Preset}")
                                        }
                                        if (latestTest.audioDriver.isNotBlank()) {
                                            InfoRowModern(Icons.Filled.Audiotrack, "Audio: ${latestTest.audioDriver}")
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
                                            InfoRowModern(Icons.Filled.Memory, "CPU: ${latestTest.cpuBackend}")
                                        }
                                        if (latestTest.spuThreads.isNotBlank()) {
                                            InfoRowModern(Icons.Filled.DeveloperBoard, "SPU Threads: ${latestTest.spuThreads}")
                                        }
                                        if (latestTest.spuBlockSize.isNotBlank()) {
                                            InfoRowModern(Icons.Filled.DeveloperBoard, "SPU Block: ${latestTest.spuBlockSize}")
                                        }

                                        if (latestTest.anisotropicFilter.isNotBlank()) {
                                            InfoRowModern(Icons.Filled.Settings, "Aniso: ${latestTest.anisotropicFilter}")
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
                                            color = CyberYellow.copy(alpha = 0.6f),
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.details_section_overview).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberYellow,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = descText,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 24.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = FontFamily.Monospace,
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
                                    stringResource(R.string.details_show_less).uppercase()
                                else
                                    stringResource(R.string.details_show_more).uppercase(),
                                color = CyberBlue,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { expandedDesc = !expandedDesc }
                            )
                        }

                        val videoTests = remember(g.testResults) {
                            g.testResults.filter {
                                isValidHttpUrl(it.mediaLink) &&
                                    (extractYouTubeId(it.mediaLink) != null || isDirectVideoUrl(it.mediaLink))
                            }
                        }

                        if (videoTests.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = stringResource(R.string.details_section_videos).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyberYellow,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                videoTests.forEach { test ->
                                    TestVideoCardCyber(
                                        test = test,
                                        onOpenVideo = onOpenVideo
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(
                                R.string.comments_section_title,
                                gameCommentsSorted.size
                            ).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberYellow,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (gameCommentsSorted.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CyberDark,
                                border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.comments_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyberYellow.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                gameCommentsSorted.forEach { comment ->
                                    CommentCardCyber(
                                        comment = comment,
                                        isOwn = comment.authorUid != null &&
                                                comment.authorUid == currentUser?.uid,
                                        onEdit = {
                                            editingComment = comment
                                            editText = comment.text
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            if (hasMoreGameComments) {
                                OutlinedButton(
                                    onClick = { viewModel.loadMoreGameComments(g.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !isLoadingMoreComments,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = CyberYellow,
                                        containerColor = Color.Transparent
                                    ),
                                    border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.35f))
                                ) {
                                    if (isLoadingMoreComments) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = CyberYellow
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        stringResource(R.string.comments_load_more).uppercase(),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                    if (currentUser == null) {
                        Surface(
                            shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
                            color = CyberDark,
                            border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.login_to_comment).uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyberYellow.copy(alpha = 0.85f),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = commentInput,
                                    onValueChange = { commentInput = it },
                                    placeholder = {
                                        Text(
                                            stringResource(R.string.test_comment_hint).uppercase(),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = false,
                                    maxLines = 4,
                                    shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberYellow,
                                        unfocusedBorderColor = CyberYellow.copy(alpha = 0.35f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = CyberYellow
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        val text = commentInput.trim()
                                        if (text.isNotBlank()) {
                                            viewModel.addGameComment(context, g.id, text)
                                            commentInput = ""
                                        }
                                    },
                                    enabled = commentInput.trim().isNotBlank()
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(R.string.cd_send_comment),
                                        tint = if (commentInput.trim().isNotBlank()) CyberYellow else CyberGray
                                    )
                                }
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
                            .clip(CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                            .background(CyberDark.copy(alpha = 0.6f))
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = CyberRed
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CyberBlack.copy(alpha = 0.6f))
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (g.isFavorite)
                                Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (g.isFavorite) CyberRed else CyberYellow
                        )
                    }
                }
            }
        }
    }

    if (editingComment != null) {
        AlertDialog(
            onDismissRequest = { editingComment = null },
            containerColor = CyberDark,
            tonalElevation = 0.dp,
            shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
            title = {
                Text(
                    stringResource(R.string.edit_comment_title).uppercase(),
                    color = CyberYellow,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberYellow,
                        unfocusedBorderColor = CyberYellow.copy(alpha = 0.35f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CyberYellow
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val c = editingComment!!
                        viewModel.editGameComment(context, c.id, editText)
                        editingComment = null
                    }
                ) {
                    Text(
                        stringResource(R.string.button_save).uppercase(),
                        color = CyberYellow,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { editingComment = null }) {
                    Text(
                        stringResource(R.string.button_cancel).uppercase(),
                        color = Color.White.copy(alpha = 0.85f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        )
    }
}

@Composable
private fun CyberChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color,
    highlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CyberGray,
        border = BorderStroke(
            1.dp,
            (if (highlight) tint else CyberYellow).copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = tint)
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium
            )
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
            tint = CyberBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TestVideoCardCyber(
    test: GameTestResult,
    onOpenVideo: (String) -> Unit
) {
    val context = LocalContext.current
    val thumbUrl = remember(test.mediaLink) {
        extractYouTubeId(test.mediaLink)?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
    }

    Surface(
        shape = CutCornerShape(
            topStart = 0.dp,
            topEnd = 18.dp,
            bottomEnd = 0.dp,
            bottomStart = 18.dp
        ),
        color = CyberDark,
        border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.15f)),
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
                    tint = CyberBlue,
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
                    color = CyberYellow.copy(alpha = 0.9f),
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberGray)
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
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CyberBlack)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            CyberBlack.copy(alpha = if (thumbUrl == null) 0.5f else 0.22f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.PlayCircle,
                            contentDescription = null,
                            tint = CyberYellow,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.open_video).uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberYellow,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCardCyber(
    comment: TestComment,
    isOwn: Boolean,
    onEdit: () -> Unit
) {
    val context = LocalContext.current

    val (dateLine, timeLine) = remember(comment.createdAt) {
        val millis = comment.createdAt?.toDate()?.time ?: 0L
        if (millis == 0L) {
            Pair("", "")
        } else {
            val locale = Locale.getDefault()
            val date = SimpleDateFormat("d MMM yyyy", locale).format(Date(millis))
            val time = SimpleDateFormat("HH:mm", locale).format(Date(millis))
            Pair(date, time)
        }
    }

    val photoUrl = comment.authorPhotoUrl

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CyberDark,
        border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.12f)),
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
                        .background(CyberGray)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = CyberYellow.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                val author = comment.authorName?.takeIf { it.isNotBlank() }
                    ?: comment.authorDevice.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.comments_unknown_author)

                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = author,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = CyberYellow,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (dateLine.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.align(Alignment.Bottom)
                        ) {
                            Text(
                                text = timeLine,
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberYellow.copy(alpha = 0.8f),
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dateLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberYellow.copy(alpha = 0.65f),
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isOwn) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = CyberYellow
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun CyberCutButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    accent: Color = CyberYellow
) {
    val shape = CutCornerShape(
        topStart = 0.dp,
        topEnd = 14.dp,
        bottomEnd = 0.dp,
        bottomStart = 14.dp
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = CyberDark,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    CyberRed.copy(alpha = if (enabled) 0.9f else 0.3f),
                    accent.copy(alpha = if (enabled) 0.9f else 0.3f)
                )
            )
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            CyberGray.copy(alpha = if (enabled) 0.35f else 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accent.copy(alpha = if (enabled) 1f else 0.45f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = text.uppercase(),
                    color = accent.copy(alpha = if (enabled) 1f else 0.45f),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(width = 24.dp, height = 2.dp)
                    .background(accent.copy(alpha = if (enabled) 0.7f else 0.25f))
            )
        }
    }
}

@Composable
private fun DataSourceBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CutCornerShape(
            topStart = 0.dp,
            topEnd = 10.dp,
            bottomEnd = 0.dp,
            bottomStart = 10.dp
        ),
        color = CyberDark.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Public,
                contentDescription = null,
                tint = CyberBlue.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.details_data_source_igdb),
                color = CyberYellow.copy(alpha = 0.9f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
