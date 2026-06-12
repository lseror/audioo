package com.serortech.audioo.audio

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Allocates one MediaStore entry per session in Music/Audioo/, returns a
 * ParcelFileDescriptor that MediaRecorder can write into, and finalizes the
 * entry (clears IS_PENDING) when the session ends.
 *
 * Filename: ideas_YYYY-MM-DD_HHhMM_session_NNN.ogg
 * NNN: per-day counter, resets at midnight (local time).
 */
class SessionFileManager(private val ctx: Context) {

    data class Entry(val uri: Uri, val fd: ParcelFileDescriptor, val filename: String)

    private val prefs = ctx.getSharedPreferences("audioo_session", Context.MODE_PRIVATE)
    private var current: Entry? = null

    fun createSession(): Entry {
        val now = Date()
        val day = dayFormat.format(now)
        val hhmm = timeFormat.format(now)
        val counter = nextCounter(day)
        val filename = "ideas_${day}_${hhmm}_session_${counter.padThree()}.ogg"

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
            put(MediaStore.Audio.Media.MIME_TYPE, MIME_TYPE)
            put(MediaStore.Audio.Media.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val resolver = ctx.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore insert returned null for $filename")
        val fd = resolver.openFileDescriptor(uri, "w")
            ?: error("openFileDescriptor returned null for $uri")

        val entry = Entry(uri, fd, filename)
        current = entry
        return entry
    }

    fun finalizeCurrent() {
        val c = current ?: return
        try { c.fd.close() } catch (_: Exception) {}
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.IS_PENDING, 0)
        }
        try {
            ctx.contentResolver.update(c.uri, values, null, null)
        } catch (_: Exception) {}
        current = null
    }

    private fun nextCounter(day: String): Int {
        val storedDay = prefs.getString(KEY_DAY, null)
        val next = if (storedDay == day) prefs.getInt(KEY_COUNTER, 0) + 1 else 1
        prefs.edit {
            putString(KEY_DAY, day)
            putInt(KEY_COUNTER, next)
        }
        return next
    }

    private fun Int.padThree() = toString().padStart(3, '0')

    companion object {
        private const val KEY_DAY = "current_day"
        private const val KEY_COUNTER = "session_counter"
        private const val MIME_TYPE = "audio/ogg"
        private val RELATIVE_PATH = Environment.DIRECTORY_MUSIC + "/Audioo/"
        private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val timeFormat = SimpleDateFormat("HH'h'mm", Locale.US)
    }
}
