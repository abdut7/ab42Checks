package com.example.ab42checks

import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ab42checks.NotificationUtils
import java.net.HttpURLConnection
import java.net.URL

class StatusCheckWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting background status check")
        NotificationUtils.createChannel(applicationContext)
        val status = checkStatus()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NotificationUtils.NOTIFICATION_ID, NotificationUtils.buildNotification(applicationContext, status))
        if (status == "Available") {
            playSound()
        }
        return Result.success()
    }

    private fun checkStatus(): String {
        return try {
            Log.d(TAG, "Calling status API")
            val url = URL("https://apply.42abudhabi.ae/users/1225298/id_checks_users")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
            connection.setRequestProperty("accept-encoding", "gzip, deflate, br")
            connection.setRequestProperty("accept-language", "en-US,en;q=0.9")
            connection.setRequestProperty("cache-control", "no-cache")
            connection.setRequestProperty("user-agent", "Mozilla/5.0 (Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36")
            val code = connection.responseCode
            Log.d(TAG, "API response code: $code")
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
            Log.e(TAG, "Error checking status", e)
            "Error: ${e.message}"
        }
    }

    // Notification creation handled by NotificationUtils

    private fun playSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound", e)
        }
    }

    companion object {
        private const val TAG = "StatusCheckWorker"
    }
}
