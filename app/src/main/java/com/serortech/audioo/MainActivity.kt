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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.serortech.audioo.drive.DriveAuth
import com.serortech.audioo.service.VoiceRecorderService
import com.serortech.audioo.ui.theme.AudiooTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleResumeIntent(intent)
        setContent {
            AudiooTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    AudiooMain(modifier = Modifier.padding(inner))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleResumeIntent(intent)
    }

    /**
     * Démarre le service quand on est arrivé via la notification de reprise au
     * boot. L'activity étant au premier plan, le démarrage du FGS micro est
     * autorisé (ce que [com.serortech.audioo.boot.BootReceiver] ne peut pas faire
     * directement sur Android 14).
     */
    private fun handleResumeIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_RESUME, false) != true) return
        val start = Intent(this, VoiceRecorderService::class.java)
            .setAction(VoiceRecorderService.ACTION_START)
        ContextCompat.startForegroundService(this, start)
    }

    companion object {
        const val EXTRA_RESUME = "com.serortech.audioo.extra.RESUME"
    }
}

@Composable
fun AudiooMain(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val perms = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_PHONE_STATE)
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
    val permsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> granted = result.values.all { it } }

    var signedIn by remember { mutableStateOf(DriveAuth.isSignedInWithDrive(ctx)) }
    var signedInEmail by remember { mutableStateOf(DriveAuth.lastAccount(ctx)?.email ?: "") }
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        signedIn = DriveAuth.isSignedInWithDrive(ctx)
        signedInEmail = DriveAuth.lastAccount(ctx)?.email ?: ""
    }

    LaunchedEffect(Unit) {
        signedIn = DriveAuth.isSignedInWithDrive(ctx)
        signedInEmail = DriveAuth.lastAccount(ctx)?.email ?: ""
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Audioo", style = MaterialTheme.typography.headlineLarge)
        Text("v0.0.8 — Drive sync", style = MaterialTheme.typography.bodyMedium)

        if (!granted) {
            Button(
                onClick = { permsLauncher.launch(perms) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Demander permissions") }
            return@Column
        }

        if (signedIn) {
            Text(
                "Drive: $signedInEmail",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = {
                    DriveAuth.signInClient(ctx).signOut()
                    signedIn = false
                    signedInEmail = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Se déconnecter de Drive") }
        } else {
            Button(
                onClick = { signInLauncher.launch(DriveAuth.signInClient(ctx).signInIntent) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Connecter Google Drive") }
        }

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
        OutlinedButton(
            onClick = { ctx.startActivity(Intent(ctx, LibraryActivity::class.java)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Mes enregistrements") }
        OutlinedButton(
            onClick = { ctx.startActivity(Intent(ctx, SettingsActivity::class.java)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Réglages") }
    }
}
