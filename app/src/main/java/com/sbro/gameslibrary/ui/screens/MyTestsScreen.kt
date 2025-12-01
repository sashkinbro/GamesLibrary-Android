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
import androidx.compose.material.icons.filled.History
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
import com.sbro.gameslibrary.components.GameTestResult
import com.sbro.gameslibrary.components.WorkStatusBadge
import com.sbro.gameslibrary.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTestsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onOpenTestDetails: (gameId: String, testMillis: Long) -> Unit
) {
    LocalContext.current
    val cs = MaterialTheme.colorScheme

    val user by viewModel.currentUser.collectAsState()
    val games by viewModel.games.collectAsState()

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 450L) return
        lastClickTime.longValue = now
        action()
    }

    val myTests: List<Pair<Game, GameTestResult>> = remember(user, games) {
        val uid = user?.uid
        if (uid == null) emptyList()
        else games.flatMap { g ->
            g.testResults
                .filter { it.authorUid == uid }
                .map { test -> g to test }
        }.sortedByDescending { (_, test) -> test.updatedAtMillis }
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
                            text = stringResource(R.string.my_tests),
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
                Modifier
                    .fillMaxSize()
                    .padding(pv),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.need_login))
            }
            return@Scaffold
        }

        if (myTests.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pv),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = cs.outline,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.my_tests_empty),
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
            items(
                myTests,
                key = { (game, test) -> "${game.id}_${test.updatedAtMillis}" }
            ) { (game, test) ->
                MyTestCard(
                    game = game,
                    test = test,
                    onClick = {
                        safeClick {
                            onOpenTestDetails(game.id, test.updatedAtMillis)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MyTestCard(
    game: Game,
    test: GameTestResult,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(game.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 88.dp, height = 120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceVariant)
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WorkStatusBadge(status = test.status)

                        if (test.fromAccount) {
                            Spacer(Modifier.width(6.dp))
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

                Text(
                    text = test.testedDateFormatted.ifBlank { "—" },
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmartChip(
                        text = game.platform,
                        container = cs.secondaryContainer,
                        content = cs.onSecondaryContainer
                    )
                    SmartChip(
                        text = game.year,
                        container = cs.tertiaryContainer,
                        content = cs.onTertiaryContainer
                    )
                }

                if (test.issueNote.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = test.issueNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurface.copy(alpha = 0.8f),
                        maxLines = 2
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
