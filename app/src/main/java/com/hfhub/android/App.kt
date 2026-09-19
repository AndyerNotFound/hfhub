package com.hfhub.android

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.hfhub.android.data.SettingsStore

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        DynamicColors.applyToActivitiesIfAvailable(this)
        SettingsStore.init(this)
        com.hfhub.android.util.ImageLoader.init(this)
    }
}
