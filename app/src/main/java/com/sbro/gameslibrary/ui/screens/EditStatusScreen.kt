package com.sbro.gameslibrary.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sbro.gameslibrary.R
import com.sbro.gameslibrary.components.EmulatorBuildType
import com.sbro.gameslibrary.components.Game
import com.sbro.gameslibrary.components.GameTestResult
import com.sbro.gameslibrary.components.IssueType
import com.sbro.gameslibrary.components.Reproducibility
import com.sbro.gameslibrary.components.WorkStatus
import com.sbro.gameslibrary.util.PhoneDbItem
import com.sbro.gameslibrary.util.loadPhonesFromAssets
import com.sbro.gameslibrary.viewmodel.GameDetailViewModel
import com.sbro.gameslibrary.viewmodel.MyDevicesState
import com.sbro.gameslibrary.viewmodel.MyDevicesViewModel
import kotlinx.coroutines.launch
import java.util.Locale

data class EditDialogResult(
    val status: WorkStatus,
    val testedAndroidVersion: String,
    val testedDeviceModel: String,
    val testedGpuModel: String,
    val testedDriverVersion: String,
    val testedRam: String,
    val testedWrapper: String,
    val testedPerformanceMode: String,
    val testedApp: String,
    val testedAppVersion: String,
    val testedGameVersionOrBuild: String,
    val issueType: IssueType,
    val reproducibility: Reproducibility,
    val workaround: String,
    val issueNote: String,
    val emulatorBuildType: EmulatorBuildType,
    val accuracyLevel: String,
    val resolutionScale: String,
    val asyncShaderEnabled: Boolean,
    val frameSkip: String,
    val resolutionWidth: String,
    val resolutionHeight: String,
    val fpsMin: String,
    val fpsMax: String,
    val mediaLink: String
)

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStatusScreen(
    viewModel: GameDetailViewModel,
    gameId: String,
    testMillis: Long? = null,
    onBack: () -> Unit,
    onTestSaved: () -> Unit
) {
    val context = LocalContext.current

    val devicesVm: MyDevicesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    LaunchedEffect(Unit) { devicesVm.init(context) }
    val devicesState by devicesVm.state.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(gameId) {
        viewModel.init(context, gameId)
    }

    val game by viewModel.game.collectAsState()

    if (game == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.dialog_edit_status_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                )
            }
        ) { pv ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pv),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.edit_status_game_not_found))
                }
            }
        }
        return
    }

    EditStatusContent(
        game = game!!,
        testMillis = testMillis,
        devicesState = devicesState,
        onBack = onBack,
        onSave = { result ->

            if (testMillis == null) {

                val payload = GameDetailViewModel.NewTestPayload(
                    newStatus = result.status,

                    testedAndroidVersion = result.testedAndroidVersion,
                    testedDeviceModel = result.testedDeviceModel,
                    testedGpuModel = result.testedGpuModel,
                    testedDriverVersion = result.testedDriverVersion,
                    testedRam = result.testedRam,
                    testedWrapper = result.testedWrapper,
                    testedPerformanceMode = result.testedPerformanceMode,

                    testedApp = result.testedApp,
                    testedAppVersion = result.testedAppVersion,
                    testedGameVersionOrBuild = result.testedGameVersionOrBuild,

                    issueType = result.issueType,
                    reproducibility = result.reproducibility,
                    workaround = result.workaround,
                    issueNote = result.issueNote,

                    emulatorBuildType = result.emulatorBuildType,
                    accuracyLevel = result.accuracyLevel,
                    resolutionScale = result.resolutionScale,
                    asyncShaderEnabled = result.asyncShaderEnabled,
                    frameSkip = result.frameSkip,

                    resolutionWidth = result.resolutionWidth,
                    resolutionHeight = result.resolutionHeight,
                    fpsMin = result.fpsMin,
                    fpsMax = result.fpsMax,

                    mediaLink = result.mediaLink
                )

                viewModel.updateGameStatus(
                    context = context,
                    gameId = gameId,
                    payload = payload
                )
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_status_saved),
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                val updated = GameTestResult(
                    status = result.status,
                    testedAndroidVersion = result.testedAndroidVersion,
                    testedDeviceModel = result.testedDeviceModel,
                    testedGpuModel = result.testedGpuModel,
                    testedDriverVersion = result.testedDriverVersion,
                    testedRam = result.testedRam,
                    testedWrapper = result.testedWrapper,
                    testedPerformanceMode = result.testedPerformanceMode,
                    testedApp = result.testedApp,
                    testedAppVersion = result.testedAppVersion,
                    testedGameVersionOrBuild = result.testedGameVersionOrBuild,
                    issueType = result.issueType,
                    reproducibility = result.reproducibility,
                    workaround = result.workaround,
                    issueNote = result.issueNote,
                    emulatorBuildType = result.emulatorBuildType,
                    accuracyLevel = result.accuracyLevel,
                    resolutionScale = result.resolutionScale,
                    asyncShaderEnabled = result.asyncShaderEnabled,
                    frameSkip = result.frameSkip,
                    resolutionWidth = result.resolutionWidth,
                    resolutionHeight = result.resolutionHeight,
                    fpsMin = result.fpsMin,
                    fpsMax = result.fpsMax,
                    mediaLink = result.mediaLink,
                    testedDateFormatted = "",
                    updatedAtMillis = testMillis
                )

                viewModel.editTestResult(
                    context = context,
                    gameId = gameId,
                    testMillis = testMillis,
                    newResult = updated
                )
                Toast.makeText(
                    context,
                    context.getString(R.string.test_updated),
                    Toast.LENGTH_SHORT
                ).show()
            }

            onTestSaved()
            onBack()
        }
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditStatusContent(
    game: Game,
    testMillis: Long?,
    devicesState: MyDevicesState,
    onBack: () -> Unit,
    onSave: (EditDialogResult) -> Unit
) {
    val context = LocalContext.current
    var phoneDb by remember { mutableStateOf<List<PhoneDbItem>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        phoneDb = loadPhonesFromAssets(context)
    }

    var currentStatus by remember { mutableStateOf(WorkStatus.UNTESTED) }
    val otherLabel = stringResource(R.string.option_other)
    val customLabel = stringResource(R.string.option_custom)
    var driverVersionText by remember { mutableStateOf("") }
    val platformLower = game.platform.lowercase()
    val isSwitchPlatform = platformLower.contains("switch") || platformLower.contains("nintendo")
    val isPcPlatform = platformLower.contains("pc") || platformLower.contains("windows")

    val appOptions = when {
        isSwitchPlatform -> listOf("Yuzu", "Eden", "Citron", "Torzu", "Sumi", "Sudachi", "Strato")
        isPcPlatform -> listOf("Winlator", "GameHub")
        else -> listOf("RPCSX-UI-Android", "aPS3e")
    }

    val androidMajor = remember {
        Build.VERSION.RELEASE?.substringBefore(".") ?: ""
    }

    var androidVersionSelected by remember { mutableStateOf(androidMajor) }
    var androidVersionCustom by remember { mutableStateOf(if (androidMajor.isBlank()) androidMajor else "") }

    var deviceModelText by remember { mutableStateOf("") }
    var gpuModelText by remember { mutableStateOf("") }

    var ramSelected by remember { mutableStateOf("") }
    var ramCustom by remember { mutableStateOf("") }

    var wrapperSelected by remember { mutableStateOf("") }
    var wrapperCustom by remember { mutableStateOf("") }

    var perfModeSelected by remember { mutableStateOf("") }
    var perfModeCustom by remember { mutableStateOf("") }

    var selectedApp by remember { mutableStateOf(appOptions.first()) }
    var appVersionText by remember { mutableStateOf("") }
    var gameVersionText by remember { mutableStateOf("") }

    var selectedIssueType by remember { mutableStateOf(IssueType.CRASH) }
    var selectedRepro by remember { mutableStateOf(Reproducibility.ALWAYS) }

    var workaroundText by remember { mutableStateOf("") }
    var issueNoteText by remember { mutableStateOf("") }

    var selectedEmuBuild by remember { mutableStateOf(EmulatorBuildType.STABLE) }

    var accuracySelected by remember { mutableStateOf("") }
    var accuracyCustom by remember { mutableStateOf("") }

    var scaleSelected by remember { mutableStateOf("") }
    var scaleCustom by remember { mutableStateOf("") }

    var asyncShaderEnabled by remember { mutableStateOf(false) }

    var frameSkipSelected by remember { mutableStateOf("") }
    var frameSkipCustom by remember { mutableStateOf("") }

    val resolutionPresets = listOf(
        "640×360", "854×480", "960×540", "1280×720", "1600×900",
        "1920×1080", "2340×1080", "2400×1080", customLabel
    )
    var resolutionPresetSelected by remember { mutableStateOf("") }

    var resW by remember { mutableStateOf("") }
    var resH by remember { mutableStateOf("") }

    var fpsFrom by remember { mutableStateOf("") }
    var fpsTo by remember { mutableStateOf("") }

    var mediaLinkText by remember { mutableStateOf("") }

    val androidVersions = listOf("16", "15", "14", "13", "12", "11", otherLabel)
    val ramOptions = listOf("4 GB", "6 GB", "8 GB", "12 GB", "16 GB", "24 GB", otherLabel)

    val wrapperOptions = when {
        isPcPlatform -> listOf("DXVK", "VKD3D", "WineD3D", "OpenGL", otherLabel)
        else -> listOf("Vulkan", "OpenGL", "D3D wrapper", otherLabel)
    }

    val perfModeOptions = listOf("Extreme performance", "Performance", "Balanced", "Quality", otherLabel)
    val accuracyOptions = listOf("Performance", "Balanced", "Accuracy", otherLabel)
    val scaleOptions = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x", "2.25x", "2.5x", "2.75x", "3x", otherLabel)
    val frameSkipOptions = listOf("0", "1", "2", otherLabel)

    val showEmulatorSettings = !isPcPlatform
    val showWrapper = true
    val showPerfMode = true

    val androidVersionFinal =
        if (androidVersionSelected == otherLabel) androidVersionCustom.trim()
        else androidVersionSelected.trim()

    val ramFinal =
        if (ramSelected == otherLabel) ramCustom.trim()
        else ramSelected.trim()

    val wrapperFinal =
        if (wrapperSelected == otherLabel) wrapperCustom.trim()
        else wrapperSelected.trim()

    val perfModeFinal =
        if (perfModeSelected == otherLabel) perfModeCustom.trim()
        else perfModeSelected.trim()

    val accuracyFinal =
        if (accuracySelected == otherLabel) accuracyCustom.trim()
        else accuracySelected.trim()

    val scaleFinal =
        if (scaleSelected == otherLabel) scaleCustom.trim()
        else scaleSelected.trim()

    val frameSkipFinal =
        if (frameSkipSelected == otherLabel) frameSkipCustom.trim()
        else frameSkipSelected.trim()

    var didPrefill by remember(testMillis) { mutableStateOf(false) }

    LaunchedEffect(testMillis, game.testResults) {
        if (testMillis == null || didPrefill) return@LaunchedEffect

        val canonicalId = "${game.id}_${testMillis}"
        val t = game.testResults.firstOrNull { it.updatedAtMillis == testMillis }
            ?: game.testResults.firstOrNull { it.testId == canonicalId }

        if (t != null) {
            didPrefill = true

            currentStatus = t.status

            androidVersionSelected =
                if (androidVersions.contains(t.testedAndroidVersion)) t.testedAndroidVersion else otherLabel
            androidVersionCustom =
                if (androidVersionSelected == otherLabel) t.testedAndroidVersion else ""

            deviceModelText = t.testedDeviceModel
            gpuModelText = t.testedGpuModel
            driverVersionText = t.testedDriverVersion

            ramSelected =
                if (ramOptions.contains(t.testedRam)) t.testedRam else otherLabel
            ramCustom =
                if (ramSelected == otherLabel) t.testedRam else ""

            wrapperSelected =
                if (wrapperOptions.contains(t.testedWrapper)) t.testedWrapper else otherLabel
            wrapperCustom =
                if (wrapperSelected == otherLabel) t.testedWrapper else ""

            perfModeSelected =
                if (perfModeOptions.contains(t.testedPerformanceMode)) t.testedPerformanceMode else otherLabel
            perfModeCustom =
                if (perfModeSelected == otherLabel) t.testedPerformanceMode else ""

            selectedApp =
                if (appOptions.contains(t.testedApp)) t.testedApp else appOptions.first()
            appVersionText = t.testedAppVersion
            gameVersionText = t.testedGameVersionOrBuild

            selectedIssueType = t.issueType
            selectedRepro = t.reproducibility
            workaroundText = t.workaround
            issueNoteText = t.issueNote

            selectedEmuBuild = t.emulatorBuildType
            accuracySelected =
                if (accuracyOptions.contains(t.accuracyLevel)) t.accuracyLevel else otherLabel
            accuracyCustom =
                if (accuracySelected == otherLabel) t.accuracyLevel else ""

            scaleSelected =
                if (scaleOptions.contains(t.resolutionScale)) t.resolutionScale else otherLabel
            scaleCustom =
                if (scaleSelected == otherLabel) t.resolutionScale else ""

            asyncShaderEnabled = t.asyncShaderEnabled

            frameSkipSelected =
                if (frameSkipOptions.contains(t.frameSkip)) t.frameSkip else otherLabel
            frameSkipCustom =
                if (frameSkipSelected == otherLabel) t.frameSkip else ""

            resW = t.resolutionWidth
            resH = t.resolutionHeight
            fpsFrom = t.fpsMin
            fpsTo = t.fpsMax

            mediaLinkText = t.mediaLink
        }
    }

    LaunchedEffect(resolutionPresetSelected) {
        if (resolutionPresetSelected.isNotBlank() &&
            resolutionPresetSelected != customLabel
        ) {
            val parts = resolutionPresetSelected.split("×")
            if (parts.size == 2) {
                resW = parts[0]
                resH = parts[1]
            }
        }
    }

    val isFormValid by remember(
        currentStatus,
        androidVersionFinal,
        deviceModelText, gpuModelText, driverVersionText, ramFinal,
        wrapperFinal, perfModeFinal,
        selectedApp, appVersionText, gameVersionText,
        accuracyFinal, scaleFinal, frameSkipFinal,
        resW, resH, fpsFrom, fpsTo,
        issueNoteText
    ) {
        derivedStateOf {
            val baseValid =
                androidVersionFinal.isNotEmpty() &&
                        deviceModelText.trim().isNotEmpty() &&
                        gpuModelText.trim().isNotEmpty() &&
                        driverVersionText.trim().isNotEmpty() &&
                        ramFinal.isNotEmpty() &&
                        wrapperFinal.isNotEmpty() &&
                        perfModeFinal.isNotEmpty() &&
                        selectedApp.trim().isNotEmpty() &&
                        appVersionText.trim().isNotEmpty() &&
                        gameVersionText.trim().isNotEmpty() &&
                        resW.trim().isNotEmpty() &&
                        resH.trim().isNotEmpty() &&
                        fpsFrom.trim().isNotEmpty() &&
                        fpsTo.trim().isNotEmpty()

            val issueValid =
                currentStatus != WorkStatus.NOT_WORKING || issueNoteText.trim().isNotEmpty()

            val emuValid =
                !showEmulatorSettings || (
                        accuracyFinal.isNotEmpty() &&
                                scaleFinal.isNotEmpty() &&
                                frameSkipFinal.isNotEmpty()
                        )

            baseValid && issueValid && emuValid
        }
    }

    val configuration = LocalConfiguration.current
    val maxHeight = configuration.screenHeightDp.dp * 0.95f
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scrollState = rememberScrollState()

    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var showMyDevicesDialog by remember { mutableStateOf(false) }

    val filteredPhones = remember(phoneDb, searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        if (q.length < 2) emptyList()
        else phoneDb.asSequence()
            .filter { it.name.orEmpty().lowercase(Locale.getDefault()).contains(q) }
            .take(40)
            .toList()
    }

    fun applyPhoneSpec(spec: PhoneDbItem) {
        val name = spec.name.orEmpty().trim()
        val cpu = spec.cpu.orEmpty().trim()
        val ram = spec.ram.orEmpty().trim()

        if (name.isNotBlank()) deviceModelText = name
        if (cpu.isNotBlank()) gpuModelText = cpu

        if (ram.isNotBlank()) {
            if (ramOptions.contains(ram)) {
                ramSelected = ram
                ramCustom = ""
            } else {
                ramSelected = otherLabel
                ramCustom = ram
            }
        }
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
                    Spacer(Modifier.padding(top = 6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_device_input_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.padding(top = 10.dp))

                    if (searchQuery.trim().length < 2) {
                        Text(
                            text = stringResource(R.string.search_device_start_typing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        )
                    } else if (filteredPhones.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_device_no_matches),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                        ) {
                            items(filteredPhones) { spec ->
                                val safeName = spec.name.orEmpty()
                                val safeCpu = spec.cpu.orEmpty()
                                val safeRam = spec.ram.orEmpty()

                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 5.dp)
                                        .clickable {
                                            applyPhoneSpec(spec)
                                            searchQuery = safeName
                                            showSearchDialog = false
                                        }
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PhoneAndroid,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = safeName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(Modifier.padding(top = 3.dp))
                                            if (safeCpu.isNotBlank()) {
                                                Text(
                                                    text = stringResource(R.string.search_device_cpu_prefix, safeCpu),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (safeRam.isNotBlank()) {
                                                Text(
                                                    text = stringResource(R.string.search_device_ram_prefix, safeRam),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

    if (showMyDevicesDialog) {
        AlertDialog(
            onDismissRequest = { showMyDevicesDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.pick_from_my_devices),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            confirmButton = {
                TextButton(onClick = { showMyDevicesDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(devicesState.devices) { d ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable {
                                    applyPhoneSpec(d)
                                    showMyDevicesDialog = false
                                }
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.PhoneAndroid, null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(d.name.orEmpty(), fontWeight = FontWeight.SemiBold)
                                    if (!d.cpu.isNullOrBlank()) {
                                        Text(
                                            text = stringResource(
                                                R.string.search_device_cpu_prefix,
                                                d.cpu
                                            ),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (!d.ram.isNullOrBlank()) {
                                        Text(
                                            text = stringResource(
                                                R.string.search_device_ram_prefix,
                                                d.ram
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
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.dialog_edit_status_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(pv)
                .heightIn(max = maxHeight)
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            Text(
                text = stringResource(R.string.dialog_edit_status_description, game.title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            ModernSectionCard(title = null) {
                WorkStatusRadioGroup(
                    selected = currentStatus,
                    onSelectedChange = { currentStatus = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ModernSectionCard(title = stringResource(R.string.section_device_env)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            searchQuery = ""
                            showSearchDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.search_in_db_button)) }

                    Button(
                        onClick = { showMyDevicesDialog = true },
                        enabled = devicesState.isLoggedIn && devicesState.devices.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.pick_from_my_devices)) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                BottomSheetDropdownWithCustom(
                    label = stringResource(R.string.label_android_version),
                    options = androidVersions,
                    selected = androidVersionSelected,
                    onSelectedChange = { androidVersionSelected = it },
                    customValue = androidVersionCustom,
                    onCustomChange = { androidVersionCustom = it },
                    customPlaceholder = stringResource(R.string.label_android_version),
                    otherLabel = otherLabel
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = deviceModelText,
                    onValueChange = { deviceModelText = it },
                    label = { Text(stringResource(R.string.label_device_model)) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = gpuModelText,
                    onValueChange = { gpuModelText = it },
                    label = { Text(stringResource(R.string.label_gpu_model)) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = driverVersionText,
                    onValueChange = { driverVersionText = it },
                    label = { Text(stringResource(R.string.label_driver_version)) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                BottomSheetDropdownWithCustom(
                    label = stringResource(R.string.label_ram),
                    options = ramOptions,
                    selected = ramSelected,
                    onSelectedChange = { ramSelected = it },
                    customValue = ramCustom,
                    onCustomChange = { ramCustom = it },
                    customPlaceholder = stringResource(R.string.label_ram),
                    otherLabel = otherLabel
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (showWrapper) {
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_wrapper),
                        options = wrapperOptions,
                        selected = wrapperSelected,
                        onSelectedChange = { wrapperSelected = it },
                        customValue = wrapperCustom,
                        onCustomChange = { wrapperCustom = it },
                        customPlaceholder = stringResource(R.string.label_wrapper),
                        otherLabel = otherLabel
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (showPerfMode) {
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_performance_mode),
                        options = perfModeOptions,
                        selected = perfModeSelected,
                        onSelectedChange = { perfModeSelected = it },
                        customValue = perfModeCustom,
                        onCustomChange = { perfModeCustom = it },
                        customPlaceholder = stringResource(R.string.label_performance_mode),
                        otherLabel = otherLabel
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ModernSectionCard(title = stringResource(R.string.section_app_game)) {

                BottomSheetSelectorField(
                    label = stringResource(R.string.dialog_edit_status_app_label),
                    value = selectedApp,
                    options = appOptions,
                    onSelect = { selectedApp = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = appVersionText,
                    onValueChange = { appVersionText = it },
                    label = { Text(stringResource(R.string.dialog_edit_status_app_version_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = gameVersionText,
                    onValueChange = { gameVersionText = it },
                    label = { Text(stringResource(R.string.label_game_version_build)) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (currentStatus == WorkStatus.NOT_WORKING) {
                Spacer(modifier = Modifier.height(12.dp))

                ModernSectionCard(title = stringResource(R.string.section_issue_details)) {

                    BottomSheetEnumSelectorField(
                        label = stringResource(R.string.label_issue_type),
                        current = selectedIssueType,
                        items = IssueType.entries,
                        labelOf = { stringResource(issueTypeToLabel(it)) },
                        onSelect = { selectedIssueType = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    BottomSheetEnumSelectorField(
                        label = stringResource(R.string.label_reproducibility),
                        current = selectedRepro,
                        items = Reproducibility.entries,
                        labelOf = { stringResource(reproToLabel(it)) },
                        onSelect = { selectedRepro = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = workaroundText,
                        onValueChange = { workaroundText = it },
                        label = { Text(stringResource(R.string.label_workaround)) },
                        minLines = 1,
                        maxLines = 2,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = issueNoteText,
                        onValueChange = { issueNoteText = it },
                        label = { Text(stringResource(R.string.dialog_issue_note_label)) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                if (issueNoteText.isNotBlank()) issueNoteText = ""
                if (workaroundText.isNotBlank()) workaroundText = ""
            }

            if (showEmulatorSettings) {
                Spacer(modifier = Modifier.height(12.dp))

                ModernSectionCard(title = stringResource(R.string.section_emulator_settings)) {

                    BottomSheetEnumSelectorField(
                        label = stringResource(R.string.label_emulator_build_type),
                        current = selectedEmuBuild,
                        items = EmulatorBuildType.entries,
                        labelOf = { stringResource(emuBuildToLabel(it)) },
                        onSelect = { selectedEmuBuild = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_accuracy_level),
                        options = accuracyOptions,
                        selected = accuracySelected,
                        onSelectedChange = { accuracySelected = it },
                        customValue = accuracyCustom,
                        onCustomChange = { accuracyCustom = it },
                        customPlaceholder = stringResource(R.string.label_accuracy_level),
                        otherLabel = otherLabel
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_resolution_scale),
                        options = scaleOptions,
                        selected = scaleSelected,
                        onSelectedChange = { scaleSelected = it },
                        customValue = scaleCustom,
                        onCustomChange = { scaleCustom = it },
                        customPlaceholder = stringResource(R.string.label_resolution_scale),
                        otherLabel = otherLabel
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked = asyncShaderEnabled,
                            onCheckedChange = { asyncShaderEnabled = it }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.label_async_shader),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_frame_skip),
                        options = frameSkipOptions,
                        selected = frameSkipSelected,
                        onSelectedChange = { frameSkipSelected = it },
                        customValue = frameSkipCustom,
                        onCustomChange = { frameSkipCustom = it },
                        customPlaceholder = stringResource(R.string.label_frame_skip),
                        otherLabel = otherLabel
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ModernSectionCard(title = stringResource(R.string.section_result_metrics)) {

                BottomSheetSelectorField(
                    label = stringResource(R.string.dialog_edit_status_resolution_label),
                    value = resolutionPresetSelected,
                    options = resolutionPresets,
                    onSelect = { resolutionPresetSelected = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = resW,
                        onValueChange = { resW = it },
                        label = { Text(stringResource(R.string.resolution_width_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "×", style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = resH,
                        onValueChange = { resH = it },
                        label = { Text(stringResource(R.string.resolution_height_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.dialog_edit_status_fps_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = fpsFrom,
                        onValueChange = { fpsFrom = it },
                        label = { Text(stringResource(R.string.fps_min_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text(text = "–", style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = fpsTo,
                        onValueChange = { fpsTo = it },
                        label = { Text(stringResource(R.string.fps_max_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ModernSectionCard(title = stringResource(R.string.section_media)) {

                OutlinedTextField(
                    value = mediaLinkText,
                    onValueChange = { mediaLinkText = it },
                    label = { Text(stringResource(R.string.label_media_link)) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusEvent { state ->
                            if (state.isFocused) {
                                scope.launch { bringIntoViewRequester.bringIntoView() }
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isFormValid) {
                Text(
                    text = stringResource(R.string.dialog_fill_all_fields),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.button_cancel))
                }

                TextButton(
                    enabled = isFormValid,
                    onClick = {
                        onSave(
                            EditDialogResult(
                                status = currentStatus,

                                testedAndroidVersion = androidVersionFinal,
                                testedDeviceModel = deviceModelText.trim(),
                                testedGpuModel = gpuModelText.trim(),
                                testedDriverVersion = driverVersionText.trim(),
                                testedRam = ramFinal,
                                testedWrapper = wrapperFinal,
                                testedPerformanceMode = perfModeFinal,

                                testedApp = selectedApp.trim(),
                                testedAppVersion = appVersionText.trim(),
                                testedGameVersionOrBuild = gameVersionText.trim(),

                                issueType = selectedIssueType,
                                reproducibility = selectedRepro,
                                workaround = workaroundText.trim(),
                                issueNote = issueNoteText.trim(),

                                emulatorBuildType = selectedEmuBuild,
                                accuracyLevel = if (showEmulatorSettings) accuracyFinal else "",
                                resolutionScale = if (showEmulatorSettings) scaleFinal else "",
                                asyncShaderEnabled = if (showEmulatorSettings) asyncShaderEnabled else false,
                                frameSkip = if (showEmulatorSettings) frameSkipFinal else "",

                                resolutionWidth = resW.trim(),
                                resolutionHeight = resH.trim(),
                                fpsMin = fpsFrom.trim(),
                                fpsMax = fpsTo.trim(),

                                mediaLink = mediaLinkText.trim()
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.button_save))
                }
            }
        }
    }
}
@Composable
private fun ModernSectionCard(
    title: String?,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetSelectorField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    SelectorTextField(
        label = label,
        value = value,
        onClick = { showSheet = true }
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            BottomSheetTitle(label)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                items(options) { opt ->
                    BottomSheetOptionRow(
                        text = opt,
                        selected = opt == value,
                        onClick = {
                            onSelect(opt)
                            showSheet = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> BottomSheetEnumSelectorField(
    label: String,
    current: T,
    items: List<T>,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentLabel = labelOf(current)

    SelectorTextField(
        label = label,
        value = currentLabel,
        onClick = { showSheet = true }
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            BottomSheetTitle(label)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                items(items) { item ->
                    val text = labelOf(item)
                    BottomSheetOptionRow(
                        text = text,
                        selected = item == current,
                        onClick = {
                            onSelect(item)
                            showSheet = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetDropdownWithCustom(
    label: String,
    options: List<String>,
    selected: String,
    onSelectedChange: (String) -> Unit,
    customValue: String,
    onCustomChange: (String) -> Unit,
    customPlaceholder: String,
    otherLabel: String
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    SelectorTextField(
        label = label,
        value = selected,
        onClick = { showSheet = true }
    )

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            BottomSheetTitle(label)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                items(options) { opt ->
                    BottomSheetOptionRow(
                        text = opt,
                        selected = opt == selected,
                        onClick = {
                            onSelectedChange(opt)
                            showSheet = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (selected == otherLabel) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = customValue,
            onValueChange = onCustomChange,
            label = { Text(customPlaceholder) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SelectorTextField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun BottomSheetTitle(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun BottomSheetOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceContainerLow

    val contentColor = if (selected)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = contentColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                color = contentColor
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }
    }
}


fun issueTypeToLabel(type: IssueType): Int = when (type) {
    IssueType.CRASH -> R.string.issue_type_crash
    IssueType.BLACK_SCREEN -> R.string.issue_type_black_screen
    IssueType.SOFTLOCK -> R.string.issue_type_softlock
    IssueType.GRAPHICS_GLITCHES -> R.string.issue_type_graphics
    IssueType.AUDIO_ISSUES -> R.string.issue_type_audio
    IssueType.CONTROLS_NOT_WORKING -> R.string.issue_type_controls
    IssueType.SLOW_PERFORMANCE -> R.string.issue_type_performance
}

fun reproToLabel(r: Reproducibility): Int = when (r) {
    Reproducibility.ALWAYS -> R.string.repro_always
    Reproducibility.OFTEN -> R.string.repro_often
    Reproducibility.RARE -> R.string.repro_rare
    Reproducibility.ONCE -> R.string.repro_once
}

fun emuBuildToLabel(e: EmulatorBuildType): Int = when (e) {
    EmulatorBuildType.STABLE -> R.string.emu_build_stable
    EmulatorBuildType.CANARY -> R.string.emu_build_canary
    EmulatorBuildType.GIT_HASH -> R.string.emu_build_git
}

@Composable
fun WorkStatusRadioGroup(
    selected: WorkStatus,
    onSelectedChange: (WorkStatus) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        WorkStatusRadioRow(
            text = stringResource(id = R.string.work_status_working),
            isSelected = selected == WorkStatus.WORKING,
            onClick = { onSelectedChange(WorkStatus.WORKING) }
        )

        WorkStatusRadioRow(
            text = stringResource(id = R.string.work_status_untested),
            isSelected = selected == WorkStatus.UNTESTED,
            onClick = { onSelectedChange(WorkStatus.UNTESTED) }
        )

        WorkStatusRadioRow(
            text = stringResource(id = R.string.work_status_not_working),
            isSelected = selected == WorkStatus.NOT_WORKING,
            onClick = { onSelectedChange(WorkStatus.NOT_WORKING) }
        )
    }
}

@Composable
private fun WorkStatusRadioRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton,
                interactionSource = interaction,
                indication = null
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
