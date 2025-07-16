package com.example.ab42checks

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ab42checks.databinding.ActivityMainBinding
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.reloadButton.setOnClickListener { checkPiscine() }
        binding.checkStatusButton.setOnClickListener {
            startActivity(Intent(this, StatusActivity::class.java))
        }

        checkPiscine()
    }

    private fun checkPiscine() {
        binding.statusText.text = "Loading..."
        thread {
            try {
                val url = URL("https://apply.42abudhabi.ae/users/1225298/id_checks_users")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
                connection.setRequestProperty("accept-encoding", "gzip, deflate, br")
                connection.setRequestProperty("accept-language", "en-US,en;q=0.9")
                connection.setRequestProperty("cache-control", "no-cache")
                connection.setRequestProperty(
                    "cookie",
                    "_scid=noOSqzHmjYqvpfWkVsGDR4qYXiqs703s; _fbp=fb.1.1749910519931.318927756812476318; _tt_enable_cookie=1; _ttp=01JXQCQ107NWE8M9S3X0QG1456_.tt.1; cookieconsent_status=allow; _gid=GA1.2.581311328.1752448607; _ScCbts=%5B%5D; _sctr=1%7C1752436800000; locale=en; _gcl_au=1.1.965288889.1749910519.579472200.1752486232.1752486233; _admissions_session_production=da457ae5ca6ae36073dff5d9e368bc4f; _scid_r=ogOSqzHmjYqvpfWkVsGDR4qYXiqs703sEbmQrw; _ga=GA1.1.1596495760.1749910520; _ga_8M0TZSR8V1=GS2.1.s1752486212\$o6\$g1\$t1752487112\$j51\$l0\$h2104396700; _ga_6H0SY0TE1H=GS2.1.s1752486212\$o6\$g1\$t1752487112\$j51\$l0\$h0; ttcsi=1752486212829::rTLIzKr-CyDZx_hOdhfw.5.1752487113580; ttcsid_BTG7E331811BQC941EDG=1752486212829::H0Kntb78IEXcWFdcqwU3.5.1752487113806; ttcsid_CPHGJRJC77UAVM1484PG=1752486212830::VfLuXLo3eCPLEA_boFsY.5.1752487113806; ttcsid_CQB3KEBC77UCPKFUIH0=1752486212909::D5bsCp5FXkK9B4B9SRsM.5.1752487113806; ph_phc_w0Uj0THoEoBYOEhEmdFtz36tIi21gTdD7eINnBpF3Dc_posthog=%7B%22distinct_id%22%3A%2201976ecb-8511-7a9a-83c8-013651896d52%22%2C%22%24sesid%22%3A%5Bnull%2Cnull%2Cnull%5D%2C%22%24initial_person_info%22%3A%7B%22r%22%3A%22https%3A%2F%2Fwww.google.com%2F%22%2C%22u%22%3A%22https%3A%2F%2F42abudhabi.ae%2F%22%7D%7D"
                )
                connection.setRequestProperty("user-agent", "Mozilla/5.0 (Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36")

                val code = connection.responseCode
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

                runOnUiThread { binding.statusText.text = message }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { binding.statusText.text = "Error: ${e.message}" }
            }
        }
    }
}
