package com.example.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

object PinColorHelper {
    // 24 distinct, high-contrast vibrant colors for pins and matching event cards
    val PALETTE = listOf(
        "#4F46E5", // Indigo
        "#059669", // Emerald
        "#D97706", // Amber
        "#E11D48", // Rose
        "#7C3AED", // Violet
        "#0284C7", // Sky Blue
        "#EA580C", // Warm Orange
        "#0D9488", // Teal
        "#DB2777", // Pink
        "#2563EB", // Royal Blue
        "#16A34A", // Green
        "#9333EA", // Purple
        "#CA8A04", // Dark Gold
        "#DC2626", // Red
        "#0891B2", // Cyan
        "#C026D3", // Fuchsia
        "#4338CA", // Deep Indigo
        "#047857", // Deep Emerald
        "#B45309", // Deep Amber
        "#BE123C", // Deep Rose
        "#6D28D9", // Deep Violet
        "#0369A1", // Deep Sky
        "#C2410C", // Deep Orange
        "#0F766E"  // Deep Teal
    )

    fun getColorForLocation(locationName: String, existingLocationIndex: Int = -1): String {
        if (existingLocationIndex >= 0) {
            return PALETTE[existingLocationIndex % PALETTE.size]
        }
        val hash = abs(locationName.trim().lowercase().hashCode())
        return PALETTE[hash % PALETTE.size]
    }

    /**
     * Pick a color that is guaranteed NOT present in [usedColors].
     * Even if a venue has no future events, its color is considered used and will not be reused.
     */
    fun getUnusedOrDistinctColor(usedColors: Set<String>, seedName: String = ""): String {
        val normalizedUsed = usedColors.map { it.trim().uppercase() }.toSet()
        val available = PALETTE.filter { it.uppercase() !in normalizedUsed }
        if (available.isNotEmpty()) {
            if (seedName.isNotBlank()) {
                val idx = abs(seedName.trim().lowercase().hashCode()) % available.size
                return available[idx]
            }
            return available.first()
        }

        // If all 24 palette colors are already used, generate a distinct new color via golden ratio
        var hue = if (seedName.isNotBlank()) (abs(seedName.hashCode()) % 360).toFloat() else 45f
        for (i in 0..72) {
            val hex = hslToHex(hue, 0.75f, 0.48f)
            if (hex.uppercase() !in normalizedUsed) {
                return hex
            }
            hue = (hue + 137.508f) % 360f // Golden angle distribution
        }
        return PALETTE.first()
    }

    private fun hslToHex(h: Float, s: Float, l: Float): String {
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val red = ((r + m) * 255f).toInt().coerceIn(0, 255)
        val green = ((g + m) * 255f).toInt().coerceIn(0, 255)
        val blue = ((b + m) * 255f).toInt().coerceIn(0, 255)
        return String.format(java.util.Locale.US, "#%02X%02X%02X", red, green, blue)
    }

    fun parseColor(hex: String, defaultColor: Color = Color(0xFF4F46E5)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val colorLong = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color(colorLong or 0x00000000FF000000L)
            } else if (cleanHex.length == 8) {
                Color(colorLong)
            } else {
                defaultColor
            }
        } catch (_: Exception) {
            defaultColor
        }
    }

    fun getHue(hex: String): Float {
        val color = parseColor(hex)
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        if (delta == 0f) return 0f

        val hue = when (max) {
            r -> ((g - b) / delta) % 6f
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        } * 60f

        return if (hue < 0f) hue + 360f else hue
    }
}
