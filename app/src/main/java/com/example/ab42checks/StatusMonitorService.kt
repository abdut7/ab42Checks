package com.example.ab42checks

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import kotlin.concurrent.thread
import java.net.HttpURLConnection
import java.net.URL

class StatusMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable: Runnable = object : Runnable {
        override fun run() {
            checkStatus()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationUtils.createChannel(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(
            NotificationUtils.NOTIFICATION_ID,
            NotificationUtils.buildNotification(this, "Checking status...")
        )
        startForeground(
            NotificationUtils.NOTIFICATION_ID,
            NotificationUtils.buildNotification(this, "Checking status...")
        )
        handler.post(checkRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun checkStatus() {
        thread {
            val status = fetchStatus()
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(
                NotificationUtils.NOTIFICATION_ID,
                NotificationUtils.buildNotification(this, status)
            )
        }
    }

    private fun fetchStatus(): String {
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
            Log.e(TAG, "Error checking status", e)
            "Error: ${e.message}"
        }
    }

    companion object {
        private const val TAG = "StatusMonitorService"
        private const val CHECK_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
    }
}
