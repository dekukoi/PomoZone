package com.dekukoi.pomozone

import android.app.Application

class PomoZoneApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.create(this)
    }
}
