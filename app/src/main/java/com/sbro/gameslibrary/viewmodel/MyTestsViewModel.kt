package com.sbro.gameslibrary.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sbro.gameslibrary.components.EmulatorBuildType
import com.sbro.gameslibrary.components.Game
import com.sbro.gameslibrary.components.GameTestResult
import com.sbro.gameslibrary.components.IssueType
import com.sbro.gameslibrary.components.Reproducibility
import com.sbro.gameslibrary.components.WorkStatus
import com.sbro.gameslibrary.util.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class MyTestsViewModel(
    private val appContext: Context
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val testsCollection = firestore.collection("gameTests")
    private val auth = FirebaseAuth.getInstance()

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _games = MutableStateFlow<List<Game>>(emptyList())

    private val _myTests =
        MutableStateFlow<List<Pair<Game, GameTestResult>>>(emptyList())
    val myTests: StateFlow<List<Pair<Game, GameTestResult>>> = _myTests.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasLoadedTests = MutableStateFlow(false)
    val hasLoadedTests: StateFlow<Boolean> = _hasLoadedTests.asStateFlow()

    private val _hasMoreTests = MutableStateFlow(true)
    val hasMoreTests: StateFlow<Boolean> = _hasMoreTests.asStateFlow()

    private var loadJob: Job? = null
    private var lastDoc: DocumentSnapshot? = null

    private val PAGE_SIZE = 5

    private var authListener: AuthStateListener? = null

    init {
        observeAuth()
        loadGamesOnceThenMyTests()
    }

    private fun observeAuth() {
        val listener = AuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            loadMyTests(loadMore = false)
        }
        authListener = listener
        auth.addAuthStateListener(listener)
    }

    private fun loadGamesOnceThenMyTests() {
        viewModelScope.launch {
            _isLoading.value = true

            val parsedGames = withContext(Dispatchers.IO) {
                JsonParser.parseFromAssetsJson(appContext)
                    .getOrElse { emptyList() }
            }
            _games.value = parsedGames

            loadMyTests(loadMore = false)
        }
    }

    fun loadFirstPage() {
        loadMyTests(loadMore = false)
    }

    fun loadMoreMyTests() {
        if (_isLoading.value) return
        if (!_hasMoreTests.value) return
        if (!_hasLoadedTests.value) return
        loadMyTests(loadMore = true)
    }

    private fun loadMyTests(loadMore: Boolean) {
        val user = _currentUser.value
        if (user == null) {
            _myTests.value = emptyList()
            _isLoading.value = false
            _hasLoadedTests.value = true
            _hasMoreTests.value = false
            lastDoc = null
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            if (!loadMore) {
                _hasLoadedTests.value = false
                lastDoc = null
                _hasMoreTests.value = true
            }

            _isLoading.value = true
            try {
                var q: Query = testsCollection
                    .whereEqualTo("authorUid", user.uid)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE.toLong())

                if (loadMore && lastDoc != null) {
                    q = q.startAfter(lastDoc!!)
                }

                val snap = q.get().await()

                if (!snap.isEmpty) {
                    lastDoc = snap.documents.last()
                }
                _hasMoreTests.value = snap.size() == PAGE_SIZE

                val remoteTests = snap.documents.mapNotNull { doc ->
                    val gameId = doc.getString("gameId")
                        ?: doc.getString("id")
                        ?: return@mapNotNull null
                    if (gameId.isBlank()) return@mapNotNull null

                    val test = doc.toGameTestResult(gameId) ?: return@mapNotNull null
                    test to gameId
                }

                val gamesMap = _games.value.associateBy { it.id }

                val mergedNew = remoteTests.mapNotNull { (test, gameId) ->
                    val game = gamesMap[gameId] ?: return@mapNotNull null
                    game to test
                }

                val finalList = if (loadMore) {
                    (_myTests.value + mergedNew)
                        .distinctBy { (_, t) -> t.testId }
                        .sortedByDescending { (_, t) -> t.updatedAtMillis }
                } else {
                    mergedNew.sortedByDescending { (_, t) -> t.updatedAtMillis }
                }

                _myTests.value = finalList
            } catch (_: Exception) {
                if (!loadMore) _myTests.value = emptyList()
                _hasMoreTests.value = false
            } finally {
                _isLoading.value = false
                _hasLoadedTests.value = true
            }
        }
    }

    private fun DocumentSnapshot.toGameTestResult(gameId: String): GameTestResult? {
        return try {
            val statusStr = getString("status") ?: WorkStatus.UNTESTED.name

            val testedAndroidVersion = getString("testedAndroidVersion") ?: ""
            val testedDeviceModel = getString("testedDeviceModel") ?: ""
            val testedGpuModel = getString("testedGpuModel") ?: ""
            val testedRam = getString("testedRam") ?: ""
            val testedWrapper = getString("testedWrapper") ?: ""
            val testedPerformanceMode = getString("testedPerformanceMode") ?: ""

            val testedApp = getString("testedApp") ?: ""
            val testedAppVersion = getString("testedAppVersion") ?: ""
            val testedGameVersionOrBuild = getString("testedGameVersionOrBuild") ?: ""

            val issueTypeStr = getString("issueType") ?: IssueType.CRASH.firestoreValue
            val reproducibilityStr =
                getString("reproducibility") ?: Reproducibility.ALWAYS.firestoreValue

            val workaround = getString("workaround") ?: ""
            val issueNote = getString("issueNote") ?: ""

            val emulatorBuildTypeStr =
                getString("emulatorBuildType") ?: EmulatorBuildType.STABLE.firestoreValue
            val accuracyLevel = getString("accuracyLevel") ?: ""
            val resolutionScale = getString("resolutionScale") ?: ""
            val asyncShaderEnabled = getBoolean("asyncShaderEnabled") ?: false
            val frameSkip = getString("frameSkip") ?: ""

            val resolutionWidth = getString("resolutionWidth") ?: ""
            val resolutionHeight = getString("resolutionHeight") ?: ""
            val fpsMin = getString("fpsMin") ?: ""
            val fpsMax = getString("fpsMax") ?: ""

            val mediaLink = getString("mediaLink") ?: ""

            val updatedAt = getTimestamp("updatedAt")
            val updatedAtMillis =
                getLong("updatedAtMillis")
                    ?: updatedAt?.toDate()?.time
                    ?: 0L

            val formattedDate = updatedAt?.toDate()?.let { date ->
                SimpleDateFormat(
                    "d MMM yyyy • HH:mm",
                    Locale.getDefault()
                ).format(date)
            } ?: ""

            val testIdFromCloud = getString("testId").orEmpty()
            val finalTestId =
                testIdFromCloud.ifBlank { "${gameId}_${updatedAtMillis}" }

            GameTestResult(
                testId = finalTestId,
                status = when (statusStr) {
                    WorkStatus.WORKING.name -> WorkStatus.WORKING
                    WorkStatus.NOT_WORKING.name -> WorkStatus.NOT_WORKING
                    else -> WorkStatus.UNTESTED
                },

                testedAndroidVersion = testedAndroidVersion,
                testedDeviceModel = testedDeviceModel,
                testedGpuModel = testedGpuModel,
                testedRam = testedRam,
                testedWrapper = testedWrapper,
                testedPerformanceMode = testedPerformanceMode,

                testedApp = testedApp,
                testedAppVersion = testedAppVersion,
                testedGameVersionOrBuild = testedGameVersionOrBuild,

                issueType = IssueType.fromFirestore(issueTypeStr),
                reproducibility = Reproducibility.fromFirestore(reproducibilityStr),
                workaround = workaround,
                issueNote = issueNote,

                emulatorBuildType = EmulatorBuildType.fromFirestore(emulatorBuildTypeStr),
                accuracyLevel = accuracyLevel,
                resolutionScale = resolutionScale,
                asyncShaderEnabled = asyncShaderEnabled,
                frameSkip = frameSkip,

                resolutionWidth = resolutionWidth,
                resolutionHeight = resolutionHeight,
                fpsMin = fpsMin,
                fpsMax = fpsMax,

                mediaLink = mediaLink,

                testedDateFormatted = formattedDate,
                updatedAtMillis = updatedAtMillis,

                authorUid = getString("authorUid"),
                authorName = getString("authorName"),
                authorEmail = getString("authorEmail"),
                authorPhotoUrl = getString("authorPhotoUrl"),
                fromAccount = getBoolean("fromAccount") ?: false
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun onCleared() {
        authListener?.let { auth.removeAuthStateListener(it) }
        super.onCleared()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MyTestsViewModel::class.java)) {
                        return MyTestsViewModel(context.applicationContext) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
