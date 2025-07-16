package com.example.ab42checks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.net.HttpURLConnection
import java.net.URL

class StatusCheckWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        createNotificationChannel()
        val status = checkStatus()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(status))
        return Result.success()
    }

    private fun checkStatus(): String {
        return try {
            val url = URL("https://apply.42abudhabi.ae/users/1225298/id_checks_users")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
            connection.setRequestProperty("accept-encoding", "gzip, deflate, br")
            connection.setRequestProperty("accept-language", "en-US,en;q=0.9")
            connection.setRequestProperty("cache-control", "no-cache")
            connection.setRequestProperty("user-agent", "Mozilla/5.0 (Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36")
            val code = connection.responseCode
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            if (code == HttpURLConnection.HTTP_OK) {
                if (html.contains("There are no available piscines right now")) {
                    "There are no available piscines right now"
                } else {
                    "Available"
                }
            } else {
                "Error: $code"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun buildNotification(status: String): Notification {
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Piscine Status")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Status Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = applicationContext.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "status_channel"
        private const val NOTIFICATION_ID = 1
    }
}
