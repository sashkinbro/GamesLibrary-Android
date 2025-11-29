package com.sbro.gameslibrary.components

//noinspection SuspiciousImport
import android.R
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sbro.gameslibrary.R as AppR

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun GameCard(
    game: Game,
    onEditStatus: (Game) -> Unit,
    onToggleFavorite: (Game) -> Unit,
    onShowTestHistory: (Game) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTabletOrWide = screenWidthDp >= 600

    val cardCorner = if (isTabletOrWide) 20.dp else 16.dp
    val outerPadding = if (isTabletOrWide) 20.dp else 16.dp
    val imageColumnWidth = if (isTabletOrWide) 190.dp else 150.dp
    val imageWidth = if (isTabletOrWide) 150.dp else 120.dp
    val favoriteButtonSize = if (isTabletOrWide) 56.dp else 48.dp
    val favoriteIconSize = if (isTabletOrWide) 34.dp else 28.dp
    val smallIconSize = if (isTabletOrWide) 20.dp else 16.dp
    val tinyIconSize = if (isTabletOrWide) 16.dp else 14.dp
    val betweenBlocks = if (isTabletOrWide) 12.dp else 10.dp
    val bottomPaddingH = if (isTabletOrWide) 16.dp else 12.dp
    val bottomPaddingV = if (isTabletOrWide) 10.dp else 8.dp
    val badgeRadius = if (isTabletOrWide) 6.dp else 4.dp
    val badgeIcon = if (isTabletOrWide) 12.dp else 10.dp
    val statusIcon = if (isTabletOrWide) 16.dp else 14.dp
    val statusHPadding = if (isTabletOrWide) 12.dp else 10.dp
    val statusVPadding = if (isTabletOrWide) 7.dp else 5.dp

    var expanded by remember { mutableStateOf(false) }
    var showIssueDialog by remember { mutableStateOf(false) }

    val cardColor = MaterialTheme.colorScheme.surface
    val latestTest = game.latestTestOrNull()
    val latestStatus = game.overallStatus()

    Card(
        shape = RoundedCornerShape(cardCorner),
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isTabletOrWide) 18.dp else 16.dp)
            .animateContentSize(animationSpec = spring())
    ) {
        Column(
            modifier = Modifier.clipToBounds()
        ) {
            Row(modifier = Modifier.padding(outerPadding)) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(imageColumnWidth)
                ) {
                    Card(
                        shape = RoundedCornerShape(if (isTabletOrWide) 14.dp else 12.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier
                            .width(imageWidth)
                            .aspectRatio(3f / 4f)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(game.imageUrl)
                                .crossfade(true)
                                .error(R.drawable.ic_menu_gallery)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isTabletOrWide) 10.dp else 8.dp))

                    IconButton(
                        onClick = { onToggleFavorite(game) },
                        modifier = Modifier.size(favoriteButtonSize)
                    ) {
                        Icon(
                            imageVector = if (game.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (game.isFavorite) Color(0xFFE91E63) else colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(favoriteIconSize)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(if (isTabletOrWide) 20.dp else 16.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = game.title,
                        style = if (isTabletOrWide)
                            MaterialTheme.typography.titleLarge
                        else
                            MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(if (isTabletOrWide) 8.dp else 6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (game.year.isNotEmpty()) {
                            InfoBadge(
                                text = game.year,
                                icon = Icons.Filled.CalendarToday,
                                color = colorScheme.surfaceVariant,
                                textColor = colorScheme.onSurfaceVariant,
                                isTabletOrWide = isTabletOrWide,
                                badgeRadius = badgeRadius,
                                iconSize = badgeIcon
                            )
                        }
                        if (game.year.isNotEmpty() && game.rating.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (game.rating.isNotEmpty()) {
                            InfoBadge(
                                text = game.rating,
                                icon = Icons.Filled.Star,
                                color = Color(0xFFFFF9C4),
                                textColor = Color(0xFFF57F17),
                                isTabletOrWide = isTabletOrWide,
                                badgeRadius = badgeRadius,
                                iconSize = badgeIcon
                            )
                        }
                    }

                    if (game.platform.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(if (isTabletOrWide) 8.dp else 6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InfoBadge(
                                text = game.platform,
                                icon = Icons.Filled.SportsEsports,
                                color = colorScheme.secondaryContainer,
                                textColor = colorScheme.onSecondaryContainer,
                                isTabletOrWide = isTabletOrWide,
                                badgeRadius = badgeRadius,
                                iconSize = badgeIcon
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(betweenBlocks))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(if (isTabletOrWide) 6.dp else 4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {

                        WorkStatusBadge(
                            status = latestStatus,
                            onClick = {
                                if (latestStatus == WorkStatus.NOT_WORKING &&
                                    latestTest?.issueNote?.isNotBlank() == true
                                ) {
                                    showIssueDialog = true
                                }
                            },
                            isTabletOrWide = isTabletOrWide,
                            iconSize = statusIcon,
                            horizontalPadding = statusHPadding,
                            verticalPadding = statusVPadding
                        )

                        latestTest?.testedDeviceModel?.let { device ->
                            if (device.isNotBlank() && !device.equals("NaN", ignoreCase = true)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(smallIconSize)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = device,
                                        style = if (isTabletOrWide)
                                            MaterialTheme.typography.bodyLarge
                                        else
                                            MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurface.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        latestTest?.testedGpuModel?.let { gpu ->
                            if (gpu.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = colorScheme.secondary,
                                        modifier = Modifier.size(smallIconSize)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = gpu,
                                        style = if (isTabletOrWide)
                                            MaterialTheme.typography.bodyLarge
                                        else
                                            MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurface.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        latestTest?.testedAndroidVersion?.let { av ->
                            if (av.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = colorScheme.secondary,
                                        modifier = Modifier.size(smallIconSize)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Android $av",
                                        style = if (isTabletOrWide)
                                            MaterialTheme.typography.bodyLarge
                                        else
                                            MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurface.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        latestTest?.let { t ->
                            val hasRes = t.resolutionWidth.isNotBlank() && t.resolutionHeight.isNotBlank()
                            val hasFps = t.fpsMin.isNotBlank() && t.fpsMax.isNotBlank()

                            if (hasRes || hasFps) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = colorScheme.secondary,
                                        modifier = Modifier.size(smallIconSize)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    val resText = if (hasRes) "${t.resolutionWidth}×${t.resolutionHeight}" else ""
                                    val fpsText = if (hasFps) "${t.fpsMin}–${t.fpsMax} FPS" else ""

                                    val finalText = when {
                                        hasRes && hasFps -> "$resText • $fpsText"
                                        hasRes -> resText
                                        else -> fpsText
                                    }

                                    Text(
                                        text = finalText,
                                        style = if (isTabletOrWide)
                                            MaterialTheme.typography.bodyLarge
                                        else
                                            MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurface.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        latestTest?.let { t ->
                            val envParts = buildList {
                                if (t.testedWrapper.isNotBlank()) add(t.testedWrapper)
                                if (t.testedPerformanceMode.isNotBlank()) add(t.testedPerformanceMode)
                                if (t.testedGameVersionOrBuild.isNotBlank()) add("Build ${t.testedGameVersionOrBuild}")
                            }
                            if (envParts.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = colorScheme.tertiary,
                                        modifier = Modifier.size(smallIconSize)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = envParts.joinToString(" • "),
                                        style = if (isTabletOrWide)
                                            MaterialTheme.typography.bodyLarge
                                        else
                                            MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurface.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        latestTest?.testedApp?.let { app ->
                            if (app.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Android,
                                        contentDescription = null,
                                        tint = colorScheme.tertiary,
                                        modifier = Modifier.size(smallIconSize)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    Column {
                                        Text(
                                            text = app,
                                            style = if (isTabletOrWide)
                                                MaterialTheme.typography.bodyLarge
                                            else
                                                MaterialTheme.typography.bodyMedium,
                                            color = colorScheme.onSurface.copy(alpha = 0.9f)
                                        )

                                        val appVersion = latestTest.testedAppVersion
                                        if (appVersion.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Info,
                                                    contentDescription = null,
                                                    tint = colorScheme.onSurface.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(tinyIconSize)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "v$appVersion",
                                                    style = if (isTabletOrWide)
                                                        MaterialTheme.typography.labelLarge
                                                    else
                                                        MaterialTheme.typography.labelMedium,
                                                    color = colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        latestTest?.testedDateFormatted?.let { date ->
                            if (date.isNotBlank()) {
                                Text(
                                    text = "${stringResource(AppR.string.tested_label)} $date",
                                    style = if (isTabletOrWide)
                                        MaterialTheme.typography.labelLarge
                                    else
                                        MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = bottomPaddingH, vertical = bottomPaddingV)) {
                if (game.genre.isNotEmpty()) {
                    Text(
                        text = game.genre,
                        style = if (isTabletOrWide)
                            MaterialTheme.typography.labelLarge
                        else
                            MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                AnimatedVisibility(visible = expanded) {
                    Text(
                        text = game.description.ifEmpty {
                            stringResource(AppR.string.no_description)
                        },
                        style = if (isTabletOrWide)
                            MaterialTheme.typography.bodyMedium
                        else
                            MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(
                                if (expanded)
                                    stringResource(AppR.string.button_hide_info)
                                else
                                    stringResource(AppR.string.button_show_info),
                                style = if (isTabletOrWide)
                                    MaterialTheme.typography.titleMedium
                                else
                                    MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        TextButton(onClick = { onEditStatus(game) }) {
                            Text(
                                stringResource(id = AppR.string.button_edit_status),
                                style = if (isTabletOrWide)
                                    MaterialTheme.typography.titleMedium
                                else
                                    MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (game.testResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = { onShowTestHistory(game) }
                        ) {
                            Text(
                                stringResource(id = AppR.string.button_tested_history),
                                style = if (isTabletOrWide)
                                    MaterialTheme.typography.titleMedium
                                else
                                    MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }

    if (showIssueDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showIssueDialog = false },
            confirmButton = {
                TextButton(onClick = { showIssueDialog = false }) {
                    Text(stringResource(AppR.string.button_ok))
                }
            },
            title = {
                Text(stringResource(AppR.string.dialog_issue_title))
            },
            text = {
                Text(
                    text = latestTest?.issueNote?.ifBlank {
                        stringResource(AppR.string.dialog_issue_empty)
                    } ?: stringResource(AppR.string.dialog_issue_empty)
                )
            }
        )
    }
}

@Composable
fun InfoBadge(
    text: String,
    icon: ImageVector,
    color: Color,
    textColor: Color = Color.Black,
    isTabletOrWide: Boolean = false,
    badgeRadius: androidx.compose.ui.unit.Dp = 4.dp,
    iconSize: androidx.compose.ui.unit.Dp = 10.dp
) {
    Surface(color = color, shape = RoundedCornerShape(badgeRadius)) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isTabletOrWide) 8.dp else 6.dp,
                vertical = if (isTabletOrWide) 3.dp else 2.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(iconSize), tint = textColor)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = if (isTabletOrWide)
                    MaterialTheme.typography.labelLarge
                else
                    MaterialTheme.typography.labelSmall,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun WorkStatusBadge(
    status: WorkStatus,
    onClick: (() -> Unit)? = null,
    isTabletOrWide: Boolean = false,
    iconSize: androidx.compose.ui.unit.Dp = 14.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 10.dp,
    verticalPadding: androidx.compose.ui.unit.Dp = 5.dp
) {
    val bgColor: Color
    val contentColor: Color
    val textResId: Int
    val icon: ImageVector

    when (status) {
        WorkStatus.WORKING -> {
            bgColor = Color(0xFFE8F5E9)
            contentColor = Color(0xFF2E7D32)
            textResId = AppR.string.work_status_working
            icon = Icons.Filled.CheckCircle
        }

        WorkStatus.UNTESTED -> {
            bgColor = Color(0xFFFFF3E0)
            contentColor = Color(0xFFEF6C00)
            textResId = AppR.string.work_status_untested
            icon = Icons.AutoMirrored.Filled.HelpOutline
        }

        WorkStatus.NOT_WORKING -> {
            bgColor = Color(0xFFFFEBEE)
            contentColor = Color(0xFFC62828)
            textResId = AppR.string.work_status_not_working
            icon = Icons.Filled.Warning
        }
    }

    val clickableModifier =
        if (status == WorkStatus.NOT_WORKING && onClick != null) {
            Modifier.clickable { onClick() }
        } else {
            Modifier
        }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f)),
        modifier = clickableModifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = stringResource(id = textResId),
                style = if (isTabletOrWide)
                    MaterialTheme.typography.labelLarge
                else
                    MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
