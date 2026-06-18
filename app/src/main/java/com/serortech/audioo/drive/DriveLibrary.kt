package com.serortech.audioo.drive

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Liste et télécharge les enregistrements présents dans le dossier Audioo du
 * Drive (ceux déjà synchronisés, supprimés du téléphone).
 */
class DriveLibrary(private val ctx: Context, account: GoogleSignInAccount) {

    data class DriveRec(val id: String, val name: String, val sizeBytes: Long, val modifiedMs: Long)

    private val drive: Drive = Drive.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        DriveAuth.credential(ctx, account),
    ).setApplicationName("Audioo").build()

    suspend fun list(): List<DriveRec> = withContext(Dispatchers.IO) {
        val folderId = findFolderId() ?: return@withContext emptyList()
        val q = "'$folderId' in parents and trashed=false " +
            "and mimeType != 'application/vnd.google-apps.folder'"
        drive.files().list()
            .setQ(q)
            .setSpaces("drive")
            .setFields("files(id,name,size,modifiedTime)")
            .setPageSize(1000)
            .execute()
            .files
            .map {
                DriveRec(
                    id = it.id,
                    name = it.name,
                    sizeBytes = it.getSize() ?: 0L,
                    modifiedMs = it.modifiedTime?.value ?: 0L,
                )
            }
    }

    /** Télécharge le fichier dans le cache et renvoie le fichier local (réutilise le cache). */
    suspend fun download(fileId: String, name: String): File = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "drive_audio").apply { mkdirs() }
        val out = File(dir, name)
        if (out.exists() && out.length() > 0) return@withContext out
        FileOutputStream(out).use { drive.files().get(fileId).executeMediaAndDownloadTo(it) }
        out
    }

    suspend fun delete(fileId: String) = withContext(Dispatchers.IO) {
        drive.files().delete(fileId).execute()
    }

    private fun findFolderId(): String? {
        val query = "mimeType='application/vnd.google-apps.folder' " +
            "and name='$FOLDER_NAME' and trashed=false and 'root' in parents"
        return drive.files().list()
            .setQ(query).setSpaces("drive").setFields("files(id,name)")
            .execute().files.firstOrNull()?.id
    }

    companion object {
        private const val FOLDER_NAME = "Audioo"
    }
}
