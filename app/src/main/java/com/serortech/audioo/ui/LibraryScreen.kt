package com.serortech.audioo.ui

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.serortech.audioo.drive.DriveAuth
import com.serortech.audioo.drive.DriveLibrary
import com.serortech.audioo.library.SessionLibrary
import com.serortech.audioo.transcribe.OpenAiTranscriber
import com.serortech.audioo.transcribe.TranscriptStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Un enregistrement, qu'il soit local (pas encore synchro) ou sur Drive. */
private data class LibItem(
    val name: String,
    val dateMs: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val localUri: Uri?,
    val driveId: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val transcriptStore = remember { TranscriptStore(ctx) }
    val transcriber = remember { OpenAiTranscriber(ctx) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val account = remember { DriveAuth.lastAccount(ctx) }
    val driveLib = remember(account) { account?.let { DriveLibrary(ctx, it) } }

    var items by remember { mutableStateOf<List<LibItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    val transcripts = remember { mutableStateMapOf<String, String>() }
    val transcribing = remember { mutableStateMapOf<String, Boolean>() }
    val preparing = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        val local = withContext(Dispatchers.IO) { SessionLibrary(ctx).list() }
        val drive = withContext(Dispatchers.IO) {
            runCatching { driveLib?.list() }.getOrNull() ?: emptyList()
        }
        val byName = LinkedHashMap<String, LibItem>()
        local.forEach {
            byName[it.name] = LibItem(it.name, it.dateAddedSec * 1000, it.durationMs, it.sizeBytes, it.uri, null)
        }
        drive.forEach { d ->
            val ex = byName[d.name]
            byName[d.name] = ex?.copy(driveId = d.id)
                ?: LibItem(d.name, d.modifiedMs, 0L, d.sizeBytes, null, d.id)
        }
        val merged = byName.values.sortedByDescending { it.name }
        items = merged
        transcripts.putAll(
            withContext(Dispatchers.IO) {
                merged.mapNotNull { m -> transcriptStore.load(m.name)?.let { m.name to it } }
            },
        )
        loaded = true
    }

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingName by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }

    suspend fun sourceUri(item: LibItem): Uri? {
        item.localUri?.let { return it }
        val lib = driveLib ?: run {
            snackbar.showSnackbar("Connecte-toi à Drive pour ce fichier.")
            return null
        }
        return Uri.fromFile(lib.download(item.driveId!!, item.name))
    }

    fun play(item: LibItem) {
        if (playingName == item.name && player != null) {
            val p = player!!
            if (p.isPlaying) { p.pause(); isPlaying = false } else { p.start(); isPlaying = true }
            return
        }
        scope.launch {
            player?.release(); player = null; isPlaying = false
            preparing[item.name] = true
            val uri = try { sourceUri(item) } catch (e: Exception) {
                snackbar.showSnackbar(e.message ?: "Téléchargement impossible"); null
            } finally { preparing[item.name] = false }
            if (uri == null) return@launch
            try {
                val p = MediaPlayer()
                p.setDataSource(ctx, uri)
                p.setOnCompletionListener { isPlaying = false; positionMs = 0 }
                p.prepare(); p.start()
                player = p
                playingName = item.name
                isPlaying = true
                durationMs = p.duration
                positionMs = 0
            } catch (e: Exception) {
                snackbar.showSnackbar("Lecture impossible.")
            }
        }
    }

    fun transcribe(item: LibItem) {
        if (transcribing[item.name] == true) return
        transcribing[item.name] = true
        scope.launch {
            try {
                val uri = sourceUri(item) ?: return@launch
                val text = transcriber.transcribe(uri, item.name)
                withContext(Dispatchers.IO) { transcriptStore.save(item.name, text) }
                transcripts[item.name] = text
            } catch (e: Exception) {
                snackbar.showSnackbar(e.message ?: "Échec de la transcription")
            } finally {
                transcribing[item.name] = false
            }
        }
    }

    DisposableEffect(Unit) { onDispose { player?.release(); player = null } }

    LaunchedEffect(isPlaying, playingName) {
        while (isPlaying) {
            player?.let { positionMs = it.currentPosition }
            delay(250)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes enregistrements") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        if (loaded && items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(inner).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Aucun enregistrement.", style = MaterialTheme.typography.bodyLarge)
                if (account == null) {
                    Text(
                        "Connecte-toi à Google Drive pour voir les enregistrements synchronisés.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.name }) { item ->
                RecordingCard(
                    item = item,
                    isCurrent = playingName == item.name,
                    isPlaying = isPlaying && playingName == item.name,
                    isPreparing = preparing[item.name] == true,
                    positionMs = if (playingName == item.name) positionMs else 0,
                    durationMs = if (playingName == item.name && durationMs > 0) durationMs else item.durationMs.toInt(),
                    transcript = transcripts[item.name],
                    isTranscribing = transcribing[item.name] == true,
                    onToggle = { play(item) },
                    onSeek = { ms -> if (playingName == item.name) { player?.seekTo(ms); positionMs = ms } },
                    onTranscribe = { transcribe(item) },
                )
            }
        }
    }
}

@Composable
private fun RecordingCard(
    item: LibItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isPreparing: Boolean,
    positionMs: Int,
    durationMs: Int,
    transcript: String?,
    isTranscribing: Boolean,
    onToggle: () -> Unit,
    onSeek: (Int) -> Unit,
    onTranscribe: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
            ) {
                IconButton(onClick = onToggle) {
                    if (isPreparing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (isPlaying) "⏸" else "▶",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isPlaying) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = if (item.durationMs > 0) formatMs(item.durationMs.toInt())
                    else formatSize(item.sizeBytes)
                    val source = if (item.localUri != null) "📱 local" else "☁︎ Drive"
                    Text(
                        "${formatDate(item.dateMs)} · $meta · $source",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isCurrent && durationMs > 0) {
                val max = durationMs.coerceAtLeast(1)
                Slider(
                    value = positionMs.coerceIn(0, max).toFloat(),
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..max.toFloat(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMs(positionMs), style = MaterialTheme.typography.bodySmall)
                    Text(formatMs(durationMs), style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isTranscribing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("  Transcription…", style = MaterialTheme.typography.bodySmall)
                } else {
                    TextButton(onClick = onTranscribe) {
                        Text(if (transcript == null) "Transcrire" else "Re-transcrire")
                    }
                }
            }
            if (transcript != null) {
                Text(
                    transcript,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

private fun formatSize(bytes: Long): String = "%.1f Mo".format(bytes / 1_000_000.0)

private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
private fun formatDate(epochMs: Long): String = dateFormat.format(Date(epochMs))
