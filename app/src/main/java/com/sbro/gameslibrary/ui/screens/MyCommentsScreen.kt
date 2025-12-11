package com.sbro.gameslibrary.ui.screens

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCommentsScreen(
    onBack: () -> Unit,
    onOpenComment: (gameId: String, testMillis: Long) -> Unit,
    viewModel: MyCommentsViewModel = viewModel()
) {
    val cs = MaterialTheme.colorScheme
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

    val background = Brush.verticalGradient(
        listOf(cs.background, cs.surfaceContainer)
    )

    Scaffold(contentWindowInsets = WindowInsets(0),
        containerColor = cs.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.my_comments),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { safeClick(onBack) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                HorizontalDivider(color = cs.outline.copy(alpha = 0.4f))
            }
        }
    ) { pv ->

        if (user == null) {
            Box(
                Modifier.fillMaxSize().padding(pv),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.need_login))
            }
            return@Scaffold
        }

        if (isLoading || !hasLoadedComments) {
            Box(
                Modifier.fillMaxSize().padding(pv),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (myComments.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(pv),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        tint = cs.outline,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.my_comments_empty),
                        color = cs.onSurface.copy(alpha = 0.75f)
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(pv)
                .navigationBarsPadding(),
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
                                    c.testId.substringAfterLast("_").toLongOrNull() ?: 0L
                                else -> 0L
                            }
                            onOpenComment(c.gameId, millis)
                        }
                    }
                )
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
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cs.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
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
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = gameTitle ?: stringResource(R.string.value_dash),
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.onSurface,
                    maxLines = 1
                )

                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!gamePlatform.isNullOrBlank()) {
                        SmartChip(
                            text = gamePlatform,
                            container = cs.secondaryContainer,
                            content = cs.onSecondaryContainer
                        )
                    }
                    if (!gameYear.isNullOrBlank() && gameYear != "0") {
                        SmartChip(
                            text = gameYear,
                            container = cs.tertiaryContainer,
                            content = cs.onTertiaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface
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
                    SmartChip(
                        text = dateText,
                        container = cs.surfaceVariant,
                        content = cs.onSurfaceVariant
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
                                tint = cs.primary,
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
private fun SmartChip(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color
) {
    if (text.isBlank()) return
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = container,
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
