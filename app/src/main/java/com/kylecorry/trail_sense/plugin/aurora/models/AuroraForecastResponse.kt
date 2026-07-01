package com.kylecorry.trail_sense.plugin.aurora.models

import com.google.gson.annotations.SerializedName

data class AuroraForecastResponse(
    @SerializedName("Observation Time")
    val observationTime: String? = null,
    @SerializedName("Forecast Time")
    val forecastTime: String? = null,
    val coordinates: List<List<Double>> = emptyList()
)

data class AuroraForecast(
    val forecastTimeMillis: Long,
    val points: List<AuroraForecastPoint>
)

data class AuroraForecastPoint(
    val longitude: Double,
    val latitude: Double,
    val probability: Int
)
