package com.serortech.audioo.util

import android.content.Context

/**
 * Flag persistant indiquant que l'utilisateur veut un enregistrement actif.
 *
 * Posé à `true` quand le service démarre, `false` uniquement quand l'utilisateur
 * stoppe explicitement. Un kill système ou un reboot laisse le flag à `true`, ce
 * qui permet à [com.serortech.audioo.boot.BootReceiver] de proposer la reprise.
 */
object RecordingState {

    private const val PREFS = "audioo_state"
    private const val KEY_WAS_RECORDING = "was_recording"

    fun setRecording(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_WAS_RECORDING, value).apply()
    }

    fun wasRecording(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_WAS_RECORDING, false)

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
