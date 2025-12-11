package com.sbro.gameslibrary.ui.screens

import android.os.SystemClock
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
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
import com.sbro.gameslibrary.viewmodel.MyTestsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTestsScreen(
    viewModel: MyTestsViewModel,
    onBack: () -> Unit,
    onOpenTestDetails: (gameId: String, testId: String) -> Unit
) {
    val cs = MaterialTheme.colorScheme

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

    val background = Brush.verticalGradient(
        listOf(cs.background, cs.surfaceContainer)
    )

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
            .collect {
                viewModel.loadMoreMyTests()
            }
    }

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets(0),
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
                    .background(background)
                    .padding(pv),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.need_login))
            }
            return@Scaffold
        }

        if (isLoading && !hasLoadedTests) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pv),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(pv),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                myTests,
                key = { (game, test) -> "${game.id}_${test.updatedAtMillis}" }
            ) { (game, test) ->

                val testId = "${game.id}_${test.updatedAtMillis}"

                MyTestCard(
                    game = game,
                    test = test,
                    onClick = {
                        safeClick {
                            onOpenTestDetails(game.id, testId)
                        }
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
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        } else {
                            Spacer(Modifier.height(22.dp))
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                }
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
                    color = cs.onSurface.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(6.dp))

                if (test.testedDeviceModel.isNotBlank()
                    || test.testedAndroidVersion.isNotBlank()
                ) {
                    Text(
                        text = "${test.testedDeviceModel} • Android ${test.testedAndroidVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }

                if (test.issueNote.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = test.issueNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurface.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
