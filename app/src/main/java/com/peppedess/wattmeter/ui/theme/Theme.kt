package com.peppedess.wattmeter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF2E6B3E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F1BE),
    onPrimaryContainer = Color(0xFF00210C),
    secondary = Color(0xFF516350),
    secondaryContainer = Color(0xFFD3E8D0),
    tertiary = Color(0xFF39656B),
    tertiaryContainer = Color(0xFFBCEBF2),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFF7FBF3),
    surface = Color(0xFFF7FBF3),
    surfaceVariant = Color(0xFFDDE5D9)
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF99D4A3),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF145229),
    onPrimaryContainer = Color(0xFFB4F1BE),
    secondary = Color(0xFFB8CCB5),
    secondaryContainer = Color(0xFF394B39),
    tertiary = Color(0xFFA1CED6),
    tertiaryContainer = Color(0xFF204D53),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF0F1511),
    surface = Color(0xFF0F1511),
    surfaceVariant = Color(0xFF414941)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WattMeterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
