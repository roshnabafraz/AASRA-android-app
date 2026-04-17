package com.roshnab.aasra

import android.os.Bundle
import android.content.Context
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.roshnab.aasra.data.SettingsStore
import com.roshnab.aasra.ui.theme.AASRATheme
import com.roshnab.aasra.utils.LocalPushHelper

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("aasra_settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "English") ?: "English"
        val localeStr = if (lang == "Urdu") "ur" else "en"
        val locale = Locale(localeStr)
        Locale.setDefault(locale)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SettingsStore.init(this)
        LocalPushHelper.init(this)

        setContent {
            val systemIsDark = isSystemInDarkTheme()

            val isDarkTheme = remember {
                mutableStateOf(SettingsStore.getDarkMode(systemIsDark))
            }

            AASRATheme(darkTheme = isDarkTheme.value) {
                AasraNavigation(
                    isDarkTheme = isDarkTheme.value,
                    onThemeChanged = { newMode ->
                        isDarkTheme.value = newMode
                        SettingsStore.isDarkMode = newMode
                    }
                )
            }
        }
    }
}