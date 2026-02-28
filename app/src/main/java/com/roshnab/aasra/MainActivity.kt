package com.roshnab.aasra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.roshnab.aasra.data.SettingsStore
import com.roshnab.aasra.ui.theme.AASRATheme
import com.roshnab.aasra.utils.LocalPushHelper

class MainActivity : ComponentActivity() {
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