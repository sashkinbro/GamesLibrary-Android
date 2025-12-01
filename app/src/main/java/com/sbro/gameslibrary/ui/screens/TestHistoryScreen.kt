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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
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
    val currentUser by viewModel.currentUser.collectAsState()

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
                        viewModel = viewModel,
                        test = test,
                        comments = testComments,
                        currentUserUid = currentUser?.uid,
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
    viewModel: GameViewModel,
    test: GameTestResult,
    comments: List<TestComment>,
    currentUserUid: String?,
    onAddComment: (String) -> Unit,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    var input by rememberSaveable(test.updatedAtMillis) { mutableStateOf("") }

    var editingComment by remember { mutableStateOf<TestComment?>(null) }
    var editText by remember { mutableStateOf("") }

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

                    if (test.fromAccount) {
                        val authorLabel = test.authorName?.takeIf { it.isNotBlank() }.orEmpty()
                        if (authorLabel.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = authorLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
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
                        shape = RoundedCornerShape(12.dp),
                        color = cs.surface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            if (c.fromAccount && !c.authorPhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = c.authorPhotoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(17.dp))
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(17.dp),
                                    color = cs.surfaceVariant,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = cs.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(Modifier.weight(1f)) {
                                val authorLabel =
                                    c.authorName?.takeIf { it.isNotBlank() }.orEmpty()

                                if (c.fromAccount && authorLabel.isNotBlank()) {
                                    Text(
                                        text = authorLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = cs.onSurface.copy(alpha = 0.85f)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }

                                Text(
                                    text = c.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cs.onSurface
                                )
                            }

                            if (currentUserUid != null && c.authorUid == currentUserUid) {
                                Spacer(Modifier.width(2.dp))
                                IconButton(
                                    onClick = {
                                        editingComment = c
                                        editText = c.text
                                    }
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit comment")
                                }
                            }
                        }
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

    if (editingComment != null) {
        AlertDialog(
            onDismissRequest = { editingComment = null },
            title = { Text(stringResource(R.string.edit_comment_title)) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val c = editingComment!!
                        viewModel.editComment(context, c.id, editText)
                        editingComment = null
                    }
                ) {
                    Text(stringResource(R.string.button_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingComment = null }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
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
