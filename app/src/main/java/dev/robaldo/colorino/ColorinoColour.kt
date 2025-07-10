package dev.robaldo.colorino

import kotlin.math.abs

data class ColorinoColour(
    val red: Int,
    val green: Int,
    val blue: Int
) {
    fun toHSL(): Triple<Float, Float, Float> {
        val r = red / 255f
        val g = green / 255f
        val b = blue / 255f

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        var h = 0f
        val l = (max + min) / 2f

        val d = max - min
        val s = if (d == 0f) 0f else d / (1f - abs(2f * l - 1f))

        if (d != 0f) {
            h = when (max) {
                r -> ((g - b) / d) % 6f
                g -> ((b - r) / d) + 2f
                else -> ((r - g) / d) + 4f
            }
            h *= 60f
            if (h < 0f) h += 360f
        }

        return Triple(h, s, l)
    }

    fun toNamedColor(): String {
        val (h, s, l) = toHSL()

        if (s < 0.4f) {
            return when {
                l < 0.3f -> "Black"
                l > 0.6f -> "White"
                else     -> "Gray"
            }
        }

        var minDiff = 360f
        var closest = "N/D"

        for ((name, hue) in predefinedColours) {
            var diff = abs(h - hue)
            if (diff > 180f) diff = 360f - diff

            if (diff < minDiff) {
                minDiff = diff
                closest = name
            }
        }

        return closest
    }

    companion object {
        val predefinedColours = listOf(
            "Red" to 11f,
            "Green" to 123f,
            "Blue" to 243f,
            "Yellow" to 60f,
            "Orange" to 27f,
            "Light Blue" to 181f,
            "Magenta" to 298f,
            "Purple" to 279f
        )
    }
}
