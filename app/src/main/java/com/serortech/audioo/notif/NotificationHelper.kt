package com.serortech.audioo.notif

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.serortech.audioo.MainActivity
import com.serortech.audioo.R

class NotificationHelper(private val ctx: Context) {

    private val nm = ctx.getSystemService(NotificationManager::class.java)

    init {
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audioo recording",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Ongoing voice memo recording session"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun buildOngoing(title: String, text: String, startTimeMs: Long): Notification {
        val contentIntent = PendingIntent.getActivity(
            ctx,
            0,
            Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_mic)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setWhen(startTimeMs)
            .setUsesChronometer(true)
            .setContentIntent(contentIntent)
            .build()
    }

    fun update(title: String, text: String, startTimeMs: Long) {
        nm.notify(NOTIF_ID, buildOngoing(title, text, startTimeMs))
    }

    companion object {
        const val CHANNEL_ID = "audioo_recording"
        const val NOTIF_ID = 1001
    }
}
