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
                Log.d(TAG, "Calling status API in Service llllll")
                val url = URL("https://apply.42abudhabi.ae/users/1225298/id_checks_users")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
                connection.setRequestProperty("accept-encoding", "gzip, deflate, br")
                connection.setRequestProperty("accept-language", "en-US,en;q=0.9")
                connection.setRequestProperty("cache-control", "no-cache")
                connection.setRequestProperty("user-agent", "Mozilla/5.0 (Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36")
                connection.setRequestProperty(
                    "cookie",
                    "_scid=noOSqzHmjYqvpfWkVsGDR4qYXiqs703s; _fbp=fb.1.1749910519931.318927756812476318; _tt_enable_cookie=1; _ttp=01JXQCQ107NWE8M9S3X0QG1456_.tt.1; cookieconsent_status=allow; locale=en; _gid=GA1.2.1924434881.1755887521; _ScCbts=%5B%22607%3Bchrome.2%3A2%3A5%22%2C%22626%3Bchrome.2%3A2%3A5%22%5D; _sctr=1%7C1755806400000; _gcl_au=1.1.965288889.1749910519.861386868.1755887538.1755887539; _admissions_session_production=e69da6aad9b7eb0adeb246c6aa1a2faa; _ga=GA1.2.1596495760.1749910520; _scid_r=owOSqzHmjYqvpfWkVsGDR4qYXiqs703sEbmQ3Q; ttcsid=1755902309516::5jThrHy7o2S7BeEvJ26b.22.1755902375566; ttcsid_CPHGJRJC77UAVM1484PG=1755902309516::RTahyxS85xzkhlx1FJyL.22.1755902375786; ttcsid_CQB3KEBC77UCUPKFUIH0=1755902309517::pWFtIjyd1lg4JWq81J_Z.22.1755902375786; ttcsid_BTG7E331811BQC941EDG=1755902309516::7blj5eBmtoM-pV7QMEWy.22.1755902375786; ph_phc_w0Uj0THoEoBYOEhEmdFtz36tIi21gTdD7eINnBpF3Dc_posthog=%7B%22distinct_id%22%3A%2201976ecb-8511-7a9a-83c8-013651896d52%22%2C%22%24sesid%22%3A%5B1755902376647%2C%220198d3ef-fac4-746a-9075-c06849468644%22%2C1755902376644%5D%2C%22%24initial_person_info%22%3A%7B%22r%22%3A%22https%3A%2F%2Fwww.google.com%2F%22%2C%22u%22%3A%22https%3A%2F%2F42abudhabi.ae%2F%22%7D%7D; _ga_8M0TZSR8V1=GS2.1.s1755902308\$o27\$g1\$t1755902377\$j58\$l0\$h1089439285; _ga_6H0SY0TE1H=GS2.1.s1755902309\$o27\$g1\$t1755902377\$j58\$l0\$h0"
                )
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
