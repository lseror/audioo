package com.serortech.audioo.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.serortech.audioo.audio.CallStateListener
import com.serortech.audioo.audio.RecordingEngine
import com.serortech.audioo.audio.SessionFileManager
import com.serortech.audioo.drive.UploadWorker
import com.serortech.audioo.notif.NotificationHelper

class VoiceRecorderService : Service() {

    private lateinit var notif: NotificationHelper
    private lateinit var fileMgr: SessionFileManager
    private lateinit var engine: RecordingEngine
    private lateinit var callListener: CallStateListener

    private val handler = Handler(Looper.getMainLooper())
    private val rotationRunnable = Runnable { rotate() }
    private var sessionStartedAt: Long = 0L
    private var currentFilename: String = "—"
    private var pausedAt: Long = 0L

    override fun onCreate() {
        super.onCreate()
        notif = NotificationHelper(this)
        fileMgr = SessionFileManager(this)
        engine = RecordingEngine(this)
        callListener = CallStateListener(
            ctx = this,
            onCallStarted = ::handleCallStarted,
            onCallEnded = ::handleCallEnded,
        )
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
            title = TITLE_RECORDING,
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
        callListener.start()
    }

    private fun beginNewSession() {
        try {
            val entry = fileMgr.createSession()
            engine.start(entry.fd)
            sessionStartedAt = System.currentTimeMillis()
            currentFilename = entry.filename
            notif.update(TITLE_RECORDING, entry.filename, sessionStartedAt)
            handler.postDelayed(rotationRunnable, SESSION_DURATION_MS)
        } catch (e: Exception) {
            Log.e(TAG, "beginNewSession failed", e)
            stopAll()
            stopSelf()
        }
    }

    private fun closeAndUploadCurrent() {
        try {
            engine.stop()
        } catch (e: Exception) {
            Log.e(TAG, "engine.stop failed during close", e)
        }
        val finalized = try { fileMgr.finalizeCurrent() } catch (e: Exception) {
            Log.e(TAG, "finalizeCurrent failed", e); null
        }
        if (finalized != null) {
            enqueueUpload(finalized.uri, finalized.filename)
        }
    }

    private fun rotate() {
        closeAndUploadCurrent()
        beginNewSession()
    }

    private fun handleCallStarted() {
        if (engine.state != RecordingEngine.State.Recording) return
        try {
            engine.pause()
        } catch (e: Exception) {
            Log.e(TAG, "engine.pause failed", e)
            return
        }
        pausedAt = System.currentTimeMillis()
        handler.removeCallbacks(rotationRunnable)
        notif.update(TITLE_PAUSED, currentFilename, sessionStartedAt)
    }

    private fun handleCallEnded() {
        if (engine.state != RecordingEngine.State.Paused) {
            pausedAt = 0L
            return
        }
        val pauseDuration = if (pausedAt > 0L) System.currentTimeMillis() - pausedAt else 0L
        pausedAt = 0L
        if (pauseDuration >= CALL_LONG_THRESHOLD_MS) {
            closeAndUploadCurrent()
            beginNewSession()
        } else {
            try {
                engine.resume()
            } catch (e: Exception) {
                Log.e(TAG, "engine.resume failed", e)
                return
            }
            notif.update(TITLE_RECORDING, currentFilename, sessionStartedAt)
            val elapsed = System.currentTimeMillis() - sessionStartedAt
            val remaining = (SESSION_DURATION_MS - elapsed).coerceAtLeast(1_000L)
            handler.postDelayed(rotationRunnable, remaining)
        }
    }

    private fun enqueueUpload(uri: Uri, name: String) {
        try {
            UploadWorker.enqueue(applicationContext, uri, name)
            Log.i(TAG, "upload queued: $name")
        } catch (e: Exception) {
            Log.e(TAG, "enqueueUpload failed for $name", e)
        }
    }

    private fun stopAll() {
        callListener.stop()
        handler.removeCallbacks(rotationRunnable)
        closeAndUploadCurrent()
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
        const val CALL_LONG_THRESHOLD_MS = 30_000L
        private const val TITLE_RECORDING = "Audioo — recording"
        private const val TITLE_PAUSED = "Audioo — paused (call)"
        private const val TAG = "VoiceRecorderSvc"
    }
}
