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
        if (nm.getNotificationChannel(RESUME_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                RESUME_CHANNEL_ID,
                "Audioo reprise",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Proposition de reprise de l'enregistrement après redémarrage"
                setShowBadge(true)
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

    /**
     * Notification proposée au boot pour reprendre l'enregistrement. Le tap ouvre
     * [MainActivity] avec l'extra [MainActivity.EXTRA_RESUME] qui déclenche le
     * démarrage du service depuis le premier plan (autorisé sur Android 14).
     */
    fun showResume() {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_RESUME, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            ctx,
            RESUME_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(ctx, RESUME_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_mic)
            .setContentTitle("Audioo")
            .setContentText("Tape pour reprendre l'enregistrement")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .build()
        nm.notify(RESUME_NOTIF_ID, notification)
    }

    fun cancelResume() {
        nm.cancel(RESUME_NOTIF_ID)
    }

    companion object {
        const val CHANNEL_ID = "audioo_recording"
        const val NOTIF_ID = 1001
        const val RESUME_CHANNEL_ID = "audioo_resume"
        const val RESUME_NOTIF_ID = 1002
        private const val RESUME_REQUEST_CODE = 2001
    }
}
