package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EmeraldColorScheme = lightColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.Black,
    secondary = MintTeal,
    onSecondary = Color.Black,
    tertiary = CoralAccent,
    onTertiary = Color.Black,
    background = Color(0xFFF2FBF6), // Super bright succulent mint-cream tint
    onBackground = Color(0xFF0F172A), // High contrast slate-navy for max crispness
    surface = Color(0xFFFFFFFF), // Pristine bright interactive surface
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2F9EE), // Highly vibrant green light container
    onSurfaceVariant = Color(0xFF047857), // Energetic dark mint text
    outline = Color(0xFFBCF3D6)
)

private val OceanColorScheme = lightColorScheme(
    primary = OceanSilverBlue,
    onPrimary = Color.Black,
    secondary = OceanTeal,
    onSecondary = Color.Black,
    tertiary = OceanCyanAccent,
    onTertiary = Color.Black,
    background = Color(0xFFF0F9FF), // Luminous bright sea reflection tint
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE0F2FE), // Waves blue clear light container
    onSurfaceVariant = Color(0xFF0369A1), // Intense ocean blue text
    outline = Color(0xFFBAE6FD)
)

private val CyberSunsetColorScheme = lightColorScheme(
    primary = CyberRose,
    onPrimary = Color.White,
    secondary = CyberViolet,
    onSecondary = Color.White,
    tertiary = CyberSunsetAmber,
    onTertiary = Color.Black,
    background = Color(0xFFFFF1F2), // Playful bubblegum pink base tint
    onBackground = Color(0xFF1E293B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFFFE4E6), // Vivid rose-candy light container
    onSurfaceVariant = Color(0xFFBE123C), // Cyber wine intense text
    outline = Color(0xFFFECDD3)
)

private val CrimsonColorScheme = lightColorScheme(
    primary = CrimsonScarlet,
    onPrimary = Color.White,
    secondary = CrimsonFieryOrange,
    onSecondary = Color.White,
    tertiary = CrimsonLightPink,
    onTertiary = Color.Black,
    background = Color(0xFFFFF5F5), // Sweet cherry blossom base tint
    onBackground = Color(0xFF1E293B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFFEE2E2), // Crimson dynamic light container
    onSurfaceVariant = Color(0xFFB91C1C), // Rich berry crimson text
    outline = Color(0xFFFCA5A5)
)

// Dynamic theme selector for dynamic Command Center vibe
@Composable
fun MyApplicationTheme(
    themeName: String = "EMERALD",
    content: @Composable () -> Unit
) {
    val selectedScheme = when (themeName) {
        "OCEAN" -> OceanColorScheme
        "CYBER_SUNSET" -> CyberSunsetColorScheme
        "CRIMSON" -> CrimsonColorScheme
        else -> EmeraldColorScheme
    }

    MaterialTheme(
        colorScheme = selectedScheme,
        typography = Typography,
        content = content
    )
}
