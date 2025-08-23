package com.example.ab42checks

import android.content.Intent
import android.content.SharedPreferences
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
import com.example.ab42checks.CookieStore
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class MainActivity: AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable: Runnable = object: Runnable {
        override fun run() {
            checkPiscine()
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

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        updateLastApiText()

        setSupportActionBar(binding.toolbar)

        binding.reloadButton.setOnClickListener {
            checkPiscine()
        }
        binding.checkStatusButton.setOnClickListener {
            startActivity(Intent(this, StatusActivity::class.java))
        }
        binding.startServiceButton.setOnClickListener {
            val intent = Intent(this, StatusCheckService::class.java)
            ContextCompat.startForegroundService(this, intent)
        }
        binding.editCookieButton.setOnClickListener {
            binding.cookieInput.visibility = View.VISIBLE
            binding.saveCookieButton.visibility = View.VISIBLE
            binding.cookieInput.setText(prefs.getString("cookie", CookieStore.DEFAULT_COOKIE))
        }
        binding.saveCookieButton.setOnClickListener {
            val cookie = binding.cookieInput.text.toString().replace("\$", "\\$")
            prefs.edit().putString("cookie", cookie).apply()
            binding.cookieInput.visibility = View.GONE
            binding.saveCookieButton.visibility = View.GONE
        }

        binding.cookieInput.setText(prefs.getString("cookie", CookieStore.DEFAULT_COOKIE))

        // Show a sticky notification immediately and trigger an initial status check
        NotificationUtils.createChannel(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(
            NotificationUtils.NOTIFICATION_ID,
            NotificationUtils.buildNotification(this, "Checking status...")
        )

        handler.post(pollRunnable)
    }

    private fun checkPiscine() {
        binding.statusText.text = "Loading..."
        thread {
            try {
                Log.d(TAG, "Calling piscine API")
                val url = URL("https://apply.42abudhabi.ae/users/1225298/id_checks_users")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
                connection.setRequestProperty("accept-encoding", "gzip, deflate, br")
                connection.setRequestProperty("accept-language", "en-US,en;q=0.9")
                connection.setRequestProperty("cache-control", "no-cache")
                val cookie = prefs.getString("cookie", CookieStore.DEFAULT_COOKIE) ?: CookieStore.DEFAULT_COOKIE
                connection.setRequestProperty("cookie", cookie)
                connection.setRequestProperty("user-agent", "Mozilla/5.0 (Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36")

                val code = connection.responseCode
                Log.d(TAG, "API response code: $code")
                val html = connection.inputStream.bufferedReader().use {
                    it.readText()
                }
                connection.disconnect()

                val message =
                    if (code == HttpURLConnection.HTTP_OK) {
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
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(
                    NotificationUtils.NOTIFICATION_ID,
                    NotificationUtils.buildNotification(this@MainActivity, message)
                )
                runOnUiThread {
                    binding.statusText.text = message
                    updateLastApiText()
                }
                if (message == "Available") {
                    playSound()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking piscine", e)
                runOnUiThread {
                    binding.statusText.text = "Error: ${e.message}"
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

    private fun updateLastApiText() {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val api = prefs.getLong("last_api_call", 0L)
        val apiText = if (api != 0L) formatter.format(Date(api)) else "never"
        binding.lastApiText.text = "Last API: $apiText"
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
