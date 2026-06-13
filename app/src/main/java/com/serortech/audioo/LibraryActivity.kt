package com.serortech.audioo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.serortech.audioo.ui.LibraryScreen
import com.serortech.audioo.ui.theme.AudiooTheme

class LibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudiooTheme {
                LibraryScreen(onBack = { finish() })
            }
        }
    }
}
