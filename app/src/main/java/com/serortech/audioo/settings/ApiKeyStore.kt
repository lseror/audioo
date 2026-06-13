package com.serortech.audioo.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stockage chiffré des clés API saisies par l'utilisateur, via
 * EncryptedSharedPreferences (clé maître Android Keystore, AES-256).
 *
 * OpenAI : utilisée pour la transcription (cf. AOO-9).
 * Anthropic : conservée pour un futur post-traitement texte — l'API Claude
 * n'accepte pas d'audio, donc pas de transcription côté Anthropic.
 */
class ApiKeyStore(ctx: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx.applicationContext,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var openAiKey: String
        get() = prefs.getString(KEY_OPENAI, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_OPENAI, value.trim()) }

    var anthropicKey: String
        get() = prefs.getString(KEY_ANTHROPIC, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_ANTHROPIC, value.trim()) }

    fun hasOpenAiKey(): Boolean = openAiKey.isNotBlank()

    companion object {
        private const val PREFS = "audioo_secrets"
        private const val KEY_OPENAI = "openai_api_key"
        private const val KEY_ANTHROPIC = "anthropic_api_key"
    }
}
