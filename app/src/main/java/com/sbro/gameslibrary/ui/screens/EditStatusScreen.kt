package com.sbro.gameslibrary.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Save
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
import com.sbro.gameslibrary.util.PresetRepository
import com.sbro.gameslibrary.util.TestPreset
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
    val mediaLink: String,
    val audioDriver: String,
    val downloadSize: String,
    val wineVersion: String,
    val winlatorFork: String,
    val turnipVersion: String,
    val controllerSupport: String,
    val box64Preset: String,
    val box64Version: String,
    val startupSelection: String,
    val envVariables: String,
    val vkd3dVersion: String,
    val dxvkVersion: String,
    val anisotropicFilter: String,
    val antiAliasing: String,
    val vSync: String,
    val windowAdaptingFilter: String,
    val spuThreads: String,
    val spuBlockSize: String,
    val dockedMode: Boolean,
    val audioOutputEngine: String,
    val diskShaderCache: Boolean,
    val reactiveFlushing: Boolean,
    val cpuBackend: String
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

    val currentUser by viewModel.currentUser.collectAsState()
    val isLoggedIn = currentUser != null

    LaunchedEffect(gameId) {
        viewModel.init(context, gameId)
    }

    val game by viewModel.game.collectAsState()

    if (game == null) {
        Scaffold( contentWindowInsets = WindowInsets(0),
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
        isLoggedIn = isLoggedIn,
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

                    mediaLink = result.mediaLink,

                    audioDriver = result.audioDriver,
                    downloadSize = result.downloadSize,
                    wineVersion = result.wineVersion,
                    winlatorFork = result.winlatorFork,
                    turnipVersion = result.turnipVersion,
                    controllerSupport = result.controllerSupport,
                    box64Preset = result.box64Preset,
                    box64Version = result.box64Version,
                    startupSelection = result.startupSelection,
                    envVariables = result.envVariables,
                    vkd3dVersion = result.vkd3dVersion,
                    dxvkVersion = result.dxvkVersion,
                    anisotropicFilter = result.anisotropicFilter,
                    antiAliasing = result.antiAliasing,
                    vSync = result.vSync,
                    windowAdaptingFilter = result.windowAdaptingFilter,
                    spuThreads = result.spuThreads,
                    spuBlockSize = result.spuBlockSize,
                    dockedMode = result.dockedMode,
                    audioOutputEngine = result.audioOutputEngine,
                    diskShaderCache = result.diskShaderCache,
                    reactiveFlushing = result.reactiveFlushing,
                    cpuBackend = result.cpuBackend
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
                    updatedAtMillis = testMillis,
                    audioDriver = result.audioDriver,
                    downloadSize = result.downloadSize,
                    wineVersion = result.wineVersion,
                    winlatorFork = result.winlatorFork,
                    turnipVersion = result.turnipVersion,
                    controllerSupport = result.controllerSupport,
                    box64Preset = result.box64Preset,
                    box64Version = result.box64Version,
                    startupSelection = result.startupSelection,
                    envVariables = result.envVariables,
                    vkd3dVersion = result.vkd3dVersion,
                    dxvkVersion = result.dxvkVersion,
                    anisotropicFilter = result.anisotropicFilter,
                    antiAliasing = result.antiAliasing,
                    vSync = result.vSync,
                    windowAdaptingFilter = result.windowAdaptingFilter,
                    spuThreads = result.spuThreads,
                    spuBlockSize = result.spuBlockSize,
                    dockedMode = result.dockedMode,
                    audioOutputEngine = result.audioOutputEngine,
                    diskShaderCache = result.diskShaderCache,
                    reactiveFlushing = result.reactiveFlushing,
                    cpuBackend = result.cpuBackend
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
    isLoggedIn: Boolean,
    onBack: () -> Unit,
    onSave: (EditDialogResult) -> Unit
) {
    val context = LocalContext.current
    var phoneDb by remember { mutableStateOf<List<PhoneDbItem>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val presetRepo = remember { PresetRepository(context) }

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
    val isPs3Platform = platformLower.contains("playstation") || platformLower.contains("ps3")

    val appOptions = when {
        isSwitchPlatform -> listOf("Yuzu", "Eden", "Citron", "Torzu", "Sumi", "Sudachi", "Strato")
        isPcPlatform -> listOf("Winlator", "GameHub", "Mobox", "Exagear")
        else -> listOf("RPCSX-UI-Android", "aPS3e")
    }
    val androidVersions = listOf("16", "15", "14", "13", "12", "11", otherLabel)
    val ramOptions = listOf("4 GB", "6 GB", "8 GB", "12 GB", "16 GB", "24 GB", otherLabel)
    val turnipOptions = listOf(
        "v26.0.0 R7",
        "v26.0.0 R6",
        "v26.0.0 R5",
        "v25.3.0 (new autotuner)",
        "v25.2.0",
        "v25.0.0",
        "System Default",
        otherLabel
    )
    val audioDriverOptions = listOf("PulseAudio", "Alsa", "AAudio", "OpenSL ES", "Oboe", otherLabel)
    val winlatorForkOptions = listOf("Official", "Glibc", "Frost", "CJ", "Afei", "Mod by Ajay", otherLabel)
    val wineVersionOptions = listOf(
        "Wine 11.0-rc4",
        "Wine 11.0-rc2",
        "Wine 11.0-rc1",
        "Wine 10.15",
        "Wine 10.0",
        "Wine 9.0",
        "Wine 8.0",
        "Proton GE 10-28",
        otherLabel
    )
    val box64PresetOptions = listOf("Performance", "Compatible", "Stability", "Safe", "Aggressive", otherLabel)
    val box64VersionOptions = listOf(
        "0.4.0",
        "0.3.8",
        "0.3.6",
        "0.3.2",
        "git-latest",
        otherLabel
    )
    val vkd3dOptions = listOf(
        "3.0b",
        "3.0a",
        "3.0",
        "2.13",
        "2.12",
        "2.11.1",
        otherLabel
    )
    val dxvkOptions = listOf(
        "2.7.1",
        "2.7",
        "2.6.2",
        "2.6",
        "2.5.3",
        "2.5",
        "2.4.1",
        "2.4",
        "1.10.3",
        otherLabel
    )
    val controllerOptions = listOf("Native (XInput)", "Virtual", "Keyboard", "Touchscreen", "External Controller", otherLabel)
    val vsyncOptions = listOf("Off", "On", "Mailbox", "FIFO", "Immediate", otherLabel)
    val anisotropicOptions = listOf("Off", "2x", "4x", "8x", "16x", "Automatic", otherLabel)
    val aaOptions = listOf("Off", "FXAA", "SMAA", "MSAA 2x", "MSAA 4x", "TAA", otherLabel)
    val cpuBackendOptions = listOf("NCE", "JIT", "Interpreter", otherLabel)
    val audioEngineOptions = listOf("auto", "cubeb", "sdl2", "openal", otherLabel)
    val windowFilterOptions = listOf("Bilinear", "Bicubic", "Gaussian", "ScaleForce", "AMD FSR", "Nearest", otherLabel)

    val wrapperOptions = when {
        isPcPlatform -> listOf("DXVK", "VKD3D", "WineD3D", "OpenGL", otherLabel)
        else -> listOf("Vulkan", "OpenGL", "D3D wrapper", otherLabel)
    }

    val perfModeOptions = listOf("Extreme performance", "Performance", "Balanced", "Quality", otherLabel)
    val accuracyOptions = listOf("Performance", "Balanced", "Accuracy", otherLabel)
    val scaleOptions = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x", "2.25x", "2.5x", "2.75x", "3x", otherLabel)
    val frameSkipOptions = listOf("0", "1", "2", otherLabel)

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
    var audioDriverSelected by remember { mutableStateOf("") }
    var audioDriverCustom by remember { mutableStateOf("") }

    var downloadSizeText by remember { mutableStateOf("") }

    var wineVersionSelected by remember { mutableStateOf("") }
    var wineVersionCustom by remember { mutableStateOf("") }

    var winlatorForkSelected by remember { mutableStateOf("") }
    var winlatorForkCustom by remember { mutableStateOf("") }

    var turnipVersionSelected by remember { mutableStateOf("") }
    var turnipVersionCustom by remember { mutableStateOf("") }

    var controllerSupportSelected by remember { mutableStateOf("") }
    var controllerSupportCustom by remember { mutableStateOf("") }

    var box64PresetSelected by remember { mutableStateOf("") }
    var box64PresetCustom by remember { mutableStateOf("") }

    var box64VersionSelected by remember { mutableStateOf("") }
    var box64VersionCustom by remember { mutableStateOf("") }

    var startupSelectionText by remember { mutableStateOf("") }
    var envVariablesText by remember { mutableStateOf("") }

    var vkd3dVersionSelected by remember { mutableStateOf("") }
    var vkd3dVersionCustom by remember { mutableStateOf("") }

    var dxvkVersionSelected by remember { mutableStateOf("") }
    var dxvkVersionCustom by remember { mutableStateOf("") }

    var anisotropicFilterSelected by remember { mutableStateOf("") }
    var anisotropicFilterCustom by remember { mutableStateOf("") }

    var antiAliasingSelected by remember { mutableStateOf("") }
    var antiAliasingCustom by remember { mutableStateOf("") }

    var vSyncSelected by remember { mutableStateOf("") }
    var vSyncCustom by remember { mutableStateOf("") }

    var windowAdaptingFilterSelected by remember { mutableStateOf("") }
    var windowAdaptingFilterCustom by remember { mutableStateOf("") }

    var spuThreadsText by remember { mutableStateOf("") }
    var spuBlockSizeText by remember { mutableStateOf("") }

    var dockedMode by remember { mutableStateOf(false) }

    var audioOutputEngineSelected by remember { mutableStateOf("") }
    var audioOutputEngineCustom by remember { mutableStateOf("") }

    var diskShaderCache by remember { mutableStateOf(false) }
    var reactiveFlushing by remember { mutableStateOf(false) }

    var cpuBackendSelected by remember { mutableStateOf("") }
    var cpuBackendCustom by remember { mutableStateOf("") }

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
    var saveAsPreset by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var presetsList by remember { mutableStateOf<List<TestPreset>>(emptyList()) }

    fun refreshPresets() {
        presetsList = presetRepo.getAllPresets()
    }

    LaunchedEffect(showPresetDialog) {
        if (showPresetDialog) refreshPresets()
    }

    val showEmulatorSettings = !isPcPlatform
    val showWrapper = true
    val showPerfMode = true

    fun getFinal(selected: String, custom: String): String {
        return if (selected == otherLabel) custom.trim() else selected.trim()
    }


    val androidVersionFinal = getFinal(androidVersionSelected, androidVersionCustom)
    val ramFinal = getFinal(ramSelected, ramCustom)
    val wrapperFinal = getFinal(wrapperSelected, wrapperCustom)
    val perfModeFinal = getFinal(perfModeSelected, perfModeCustom)
    val accuracyFinal = getFinal(accuracySelected, accuracyCustom)
    val scaleFinal = getFinal(scaleSelected, scaleCustom)
    val frameSkipFinal = getFinal(frameSkipSelected, frameSkipCustom)
    val audioDriverFinal = getFinal(audioDriverSelected, audioDriverCustom)
    val turnipVersionFinal = getFinal(turnipVersionSelected, turnipVersionCustom)
    val winlatorForkFinal = getFinal(winlatorForkSelected, winlatorForkCustom)
    val wineVersionFinal = getFinal(wineVersionSelected, wineVersionCustom)
    val box64PresetFinal = getFinal(box64PresetSelected, box64PresetCustom)
    val box64VersionFinal = getFinal(box64VersionSelected, box64VersionCustom)
    val vkd3dVersionFinal = getFinal(vkd3dVersionSelected, vkd3dVersionCustom)
    val dxvkVersionFinal = getFinal(dxvkVersionSelected, dxvkVersionCustom)
    val controllerSupportFinal = getFinal(controllerSupportSelected, controllerSupportCustom)
    val vSyncFinal = getFinal(vSyncSelected, vSyncCustom)
    val anisotropicFilterFinal = getFinal(anisotropicFilterSelected, anisotropicFilterCustom)
    val antiAliasingFinal = getFinal(antiAliasingSelected, antiAliasingCustom)
    val cpuBackendFinal = getFinal(cpuBackendSelected, cpuBackendCustom)
    val audioOutputEngineFinal = getFinal(audioOutputEngineSelected, audioOutputEngineCustom)
    val windowAdaptingFilterFinal = getFinal(windowAdaptingFilterSelected, windowAdaptingFilterCustom)

    var didPrefill by remember(testMillis) { mutableStateOf(false) }

    fun setField(options: List<String>, value: String, setSelect: (String) -> Unit, setCustom: (String) -> Unit) {
        if (value.isNotBlank()) {
            if (options.contains(value)) {
                setSelect(value)
                setCustom("")
            } else {
                setSelect(otherLabel)
                setCustom(value)
            }
        }
    }

    LaunchedEffect(testMillis, game.testResults) {
        if (testMillis == null || didPrefill) return@LaunchedEffect

        val canonicalId = "${game.id}_${testMillis}"
        val t = game.testResults.firstOrNull { it.updatedAtMillis == testMillis }
            ?: game.testResults.firstOrNull { it.testId == canonicalId }

        if (t != null) {
            didPrefill = true

            currentStatus = t.status

            setField(androidVersions, t.testedAndroidVersion, { androidVersionSelected = it }, { androidVersionCustom = it })

            deviceModelText = t.testedDeviceModel
            gpuModelText = t.testedGpuModel
            driverVersionText = t.testedDriverVersion

            setField(ramOptions, t.testedRam, { ramSelected = it }, { ramCustom = it })
            setField(wrapperOptions, t.testedWrapper, { wrapperSelected = it }, { wrapperCustom = it })
            setField(perfModeOptions, t.testedPerformanceMode, { perfModeSelected = it }, { perfModeCustom = it })

            selectedApp =
                if (appOptions.contains(t.testedApp)) t.testedApp else appOptions.first()
            appVersionText = t.testedAppVersion
            gameVersionText = t.testedGameVersionOrBuild

            selectedIssueType = t.issueType
            selectedRepro = t.reproducibility
            workaroundText = t.workaround
            issueNoteText = t.issueNote

            selectedEmuBuild = t.emulatorBuildType
            setField(accuracyOptions, t.accuracyLevel, { accuracySelected = it }, { accuracyCustom = it })
            setField(scaleOptions, t.resolutionScale, { scaleSelected = it }, { scaleCustom = it })

            asyncShaderEnabled = t.asyncShaderEnabled

            setField(frameSkipOptions, t.frameSkip, { frameSkipSelected = it }, { frameSkipCustom = it })

            resW = t.resolutionWidth
            resH = t.resolutionHeight
            fpsFrom = t.fpsMin
            fpsTo = t.fpsMax

            mediaLinkText = t.mediaLink

            setField(audioDriverOptions, t.audioDriver, { audioDriverSelected = it }, { audioDriverCustom = it })
            downloadSizeText = t.downloadSize
            setField(wineVersionOptions, t.wineVersion, { wineVersionSelected = it }, { wineVersionCustom = it })
            setField(winlatorForkOptions, t.winlatorFork, { winlatorForkSelected = it }, { winlatorForkCustom = it })
            setField(turnipOptions, t.turnipVersion, { turnipVersionSelected = it }, { turnipVersionCustom = it })
            setField(controllerOptions, t.controllerSupport, { controllerSupportSelected = it }, { controllerSupportCustom = it })
            setField(box64PresetOptions, t.box64Preset, { box64PresetSelected = it }, { box64PresetCustom = it })
            setField(box64VersionOptions, t.box64Version, { box64VersionSelected = it }, { box64VersionCustom = it })
            startupSelectionText = t.startupSelection
            envVariablesText = t.envVariables
            setField(vkd3dOptions, t.vkd3dVersion, { vkd3dVersionSelected = it }, { vkd3dVersionCustom = it })
            setField(dxvkOptions, t.dxvkVersion, { dxvkVersionSelected = it }, { dxvkVersionCustom = it })
            setField(anisotropicOptions, t.anisotropicFilter, { anisotropicFilterSelected = it }, { anisotropicFilterCustom = it })
            setField(aaOptions, t.antiAliasing, { antiAliasingSelected = it }, { antiAliasingCustom = it })
            setField(vsyncOptions, t.vSync, { vSyncSelected = it }, { vSyncCustom = it })
            setField(windowFilterOptions, t.windowAdaptingFilter, { windowAdaptingFilterSelected = it }, { windowAdaptingFilterCustom = it })
            spuThreadsText = t.spuThreads
            spuBlockSizeText = t.spuBlockSize
            dockedMode = t.dockedMode
            setField(audioEngineOptions, t.audioOutputEngine, { audioOutputEngineSelected = it }, { audioOutputEngineCustom = it })
            diskShaderCache = t.diskShaderCache
            reactiveFlushing = t.reactiveFlushing
            setField(cpuBackendOptions, t.cpuBackend, { cpuBackendSelected = it }, { cpuBackendCustom = it })
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
        deviceModelText, gpuModelText,
        ramFinal,
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

    fun applyPreset(preset: TestPreset) {
        setField(androidVersions, preset.androidVersion, { androidVersionSelected = it }, { androidVersionCustom = it })

        deviceModelText = preset.deviceModel
        gpuModelText = preset.gpuModel
        driverVersionText = preset.driverVersion

        setField(ramOptions, preset.ram, { ramSelected = it }, { ramCustom = it })
        setField(wrapperOptions, preset.wrapper, { wrapperSelected = it }, { wrapperCustom = it })
        setField(perfModeOptions, preset.performanceMode, { perfModeSelected = it }, { perfModeCustom = it })

        if (appOptions.contains(preset.app)) {
            selectedApp = preset.app
        } else {
            selectedApp = appOptions.firstOrNull() ?: ""
        }
        appVersionText = preset.appVersion

        try {
            selectedEmuBuild = EmulatorBuildType.valueOf(preset.emulatorBuildType)
        } catch (_: Exception) {
            selectedEmuBuild = EmulatorBuildType.STABLE
        }

        setField(accuracyOptions, preset.accuracy, { accuracySelected = it }, { accuracyCustom = it })
        setField(scaleOptions, preset.scale, { scaleSelected = it }, { scaleCustom = it })

        asyncShaderEnabled = preset.asyncShader

        setField(frameSkipOptions, preset.frameSkip, { frameSkipSelected = it }, { frameSkipCustom = it })
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
    if (showPresetDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPresetDialog = false },
            sheetState = sheetState
        ) {
            BottomSheetTitle(stringResource(R.string.title_presets))

            if (presetsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_presets_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = presetsList, key = { it.id }) { preset ->
                        PresetCard(
                            preset = preset,
                            onApply = {
                                applyPreset(preset)
                                showPresetDialog = false
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.preset_applied),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onRename = { newName ->
                                presetRepo.renamePreset(preset.id, newName)
                                refreshPresets()
                            },
                            onDelete = {
                                presetRepo.deletePreset(preset.id)
                                refreshPresets()
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0),
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

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showPresetDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_presets))
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
                if (isPcPlatform) {
                    Spacer(modifier = Modifier.height(10.dp))
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_turnip_version),
                        options = turnipOptions,
                        selected = turnipVersionSelected,
                        onSelectedChange = { turnipVersionSelected = it },
                        customValue = turnipVersionCustom,
                        onCustomChange = { turnipVersionCustom = it },
                        customPlaceholder = stringResource(R.string.label_turnip_version),
                        otherLabel = otherLabel
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_audio_driver),
                        options = audioDriverOptions,
                        selected = audioDriverSelected,
                        onSelectedChange = { audioDriverSelected = it },
                        customValue = audioDriverCustom,
                        onCustomChange = { audioDriverCustom = it },
                        customPlaceholder = stringResource(R.string.label_audio_driver),
                        otherLabel = otherLabel
                    )
                }

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

            if (isPcPlatform) {
                ModernSectionCard(title = stringResource(R.string.section_pc_settings)) {
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_winlator_fork),
                        options = winlatorForkOptions,
                        selected = winlatorForkSelected,
                        onSelectedChange = { winlatorForkSelected = it },
                        customValue = winlatorForkCustom,
                        onCustomChange = { winlatorForkCustom = it },
                        customPlaceholder = stringResource(R.string.label_winlator_fork),
                        otherLabel = otherLabel
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_wine_version),
                        options = wineVersionOptions,
                        selected = wineVersionSelected,
                        onSelectedChange = { wineVersionSelected = it },
                        customValue = wineVersionCustom,
                        onCustomChange = { wineVersionCustom = it },
                        customPlaceholder = stringResource(R.string.label_wine_version),
                        otherLabel = otherLabel
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_box64_preset),
                        options = box64PresetOptions,
                        selected = box64PresetSelected,
                        onSelectedChange = { box64PresetSelected = it },
                        customValue = box64PresetCustom,
                        onCustomChange = { box64PresetCustom = it },
                        customPlaceholder = stringResource(R.string.label_box64_preset),
                        otherLabel = otherLabel
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_box64_version),
                        options = box64VersionOptions,
                        selected = box64VersionSelected,
                        onSelectedChange = { box64VersionSelected = it },
                        customValue = box64VersionCustom,
                        onCustomChange = { box64VersionCustom = it },
                        customPlaceholder = stringResource(R.string.label_box64_version),
                        otherLabel = otherLabel
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_vkd3d_version),
                        options = vkd3dOptions,
                        selected = vkd3dVersionSelected,
                        onSelectedChange = { vkd3dVersionSelected = it },
                        customValue = vkd3dVersionCustom,
                        onCustomChange = { vkd3dVersionCustom = it },
                        customPlaceholder = stringResource(R.string.label_vkd3d_version),
                        otherLabel = otherLabel
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_dxvk_version),
                        options = dxvkOptions,
                        selected = dxvkVersionSelected,
                        onSelectedChange = { dxvkVersionSelected = it },
                        customValue = dxvkVersionCustom,
                        onCustomChange = { dxvkVersionCustom = it },
                        customPlaceholder = stringResource(R.string.label_dxvk_version),
                        otherLabel = otherLabel
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = envVariablesText,
                        onValueChange = { envVariablesText = it },
                        label = { Text(stringResource(R.string.label_env_variables)) },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = startupSelectionText,
                        onValueChange = { startupSelectionText = it },
                        label = { Text(stringResource(R.string.label_startup_selection)) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (isPs3Platform) {
                ModernSectionCard(title = stringResource(R.string.section_ps3_settings)) {
                    OutlinedTextField(
                        value = spuThreadsText,
                        onValueChange = { spuThreadsText = it },
                        label = { Text(stringResource(R.string.label_spu_threads)) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = spuBlockSizeText,
                        onValueChange = { spuBlockSizeText = it },
                        label = { Text(stringResource(R.string.label_spu_block_size)) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

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

                if (isPcPlatform) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = downloadSizeText,
                        onValueChange = { downloadSizeText = it },
                        label = { Text(stringResource(R.string.label_download_size)) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_controller_support),
                        options = controllerOptions,
                        selected = controllerSupportSelected,
                        onSelectedChange = { controllerSupportSelected = it },
                        customValue = controllerSupportCustom,
                        onCustomChange = { controllerSupportCustom = it },
                        customPlaceholder = stringResource(R.string.label_controller_support),
                        otherLabel = otherLabel
                    )
                }
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
            if (isSwitchPlatform) {
                Spacer(modifier = Modifier.height(12.dp))
                ModernSectionCard(title = stringResource(R.string.section_switch_settings)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = dockedMode, onCheckedChange = { dockedMode = it })
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.label_docked_mode))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = diskShaderCache, onCheckedChange = { diskShaderCache = it })
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.label_disk_shader_cache))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = reactiveFlushing, onCheckedChange = { reactiveFlushing = it })
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.label_reactive_flushing))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_cpu_backend),
                        options = cpuBackendOptions,
                        selected = cpuBackendSelected,
                        onSelectedChange = { cpuBackendSelected = it },
                        customValue = cpuBackendCustom,
                        onCustomChange = { cpuBackendCustom = it },
                        customPlaceholder = stringResource(R.string.label_cpu_backend),
                        otherLabel = otherLabel
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_audio_output_engine),
                        options = audioEngineOptions,
                        selected = audioOutputEngineSelected,
                        onSelectedChange = { audioOutputEngineSelected = it },
                        customValue = audioOutputEngineCustom,
                        onCustomChange = { audioOutputEngineCustom = it },
                        customPlaceholder = stringResource(R.string.label_audio_output_engine),
                        otherLabel = otherLabel
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ModernSectionCard(title = stringResource(R.string.section_graphics_advanced)) {
                BottomSheetDropdownWithCustom(
                    label = stringResource(R.string.label_vsync),
                    options = vsyncOptions,
                    selected = vSyncSelected,
                    onSelectedChange = { vSyncSelected = it },
                    customValue = vSyncCustom,
                    onCustomChange = { vSyncCustom = it },
                    customPlaceholder = stringResource(R.string.label_vsync),
                    otherLabel = otherLabel
                )
                Spacer(modifier = Modifier.height(10.dp))
                BottomSheetDropdownWithCustom(
                    label = stringResource(R.string.label_anisotropic_filtering),
                    options = anisotropicOptions,
                    selected = anisotropicFilterSelected,
                    onSelectedChange = { anisotropicFilterSelected = it },
                    customValue = anisotropicFilterCustom,
                    onCustomChange = { anisotropicFilterCustom = it },
                    customPlaceholder = stringResource(R.string.label_anisotropic_filtering),
                    otherLabel = otherLabel
                )
                Spacer(modifier = Modifier.height(10.dp))
                BottomSheetDropdownWithCustom(
                    label = stringResource(R.string.label_anti_aliasing),
                    options = aaOptions,
                    selected = antiAliasingSelected,
                    onSelectedChange = { antiAliasingSelected = it },
                    customValue = antiAliasingCustom,
                    onCustomChange = { antiAliasingCustom = it },
                    customPlaceholder = stringResource(R.string.label_anti_aliasing),
                    otherLabel = otherLabel
                )
                if (isSwitchPlatform) {
                    Spacer(modifier = Modifier.height(10.dp))
                    BottomSheetDropdownWithCustom(
                        label = stringResource(R.string.label_window_adapting_filter),
                        options = windowFilterOptions,
                        selected = windowAdaptingFilterSelected,
                        onSelectedChange = { windowAdaptingFilterSelected = it },
                        customValue = windowAdaptingFilterCustom,
                        onCustomChange = { windowAdaptingFilterCustom = it },
                        customPlaceholder = stringResource(R.string.label_window_adapting_filter),
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Switch(
                    checked = saveAsPreset,
                    onCheckedChange = { saveAsPreset = it }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    stringResource(R.string.label_save_as_preset),
                    style = MaterialTheme.typography.bodyMedium
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

            if (!isLoggedIn) {
                Text(
                    text = stringResource(R.string.label_auth_required_to_save_test),
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

                        if (!isLoggedIn) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_auth_required_to_save_test),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@TextButton
                        }

                        if (saveAsPreset) {
                            val newPreset = TestPreset(
                                name = presetRepo.getNextDefaultName(),
                                androidVersion = androidVersionFinal,
                                deviceModel = deviceModelText.trim(),
                                gpuModel = gpuModelText.trim(),
                                driverVersion = driverVersionText.trim(),
                                ram = ramFinal,
                                wrapper = wrapperFinal,
                                performanceMode = perfModeFinal,
                                app = selectedApp.trim(),
                                appVersion = appVersionText.trim(),
                                emulatorBuildType = selectedEmuBuild.name,
                                accuracy = if(showEmulatorSettings) accuracyFinal else "",
                                scale = if(showEmulatorSettings) scaleFinal else "",
                                asyncShader = if(showEmulatorSettings) asyncShaderEnabled else false,
                                frameSkip = if(showEmulatorSettings) frameSkipFinal else ""
                            )
                            presetRepo.savePreset(newPreset)
                        }

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

                                mediaLink = mediaLinkText.trim(),
                                audioDriver = audioDriverFinal,
                                downloadSize = downloadSizeText.trim(),
                                wineVersion = wineVersionFinal,
                                winlatorFork = winlatorForkFinal,
                                turnipVersion = turnipVersionFinal,
                                controllerSupport = controllerSupportFinal,
                                box64Preset = box64PresetFinal,
                                box64Version = box64VersionFinal,
                                startupSelection = startupSelectionText.trim(),
                                envVariables = envVariablesText.trim(),
                                vkd3dVersion = vkd3dVersionFinal,
                                dxvkVersion = dxvkVersionFinal,
                                anisotropicFilter = anisotropicFilterFinal,
                                antiAliasing = antiAliasingFinal,
                                vSync = vSyncFinal,
                                windowAdaptingFilter = windowAdaptingFilterFinal,
                                spuThreads = spuThreadsText.trim(),
                                spuBlockSize = spuBlockSizeText.trim(),
                                dockedMode = dockedMode,
                                audioOutputEngine = audioOutputEngineFinal,
                                diskShaderCache = diskShaderCache,
                                reactiveFlushing = reactiveFlushing,
                                cpuBackend = cpuBackendFinal
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.button_save))
                }
            }
            Spacer(
                modifier = Modifier.windowInsetsBottomHeight(
                    WindowInsets.navigationBars
                )
            )
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
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

@Composable
private fun PresetCard(
    preset: TestPreset,
    onApply: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by remember(preset.id) { mutableStateOf(false) }
    var editText by remember(preset.id) { mutableStateOf(preset.name) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isEditing) { onApply() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Outlined.Save,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = preset.deviceModel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = "${preset.gpuModel} • ${preset.ram}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            if (isEditing) {
                IconButton(onClick = {
                    val name = editText.trim()
                    if (name.isNotEmpty()) {
                        onRename(name)
                        isEditing = false
                    }
                }) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                IconButton(onClick = { isEditing = true }) {
                    Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}