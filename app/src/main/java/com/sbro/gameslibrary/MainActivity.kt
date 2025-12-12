package com.sbro.gameslibrary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.FirebaseApp
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.sbro.gameslibrary.util.CYBERPUNK_MODE
import com.sbro.gameslibrary.util.dataStore
import kotlinx.coroutines.flow.map
import com.sbro.gameslibrary.ui.screens.PSGamesApp
import com.sbro.gameslibrary.ui.theme.PSGamesTheme
import com.sbro.gameslibrary.cyberpunk.ui.screens.CyberGamesApp
import com.sbro.gameslibrary.cyberpunk.ui.theme.PSGamesThemeCyberpunk
import com.sbro.gameslibrary.viewmodel.FavoritesRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        FavoritesRepository.init(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            val context = LocalContext.current
            val isCyberpunkEnabled by context.dataStore.data
                .map { prefs -> prefs[CYBERPUNK_MODE] ?: false }
                .collectAsState(initial = false)

            if (isCyberpunkEnabled) {
                PSGamesThemeCyberpunk {
                    CyberGamesApp()
                }
            } else {
                PSGamesTheme {
                    PSGamesApp()
                }
            }
        }
    }
}
