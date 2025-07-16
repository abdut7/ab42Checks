package com.example.ab42checks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.net.HttpURLConnection
import java.net.URL

class StatusCheckService : Service() {
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting"))
        handlerThread = HandlerThread("StatusCheckThread").also { it.start() }
        handler = Handler(handlerThread.looper)
        handler.post(checkRunnable)
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            val status = checkStatus()
            Log.d(TAG, "Status: $status")
            if (status == "Available") {
                playSound()
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        handlerThread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
            Log.e(TAG, "Error checking status", e)
            "Error: ${e.message}"
        }
    }

    private fun buildNotification(status: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
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
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun playSound() {
        try {
            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(this, uri)
            ringtone.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound", e)
        }
    }

    companion object {
        private const val CHANNEL_ID = "status_channel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "StatusCheckService"
    }
}
