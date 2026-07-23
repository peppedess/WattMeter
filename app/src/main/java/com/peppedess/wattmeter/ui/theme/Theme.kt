package com.peppedess.wattmeter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/* Verde energia come colore portante, ciano per i dati, magenta per gli accenti. */

private val LightScheme = lightColorScheme(
    primary = Color(0xFF00A050),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF8DF7B0),
    onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF0090C4),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB6E9FF),
    onSecondaryContainer = Color(0xFF001E2C),
    tertiary = Color(0xFF8B45D6),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEEDCFF),
    onTertiaryContainer = Color(0xFF29003F),
    error = Color(0xFFC4231A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD5),
    onErrorContainer = Color(0xFF410001),
    background = Color(0xFFF4FBF4),
    onBackground = Color(0xFF0E1F14),
    surface = Color(0xFFF4FBF4),
    onSurface = Color(0xFF0E1F14),
    surfaceVariant = Color(0xFFD8E8D9),
    onSurfaceVariant = Color(0xFF3F4B42),
    outline = Color(0xFF6F7B71),
    outlineVariant = Color(0xFFBFCFC1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDF7EE),
    surfaceContainer = Color(0xFFE7F3E9),
    surfaceContainerHigh = Color(0xFFE1EEE3),
    surfaceContainerHighest = Color(0xFFDBE9DE),
    inverseSurface = Color(0xFF223328),
    inverseOnSurface = Color(0xFFE9F5EA),
    inversePrimary = Color(0xFF52DE8C),
    scrim = Color(0xFF000000)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF43E68A),
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF006B37),
    onPrimaryContainer = Color(0xFF8DF7B0),
    secondary = Color(0xFF6FD6FF),
    onSecondary = Color(0xFF003549),
    secondaryContainer = Color(0xFF004D69),
    onSecondaryContainer = Color(0xFFB6E9FF),
    tertiary = Color(0xFFD9B4FF),
    onTertiary = Color(0xFF460066),
    tertiaryContainer = Color(0xFF6B2CA8),
    onTertiaryContainer = Color(0xFFEEDCFF),
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF690002),
    errorContainer = Color(0xFF930006),
    onErrorContainer = Color(0xFFFFDAD5),
    background = Color(0xFF08150D),
    onBackground = Color(0xFFDFEDE2),
    surface = Color(0xFF08150D),
    onSurface = Color(0xFFDFEDE2),
    surfaceVariant = Color(0xFF3F4B42),
    onSurfaceVariant = Color(0xFFBFCFC1),
    outline = Color(0xFF89998C),
    outlineVariant = Color(0xFF3F4B42),
    surfaceContainerLowest = Color(0xFF031007),
    surfaceContainerLow = Color(0xFF111E16),
    surfaceContainer = Color(0xFF15221A),
    surfaceContainerHigh = Color(0xFF1F2D24),
    surfaceContainerHighest = Color(0xFF2A382E),
    inverseSurface = Color(0xFFDFEDE2),
    inverseOnSurface = Color(0xFF1B2C21),
    inversePrimary = Color(0xFF00A050),
    scrim = Color(0xFF000000)
)

/** Colori dedicati agli stati energetici, indipendenti dallo schema Material. */
@Immutable
data class EnergyColors(
    val fast: Color,
    val fastContainer: Color,
    val normal: Color,
    val normalContainer: Color,
    val drain: Color,
    val drainContainer: Color,
    val heat: Color
)

private val LightEnergy = EnergyColors(
    fast = Color(0xFF00A050),
    fastContainer = Color(0xFFB9F7CF),
    normal = Color(0xFF0090C4),
    normalContainer = Color(0xFFC3ECFF),
    drain = Color(0xFFE07B00),
    drainContainer = Color(0xFFFFE2B8),
    heat = Color(0xFFE23D2E)
)

private val DarkEnergy = EnergyColors(
    fast = Color(0xFF43E68A),
    fastContainer = Color(0xFF01572C),
    normal = Color(0xFF6FD6FF),
    normalContainer = Color(0xFF00415A),
    drain = Color(0xFFFFB35C),
    drainContainer = Color(0xFF5C3600),
    heat = Color(0xFFFF8A75)
)

@Composable
fun energyColors(): EnergyColors =
    if (isSystemInDarkTheme()) DarkEnergy else LightEnergy

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WattMeterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        content = content
    )
}
