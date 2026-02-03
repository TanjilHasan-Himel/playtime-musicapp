package com.eplaytime.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PlayTimeApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
