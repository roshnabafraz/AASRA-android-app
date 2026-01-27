package com.roshnab.aasra.data

import android.content.Context
import android.content.SharedPreferences

object SettingsStore {
    private const val PREF_NAME = "aasra_settings"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getDarkMode(systemDefault: Boolean): Boolean {
        // If "dark_mode" key exists, use it. Otherwise, use the systemDefault.
        return if (prefs.contains("dark_mode")) {
            prefs.getBoolean("dark_mode", false)
        } else {
            systemDefault
        }
    }

    var isDarkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()
}