package com.kylecorry.trail_sense.plugin.aurora.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.science.geography.projections.MercatorProjection
import com.kylecorry.trail_sense.plugin.aurora.andromeda_temp.MAX_LATITUDE
import com.kylecorry.trail_sense.plugin.aurora.andromeda_temp.MIN_LATITUDE
import com.kylecorry.trail_sense.plugin.aurora.andromeda_temp.colormaps.SampledColorMap
import com.kylecorry.trail_sense.plugin.aurora.models.AuroraForecastPoint
import com.kylecorry.trail_sense.plugin.aurora.models.MapTile
import com.kylecorry.trail_sense.plugin.aurora.models.MapTileLayerRequest
import com.kylecorry.trail_sense.plugin.aurora.models.TileBounds
import java.io.ByteArrayOutputStream
import kotlin.math.PI

object AuroraTileRenderer {
    private const val TILE_SIZE = 256
    private const val CELL_SIZE_DEGREES = 1.0
    private val NOAA_COLOR_MAP = SampledColorMap(
        mapOf(
            0f to Color.argb(0, 0, 204, 0),
            0.025f to Color.argb(16, 0, 214, 0),
            0.05f to Color.argb(64, 0, 214, 0),
            0.10f to Color.argb(255, 20, 223, 17),
            0.25f to Color.argb(255, 44, 246, 4),
            0.50f to Color.argb(255, 254, 254, 0),
            0.75f to Color.argb(255, 255, 147, 2),
            0.90f to Color.argb(255, 253, 6, 0),
            1f to Color.argb(255, 204, 0, 0)
        ),
        interpolationResolution = 0.01f
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
            val north = (point.latitude + CELL_SIZE_DEGREES / 2.0).coerceAtMost(MercatorProjection.MAX_LATITUDE)
            val south = (point.latitude - CELL_SIZE_DEGREES / 2.0).coerceAtLeast(MercatorProjection.MIN_LATITUDE)

            val topLeft = toPixels(tile, north, west)
            val bottomRight = toPixels(tile, south, east)
            canvas.drawRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, paint)
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

    private fun toPixels(tile: MapTile, latitude: Double, longitude: Double): Vector2 {
        val n = 1 shl tile.z
        val projection = MercatorProjection(tileScale(n))
        val clamped = latitude.coerceIn(MercatorProjection.MIN_LATITUDE, MercatorProjection.MAX_LATITUDE)
        val projected = projection.toPixels(clamped, longitude)
        val globalX = projected.x + worldSize(n) / 2.0
        val globalY = worldSize(n) / 2.0 - projected.y
        return Vector2(
            (globalX - tile.x * TILE_SIZE).toFloat(),
            (globalY - tile.y * TILE_SIZE).toFloat()
        )
    }

    private fun tileScale(tileCount: Int): Float {
        return (worldSize(tileCount) / (2.0 * PI)).toFloat()
    }

    private fun worldSize(tileCount: Int): Double {
        return tileCount * TILE_SIZE.toDouble()
    }

    private fun AuroraForecastPoint.color(): Int {
        val scaledProbability = probability.toFloat().coerceIn(0f, 100f)
        return NOAA_COLOR_MAP.getColor(scaledProbability / 100f)
    }

    private fun Bitmap.toPng(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
