package com.serortech.audioo.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.ParcelFileDescriptor

class RecordingEngine(private val ctx: Context) {

    private var recorder: MediaRecorder? = null
    @Volatile
    var state: State = State.Idle
        private set

    enum class State { Idle, Recording, Paused }

    fun start(fd: ParcelFileDescriptor) {
        check(state == State.Idle) { "RecordingEngine.start() in state=$state" }
        val r = newRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.OGG)
            setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            setAudioChannels(1)
            setAudioSamplingRate(SAMPLE_RATE_HZ)
            setAudioEncodingBitRate(BITRATE_BPS)
            setOutputFile(fd.fileDescriptor)
            prepare()
            start()
        }
        recorder = r
        state = State.Recording
    }

    fun pause() {
        if (state != State.Recording) return
        recorder?.pause()
        state = State.Paused
    }

    fun resume() {
        if (state != State.Paused) return
        recorder?.resume()
        state = State.Recording
    }

    fun stop() {
        val r = recorder ?: return
        try { r.stop() } catch (_: Exception) {}
        try { r.release() } catch (_: Exception) {}
        recorder = null
        state = State.Idle
    }

    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(ctx)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val BITRATE_BPS = 32_000
    }
}
