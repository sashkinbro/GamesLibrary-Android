package com.sbro.gameslibrary

import android.app.Application
import com.sbro.gameslibrary.data.manager.AnalyticsManager
import com.sbro.gameslibrary.data.manager.CrashlyticsManager

class GamesLibraryApp : Application() {

    companion object {
        lateinit var analyticsManager: AnalyticsManager
            private set
        
        lateinit var crashlyticsManager: CrashlyticsManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        
        analyticsManager = AnalyticsManager(this)
        crashlyticsManager = CrashlyticsManager()
    }
}
