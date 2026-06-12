package com.serortech.audioo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.serortech.audioo.ui.theme.AudiooTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudiooTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    HelloAudioo(modifier = Modifier.padding(inner))
                }
            }
        }
    }
}

@Composable
fun HelloAudioo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Audioo", style = MaterialTheme.typography.headlineMedium)
        Text("v0.0.1 — bootstrap")
    }
}

@Preview(showBackground = true)
@Composable
fun HelloAudiooPreview() {
    AudiooTheme { HelloAudioo() }
}
