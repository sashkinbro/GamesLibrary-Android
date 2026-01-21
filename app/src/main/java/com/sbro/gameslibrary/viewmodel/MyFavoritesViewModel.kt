package com.sbro.gameslibrary.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbro.gameslibrary.auth.AuthManager
import com.sbro.gameslibrary.components.Game
import com.sbro.gameslibrary.util.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyFavoritesViewModel : ViewModel() {

    sealed class UiState {
        data object Idle : UiState()
        data object Loading : UiState()
        data class Success(val count: Int) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games = _games.asStateFlow()

    private val _favorites = MutableStateFlow<List<Game>>(emptyList())
    val favorites = _favorites.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val authManager = AuthManager(
        onLoggedIn = { _ -> FavoritesRepository.onUserLoggedIn() },
        onLoggedOut = { FavoritesRepository.onUserLoggedOut() }
    )
    val currentUser = authManager.currentUser

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            FavoritesRepository.init(appContext!!)
            authManager.setAppContext(appContext!!)
        }

        if (_games.value.isNotEmpty() || _uiState.value is UiState.Loading) return

        observeRepositoryFavorites()
        loadFromAssets(context)
    }

    private fun observeRepositoryFavorites() {
        viewModelScope.launch {
            FavoritesRepository.favoriteIds.collectLatest { ids ->
                val updated = _games.value.map { g ->
                    val fav = ids.contains(g.id)
                    if (g.isFavorite == fav) g else g.copy(isFavorite = fav)
                }
                _games.value = updated
                recomputeFavorites()
                _uiState.value = UiState.Success(_favorites.value.size)
            }
        }
    }

    private fun loadFromAssets(context: Context) {
        _uiState.value = UiState.Loading

        viewModelScope.launch {
            val result = JsonParser.parseFromAssetsJson(context)

            result.onSuccess { parsedGames ->
                if (parsedGames.isEmpty()) {
                    _games.value = emptyList()
                    _favorites.value = emptyList()
                    _uiState.value = UiState.Error("No games in assets")
                } else {
                    val ids = FavoritesRepository.favoriteIds.value
                    _games.value = parsedGames.map { g ->
                        g.copy(isFavorite = ids.contains(g.id))
                    }

                    recomputeFavorites()
                    _uiState.value = UiState.Success(_favorites.value.size)
                }
            }.onFailure {
                _games.value = emptyList()
                _favorites.value = emptyList()
                _uiState.value = UiState.Error("Parse error")
            }
        }
    }

    private fun recomputeFavorites() {
        _favorites.value = _games.value.filter { it.isFavorite }
    }

    fun toggleFavorite(gameId: String) {
        FavoritesRepository.toggleFavorite(gameId)
    }

    override fun onCleared() {
        authManager.clear()
        super.onCleared()
    }
}
