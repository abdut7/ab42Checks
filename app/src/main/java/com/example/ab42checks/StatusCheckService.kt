package com.example.ab42checks

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class StatusCheckService : Service() {

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
                Log.d(TAG, "Calling piscine status API in service")
                val url = URL("https://42abudhabi.ae/piscine-status/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val code = connection.responseCode
                Log.d(TAG, "Piscine status response code: $code")
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val open = html.split("08/2025").size - 1 > 1 ||
                    html.contains("09/2025") || html.contains("10/2025")

                val message = if (code == HttpURLConnection.HTTP_OK) {
                    if (open) "Open" else "No new opens"
                } else {
                    "Error: $code"
                }

                if (message == "Open") {
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
