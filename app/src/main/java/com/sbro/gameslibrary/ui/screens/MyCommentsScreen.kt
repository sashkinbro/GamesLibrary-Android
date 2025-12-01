package com.sbro.gameslibrary.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.Game
import com.sbro.gameslibrary.components.WorkStatus
import com.sbro.gameslibrary.components.WorkStatusBadge
import com.sbro.gameslibrary.viewmodel.GameViewModel
import com.sbro.gameslibrary.viewmodel.TestComment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCommentsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onOpenComment: (gameId: String, testMillis: Long) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val user by viewModel.currentUser.collectAsState()
    val commentsMap by viewModel.commentsByTest.collectAsState()
    val games by viewModel.games.collectAsState()

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 450L) return
        lastClickTime.longValue = now
        action()
    }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            viewModel.loadAllComments()
        }
    }

    val myComments: List<Pair<Game?, TestComment>> = remember(user, commentsMap, games) {
        val uid = user?.uid ?: return@remember emptyList()
        commentsMap.values
            .flatten()
            .filter { it.authorUid == uid }
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            .map { c ->
                val game = games.firstOrNull { it.id == c.gameId }
                game to c
            }
    }

    val background = Brush.verticalGradient(
        listOf(cs.background, cs.surfaceContainer)
    )

    Scaffold(
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

        if (myComments.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(pv),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Message,
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
                    game?.testResults?.firstOrNull { it.updatedAtMillis == c.testMillis }?.status

                MyCommentCard(
                    gameTitle = game?.title,
                    gameImageUrl = game?.imageUrl,
                    gamePlatform = game?.platform,
                    gameYear = game?.year,
                    testStatus = testStatus,
                    comment = c,
                    onClick = {
                        safeClick {
                            onOpenComment(c.gameId, c.testMillis)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = gameTitle ?: stringResource(R.string.value_dash),
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (testStatus != null) {
                            WorkStatusBadge(status = testStatus)
                            Spacer(Modifier.width(6.dp))
                        }

                        if (comment.fromAccount) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

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

                Spacer(Modifier.height(6.dp))

                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface
                )

                Spacer(Modifier.height(8.dp))

                val dateText = comment.createdAt?.toDate()?.let {
                    java.text.SimpleDateFormat("d MMM yyyy • HH:mm", java.util.Locale.getDefault())
                        .format(it)
                } ?: "—"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SmartChip(
                        text = dateText,
                        container = cs.surfaceVariant,
                        content = cs.onSurfaceVariant
                    )
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
