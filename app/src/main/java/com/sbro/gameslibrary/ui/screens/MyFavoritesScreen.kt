package com.sbro.gameslibrary.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
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
import com.sbro.gameslibrary.viewmodel.MyFavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFavoritesScreen(
    viewModel: MyFavoritesViewModel,
    onBack: () -> Unit,
    onOpenGame: (String) -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    val user by viewModel.currentUser.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

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

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text(
                            text = stringResource(R.string.my_favorites),
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
                Text(stringResource(R.string.need_login_favorites))
            }
            return@Scaffold
        }

        if (uiState is MyFavoritesViewModel.UiState.Loading ||
            uiState is MyFavoritesViewModel.UiState.Idle
        ) {
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

        if (uiState is MyFavoritesViewModel.UiState.Error) {
            val msg = (uiState as MyFavoritesViewModel.UiState.Error).message
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pv),
                contentAlignment = Alignment.Center
            ) {
                Text(msg)
            }
            return@Scaffold
        }

        if (favorites.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pv),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.my_favorites_empty))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(pv),
            contentPadding = PaddingValues(
                horizontal = 14.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(favorites, key = { it.id }) { g ->
                FavoriteGameCard(
                    game = g,
                    onClick = { safeClick { onOpenGame(g.id) } },
                    onRemoveFavorite = {
                        safeClick { viewModel.toggleFavorite(g.id) }
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

@Composable
private fun FavoriteGameCard(
    game: Game,
    onClick: () -> Unit,
    onRemoveFavorite: () -> Unit
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))

                    IconButton(
                        onClick = onRemoveFavorite,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Remove from favorites",
                            tint = cs.error,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

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

                    val ratingText = game.rating
                    if (ratingText.isNotBlank() && ratingText != "0") {
                        SmartChip(
                            text = "★ $ratingText",
                            container = cs.primaryContainer,
                            content = cs.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                FavoriteBadge()
            }
        }
    }
}

@Composable
private fun FavoriteBadge() {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = cs.error.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = cs.error,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.favorite_badge),
                style = MaterialTheme.typography.labelMedium,
                color = cs.error
            )
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
