package com.sbro.gameslibrary.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.GameTestResult
import com.sbro.gameslibrary.components.WorkStatusBadge
import com.sbro.gameslibrary.viewmodel.GameViewModel
import com.sbro.gameslibrary.viewmodel.TestComment

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestHistoryScreen(
    viewModel: GameViewModel,
    gameId: String,
    onBack: () -> Unit,
    onOpenTestDetails: (Long) -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(Unit) { viewModel.init(context) }

    val games by viewModel.games.collectAsState()
    val commentsByTest by viewModel.commentsByTest.collectAsState()

    val game = remember(games, gameId) { games.firstOrNull { it.id == gameId } }

    LaunchedEffect(gameId, game?.id) {
        if (game != null) viewModel.loadCommentsForGame(gameId)
    }

    if (game == null) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text(stringResource(R.string.test_history_title)) },
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
            Box(
                Modifier.fillMaxSize().padding(pv),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.error_game_not_found))
            }
        }
        return
    }

    val sortedTests = remember(game.testResults) {
        game.testResults.sortedByDescending { it.updatedAtMillis }
    }

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = { Text(stringResource(R.string.test_history_title)) },
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
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = pv.calculateTopPadding(),
                    bottom = pv.calculateBottomPadding()
                )
                .navigationBarsPadding()
        ) {
            item {
                Box(
                    modifier = Modifier
                        .height(500.dp)
                        .fillMaxWidth()
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(game.imageUrl)
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
                            .data(game.imageUrl)
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

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = cs.outline.copy(alpha = 0.35f)
                )

                Spacer(Modifier.height(12.dp))
            }

            if (sortedTests.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            tint = cs.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.dialog_test_history_empty),
                            color = cs.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                items(sortedTests, key = { it.updatedAtMillis }) { test ->
                    val testComments = commentsByTest[test.updatedAtMillis].orEmpty()

                    TestHistoryCard(
                        test = test,
                        comments = testComments,
                        onAddComment = { text ->
                            viewModel.addTestComment(
                                context = context,
                                gameId = gameId,
                                testMillis = test.updatedAtMillis,
                                text = text
                            )
                        },
                        onClick = { onOpenTestDetails(test.updatedAtMillis) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TestHistoryCard(
    test: GameTestResult,
    comments: List<TestComment>,
    onAddComment: (String) -> Unit,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var input by rememberSaveable(test.updatedAtMillis) { mutableStateOf("") }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = cs.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkStatusBadge(status = test.status)

                Text(
                    text = test.testedDateFormatted.ifBlank { "—" },
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = cs.onSurface.copy(alpha = 0.08f)
            )

            InfoLine(icon = Icons.Filled.Smartphone, text = test.testedDeviceModel)
            InfoLine(
                icon = Icons.Filled.Android,
                text = test.testedAndroidVersion.takeIf { it.isNotBlank() }
                    ?.let { stringResource(R.string.test_history_android_with_version, it) }
                    ?: ""
            )
            InfoLine(icon = Icons.Filled.Memory, text = test.testedGpuModel)

            Spacer(Modifier.height(6.dp))

            val wrapper = test.testedWrapper.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.value_dash)
            val perf = test.testedPerformanceMode.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.value_dash)

            Text(
                text = stringResource(R.string.test_history_wrapper_perf, wrapper, perf),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurface.copy(alpha = 0.8f)
            )

            if (test.issueNote.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = test.issueNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurface.copy(alpha = 0.9f),
                    maxLines = 2
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = cs.onSurface.copy(alpha = 0.10f))
            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.test_comments_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface
            )

            Spacer(Modifier.height(6.dp))

            if (comments.isEmpty()) {
                Text(
                    text = stringResource(R.string.test_comments_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface.copy(alpha = 0.7f)
                )
            } else {
                comments.forEach { c ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = cs.surface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = c.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(stringResource(R.string.test_comment_hint)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        onAddComment(input)
                        input = ""
                    },
                    enabled = input.trim().isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.cd_send_comment)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    if (text.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
