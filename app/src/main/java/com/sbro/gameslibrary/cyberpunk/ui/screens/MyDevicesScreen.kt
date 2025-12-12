package com.sbro.gameslibrary.cyberpunk.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.viewmodel.MyDevicesViewModel

private val CyberYellow = Color(0xFFFCEE0A)
private val CyberRed = Color(0xFFFF003C)
private val CyberBlue = Color(0xFF00F0FF)
private val CyberBlack = Color(0xFF050505)
private val CyberDark = Color(0xFF0F0F0F)
private val CyberGray = Color(0xFF202020)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDevicesScreen(
    viewModel: MyDevicesViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.init(context) }
    val state by viewModel.state.collectAsState()

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 400L) return
        lastClickTime.longValue = now
        action()
    }

    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredPhones = remember(searchQuery) { viewModel.searchPhones(searchQuery) }

    if (showSearchDialog) {
        CyberAlertDialog(
            title = stringResource(R.string.search_device_title),
            onDismiss = { showSearchDialog = false },
            confirmText = stringResource(R.string.action_close),
            onConfirm = { showSearchDialog = false }
        ) {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.search_device_input_hint),
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = CyberYellow) },
                    singleLine = true,
                    shape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberYellow,
                        unfocusedBorderColor = CyberGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CyberYellow
                    )
                )

                Spacer(Modifier.height(10.dp))

                when {
                    searchQuery.trim().length < 2 -> {
                        Text(
                            text = stringResource(R.string.search_device_start_typing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    filteredPhones.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.search_device_no_matches),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items(filteredPhones) { spec ->
                                CyberPickRow(
                                    title = spec.name.orEmpty(),
                                    subtitle1 = spec.cpu,
                                    subtitle2 = spec.ram,
                                    onClick = {
                                        viewModel.addDevice(spec)
                                        showSearchDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = CyberBlack,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(modifier = Modifier.statusBarsPadding(),
                    title = {
                        GlitchText(
                            text = stringResource(R.string.my_devices_title).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = CyberRed
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = CyberYellow,
                        navigationIconContentColor = CyberRed
                    )
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(CyberRed, CyberYellow, Color.Transparent)
                            )
                        )
                )
            }
        }
    ) { pv ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBlack)
        ) {
            CyberGridBackground()
            ScanlinesEffect()
            VignetteEffect()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = pv.calculateTopPadding())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (!state.isLoggedIn) {
                    Text(
                        text = stringResource(R.string.my_devices_need_login),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                    return@Column
                }

                CyberCutButton(
                    text = stringResource(R.string.my_devices_add_from_db),
                    icon = Icons.Outlined.Search,
                    enabled = state.devices.size < 5 && !state.isLoading,
                    onClick = { safeClick { showSearchDialog = true; searchQuery = "" } }
                )

                Spacer(Modifier.height(12.dp))

                if (!state.isLoading) {
                    if (state.devices.isEmpty()) {
                        Text(
                            text = stringResource(R.string.my_devices_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(state.devices, key = { it.id }) { d ->
                                CyberDeviceCard(
                                    name = d.name.orEmpty(),
                                    cpu = d.cpu,
                                    ram = d.ram,
                                    onDelete = { safeClick { viewModel.removeDevice(d.id) } }
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

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBlack.copy(alpha = 0.6f))
                        .clickable(
                            enabled = true,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyberYellow)
                }
            }
        }
    }
}


@Composable
private fun CyberDeviceCard(
    name: String,
    cpu: String?,
    ram: String?,
    onDelete: () -> Unit
) {
    val accent = CyberBlue

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp))
            .background(CyberDark)
            .border(
                1.dp,
                accent.copy(alpha = 0.5f),
                CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)
            )
            .padding(1.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = 2.dp.toPx()
            val len = 14.dp.toPx()
            drawLine(accent, start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(len, 0f), strokeWidth = stroke)
            drawLine(accent, start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(0f, len), strokeWidth = stroke)

            val tri = Path().apply {
                moveTo(size.width, size.height)
                lineTo(size.width - len, size.height)
                lineTo(size.width, size.height - len)
                close()
            }
            drawPath(tri, color = accent)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.PhoneAndroid,
                null,
                tint = CyberYellow
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White
                )
                if (!cpu.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.search_device_cpu_prefix, cpu),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                if (!ram.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.search_device_ram_prefix, ram),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = CyberRed)
            }
        }
    }
}

@Composable
private fun CyberCutButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val accent = CyberYellow

    val bg = if (pressed) accent.copy(alpha = 0.18f) else CyberDark
    val border = if (enabled) accent else CyberGray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp))
            .background(bg)
            .border(1.dp, border, CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (enabled) accent else CyberGray)
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun CyberPickRow(
    title: String,
    subtitle1: String?,
    subtitle2: String?,
    onClick: () -> Unit
) {
    val accent = CyberBlue
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
            .background(CyberDark)
            .border(1.dp, accent.copy(alpha = 0.35f), CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PhoneAndroid, null, tint = accent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                if (!subtitle1.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.search_device_cpu_prefix, subtitle1),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                if (!subtitle2.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.search_device_ram_prefix, subtitle2),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CyberAlertDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberBlack,
        tonalElevation = 6.dp,
        title = {
            Column(Modifier.fillMaxWidth()) {
                GlitchText(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberYellow
                    )
                )
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = CyberGray)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = CyberYellow, fontFamily = FontFamily.Monospace)
            }
        },
        text = content
    )
}
