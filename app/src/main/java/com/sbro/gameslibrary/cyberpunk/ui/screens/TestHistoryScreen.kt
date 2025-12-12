package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.annotation.SuppressLint
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.GameTestResult
// Use cyberpunk-styled WorkStatusBadge instead of the classic version
import com.sbro.gameslibrary.cyberpunk.components.WorkStatusBadge
import com.sbro.gameslibrary.viewmodel.GameDetailViewModel
import com.sbro.gameslibrary.viewmodel.TestComment

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)
private val CyberGray = Color(0xFF202020)

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestHistoryScreen(
    viewModel: GameDetailViewModel,
    gameId: String,
    onBack: () -> Unit,
    onOpenTestDetails: (Long) -> Unit
) {
    val context = LocalContext.current

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 450L) return
        lastClickTime.longValue = now
        action()
    }

    LaunchedEffect(gameId) {
        viewModel.init(context, gameId)
    }

    val game by viewModel.game.collectAsState()
    val commentsByTest by viewModel.commentsByTest.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val bgBrush = Brush.verticalGradient(listOf(CyberBlack, CyberDark, CyberBlack))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        CyberGridBackground()
        ScanlinesEffect()
        VignetteEffect()

        when {
            isLoading -> {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        Column {
                            TopAppBar(
                                title = {
                                    GlitchText(
                                        text = stringResource(R.string.test_history_title).uppercase(),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 2.sp
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
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null,
                                            tint = CyberRed
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    titleContentColor = CyberYellow
                                )
                            )
                            HorizontalDivider(color = CyberYellow.copy(alpha = 0.12f))
                        }
                    }
                ) { pv ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(pv),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CyberYellow,
                            strokeWidth = 2.dp
                        )
                    }
                }
                return
            }

            game == null -> {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        Column {
                            TopAppBar(
                                title = {
                                    GlitchText(
                                        text = stringResource(R.string.test_history_title).uppercase(),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 2.sp
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
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null,
                                            tint = CyberRed
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    titleContentColor = CyberYellow
                                )
                            )
                            HorizontalDivider(color = CyberYellow.copy(alpha = 0.12f))
                        }
                    }
                ) { pv ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(pv),
                        contentAlignment = Alignment.Center
                    ) {
                        CyberEmptyPanel(text = stringResource(R.string.error_game_not_found))
                    }
                }
                return
            }
        }

        val g = game!!
        val sortedTests = remember(g.testResults) {
            g.testResults.sortedByDescending { it.updatedAtMillis }
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                Column {
                    TopAppBar(
                        modifier = Modifier.statusBarsPadding(),
                        title = {
                            GlitchText(
                                text = stringResource(R.string.test_history_title).uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
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
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = CyberRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = CyberYellow
                        )
                    )
                    HorizontalDivider(color = CyberYellow.copy(alpha = 0.12f))
                }
            }
        ) { pv ->

            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgBrush)
                    .padding(
                        top = pv.calculateTopPadding()
                    )
            ) {
                item {

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
                                .blur(radius = 42.dp)
                                .background(CyberBlack.copy(alpha = 0.7f))
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
                                            0.18f to CyberBlack.copy(alpha = 0.8f),
                                            0.35f to Color.Transparent,
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

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            CyberBlue.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberYellow.copy(alpha = 0.18f)
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
                                tint = CyberBlue.copy(alpha = 0.7f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.dialog_test_history_empty),
                                color = Color.White.copy(alpha = 0.75f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    items(
                        items = sortedTests,
                        key = { test ->
                            test.testId.takeIf { it.isNotBlank() }
                                ?: "legacy_${test.updatedAtMillis}"
                        }
                    ) { test ->
                        val commentsKey =
                            test.testId.takeIf { it.isNotBlank() }
                                ?: "legacy_${test.updatedAtMillis}"

                        val testComments = commentsByTest[commentsKey].orEmpty()

                        TestHistoryCardCyber(
                            viewModel = viewModel,
                            test = test,
                            comments = testComments,
                            currentUserUid = currentUser?.uid,
                            onAddComment = { text ->
                                val tid =
                                    test.testId.takeIf { it.isNotBlank() }
                                        ?: "${gameId}_${test.updatedAtMillis}"

                                viewModel.addTestComment(
                                    context = context,
                                    gameId = gameId,
                                    testId = tid,
                                    testMillis = test.updatedAtMillis,
                                    text = text
                                )
                            },
                            onClick = { safeClick { onOpenTestDetails(test.updatedAtMillis) } }
                        )
                    }
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

@Composable
private fun TestHistoryCardCyber(
    viewModel: GameDetailViewModel,
    test: GameTestResult,
    comments: List<TestComment>,
    currentUserUid: String?,
    onAddComment: (String) -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var input by rememberSaveable(test.updatedAtMillis) { mutableStateOf("") }

    var editingComment by remember { mutableStateOf<TestComment?>(null) }
    var editText by remember { mutableStateOf("") }

    val cardShape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)
    val commentShape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)

    Surface(
        onClick = onClick,
        shape = cardShape,
        color = CyberDark,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(CyberRed, CyberYellow, CyberBlue))
        ),
        tonalElevation = 0.dp,
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
                Box(Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
                    WorkStatusBadge(status = test.status)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = test.testedDateFormatted.ifBlank { "—" },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = CyberBlue
                        )
                        if (test.fromAccount) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "From account",
                                tint = CyberBlue,
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
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                fontWeight = FontWeight.Bold,
                                color = CyberYellow
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = CyberYellow.copy(alpha = 0.12f)
            )

            InfoLineCyber(icon = Icons.Filled.Smartphone, text = test.testedDeviceModel)
            InfoLineCyber(
                icon = Icons.Filled.Android,
                text = test.testedAndroidVersion.takeIf { it.isNotBlank() }
                    ?.let { stringResource(R.string.test_history_android_with_version, it) }
                    ?: ""
            )
            InfoLineCyber(icon = Icons.Filled.Memory, text = test.testedGpuModel)

            Spacer(Modifier.height(6.dp))

            val wrapper = test.testedWrapper.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.value_dash)
            val perf = test.testedPerformanceMode.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.value_dash)

            Text(
                text = stringResource(R.string.test_history_wrapper_perf, wrapper, perf),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White.copy(alpha = 0.8f)
            )

            if (test.issueNote.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = test.issueNote,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = CyberYellow.copy(alpha = 0.12f))
            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.test_comments_title).uppercase(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                ),
                fontWeight = FontWeight.Black,
                color = CyberYellow
            )

            Spacer(Modifier.height(6.dp))

            if (comments.isEmpty()) {
                Text(
                    text = stringResource(R.string.test_comments_empty),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White.copy(alpha = 0.7f)
                )
            } else {
                comments.forEach { c ->
                    Surface(
                        shape = commentShape,
                        color = CyberBlack,
                        border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.18f)),
                        tonalElevation = 0.dp,
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
                                    color = CyberGray,
                                    border = BorderStroke(1.dp, CyberBlue.copy(alpha = 0.35f)),
                                    modifier = Modifier.size(34.dp),
                                    tonalElevation = 0.dp
                                ) {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = CyberBlue,
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
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = CyberYellow
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }

                                Text(
                                    text = c.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.White
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
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Edit comment",
                                        tint = CyberBlue
                                    )
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
                    placeholder = {
                        Text(
                            stringResource(R.string.test_comment_hint),
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
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
                        onAddComment(input)
                        input = ""
                    },
                    enabled = input.trim().isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.cd_send_comment),
                        tint = if (input.trim().isNotBlank()) CyberYellow else CyberGray
                    )
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
                    shape = RoundedCornerShape(10.dp),
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
                        viewModel.editComment(context, c.id, editText)
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
private fun InfoLineCyber(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    if (text.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            icon,
            null,
            tint = CyberBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = Color.White
        )
    }
}

@Composable
private fun CyberEmptyPanel(text: String) {
    Surface(
        shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
        color = CyberDark,
        border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.18f)),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = CyberYellow.copy(alpha = 0.9f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
