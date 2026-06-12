package com.serortech.audioo.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.serortech.audioo.audio.RecordingEngine
import com.serortech.audioo.audio.SessionFileManager
import com.serortech.audioo.notif.NotificationHelper

class VoiceRecorderService : Service() {

    private lateinit var notif: NotificationHelper
    private lateinit var fileMgr: SessionFileManager
    private lateinit var engine: RecordingEngine

    private val handler = Handler(Looper.getMainLooper())
    private val rotationRunnable = Runnable { rotate() }
    private var sessionStartedAt: Long = 0L
    private var currentFilename: String = "—"

    override fun onCreate() {
        super.onCreate()
        notif = NotificationHelper(this)
        fileMgr = SessionFileManager(this)
        engine = RecordingEngine(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAll()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startRecording()
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (engine.state != RecordingEngine.State.Idle) return
        val initial = notif.buildOngoing(
            title = TITLE,
            text = "Démarrage…",
            startTimeMs = System.currentTimeMillis(),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.NOTIF_ID,
                initial,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(NotificationHelper.NOTIF_ID, initial)
        }
        beginNewSession()
    }

    private fun beginNewSession() {
        try {
            val entry = fileMgr.createSession()
            engine.start(entry.fd)
            sessionStartedAt = System.currentTimeMillis()
            currentFilename = entry.filename
            notif.update(TITLE, entry.filename, sessionStartedAt)
            handler.postDelayed(rotationRunnable, SESSION_DURATION_MS)
        } catch (e: Exception) {
            Log.e(TAG, "beginNewSession failed", e)
            stopAll()
            stopSelf()
        }
    }

    private fun rotate() {
        try {
            engine.stop()
            fileMgr.finalizeCurrent()
        } catch (e: Exception) {
            Log.e(TAG, "rotate: stop/finalize failed", e)
        }
        beginNewSession()
    }

    private fun stopAll() {
        handler.removeCallbacks(rotationRunnable)
        try { engine.stop() } catch (_: Exception) {}
        try { fileMgr.finalizeCurrent() } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.serortech.audioo.action.START"
        const val ACTION_STOP = "com.serortech.audioo.action.STOP"
        const val SESSION_DURATION_MS = 20L * 60 * 1000
        private const val TITLE = "Audioo — recording"
        private const val TAG = "VoiceRecorderSvc"
    }
}
