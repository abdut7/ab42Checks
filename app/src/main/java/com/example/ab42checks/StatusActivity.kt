package com.example.ab42checks

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.ab42checks.databinding.ActivityStatusBinding

class StatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatusBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.statusToolbar)
        setupWebView()
        binding.statusWebView.loadUrl("https://42abudhabi.ae/piscine-status/")
    }

    private fun setupWebView() {
        binding.statusWebView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
        }
    }
}
