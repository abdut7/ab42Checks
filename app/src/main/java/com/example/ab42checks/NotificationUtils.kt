package com.example.ab42checks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationUtils {
    const val CHANNEL_ID = "status_channel"
    const val NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Status Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun buildNotification(context: Context, status: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val api = prefs.getLong("last_api_call", 0L)
        val sync = prefs.getLong("last_sync_time", 0L)
        val apiText = if (api != 0L) formatter.format(Date(api)) else "never"
        val syncText = if (sync != 0L) formatter.format(Date(sync)) else "never"
        val content = "$status\nLast API: $apiText\nLast sync: $syncText"
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Piscine Status")
            .setContentText(status)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
}
