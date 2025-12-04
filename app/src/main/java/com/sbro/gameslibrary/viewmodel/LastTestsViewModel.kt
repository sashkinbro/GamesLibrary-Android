package com.sbro.gameslibrary.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sbro.gameslibrary.components.Game
import com.sbro.gameslibrary.components.WorkStatus
import com.sbro.gameslibrary.util.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LastTestsViewModel : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Error(val throwable: Throwable) : UiState()
        data class Success(val count: Int) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _latestTests = MutableStateFlow<List<LatestTestItem>>(emptyList())
    val latestTests = _latestTests.asStateFlow()

    private var appContext: Context? = null

    private val firestore = FirebaseFirestore.getInstance()
    private val testsCollection = firestore.collection("gameTests")

    private var gameCovers: Map<String, Game> = emptyMap()

    companion object {
        private const val DEFAULT_DAYS_BACK = 7
        private const val DEFAULT_LIMIT = 20L
    }

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val parsed = JsonParser.parseFromAssetsJson(context).getOrNull().orEmpty()
                gameCovers = parsed.associateBy { it.id }

                loadLatestTests()

            } catch (t: Throwable) {
                _uiState.value = UiState.Error(t)
            }
        }
    }

    private fun dynamicCutoff(daysBack: Int = DEFAULT_DAYS_BACK): Timestamp {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysBack)
        }
        return Timestamp(cal.time)
    }

    private suspend fun loadLatestTests() {
        val cutoff = dynamicCutoff()

        val snapshot = withContext(Dispatchers.IO) {
            try {
                testsCollection
                    .whereGreaterThan("updatedAt", cutoff)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .limit(DEFAULT_LIMIT)
                    .get()
                    .await()
            } catch (e: Exception) {
                testsCollection
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .limit(DEFAULT_LIMIT)
                    .get()
                    .await()
            }
        }

        val items = snapshot.documents.mapNotNull { doc ->
            try {
                val gameId = doc.getString("gameId")
                    ?: doc.getString("id")
                    ?: return@mapNotNull null

                val title = doc.getString("title").orEmpty()

                val statusStr = doc.getString("status") ?: WorkStatus.UNTESTED.name
                val status = when (statusStr) {
                    WorkStatus.WORKING.name -> WorkStatus.WORKING
                    WorkStatus.NOT_WORKING.name -> WorkStatus.NOT_WORKING
                    else -> WorkStatus.UNTESTED
                }

                val updatedAtTs = doc.getTimestamp("updatedAt")
                val updatedAtMillis = updatedAtTs?.toDate()?.time ?: 0L

                val testedDeviceModel = doc.getString("testedDeviceModel")
                val authorName = doc.getString("authorName")
                val fromAccount = doc.getBoolean("fromAccount") ?: false

                val testedDateFormatted =
                    updatedAtTs?.toDate()?.let { date ->
                        val formatter = SimpleDateFormat("d MMM yyyy • HH:mm", Locale.getDefault())
                        formatter.format(date)
                    }

                val imageUrl = gameCovers[gameId]?.imageUrl

                LatestTestItem(
                    gameId = gameId,
                    gameTitle = title.ifBlank { gameCovers[gameId]?.title.orEmpty() },
                    status = status,
                    testedDate = testedDateFormatted,
                    deviceModel = testedDeviceModel,
                    updatedAtMillis = updatedAtMillis,
                    imageUrl = imageUrl,
                    authorName = authorName,
                    fromAccount = fromAccount
                )
            } catch (_: Throwable) {
                null
            }
        }

        _latestTests.value = items
        _uiState.value = UiState.Success(items.size)
    }
}

data class LatestTestItem(
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
