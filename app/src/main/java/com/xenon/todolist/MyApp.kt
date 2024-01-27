package com.xenon.todolist

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()
        val sharedPreferenceManager = SharedPreferenceManager(this)
        AppCompatDelegate.setDefaultNightMode((sharedPreferenceManager.themeFlag[sharedPreferenceManager.theme]))
    }
}