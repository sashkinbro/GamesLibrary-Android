package com.sbro.gameslibrary.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "settings")

val CYBERPUNK_MODE = booleanPreferencesKey("cyberpunk_enabled")
val ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")
