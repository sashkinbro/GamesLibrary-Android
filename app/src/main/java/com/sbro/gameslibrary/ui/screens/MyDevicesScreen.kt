package com.sbro.gameslibrary.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.viewmodel.MyDevicesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDevicesScreen(
    viewModel: MyDevicesViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(Unit) { viewModel.init(context) }
    val state by viewModel.state.collectAsState()

    val lastClickTime = remember { mutableLongStateOf(0L) }
    fun safeClick(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime.longValue < 400L) return
        lastClickTime.longValue = now
        action()
    }

    val background = Brush.verticalGradient(listOf(cs.background, cs.surfaceContainer))

    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredPhones = remember(searchQuery, state.isLoading) {
        viewModel.searchPhones(searchQuery)
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.search_device_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = cs.outlineVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_device_input_hint)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    if (searchQuery.trim().length < 2) {
                        Text(
                            text = stringResource(R.string.search_device_start_typing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                    } else if (filteredPhones.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_device_no_matches),
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                        ) {
                            items(filteredPhones) { spec ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            viewModel.addDevice(spec)
                                            showSearchDialog = false
                                        }
                                ) {
                                    Row(
                                        Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.PhoneAndroid, null)
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                spec.name.orEmpty(),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (!spec.cpu.isNullOrBlank()) {
                                                Text(
                                                    text = stringResource(
                                                        R.string.search_device_cpu_prefix,
                                                        spec.cpu
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            if (!spec.ram.isNullOrBlank()) {
                                                Text(
                                                    text = stringResource(
                                                        R.string.search_device_ram_prefix,
                                                        spec.ram
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.my_devices_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { safeClick(onBack) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                )
                HorizontalDivider(color = cs.outline.copy(alpha = 0.4f))
            }
        }
    ) { pv ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pv)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (!state.isLoggedIn) {
                    Text(
                        text = stringResource(R.string.my_devices_need_login),
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                    return@Column
                }

                Button(
                    onClick = { safeClick { showSearchDialog = true; searchQuery = "" } },
                    enabled = state.devices.size < 5,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.my_devices_add_from_db))
                }

                Spacer(Modifier.height(12.dp))

                if (state.devices.isEmpty()) {
                    Text(
                        text = stringResource(R.string.my_devices_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.devices) { d ->
                            ElevatedCard(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = cs.surfaceContainerHigh
                                ),
                                elevation = CardDefaults.elevatedCardElevation(3.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.PhoneAndroid,
                                        null,
                                        tint = cs.primary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = d.name.orEmpty(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (!d.cpu.isNullOrBlank()) {
                                            Text(
                                                text = stringResource(
                                                    R.string.search_device_cpu_prefix,
                                                    d.cpu
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = cs.onSurfaceVariant
                                            )
                                        }
                                        if (!d.ram.isNullOrBlank()) {
                                            Text(
                                                text = stringResource(
                                                    R.string.search_device_ram_prefix,
                                                    d.ram
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = cs.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { safeClick { viewModel.removeDevice(d.id) } }
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
