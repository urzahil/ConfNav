package rocks.urzahil.confnav.wear

import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.types.argb

/**
 * The phone app's dark theme (`ui/theme/Color.kt`) as a tile colour scheme. Tiles are always
 * drawn on black, so only the dark palette applies.
 */
object ConfNavWearTheme {

    val colorScheme = ColorScheme(
        primary = 0xFF4ADE80.argb,
        primaryDim = 0xFF15803D.argb,
        primaryContainer = 0xFF166534.argb,
        onPrimary = 0xFF052E16.argb,
        onPrimaryContainer = 0xFFDCFCE7.argb,
        secondary = 0xFF2DD4BF.argb,
        secondaryDim = 0xFF0F766E.argb,
        secondaryContainer = 0xFF115E59.argb,
        onSecondary = 0xFF042F2E.argb,
        onSecondaryContainer = 0xFFCCFBF1.argb,
        tertiary = 0xFFA3E635.argb,
        tertiaryDim = 0xFF65A30D.argb,
        tertiaryContainer = 0xFF2C5000.argb,
        onTertiary = 0xFF1A3800.argb,
        onTertiaryContainer = 0xFFBFFF71.argb,
        surfaceContainerLow = 0xFF0E1A14.argb,
        surfaceContainer = 0xFF13221A.argb,
        surfaceContainerHigh = 0xFF1A3025.argb,
        onSurface = 0xFFECFDF5.argb,
        onSurfaceVariant = 0xFF9CA3AF.argb,
        outline = 0xFF6B7280.argb,
        outlineVariant = 0xFF374151.argb,
        background = 0xFF000000.argb,
        onBackground = 0xFFECFDF5.argb
    )

    /** A session's `#RRGGBB` pin colour as ARGB, with the phone app's fallback for bad values. */
    fun sessionColor(hex: String): Int {
        val clean = hex.trim().removePrefix("#")
        val value = clean.toLongOrNull(16) ?: return DEFAULT_SESSION_COLOR
        return when (clean.length) {
            6 -> (value or 0xFF000000L).toInt()
            8 -> value.toInt()
            else -> DEFAULT_SESSION_COLOR
        }
    }

    /** [argb] at [alpha] (0-1) opacity, like Compose's `Color.copy(alpha = ...)`. */
    fun withAlpha(argb: Int, alpha: Float): Int =
        ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or (argb and 0x00FFFFFF)

    private const val DEFAULT_SESSION_COLOR = 0xFF4F46E5.toInt()
}
