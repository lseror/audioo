package com.serortech.audioo.drive

import android.content.Context
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile

class DriveUploader(ctx: Context, account: GoogleSignInAccount) {

    private val drive: Drive = Drive.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        DriveAuth.credential(ctx, account),
    )
        .setApplicationName("Audioo")
        .build()

    private val resolver = ctx.contentResolver

    @Volatile
    private var cachedFolderId: String? = null

    fun ensureAudiooFolder(): String {
        cachedFolderId?.let { return it }
        val query = "mimeType='application/vnd.google-apps.folder' " +
            "and name='$FOLDER_NAME' and trashed=false and 'root' in parents"
        val existing = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id,name)")
            .execute()
            .files
            .firstOrNull()
        val id = existing?.id ?: run {
            val meta = DriveFile().apply {
                name = FOLDER_NAME
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf("root")
            }
            drive.files().create(meta).setFields("id").execute().id
        }
        cachedFolderId = id
        return id
    }

    fun upload(localUri: Uri, displayName: String): String {
        val folder = ensureAudiooFolder()
        val meta = DriveFile().apply {
            name = displayName
            parents = listOf(folder)
        }
        val input = resolver.openInputStream(localUri)
            ?: error("openInputStream returned null for $localUri")
        input.use {
            val content = InputStreamContent(MIME_OGG, it)
            return drive.files().create(meta, content).setFields("id").execute().id
        }
    }

    companion object {
        private const val FOLDER_NAME = "Audioo"
        private const val MIME_OGG = "audio/ogg"
    }
}
