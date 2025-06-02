package com.example.agoraapp

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.jakewharton.threetenabp.AndroidThreeTen

class AgoraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)

        // Initialize ThreeTenABP for LocalDate support on API < 26
        AndroidThreeTen.init(this)
    }
}
