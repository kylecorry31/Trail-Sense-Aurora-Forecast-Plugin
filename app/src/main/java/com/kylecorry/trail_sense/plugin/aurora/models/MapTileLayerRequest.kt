package com.kylecorry.trail_sense.plugin.aurora.models

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sinh

data class MapTileLayerRequest(
    val x: Int,
    val y: Int,
    val z: Int,
    val time: Long
)

data class MapTile(
    val x: Int,
    val y: Int,
    val z: Int
) {
    fun getBounds(): TileBounds {
        val n = 1 shl z
        val west = x / n.toDouble() * 360.0 - 180.0
        val east = (x + 1) / n.toDouble() * 360.0 - 180.0
        val south = Math.toDegrees(atan(sinh(PI * (1 - 2 * (y + 1).toDouble() / n))))
        val north = Math.toDegrees(atan(sinh(PI * (1 - 2 * y.toDouble() / n))))
        return TileBounds(north, east, south, west)
    }
}

data class TileBounds(
    val north: Double,
    val east: Double,
    val south: Double,
    val west: Double
)

fun MapTileLayerRequest.getTile(): MapTile {
    return MapTile(x, y, z)
}
