package com.serortech.audioo.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/**
 * Watches two sources to know when the device is on a call:
 *  - TelephonyCallback / PhoneStateListener -> cellular phone calls
 *  - AudioManager.OnModeChangedListener (MODE_IN_COMMUNICATION) -> VoIP
 *    (WhatsApp, Signal, Telegram, Meet, etc.)
 *
 * Emits onCallStarted() once when at least one source flips into in-call,
 * onCallEnded() once when both have returned to idle.
 *
 * Gracefully degrades if READ_PHONE_STATE is denied: only the VoIP source
 * remains active.
 */
class CallStateListener(
    private val ctx: Context,
    private val onCallStarted: () -> Unit,
    private val onCallEnded: () -> Unit,
) {
    private val telephony: TelephonyManager? = ctx.getSystemService()
    private val audio: AudioManager? = ctx.getSystemService()
    private val mainExecutor = ContextCompat.getMainExecutor(ctx)

    @Volatile private var phoneInCall = false
    @Volatile private var voipInCall = false
    @Volatile private var lastInCall = false
    @Volatile private var registered = false

    private var telephonyCallback: TelephonyCallback? = null

    @Suppress("DEPRECATION")
    private var legacyListener: PhoneStateListener? = null
    private var modeListener: AudioManager.OnModeChangedListener? = null

    fun start() {
        if (registered) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerNewApi()
        } else {
            registerLegacy()
        }
        registered = true
    }

    fun stop() {
        if (!registered) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephony?.unregisterTelephonyCallback(it) }
            modeListener?.let { audio?.removeOnModeChangedListener(it) }
        } else {
            @Suppress("DEPRECATION")
            legacyListener?.let { telephony?.listen(it, PhoneStateListener.LISTEN_NONE) }
        }
        telephonyCallback = null
        legacyListener = null
        modeListener = null
        phoneInCall = false
        voipInCall = false
        lastInCall = false
        registered = false
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerNewApi() {
        if (hasPhonePerm() && telephony != null) {
            val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    phoneInCall = state != TelephonyManager.CALL_STATE_IDLE
                    propagate()
                }
            }
            telephony.registerTelephonyCallback(mainExecutor, cb)
            telephonyCallback = cb
        }
        audio?.let { am ->
            val ml = AudioManager.OnModeChangedListener { mode ->
                voipInCall = mode == AudioManager.MODE_IN_COMMUNICATION ||
                    mode == AudioManager.MODE_IN_CALL
                propagate()
            }
            am.addOnModeChangedListener(mainExecutor, ml)
            modeListener = ml
        }
    }

    @Suppress("DEPRECATION")
    private fun registerLegacy() {
        if (!hasPhonePerm() || telephony == null) return
        val pl = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                phoneInCall = state != TelephonyManager.CALL_STATE_IDLE
                propagate()
            }
        }
        telephony.listen(pl, PhoneStateListener.LISTEN_CALL_STATE)
        legacyListener = pl
    }

    private fun hasPhonePerm(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    @Synchronized
    private fun propagate() {
        val now = phoneInCall || voipInCall
        if (now == lastInCall) return
        lastInCall = now
        if (now) onCallStarted() else onCallEnded()
    }
}
