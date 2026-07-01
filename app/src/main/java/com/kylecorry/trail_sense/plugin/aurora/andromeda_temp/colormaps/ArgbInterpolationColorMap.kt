package com.kylecorry.trail_sense.plugin.aurora.andromeda_temp.colormaps

import android.graphics.Color
import androidx.annotation.ColorInt
import kotlin.math.min

open class ArgbInterpolationColorMap(private val colors: Array<Int>) : ColorMap {
    override fun getColor(percent: Float): Int {
        if (colors.isEmpty()) {
            return Color.TRANSPARENT
        }
        if (colors.size == 1) {
            return colors[0]
        }

        val clampedPercent = percent.coerceIn(0f, 1f)
        val index = (clampedPercent * (colors.size - 1)).toInt()
        val start = colors[index]
        val end = colors[min(index + 1, colors.size - 1)]
        val startPercent = index.toFloat() / (colors.size - 1)
        val endPercent = (index + 1).toFloat() / (colors.size - 1)
        val percentInColor = (clampedPercent - startPercent) / (endPercent - startPercent)
        return interpolate(start, end, percentInColor)
    }

    @ColorInt
    private fun interpolate(@ColorInt color1: Int, @ColorInt color2: Int, factor: Float): Int {
        val a1 = Color.alpha(color1)
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)
        val a2 = Color.alpha(color2)
        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)
        val a = (a1 + ((a2 - a1) * factor)).toInt()
        val r = (r1 + ((r2 - r1) * factor)).toInt()
        val g = (g1 + ((g2 - g1) * factor)).toInt()
        val b = (b1 + ((b2 - b1) * factor)).toInt()
        return Color.argb(a, r, g, b)
    }
}
