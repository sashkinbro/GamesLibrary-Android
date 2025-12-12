package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.annotation.SuppressLint
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CutCornerShape
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
import com.sbro.gameslibrary.components.Game
import com.sbro.gameslibrary.components.GameTestResult
import com.sbro.gameslibrary.cyberpunk.components.WorkStatusBadge
import com.sbro.gameslibrary.viewmodel.MyTestsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)
private val CyberGray = Color(0xFF202020)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTestsScreen(
    viewModel: MyTestsViewModel,
    onBack: () -> Unit,
    onOpenTestDetails: (gameId: String, testId: String) -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val myTests by viewModel.myTests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasLoadedTests by viewModel.hasLoadedTests.collectAsState()
    val hasMoreTests by viewModel.hasMoreTests.collectAsState()

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 450L) return
        lastClickTime.longValue = now
        action()
    }

    val listState = rememberLazyListState()
    LaunchedEffect(listState, myTests.size, hasMoreTests, hasLoadedTests) {
        snapshotFlow { listState.layoutInfo }
            .map { info ->
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = info.totalItemsCount
                lastVisible to total
            }
            .distinctUntilChanged()
            .filter { (lastVisible, total) ->
                hasLoadedTests && hasMoreTests && total > 0 && lastVisible >= total - 3
            }
            .collect { viewModel.loadMoreMyTests() }
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
            contentWindowInsets = WindowInsets(0),
            topBar = {
                Column {
                    TopAppBar(modifier = Modifier.statusBarsPadding(),
                        title = {
                            GlitchText(
                                text = stringResource(R.string.my_tests).uppercase(),
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
                                    contentDescription = "Back",
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

            if (user == null) {
                CyberEmptyState(
                    text = stringResource(R.string.need_login),
                    icon = Icons.Filled.History,
                    modifier = Modifier.padding(pv)
                )
                return@Scaffold
            }

            if (isLoading && !hasLoadedTests) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(pv),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = CyberYellow,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(34.dp)
                    )
                }
                return@Scaffold
            }

            if (myTests.isEmpty()) {
                CyberEmptyState(
                    text = stringResource(R.string.my_tests_empty),
                    icon = Icons.Filled.History,
                    modifier = Modifier.padding(pv)
                )
                return@Scaffold
            }

            val listBackground = Brush.verticalGradient(
                listOf(CyberBlack, CyberDark)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(listBackground)
                    .padding(pv),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    myTests,
                    key = { (game, test) -> "${game.id}_${test.updatedAtMillis}" }
                ) { (game, test) ->

                    val testId = "${game.id}_${test.updatedAtMillis}"

                    MyTestCardCyber(
                        game = game,
                        test = test,
                        onClick = {
                            safeClick { onOpenTestDetails(game.id, testId) }
                        }
                    )
                }

                item(key = "footer") {
                    Spacer(Modifier.height(6.dp))
                    if (hasMoreTests) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = CyberYellow,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Spacer(Modifier.height(22.dp))
                            }
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item(key = "nav_inset") {
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
private fun MyTestCardCyber(
    game: Game,
    test: GameTestResult,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val cardShape = CutCornerShape(
        topStart = 0.dp,
        topEnd = 16.dp,
        bottomEnd = 0.dp,
        bottomStart = 16.dp
    )

    Surface(
        onClick = onClick,
        shape = cardShape,
        color = CyberDark,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    CyberRed.copy(alpha = 0.7f),
                    CyberYellow.copy(alpha = 0.7f),
                    CyberBlue.copy(alpha = 0.7f)
                )
            )
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(width = 92.dp, height = 126.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberGray)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(game.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBlack.copy(alpha = 0.12f))
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberYellow,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WorkStatusBadge(status = test.status)

                        if (test.fromAccount) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = CyberBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = test.testedDateFormatted.ifBlank { "—" },
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberYellow.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(6.dp))

                if (test.testedDeviceModel.isNotBlank()
                    || test.testedAndroidVersion.isNotBlank()
                ) {
                    CyberLineText(
                        text = "${test.testedDeviceModel} • ANDROID ${test.testedAndroidVersion}"
                    )
                }

                if (test.issueNote.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    CyberLineText(
                        text = test.issueNote,
                        faint = false
                    )
                }
            }
        }
    }
}

@Composable
private fun CyberLineText(
    text: String,
    faint: Boolean = true
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (faint) CyberBlue.copy(alpha = 0.85f) else CyberYellow.copy(alpha = 0.9f),
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun CyberEmptyState(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CyberDark,
            border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.12f)),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = CyberYellow.copy(alpha = 0.6f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberYellow.copy(alpha = 0.85f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
