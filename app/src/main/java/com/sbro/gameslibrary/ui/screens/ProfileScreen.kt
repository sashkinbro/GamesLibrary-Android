package com.sbro.gameslibrary.ui.screens

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onOpenMyTests: () -> Unit,
    onOpenMyComments: () -> Unit,
    onOpenMyFavorites: () -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val user by viewModel.currentUser.collectAsState()
    val games by viewModel.games.collectAsState()
    val commentsByTest by viewModel.commentsByTest.collectAsState()

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 500L) return
        lastClickTime.longValue = now
        action()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            viewModel.handleGoogleResult(context, res.data)
        }
    }

    val background = Brush.verticalGradient(listOf(cs.background, cs.surfaceContainer))

    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            viewModel.loadAllComments()
        }
    }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.profile_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { safeClick(onBack) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
                HorizontalDivider(color = cs.outline.copy(alpha = 0.4f))
            }
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(pv)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (user == null) {
                GuestCard(
                    onSignIn = {
                        safeClick {
                            launcher.launch(viewModel.getGoogleSignInIntent(context))
                        }
                    }
                )

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.profile_guest_hint),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = cs.onSurface.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                }

                return@Column
            }

            val uid = user!!.uid

            UserHeaderCard(
                displayName = user!!.displayName.orEmpty(),
                email = user!!.email.orEmpty(),
                photoUrl = user!!.photoUrl?.toString()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { safeClick { showLogoutDialog = true } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.sign_out),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
                )
            }

            Spacer(Modifier.height(14.dp))

            val myTestsCount = remember(games, uid) {
                games.sumOf { g ->
                    g.testResults.count { tr -> tr.fromAccount && tr.authorUid == uid }
                }
            }

            val myCommentsCount = remember(commentsByTest, uid) {
                commentsByTest.values.flatten()
                    .count { c -> c.fromAccount && c.authorUid == uid }
            }

            val myFavoritesCount = remember(games) {
                games.count { it.isFavorite }
            }

            ProfileStatsCard(
                testsCount = myTestsCount,
                commentsCount = myCommentsCount,
                favoritesCount = myFavoritesCount
            )

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = cs.outline.copy(alpha = 0.25f))
            Spacer(Modifier.height(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                ProfileMenuCard(
                    icon = Icons.Filled.History,
                    title = stringResource(R.string.my_tests),
                    subtitle = stringResource(R.string.profile_menu_tests_sub),
                    onClick = { safeClick(onOpenMyTests) }
                )

                ProfileMenuCard(
                    icon = Icons.Filled.Message,
                    title = stringResource(R.string.my_comments),
                    subtitle = stringResource(R.string.profile_menu_comments_sub),
                    onClick = { safeClick(onOpenMyComments) }
                )

                ProfileMenuCard(
                    icon = Icons.Filled.Favorite,
                    title = stringResource(R.string.my_favorites),
                    subtitle = stringResource(R.string.profile_menu_favorites_sub),
                    onClick = { safeClick(onOpenMyFavorites) }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.sign_out_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.signOut(context)
                    }
                ) {
                    Text(stringResource(R.string.sign_out_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.sign_out_confirm_no))
                }
            }
        )
    }
}

@Composable
private fun GuestCard(onSignIn: () -> Unit) {
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(72.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.profile_guest_title),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.profile_guest_body),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center,
                color = cs.onSurface.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onSignIn,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.sign_in_google),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
                )
            }
        }
    }
}

@Composable
private fun UserHeaderCard(
    displayName: String,
    email: String,
    photoUrl: String?
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(42.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(84.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_signed_in_as),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                    color = cs.onSurface.copy(alpha = 0.7f)
                )

                if (displayName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = cs.onSurface.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun ProfileStatsCard(
    testsCount: Int,
    commentsCount: Int,
    favoritesCount: Int
) {
    val cs = MaterialTheme.colorScheme

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cs.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_stats_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = cs.onSurface
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                StatItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.History,
                    value = testsCount,
                    label = stringResource(R.string.profile_stats_tests)
                )

                VerticalDividerThin()

                StatItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Message,
                    value = commentsCount,
                    label = stringResource(R.string.profile_stats_comments)
                )

                VerticalDividerThin()

                StatItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Favorite,
                    value = favoritesCount,
                    label = stringResource(R.string.profile_stats_favorites)
                )
            }
        }
    }
}

@Composable
private fun VerticalDividerThin() {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .height(56.dp)
            .width(1.dp)
            .background(cs.onSurface.copy(alpha = 0.08f))
    )
}

@Composable
private fun StatItem(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    label: String
) {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = cs.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = cs.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ProfileMenuCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cs.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        cs.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                        color = cs.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
