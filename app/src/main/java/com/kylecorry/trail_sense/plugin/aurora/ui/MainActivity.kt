package com.kylecorry.trail_sense.plugin.aurora.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.ColorTheme
import com.kylecorry.trail_sense.plugin.aurora.R
import com.kylecorry.trail_sense.plugin.aurora.service.AuroraForecastRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : AndromedaActivity() {

    private val permissions = mutableListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private val scope = MainScope()
    private val timeFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        setColorTheme(ColorTheme.System, true)
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        requestPermissions(permissions) {}

        setContentView(R.layout.activity_main)

        bindLayoutInsets()
        bindForecastTimes()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent ?: return
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun bindLayoutInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }
            windowInsets
        }
    }

    private fun bindForecastTimes() {
        val lastUpdate = findViewById<TextView>(R.id.last_update)
        val forecastTime = findViewById<TextView>(R.id.forecast_time)

        scope.launch {
            val forecast = withContext(Dispatchers.IO) {
                AuroraForecastRepository.getLatest()
            }

            if (forecast == null) {
                lastUpdate.setText(R.string.forecast_unavailable)
                forecastTime.text = ""
                return@launch
            }

            lastUpdate.text = getString(
                R.string.last_update_time,
                formatTime(forecast.observationTimeMillis)
            )
            forecastTime.text = getString(
                R.string.forecast_time,
                formatTime(forecast.forecastTimeMillis)
            )
        }
    }

    private fun formatTime(timeMillis: Long): String {
        return timeFormatter.format(Instant.ofEpochMilli(timeMillis))
    }
}
