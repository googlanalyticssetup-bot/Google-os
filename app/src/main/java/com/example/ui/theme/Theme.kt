package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme =
  darkColorScheme(
    primary = LuminousViolet,
    onPrimary = Color(0xFF140A26),
    primaryContainer = Color(0xFF2D1E4F),
    onPrimaryContainer = Color(0xFFEBDFFF),
    secondary = ElectricCyan,
    onSecondary = Color(0xFF042230),
    secondaryContainer = Color(0xFF10374C),
    onSecondaryContainer = Color(0xFFD0F0FF),
    tertiary = WarmAmber,
    onTertiary = Color(0xFF291A00),
    background = ObsidianVoid,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = ObsidianCard,
    outline = OutlineGlassDark,
  )

private val StudioLightColorScheme =
  lightColorScheme(
    primary = VioletDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = CyanDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF082F49),
    tertiary = AmberDeep,
    onTertiary = Color.White,
    background = LightStudioBg,
    onBackground = Color(0xFF0F172A),
    surface = LightStudioSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightStudioElevated,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0x240F172A),
  )

val StudioShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
  )

@Composable
fun ThemeStudioTheme(
  darkTheme: Boolean = true, // Obsidian studio default; still supports light/dynamic if desired
  dynamicColor: Boolean = false,
  accentOverride: Color? = null,
  secondaryOverride: Color? = null,
  content: @Composable () -> Unit,
) {
  val baseScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme || !isSystemInDarkTheme() -> StudioDarkColorScheme
      else -> StudioLightColorScheme
    }

  val finalScheme =
    if (accentOverride != null || secondaryOverride != null) {
      baseScheme.copy(
        primary = accentOverride ?: baseScheme.primary,
        secondary = secondaryOverride ?: baseScheme.secondary,
      )
    } else {
      baseScheme
    }

  MaterialTheme(
    colorScheme = finalScheme,
    typography = Typography,
    shapes = StudioShapes,
    content = content,
  )
}
