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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
    testSaved: Boolean,
    onTestSavedConsumed: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

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

    val allCommentsForGame = remember(commentsByTest, gameId) {
        commentsByTest.values
            .flatten()
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
                                .background(CyberBlack.copy(alpha = 0.6f))
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
                                            0.0f to CyberBlack,
                                            0.15f to CyberBlack.copy(alpha = 0.85f),
                                            0.3f to Color.Transparent,
                                            0.7f to Color.Transparent,
                                            0.9f to CyberBlack.copy(alpha = 0.95f),
                                            1.0f to CyberBlack
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
                                            0.0f to CyberBlack,
                                            0.2f to Color.Transparent,
                                            0.8f to Color.Transparent,
                                            1.0f to CyberBlack
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
                                shape = CutCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 10.dp,
                                    bottomEnd = 0.dp,
                                    bottomStart = 10.dp
                                ),
                                color = CyberGray,
                                border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.6f)),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = g.platform.uppercase(),
                                    color = CyberYellow,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

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
                            if (g.year.isNotBlank()) {
                                CyberChip(
                                    icon = Icons.Filled.CalendarToday,
                                    text = g.year,
                                    tint = CyberYellow
                                )
                            }

                            if (g.genre.isNotBlank()) {
                                CyberChip(
                                    icon = Icons.Filled.Category,
                                    text = g.genre,
                                    tint = CyberBlue
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
                            g.testResults.filter { it.mediaLink.isNotBlank() }
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
                            ).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberYellow,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (allCommentsForGame.isEmpty()) {
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
                                allCommentsForGame.forEach { comment ->
                                    CommentCardCyber(comment)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.loadMoreCommentsForGame(g.id) },
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
    val thumbUrl = remember(test.mediaLink) { youtubeThumbnailUrl(test.mediaLink) }

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
                    .height(190.dp)
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
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
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
private fun CommentCardCyber(comment: TestComment) {
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
                        color = CyberYellow,
                        fontFamily = FontFamily.Monospace
                    )

                    if (dateStr.isNotBlank()) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberYellow.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
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
            topEnd = 12.dp,
            bottomEnd = 0.dp,
            bottomStart = 12.dp
        ),
        color = CyberGray,
        border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Android,
                contentDescription = null,
                tint = CyberBlue,
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
