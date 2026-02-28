package com.roshnab.aasra.data

import android.content.Context
import android.content.SharedPreferences

object SettingsStore {
    private const val PREF_NAME = "aasra_settings"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_IS_MANUAL = "is_manual_theme" // To know if user manually changed it
    private const val KEY_LANGUAGE = "language"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getDarkMode(systemIsDark: Boolean): Boolean {
        return if (prefs.contains(KEY_DARK_MODE)) {
            prefs.getBoolean(KEY_DARK_MODE, false)
        } else {
            systemIsDark
        }
    }

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_DARK_MODE, value)
                .putBoolean(KEY_IS_MANUAL, true) // Mark that user made a choice
                .apply()
        }

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "English") ?: "English"
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value).apply()
        }
}