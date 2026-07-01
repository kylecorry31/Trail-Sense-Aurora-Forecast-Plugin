package com.kylecorry.trail_sense.plugin.aurora.andromeda_temp.colormaps

import android.graphics.Color
import androidx.annotation.ColorInt

open class SampledColorMap(
    samples: Map<Float, Int>,
    interpolationResolution: Float = 0.05f
) : ColorMap {
    private val interpolator =
        ArgbInterpolationColorMap(getNewColorMap(samples, interpolationResolution))

    override fun getColor(percent: Float): Int {
        return interpolator.getColor(percent)
    }

    private fun getNewColorMap(
        samples: Map<Float, Int>,
        interpolationResolution: Float
    ): Array<Int> {
        val colorMap = mutableListOf<Int>()
        var current = 0f
        val entries = samples.entries.sortedBy { it.key }
        while (current <= 1f) {
            colorMap.add(getInterpolatedColor(current, entries))
            current += interpolationResolution
        }
        return colorMap.toTypedArray()
    }

    @ColorInt
    private fun getInterpolatedColor(
        percent: Float,
        samples: List<Map.Entry<Float, Int>>
    ): Int {
        if (samples.isEmpty()) {
            return Color.TRANSPARENT
        }
        if (samples.size == 1) {
            return samples[0].value
        }

        val clampedPercent = percent.coerceIn(0f, 1f)
        val lower = samples.lastOrNull { it.key <= clampedPercent } ?: samples.first()
        val upper = samples.firstOrNull { it.key >= clampedPercent } ?: samples.last()

        if (lower == upper) {
            return lower.value
        }

        val range = upper.key - lower.key
        val relativePercent = if (range == 0f) 0f else (clampedPercent - lower.key) / range

        return interpolate(lower.value, upper.value, relativePercent)
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
