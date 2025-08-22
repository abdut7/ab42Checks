package com.example.ab42checks

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import com.example.ab42checks.CookieStore
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class StatusCheckService : Service() {

    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            checkStatus()
            handler.postDelayed(this, 60 * 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationUtils.createChannel(this)
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val notification = NotificationUtils.buildNotification(this, "Checking status...")
        startForeground(NotificationUtils.NOTIFICATION_ID, notification)
        handler.post(pollRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
    }

    private fun checkStatus() {
        thread {
            try {
                Log.d(TAG, "Calling status API in Service llllll")
                val url = URL("https://apply.42abudhabi.ae/users/1225298/id_checks_users")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
                connection.setRequestProperty("accept-encoding", "gzip, deflate, br")
                connection.setRequestProperty("accept-language", "en-US,en;q=0.9")
                connection.setRequestProperty("cache-control", "no-cache")
                connection.setRequestProperty("user-agent", "Mozilla/5.0 (Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36")
                val cookie = prefs.getString("cookie", CookieStore.DEFAULT_COOKIE) ?: CookieStore.DEFAULT_COOKIE
                connection.setRequestProperty("cookie", cookie)
                val code = connection.responseCode
                Log.d(TAG, "API response code: $code")
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val message = if (code == HttpURLConnection.HTTP_OK) {
                    if (html.contains("There are no available piscines right now")) {
                        "There are no available piscines right now"
                    } else {
                        "Available"
                    }
                } else {
                    "Error: $code"
                }

                val now = System.currentTimeMillis()
                prefs.edit().putLong("last_api_call", now).apply()

                if (message == "Available") {
                    playSound()
                }

                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(
                    NotificationUtils.NOTIFICATION_ID,
                    NotificationUtils.buildNotification(this@StatusCheckService, message)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error checking status", e)
            }
        }
    }

    private fun playSound() {
        try {
            val assetFileDescriptor = assets.openFd("iphone.mp3")
            val mediaPlayer = android.media.MediaPlayer()
            mediaPlayer.setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
            mediaPlayer.isLooping = true
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound", e)
        }
    }

    companion object {
        private const val TAG = "StatusCheckService"
    }
}
