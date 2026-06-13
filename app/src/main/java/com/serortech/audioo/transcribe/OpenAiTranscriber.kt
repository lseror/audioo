package com.serortech.audioo.transcribe

import android.content.Context
import android.net.Uri
import com.serortech.audioo.settings.ApiKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Transcription audio via l'endpoint OpenAI /v1/audio/transcriptions.
 * Multipart : file (l'OGG) + model. Clé lue depuis [ApiKeyStore].
 *
 * Anthropic n'est pas une option ici : l'API Claude n'accepte pas d'audio.
 */
class OpenAiTranscriber(private val ctx: Context) {

    private val client = OkHttpClient.Builder()
        .callTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Renvoie le texte transcrit, ou lève une exception avec un message lisible. */
    suspend fun transcribe(uri: Uri, filename: String): String = withContext(Dispatchers.IO) {
        val key = ApiKeyStore(ctx).openAiKey
        if (key.isBlank()) {
            throw TranscriptionException("Aucune clé OpenAI. Renseigne-la dans les Réglages.")
        }

        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw TranscriptionException("Impossible de lire le fichier audio.")

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", MODEL)
            .addFormDataPart(
                "file",
                filename,
                bytes.toRequestBody("audio/ogg".toMediaType()),
            )
            .build()

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Bearer $key")
            .post(body)
            .build()

        client.newCall(request).execute().use { resp ->
            val payload = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = runCatching {
                    JSONObject(payload).getJSONObject("error").getString("message")
                }.getOrNull() ?: "HTTP ${resp.code}"
                throw TranscriptionException("Échec transcription : $msg")
            }
            runCatching { JSONObject(payload).getString("text") }
                .getOrElse { throw TranscriptionException("Réponse inattendue d'OpenAI.") }
        }
    }

    class TranscriptionException(message: String) : Exception(message)

    companion object {
        private const val ENDPOINT = "https://api.openai.com/v1/audio/transcriptions"
        private const val MODEL = "gpt-4o-transcribe"
    }
}
