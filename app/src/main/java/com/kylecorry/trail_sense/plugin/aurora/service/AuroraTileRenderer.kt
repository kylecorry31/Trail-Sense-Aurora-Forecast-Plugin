package com.kylecorry.trail_sense.plugin.aurora.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import com.kylecorry.trail_sense.plugin.aurora.models.AuroraForecastPoint
import com.kylecorry.trail_sense.plugin.aurora.models.MapTile
import com.kylecorry.trail_sense.plugin.aurora.models.MapTileLayerRequest
import com.kylecorry.trail_sense.plugin.aurora.models.TileBounds
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.tan

object AuroraTileRenderer {
    private const val TILE_SIZE = 256
    private const val CELL_SIZE_DEGREES = 1.0
    private val NOAA_COLOR_STOPS = listOf(
        ColorStop(0f, 0, 204, 0),
        ColorStop(5f, 0, 214, 0),
        ColorStop(10f, 20, 223, 17),
        ColorStop(25f, 44, 246, 4),
        ColorStop(50f, 254, 254, 0),
        ColorStop(75f, 255, 147, 2),
        ColorStop(90f, 253, 6, 0),
        ColorStop(100f, 204, 0, 0)
    )
    private val NOAA_ALPHA_STOPS = listOf(
        AlphaStop(0f, 0),
        AlphaStop(1f, 2),
        AlphaStop(5f, 12),
        AlphaStop(10f, 175),
        AlphaStop(25f, 215),
        AlphaStop(50f, 225),
        AlphaStop(90f, 235),
        AlphaStop(100f, 235)
    )

    suspend fun render(request: MapTileLayerRequest): ByteArray? {
        val forecast = AuroraForecastRepository.getLatest() ?: return null
        val tile = MapTile(request.x, request.y, request.z)
        val bounds = tile.getBounds()
        val points = forecast.points.filter { it.intersects(bounds) }

        if (points.isEmpty()) {
            return null
        }

        val bitmap = createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        for (point in points) {
            paint.color = point.color()
            drawPoint(canvas, paint, tile, point)
        }

        return bitmap.toPng()
    }

    private fun drawPoint(canvas: Canvas, paint: Paint, tile: MapTile, point: AuroraForecastPoint) {
        for (longitude in point.renderLongitudes()) {
            val west = longitude - CELL_SIZE_DEGREES / 2.0
            val east = longitude + CELL_SIZE_DEGREES / 2.0
            val north = (point.latitude + CELL_SIZE_DEGREES / 2.0).coerceAtMost(85.05112878)
            val south = (point.latitude - CELL_SIZE_DEGREES / 2.0).coerceAtLeast(-85.05112878)

            val left = longitudeToPixel(tile, west)
            val right = longitudeToPixel(tile, east)
            val top = latitudeToPixel(tile, north)
            val bottom = latitudeToPixel(tile, south)

            canvas.drawRect(left, top, right, bottom, paint)
        }
    }

    private fun AuroraForecastPoint.intersects(bounds: TileBounds): Boolean {
        val south = latitude - CELL_SIZE_DEGREES / 2.0
        val north = latitude + CELL_SIZE_DEGREES / 2.0
        if (north < bounds.south || south > bounds.north) {
            return false
        }

        return renderLongitudes().any { longitude ->
            val west = longitude - CELL_SIZE_DEGREES / 2.0
            val east = longitude + CELL_SIZE_DEGREES / 2.0
            east >= bounds.west && west <= bounds.east
        }
    }

    private fun AuroraForecastPoint.renderLongitudes(): List<Double> {
        return listOf(longitude, longitude - 360.0, longitude + 360.0)
    }

    private fun longitudeToPixel(tile: MapTile, longitude: Double): Float {
        val n = 1 shl tile.z
        val globalPixel = ((longitude + 180.0) / 360.0) * n * TILE_SIZE
        return (globalPixel - tile.x * TILE_SIZE).toFloat()
    }

    private fun latitudeToPixel(tile: MapTile, latitude: Double): Float {
        val clamped = latitude.coerceIn(-85.05112878, 85.05112878)
        val latRadians = Math.toRadians(clamped)
        val n = 1 shl tile.z
        val globalPixel = (1.0 - ln(tan(latRadians) + 1.0 / cos(latRadians)) / PI) / 2.0 * n * TILE_SIZE
        return (globalPixel - tile.y * TILE_SIZE).toFloat()
    }

    private fun AuroraForecastPoint.color(): Int {
        val scaledProbability = probability.toFloat().coerceIn(0f, 100f)
        val color = interpolateColor(scaledProbability)
        val alpha = interpolateAlpha(scaledProbability)
        return Color.argb(alpha, color.red, color.green, color.blue)
    }

    private fun interpolateColor(probability: Float): ColorStop {
        val start = NOAA_COLOR_STOPS.last { probability >= it.probability }
        val end = NOAA_COLOR_STOPS.first { probability <= it.probability }
        if (start.probability == end.probability) {
            return start
        }

        val ratio = (probability - start.probability) / (end.probability - start.probability)
        return ColorStop(
            probability,
            lerp(start.red, end.red, ratio),
            lerp(start.green, end.green, ratio),
            lerp(start.blue, end.blue, ratio)
        )
    }

    private fun interpolateAlpha(probability: Float): Int {
        val start = NOAA_ALPHA_STOPS.last { probability >= it.probability }
        val end = NOAA_ALPHA_STOPS.first { probability <= it.probability }
        if (start.probability == end.probability) {
            return start.alpha
        }

        val ratio = (probability - start.probability) / (end.probability - start.probability)
        return lerp(start.alpha, end.alpha, ratio)
    }

    private fun lerp(start: Int, end: Int, ratio: Float): Int {
        return (start + (end - start) * ratio).toInt()
    }

    private fun Bitmap.toPng(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private data class ColorStop(
        val probability: Float,
        val red: Int,
        val green: Int,
        val blue: Int
    )

    private data class AlphaStop(
        val probability: Float,
        val alpha: Int
    )
}
