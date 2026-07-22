package com.wwwescape.pixelebookreader.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.wwwescape.pixelebookreader.data.settings.CURATED_COLOR_THEMES
import com.wwwescape.pixelebookreader.data.settings.ThemeContrast

/**
 * Pixel E-Book Reader theme: uses Material You dynamic color on Android 12+ when enabled,
 * otherwise a curated theme (see [CURATED_COLOR_THEMES]) or the bundled oxblood default palette
 * (see Color.kt) — see [resolvePixelEBookReaderColorScheme] for the full resolution order.
 */
@Composable
fun PixelEBookReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    colorThemeId: String? = null,
    themeContrast: ThemeContrast = ThemeContrast.STANDARD,
    pureDark: Boolean = false,
    absoluteDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = resolvePixelEBookReaderColorScheme(
        context = LocalContext.current,
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        colorThemeId = colorThemeId,
        themeContrast = themeContrast,
        pureDark = pureDark,
        absoluteDark = absoluteDark,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PixelEBookReaderTypography,
        shapes = PixelEBookReaderShapes,
        content = content,
    )
}

/**
 * The same colorScheme resolution [PixelEBookReaderTheme] uses, extracted as a plain function
 * so future non-Composable callers (e.g. a foreground-service notification) can mirror the
 * app's colors exactly instead of drifting to a separately resolved scheme.
 *
 * Resolution order: Material You dynamic color (if enabled and supported) beats everything else
 * — matching sibling-app convention — then a curated theme if [colorThemeId] names one, else the
 * app's own hand-authored default. [themeContrast] and pure/absolute dark are applied as a final
 * pass on top of whichever base scheme was picked, so they work uniformly regardless of source.
 */
fun resolvePixelEBookReaderColorScheme(
    context: Context,
    darkTheme: Boolean,
    dynamicColor: Boolean,
    colorThemeId: String? = null,
    themeContrast: ThemeContrast = ThemeContrast.STANDARD,
    pureDark: Boolean = false,
    absoluteDark: Boolean = false,
): ColorScheme {
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        colorThemeId != null -> {
            val theme = CURATED_COLOR_THEMES.find { it.id == colorThemeId }
            if (theme != null) generateColorScheme(theme.seedHue, darkTheme) else defaultStaticColorScheme(darkTheme)
        }
        else -> defaultStaticColorScheme(darkTheme)
    }
    val contrasted = base.withContrast(themeContrast)
    return if (darkTheme) contrasted.withDarkIntensity(pureDark, absoluteDark) else contrasted
}

private fun defaultStaticColorScheme(darkTheme: Boolean): ColorScheme = if (darkTheme) {
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        inversePrimary = DarkInversePrimary,
        secondary = DarkSecondary,
        onSecondary = DarkOnSecondary,
        secondaryContainer = DarkSecondaryContainer,
        onSecondaryContainer = DarkOnSecondaryContainer,
        tertiary = DarkTertiary,
        onTertiary = DarkOnTertiary,
        tertiaryContainer = DarkTertiaryContainer,
        onTertiaryContainer = DarkOnTertiaryContainer,
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        surfaceTint = DarkSurfaceTint,
        inverseSurface = DarkInverseSurface,
        inverseOnSurface = DarkInverseOnSurface,
        error = DarkError,
        onError = DarkOnError,
        errorContainer = DarkErrorContainer,
        onErrorContainer = DarkOnErrorContainer,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant,
        surfaceBright = DarkSurfaceBright,
        surfaceContainer = DarkSurfaceContainer,
        surfaceContainerHigh = DarkSurfaceContainerHigh,
        surfaceContainerHighest = DarkSurfaceContainerHighest,
        surfaceContainerLow = DarkSurfaceContainerLow,
        surfaceContainerLowest = DarkSurfaceContainerLowest,
        surfaceDim = DarkSurfaceDim,
        primaryFixed = PrimaryFixed,
        primaryFixedDim = PrimaryFixedDim,
        onPrimaryFixed = OnPrimaryFixed,
        onPrimaryFixedVariant = OnPrimaryFixedVariant,
        secondaryFixed = SecondaryFixed,
        secondaryFixedDim = SecondaryFixedDim,
        onSecondaryFixed = OnSecondaryFixed,
        onSecondaryFixedVariant = OnSecondaryFixedVariant,
        tertiaryFixed = TertiaryFixed,
        tertiaryFixedDim = TertiaryFixedDim,
        onTertiaryFixed = OnTertiaryFixed,
        onTertiaryFixedVariant = OnTertiaryFixedVariant,
    )
} else {
    lightColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer,
        onPrimaryContainer = OnPrimaryContainer,
        inversePrimary = InversePrimary,
        secondary = Secondary,
        onSecondary = OnSecondary,
        secondaryContainer = SecondaryContainer,
        onSecondaryContainer = OnSecondaryContainer,
        tertiary = Tertiary,
        onTertiary = OnTertiary,
        tertiaryContainer = TertiaryContainer,
        onTertiaryContainer = OnTertiaryContainer,
        background = Background,
        onBackground = OnBackground,
        surface = Surface,
        onSurface = OnSurface,
        surfaceVariant = SurfaceVariant,
        onSurfaceVariant = OnSurfaceVariant,
        surfaceTint = SurfaceTint,
        inverseSurface = InverseSurface,
        inverseOnSurface = InverseOnSurface,
        error = Error,
        onError = OnError,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
        outline = Outline,
        outlineVariant = OutlineVariant,
        surfaceBright = SurfaceBright,
        surfaceContainer = SurfaceContainer,
        surfaceContainerHigh = SurfaceContainerHigh,
        surfaceContainerHighest = SurfaceContainerHighest,
        surfaceContainerLow = SurfaceContainerLow,
        surfaceContainerLowest = SurfaceContainerLowest,
        surfaceDim = SurfaceDim,
        primaryFixed = PrimaryFixed,
        primaryFixedDim = PrimaryFixedDim,
        onPrimaryFixed = OnPrimaryFixed,
        onPrimaryFixedVariant = OnPrimaryFixedVariant,
        secondaryFixed = SecondaryFixed,
        secondaryFixedDim = SecondaryFixedDim,
        onSecondaryFixed = OnSecondaryFixed,
        onSecondaryFixedVariant = OnSecondaryFixedVariant,
        tertiaryFixed = TertiaryFixed,
        tertiaryFixedDim = TertiaryFixedDim,
        onTertiaryFixed = OnTertiaryFixed,
        onTertiaryFixedVariant = OnTertiaryFixedVariant,
    )
}
