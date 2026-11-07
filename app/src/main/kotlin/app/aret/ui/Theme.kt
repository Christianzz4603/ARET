package app.aret.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Dark = darkColorScheme(
    primary = Color(0xFF4FD8C8), onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF0F3D38), onPrimaryContainer = Color(0xFFB4F2EA),
    secondary = Color(0xFFFFB454), onSecondary = Color(0xFF2B1800),
    secondaryContainer = Color(0xFF4A3000), onSecondaryContainer = Color(0xFFFFDDB0),
    tertiary = Color(0xFFB9A4FF), onTertiary = Color(0xFF241055),
    tertiaryContainer = Color(0xFF3A2A75), onTertiaryContainer = Color(0xFFE6DEFF),
    background = Color(0xFF0B0F14), onBackground = Color(0xFFE4E9F0),
    surface = Color(0xFF0B0F14), onSurface = Color(0xFFE4E9F0),
    surfaceVariant = Color(0xFF171E29), onSurfaceVariant = Color(0xFF9AA6B8),
    surfaceContainerLowest = Color(0xFF080B0F), surfaceContainerLow = Color(0xFF0F141B),
    surfaceContainer = Color(0xFF121822), surfaceContainerHigh = Color(0xFF171E29),
    surfaceContainerHighest = Color(0xFF1D2633),
    outline = Color(0xFF3A4658), outlineVariant = Color(0xFF232D3B),
    error = Color(0xFFFF6B6B), onError = Color(0xFF3A0000),
)

private val Light = lightColorScheme(
    primary = Color(0xFF00796B), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBDF2EA), onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFFB26A00), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE3BD), onSecondaryContainer = Color(0xFF2B1800),
    tertiary = Color(0xFF6B4FD1), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE6DEFF), onTertiaryContainer = Color(0xFF241055),
    background = Color(0xFFF5F7FA), onBackground = Color(0xFF111820),
    surface = Color(0xFFF5F7FA), onSurface = Color(0xFF111820),
    surfaceVariant = Color(0xFFE6EBF1), onSurfaceVariant = Color(0xFF4A5666),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF9FAFC),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFEDF1F6),
    surfaceContainerHighest = Color(0xFFE3E9F0),
    outline = Color(0xFF8793A3), outlineVariant = Color(0xFFD7DEE7),
    error = Color(0xFFC62828), onError = Color(0xFFFFFFFF),
)

/** Shared monospace style for all code surfaces. */
val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.5.sp, lineHeight = 18.sp)

private val AretTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-1).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.5.sp),
)

@Composable
fun AretTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AretTypography,
        content = content,
    )
}
