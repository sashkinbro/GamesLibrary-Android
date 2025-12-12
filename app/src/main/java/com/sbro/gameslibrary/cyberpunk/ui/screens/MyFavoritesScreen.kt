package com.sbro.gameslibrary.cyberpunk.ui.screens

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.sbro.gameslibrary.viewmodel.MyFavoritesViewModel

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)
private val CyberGray = Color(0xFF202020)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFavoritesScreen(
    viewModel: MyFavoritesViewModel,
    onBack: () -> Unit,
    onOpenGame: (String) -> Unit
) {
    val context = LocalContext.current

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
                                text = stringResource(R.string.my_favorites).uppercase(),
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
                    text = stringResource(R.string.need_login_favorites),
                    modifier = Modifier.padding(pv)
                )
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
                    CircularProgressIndicator(
                        color = CyberYellow,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(34.dp)
                    )
                }
                return@Scaffold
            }

            if (uiState is MyFavoritesViewModel.UiState.Error) {
                val msg = (uiState as MyFavoritesViewModel.UiState.Error).message
                CyberEmptyState(text = msg, modifier = Modifier.padding(pv))
                return@Scaffold
            }

            if (favorites.isEmpty()) {
                CyberEmptyState(
                    text = stringResource(R.string.my_favorites_empty),
                    modifier = Modifier.padding(pv)
                )
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pv),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favorites, key = { it.id }) { g ->
                    FavoriteGameCardCyber(
                        game = g,
                        onClick = { safeClick { onOpenGame(g.id) } },
                        onRemoveFavorite = { safeClick { viewModel.toggleFavorite(g.id) } }
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
}

@Composable
private fun FavoriteGameCardCyber(
    game: Game,
    onClick: () -> Unit,
    onRemoveFavorite: () -> Unit
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

                Row(verticalAlignment = Alignment.CenterVertically) {
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

                    Spacer(Modifier.width(6.dp))

                    IconButton(
                        onClick = onRemoveFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                            .background(CyberBlack.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Remove from favorites",
                            tint = CyberRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    val ratingText = game.rating
                    if (ratingText.isNotBlank() && ratingText != "0") {
                        CyberChip(
                            text = "★ $ratingText",
                            tint = CyberYellow,
                            highlight = true
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CyberChip(text = game.platform, tint = CyberBlue)
                        CyberChip(text = game.year, tint = CyberYellow)
                    }
                }


                Spacer(Modifier.height(10.dp))

                FavoriteBadgeCyber()
            }
        }
    }
}

@Composable
private fun FavoriteBadgeCyber() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CyberGray,
        border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.45f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = CyberRed,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.favorite_badge).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = CyberRed,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun CyberChip(
    text: String,
    tint: Color,
    highlight: Boolean = false
) {
    if (text.isBlank()) return

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CyberGray,
        border = BorderStroke(
            1.dp,
            (if (highlight) tint else CyberYellow).copy(alpha = 0.35f)
        ),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (highlight) FontWeight.Black else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CyberEmptyState(
    text: String,
    modifier: Modifier = Modifier
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
            border = BorderStroke(1.dp, CyberYellow.copy(alpha = 0.12f))
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = CyberYellow.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}
