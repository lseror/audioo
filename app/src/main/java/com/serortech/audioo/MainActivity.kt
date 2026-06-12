package com.serortech.audioo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.serortech.audioo.service.VoiceRecorderService
import com.serortech.audioo.ui.theme.AudiooTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudiooTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    AudiooMain(modifier = Modifier.padding(inner))
                }
            }
        }
    }
}

@Composable
fun AudiooMain(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val perms = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }
    var granted by remember {
        mutableStateOf(
            perms.all {
                ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> granted = result.values.all { it } }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Audioo", style = MaterialTheme.typography.headlineLarge)
        Text("v0.0.2 — recording engine", style = MaterialTheme.typography.bodyMedium)
        if (!granted) {
            Button(
                onClick = { launcher.launch(perms) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Demander permissions") }
        } else {
            Button(
                onClick = {
                    val i = Intent(ctx, VoiceRecorderService::class.java)
                        .setAction(VoiceRecorderService.ACTION_START)
                    ContextCompat.startForegroundService(ctx, i)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Start recording") }
            Button(
                onClick = {
                    val i = Intent(ctx, VoiceRecorderService::class.java)
                        .setAction(VoiceRecorderService.ACTION_STOP)
                    ctx.startService(i)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Stop recording") }
        }
    }
}
