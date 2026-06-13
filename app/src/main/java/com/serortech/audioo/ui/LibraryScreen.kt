package com.serortech.audioo.ui

import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.serortech.audioo.library.SessionLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var recordings by remember { mutableStateOf<List<SessionLibrary.Recording>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        recordings = withContext(Dispatchers.IO) { SessionLibrary(ctx).list() }
        loaded = true
    }

    // Lecteur unique partagé par toute la liste.
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingId by remember { mutableStateOf<Long?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }

    fun stop() {
        player?.release()
        player = null
        playingId = null
        isPlaying = false
        positionMs = 0
        durationMs = 0
    }

    fun toggle(rec: SessionLibrary.Recording) {
        if (playingId == rec.id && player != null) {
            val p = player!!
            if (p.isPlaying) { p.pause(); isPlaying = false } else { p.start(); isPlaying = true }
            return
        }
        player?.release()
        val p = MediaPlayer()
        p.setDataSource(ctx, rec.uri)
        p.setOnCompletionListener { isPlaying = false; positionMs = 0 }
        p.prepare()
        p.start()
        player = p
        playingId = rec.id
        isPlaying = true
        durationMs = p.duration
        positionMs = 0
    }

    DisposableEffect(Unit) {
        onDispose { player?.release(); player = null }
    }

    LaunchedEffect(isPlaying, playingId) {
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
    ) { inner ->
        if (loaded && recordings.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(inner).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Aucun enregistrement.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(recordings, key = { it.id }) { rec ->
                RecordingCard(
                    rec = rec,
                    isCurrent = playingId == rec.id,
                    isPlaying = isPlaying && playingId == rec.id,
                    positionMs = if (playingId == rec.id) positionMs else 0,
                    durationMs = if (playingId == rec.id && durationMs > 0) durationMs else rec.durationMs.toInt(),
                    onToggle = { toggle(rec) },
                    onSeek = { ms ->
                        if (playingId == rec.id) {
                            player?.seekTo(ms)
                            positionMs = ms
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun RecordingCard(
    rec: SessionLibrary.Recording,
    isCurrent: Boolean,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    onToggle: () -> Unit,
    onSeek: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onToggle() }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggle) {
                    Text(
                        text = if (isPlaying) "⏸" else "▶",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isPlaying) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        rec.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${formatDate(rec.dateAddedSec)} · ${formatMs(rec.durationMs.toInt())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isCurrent) {
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
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
private fun formatDate(epochSec: Long): String = dateFormat.format(Date(epochSec * 1000))
