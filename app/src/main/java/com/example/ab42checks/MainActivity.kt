package com.example.ab42checks

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.app.NotificationManager
import com.example.ab42checks.NotificationUtils
import com.example.ab42checks.databinding.ActivityMainBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.media.MediaPlayer
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
class MainActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable: Runnable = object: Runnable {
        override fun run() {
            checkPiscineStatus()
            handler.postDelayed(this, 1 * 60 * 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 0)
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.reloadButton.setOnClickListener {
            checkPiscineStatus()
        }
        binding.checkStatusButton.setOnClickListener {
            startActivity(Intent(this, StatusActivity::class.java))
        }
        binding.startServiceButton.setOnClickListener {
            val intent = Intent(this, StatusCheckService::class.java)
            ContextCompat.startForegroundService(this, intent)
        }

        // Show a sticky notification immediately and trigger an initial status check
        NotificationUtils.createChannel(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(
            NotificationUtils.NOTIFICATION_ID,
            NotificationUtils.buildNotification(this, "Checking status...")
        )
        
        handler.post(pollRunnable)
    }


    private fun checkPiscineStatus() {
        binding.piscineStatusText.text = "Loading..."
        thread {
            try {
                Log.d(TAG, "Calling piscine status API")
                val url = URL("https://42abudhabi.ae/piscine-status/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val code = connection.responseCode
                Log.d(TAG, "Piscine status response code: $code")
                val html = connection.inputStream.bufferedReader().use {
                    it.readText()
                }
                connection.disconnect()

                val open = html.split("08/2025").size - 1 > 1 ||
                    html.contains("09/2025") || html.contains("10/2025")

                val message =
                    if (code == HttpURLConnection.HTTP_OK) {
                        if (open) "Open"
                        else "No new opens"
                    } else {
                        "Error: $code"
                    }

                runOnUiThread {
                    binding.piscineStatusText.text = message
                }
                if (message == "Open") {
                    playSound()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking piscine status", e)
                runOnUiThread {
                    binding.piscineStatusText.text = "Error: ${e.message}"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
    }

    private fun playSound() {
        try {
            val assetFileDescriptor = assets.openFd("iphone.mp3")
            val mediaPlayer = MediaPlayer()

            mediaPlayer.setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.length
            )
            mediaPlayer.isLooping = true
            mediaPlayer.prepare() // or prepareAsync() for non-blocking
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound", e)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
