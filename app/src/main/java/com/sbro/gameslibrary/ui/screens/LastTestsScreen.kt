package com.sbro.gameslibrary.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.WorkStatus
import com.sbro.gameslibrary.viewmodel.GameViewModel
import android.R as AndroidR

private data class LatestTestItem(
    val gameId: String,
    val gameTitle: String,
    val status: WorkStatus?,
    val testedDate: String?,
    val deviceModel: String?,
    val updatedAtMillis: Long,
    val imageUrl: String?,
    val authorName: String?,
    val fromAccount: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastTestsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onOpenLastTestDetails: (gameId: String) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    val games by viewModel.games.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 500L) return
        lastClickTime.longValue = now
        action()
    }

    val latestTests = remember(games) {
        games
            .flatMap { game ->
                game.testResults.map { tr ->
                    LatestTestItem(
                        gameId = game.id,
                        gameTitle = game.title,
                        status = tr.status,
                        testedDate = tr.testedDateFormatted,
                        deviceModel = tr.testedDeviceModel,
                        updatedAtMillis = tr.updatedAtMillis,
                        imageUrl = game.imageUrl,
                        authorName = tr.authorName,
                        fromAccount = tr.fromAccount
                    )
                }
            }
            .sortedByDescending { it.updatedAtMillis }
            .take(20)
    }

    val isLoading = uiState is GameViewModel.UiState.Loading

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.main_button_last_tests),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { safeClick(onBack) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
        }
    ) { padding ->

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            latestTests.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.main_last_tests_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = latestTests,
                        key = { it.gameId + it.updatedAtMillis }
                    ) { item ->
                        LatestTestCard(
                            item = item,
                            onClick = { safeClick { onOpenLastTestDetails(item.gameId) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LatestTestCard(
    item: LatestTestItem,
    onClick: () -> Unit
) {
    val statusIcon = when (item.status) {
        WorkStatus.WORKING -> Icons.Filled.CheckCircle
        WorkStatus.NOT_WORKING -> Icons.Filled.Warning
        else -> Icons.AutoMirrored.Filled.HelpOutline
    }

    val statusTint = when (item.status) {
        WorkStatus.WORKING -> MaterialTheme.colorScheme.primary
        WorkStatus.NOT_WORKING -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }

    val statusText = when (item.status) {
        WorkStatus.WORKING -> stringResource(R.string.work_status_working)
        WorkStatus.NOT_WORKING -> stringResource(R.string.work_status_not_working)
        else -> stringResource(R.string.work_status_untested)
    }

    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val cover = item.imageUrl
            if (!cover.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .height(64.dp)
                        .aspectRatio(3f / 4f)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(cover)
                            .crossfade(true)
                            .error(AndroidR.drawable.ic_menu_gallery)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.gameTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                val author = item.authorName?.trim().takeIf {
                    item.fromAccount && !it.isNullOrBlank()
                }

                val meta = listOfNotNull(
                    item.testedDate?.takeIf { it.isNotBlank() },
                    author,
                    item.deviceModel?.takeIf { it.isNotBlank() },
                    statusText
                ).joinToString(" • ")

                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
