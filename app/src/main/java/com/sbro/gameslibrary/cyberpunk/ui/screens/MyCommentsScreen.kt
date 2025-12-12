package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.WorkStatus
import com.sbro.gameslibrary.components.WorkStatusBadge
import com.sbro.gameslibrary.viewmodel.MyCommentsViewModel
import com.sbro.gameslibrary.viewmodel.TestComment
import java.text.SimpleDateFormat
import java.util.Locale

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)
private val CyberGray = Color(0xFF202020)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCommentsScreen(
    onBack: () -> Unit,
    onOpenComment: (gameId: String, testMillis: Long) -> Unit,
    viewModel: MyCommentsViewModel = viewModel()
) {
    MaterialTheme.colorScheme
    val context = LocalContext.current

    val user by viewModel.currentUser.collectAsState()
    val commentsMap by viewModel.commentsByTest.collectAsState()
    val games by viewModel.games.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasLoadedComments by viewModel.hasLoadedComments.collectAsState()

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 450L) return
        lastClickTime.longValue = now
        action()
    }

    LaunchedEffect(Unit) { viewModel.init(context) }

    LaunchedEffect(user?.uid) {
        if (user != null) viewModel.loadMyComments()
    }

    val myComments by remember(commentsMap, games) {
        derivedStateOf {
            commentsMap.values
                .flatten()
                .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                .map { c ->
                    val game = games.firstOrNull { it.id == c.gameId }
                    game to c
                }
        }
    }

    Scaffold(
        containerColor = CyberBlack,
        topBar = {
            Column {
                TopAppBar(modifier = Modifier.statusBarsPadding(),
                    title = {
                        GlitchText(
                            text = stringResource(R.string.my_comments).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = CyberRed
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = CyberYellow,
                        navigationIconContentColor = CyberRed
                    )
                )
                Box(
                    Modifier
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
    ) { pv ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBlack)
                .padding(pv)
        ) {
            CyberGridBackground()
            ScanlinesEffect()
            VignetteEffect()

            when {
                user == null -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.need_login),
                            color = Color.White.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                isLoading || !hasLoadedComments -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CyberYellow)
                    }
                }

                myComments.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Filled.Message,
                                contentDescription = null,
                                tint = CyberGray,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.my_comments_empty),
                                color = Color.White.copy(alpha = 0.75f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(myComments, key = { (_, c) -> c.id }) { (game, c) ->
                            val testStatus: WorkStatus? =
                                game?.testResults
                                    ?.firstOrNull { it.updatedAtMillis == c.testMillis }
                                    ?.status

                            MyCommentCard(
                                gameTitle = game?.title,
                                gameImageUrl = game?.imageUrl,
                                gamePlatform = game?.platform,
                                gameYear = game?.year,
                                testStatus = testStatus,
                                comment = c,
                                onClick = {
                                    safeClick {
                                        val millis = when {
                                            c.testMillis != 0L -> c.testMillis
                                            c.testId.contains("_") ->
                                                c.testId.substringAfterLast("_").toLongOrNull()
                                                    ?: 0L
                                            else -> 0L
                                        }
                                        onOpenComment(c.gameId, millis)
                                    }
                                }
                            )
                        }
                        item {
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
private fun MyCommentCard(
    gameTitle: String?,
    gameImageUrl: String?,
    gamePlatform: String?,
    gameYear: String?,
    testStatus: WorkStatus?,
    comment: TestComment,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val accent = when (testStatus) {
        WorkStatus.WORKING -> CyberBlue
        WorkStatus.NOT_WORKING -> CyberRed
        else -> CyberYellow
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp))
            .background(CyberDark)
            .border(1.dp, accent.copy(alpha = if (isPressed) 1f else 0.5f),
                CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)
            )
            .padding(1.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {

        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = 2.dp.toPx()
            val len = 14.dp.toPx()

            drawLine(accent, start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(len, 0f), strokeWidth = stroke)
            drawLine(accent, start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(0f, len), strokeWidth = stroke)

            val tri = Path().apply {
                moveTo(size.width, size.height)
                lineTo(size.width - len, size.height)
                lineTo(size.width, size.height - len)
                close()
            }
            drawPath(tri, color = accent)
        }

        Row(Modifier.padding(12.dp)) {

            if (!gameImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(gameImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 56.dp, height = 74.dp)
                        .clip(CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                        .border(1.dp, accent.copy(alpha = 0.35f),
                            CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = gameTitle ?: stringResource(R.string.value_dash),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White,
                    maxLines = 1
                )

                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!gamePlatform.isNullOrBlank()) {
                        CyberChip(
                            text = gamePlatform,
                            accent = CyberBlue
                        )
                    }
                    if (!gameYear.isNullOrBlank() && gameYear != "0") {
                        CyberChip(
                            text = gameYear,
                            accent = CyberYellow
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(Modifier.height(10.dp))
                val dateText = comment.createdAt?.toDate()?.let {
                    SimpleDateFormat(
                        "d MMM yyyy • HH:mm",
                        Locale.getDefault()
                    ).format(it)
                } ?: "—"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    CyberChip(
                        text = dateText,
                        accent = CyberGray
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        AnimatedVisibility(
                            visible = testStatus != null,
                            enter = fadeIn(tween(180)) + scaleIn(
                                initialScale = 0.85f,
                                animationSpec = tween(220)
                            )
                        ) {
                            if (testStatus != null) {
                                WorkStatusBadge(status = testStatus)
                            }
                        }

                        AnimatedVisibility(
                            visible = comment.fromAccount,
                            enter = fadeIn(tween(180)) + scaleIn(
                                initialScale = 0.85f,
                                animationSpec = tween(220)
                            )
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = CyberYellow,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CyberChip(
    text: String,
    accent: Color
) {
    if (text.isBlank()) return
    Surface(
        shape = CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp),
        color = accent.copy(alpha = 0.12f),
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            1.dp,
            accent.copy(alpha = 0.45f),
            CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
