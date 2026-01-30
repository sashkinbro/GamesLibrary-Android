package com.sbro.gameslibrary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import com.google.firebase.FirebaseApp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.sbro.gameslibrary.util.CYBERPUNK_MODE
import com.sbro.gameslibrary.util.dataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.sbro.gameslibrary.ui.screens.PSGamesApp
import com.sbro.gameslibrary.ui.theme.PSGamesTheme
import com.sbro.gameslibrary.cyberpunk.ui.screens.CyberGamesApp
import com.sbro.gameslibrary.cyberpunk.ui.theme.PSGamesThemeCyberpunk
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.review.ReviewManagerFactory
import com.sbro.gameslibrary.util.APP_LAUNCH_COUNT
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.sbro.gameslibrary.viewmodel.FavoritesRepository

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        private var cachedCyberMode: Boolean? = null
    }

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

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val prefs = dataStore.data.first()
                val currentLaunchCount = prefs[APP_LAUNCH_COUNT] ?: 0
                val newLaunchCount = currentLaunchCount + 1

                dataStore.edit { settings ->
                    settings[APP_LAUNCH_COUNT] = newLaunchCount
                }

                if (newLaunchCount == 3) {
                    delay(5000)
                    val reviewManager = ReviewManagerFactory.create(this@MainActivity)
                    val request = reviewManager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val reviewInfo = task.result
                            val flow = reviewManager.launchReviewFlow(this@MainActivity, reviewInfo)
                            flow.addOnCompleteListener { _ ->
                                // Flow finished
                            }
                        }
                    }
                }
            }
        }

        setContent {
            val context = LocalContext.current

            val isCyberpunkEnabled by produceState(initialValue = cachedCyberMode) {
                context.dataStore.data
                    .map { prefs -> prefs[CYBERPUNK_MODE] ?: false }
                    .distinctUntilChanged()
                    .collect { mode ->
                        cachedCyberMode = mode
                        value = mode
                    }
            }

            when (isCyberpunkEnabled) {
                true -> {
                    PSGamesThemeCyberpunk {
                        CyberGamesApp()
                    }
                }

                false -> {
                    PSGamesTheme {
                        PSGamesApp()
                    }
                }

                null -> {
                    PSGamesThemeCyberpunk {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Black
                        ) {}
                    }
                }
            }
        }
    }
}
