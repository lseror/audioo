package com.serortech.audioo.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore

/**
 * Lit les sessions enregistrées par l'app dans Music/Audioo/ via MediaStore.
 * L'app étant propriétaire de ces fichiers, aucune permission READ_MEDIA_AUDIO
 * n'est nécessaire pour les lister.
 */
class SessionLibrary(private val ctx: Context) {

    data class Recording(
        val id: Long,
        val uri: Uri,
        val name: String,
        val durationMs: Long,
        val dateAddedSec: Long,
        val sizeBytes: Long,
    )

    fun list(): List<Recording> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
        )
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("${RELATIVE_PATH}%")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val out = mutableListOf<Recording>()
        ctx.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    out += Recording(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        name = cursor.getString(nameCol),
                        durationMs = cursor.getLong(durCol),
                        dateAddedSec = cursor.getLong(dateCol),
                        sizeBytes = cursor.getLong(sizeCol),
                    )
                }
            }
        return out
    }

    companion object {
        private val RELATIVE_PATH = Environment.DIRECTORY_MUSIC + "/Audioo/"
    }
}
