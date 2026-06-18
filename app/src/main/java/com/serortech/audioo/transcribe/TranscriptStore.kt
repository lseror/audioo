package com.serortech.audioo.transcribe

import android.content.Context
import java.io.File

/**
 * Persiste les transcripts en stockage interne app (filesDir/transcripts/),
 * un fichier .txt par session, nommé d'après le fichier audio.
 */
class TranscriptStore(ctx: Context) {

    private val dir = File(ctx.applicationContext.filesDir, "transcripts").apply { mkdirs() }

    private fun fileFor(recordingName: String): File {
        val safe = recordingName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, "$safe.txt")
    }

    fun load(recordingName: String): String? {
        val f = fileFor(recordingName)
        return if (f.exists()) f.readText() else null
    }

    fun save(recordingName: String, text: String) {
        fileFor(recordingName).writeText(text)
    }

    fun delete(recordingName: String) {
        fileFor(recordingName).delete()
    }
}
