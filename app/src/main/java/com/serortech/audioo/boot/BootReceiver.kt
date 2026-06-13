package com.serortech.audioo.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.serortech.audioo.notif.NotificationHelper
import com.serortech.audioo.util.RecordingState

/**
 * Au redémarrage, si un enregistrement était actif avant l'arrêt, on poste une
 * notification « tap-to-resume ».
 *
 * Android 14 interdit de démarrer un foreground service de type `microphone`
 * depuis [Intent.ACTION_BOOT_COMPLETED] : on ne peut donc pas reprendre
 * silencieusement. Le tap ouvre [com.serortech.audioo.MainActivity] qui, étant
 * au premier plan, est autorisée à (re)démarrer le service.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_QUICKBOOT_POWERON,
            ACTION_HTC_QUICKBOOT_POWERON -> {
                if (RecordingState.wasRecording(context)) {
                    Log.i(TAG, "boot: enregistrement actif avant reboot, notif de reprise")
                    NotificationHelper(context).showResume()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        // Variantes OEM (Samsung, HTC) émises au démarrage rapide.
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"
    }
}
