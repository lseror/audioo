package com.serortech.audioo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.serortech.audioo.ui.SettingsScreen
import com.serortech.audioo.ui.theme.AudiooTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudiooTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}
