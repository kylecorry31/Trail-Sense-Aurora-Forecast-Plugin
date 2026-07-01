package com.kylecorry.trail_sense.plugin.aurora.service

import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.andromeda.net.HttpClient
import com.kylecorry.trail_sense.plugin.aurora.models.AuroraForecast
import com.kylecorry.trail_sense.plugin.aurora.models.AuroraForecastPoint
import com.kylecorry.trail_sense.plugin.aurora.models.AuroraForecastResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant

object AuroraForecastRepository {
    private const val URL = "https://services.swpc.noaa.gov/json/ovation_aurora_latest.json"
    private const val CACHE_INVALIDATION_MARGIN_MILLIS = 60_000L

    private val client = HttpClient()
    private val mutex = Mutex()
    private var cached: CachedForecast? = null

    suspend fun getLatest(): AuroraForecast? {
        val now = System.currentTimeMillis()
        cached?.takeIf { now < it.expiresAtMillis }?.let { return it.forecast }

        return mutex.withLock {
            val lockedNow = System.currentTimeMillis()
            cached?.takeIf { lockedNow < it.expiresAtMillis }?.let { return@withLock it.forecast }

            val response = try {
                client.send(
                    URL,
                    readTimeout = Duration.ofSeconds(10),
                    connectTimeout = Duration.ofSeconds(10)
                )
            } catch (_: Exception) {
                return@withLock null
            }

            if (!response.isSuccessful()) {
                return@withLock null
            }

            val body = response.contentAsString() ?: return@withLock null
            val forecast = JsonConvert.fromJson<AuroraForecastResponse>(body)?.toForecast()
                ?: return@withLock null
            val expiresAt = forecast.forecastTimeMillis - CACHE_INVALIDATION_MARGIN_MILLIS

            if (lockedNow >= expiresAt) {
                cached = null
                return@withLock null
            }

            cached = CachedForecast(forecast, expiresAt)
            forecast
        }
    }

    private fun AuroraForecastResponse.toForecast(): AuroraForecast? {
        val observationTime =
            observationTime?.let { Instant.parse(it).toEpochMilli() } ?: return null
        val forecastTime = forecastTime?.let { Instant.parse(it).toEpochMilli() } ?: return null
        val points = coordinates.mapNotNull { coordinate ->
            if (coordinate.size < 3) {
                return@mapNotNull null
            }

            val probability = coordinate[2].toInt()
            // SPWC produces false readings near the equator and some readings are just noise
            if (coordinate[1] in -5.0..5.0 && probability < 5) {
                return@mapNotNull null
            }

            AuroraForecastPoint(
                longitude = normalizeLongitude(coordinate[0]),
                latitude = coordinate[1],
                probability = probability
            )
        }

        return AuroraForecast(observationTime, forecastTime, points)
    }

    private fun normalizeLongitude(longitude: Double): Double {
        return if (longitude > 180.0) {
            longitude - 360.0
        } else {
            longitude
        }
    }

    private data class CachedForecast(
        val forecast: AuroraForecast,
        val expiresAtMillis: Long
    )
}
