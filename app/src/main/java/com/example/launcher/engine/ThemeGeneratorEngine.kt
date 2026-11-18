package com.example.launcher.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.example.domain.model.*
import com.example.engine.LocalAiThemeEngine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class GeneratedPaletteResult(
  val theme: ThemeSpec,
  val primaryHex: Long,
  val secondaryHex: Long,
  val tertiaryHex: Long,
  val backgroundHex: Long,
  val surfaceHex: Long,
  val accentGlowHex: Long,
  val onSurfaceHex: Long,
  val harmonyStrategy: String,
  val contrastRatio: Float,
  val keywordsMatched: List<String>,
)

/**
 * Algorithmic + deterministic color-harmony ThemeGenerator engine.
 * Converts natural-language text descriptions or wallpaper bitmaps into
 * high-contrast Material 3 color palettes and launcher visual presets offline.
 */
object ThemeGeneratorEngine {

  private data class HueAnchor(
    val keywords: List<String>,
    val label: String,
    val hueDeg: Float,
    val saturation: Float,
    val lightness: Float,
  )

  private val hueAnchors =
    listOf(
      HueAnchor(listOf("crimson", "red", "ruby", "scarlet", "cherry"), "Crimson", 352f, 0.84f, 0.56f),
      HueAnchor(listOf("sunset", "orange", "coral", "tangerine", "peach"), "Sunset", 22f, 0.88f, 0.58f),
      HueAnchor(listOf("gold", "amber", "honey", "champagne", "solar"), "Amber Gold", 42f, 0.90f, 0.56f),
      HueAnchor(listOf("emerald", "green", "forest", "jade", "mint", "sage", "leaf"), "Emerald", 154f, 0.76f, 0.48f),
      HueAnchor(listOf("teal", "aqua", "coloros", "turquoise", "lagoon"), "Aqua Teal", 174f, 0.78f, 0.48f),
      HueAnchor(listOf("cyan", "ice", "electric", "sky", "arctic"), "Electric Cyan", 196f, 0.90f, 0.56f),
      HueAnchor(listOf("blue", "cobalt", "ocean", "sapphire", "navy", "indigo"), "Cobalt Blue", 224f, 0.84f, 0.58f),
      HueAnchor(listOf("purple", "violet", "amethyst", "lavender", "plum"), "Violet", 268f, 0.82f, 0.62f),
      HueAnchor(listOf("magenta", "pink", "rose", "blush", "sakura", "neon"), "Neon Rose", 322f, 0.86f, 0.60f),
    )

  fun generatePaletteFromDescription(
    description: String,
    tier: ResolvedTier,
    formFactor: DeviceFormFactor,
  ): GeneratedPaletteResult {
    val clean = description.trim().ifEmpty { "ColorOS 7 luminous cyan glass with deep obsidian dock" }
    val lower = clean.lowercase()

    val matchedAnchors = hueAnchors.filter { anchor -> anchor.keywords.any { it in lower } }
    val hashSeed = abs(clean.hashCode())
    val baseHue = matchedAnchors.firstOrNull()?.hueDeg ?: ((hashSeed % 360).toFloat())

    val isPastel = listOf("pastel", "soft", "muted", "dream", "milk").any { it in lower }
    val isNeon = listOf("neon", "cyber", "cyberpunk", "laser", "glow", "vivid").any { it in lower }
    val isAmoled = listOf("amoled", "black", "oled", "obsidian", "pitch", "void").any { it in lower }
    val isLight = !isAmoled && listOf("white", "light", "porcelain", "ivory", "day", "clean").any { it in lower }
    val isMonochrome = listOf("monochrome", "grayscale", "silver", "minimalist").any { it in lower }

    val baseSat =
      when {
        isMonochrome -> 0.12f
        isPastel -> 0.52f
        isNeon -> 0.95f
        else -> matchedAnchors.firstOrNull()?.saturation ?: 0.78f
      }
    val baseLight =
      when {
        isPastel -> 0.74f
        isLight -> 0.44f
        isNeon -> 0.62f
        else -> matchedAnchors.firstOrNull()?.lightness ?: 0.58f
      }

    // Algorithmic Color-Harmony Selection
    val (secHueOffset, terHueOffset, harmonyName) =
      when {
        matchedAnchors.size >= 2 -> {
          val h2 = matchedAnchors[1].hueDeg
          Triple((h2 - baseHue + 360f) % 360f, 180f, "Multi-Cue Split")
        }
        isNeon || "contrast" in lower -> Triple(180f, 210f, "Complementary Contrast")
        isPastel || "calm" in lower || "nature" in lower -> Triple(34f, 68f, "Analogous Silk")
        else -> Triple(45f, 150f, "Split-Complementary")
      }

    val primary = hslToColorLong(baseHue, baseSat, baseLight)
    val secondary = hslToColorLong((baseHue + secHueOffset) % 360f, (baseSat * 0.92f).coerceIn(0.15f, 0.98f), baseLight)
    val tertiary = hslToColorLong((baseHue + terHueOffset) % 360f, (baseSat * 0.85f).coerceIn(0.15f, 0.95f), (baseLight + 0.05f).coerceAtMost(0.78f))

    val background =
      when {
        isAmoled -> 0xFF000000L
        isLight -> hslToColorLong(baseHue, 0.14f, 0.96f)
        else -> hslToColorLong(baseHue, 0.34f, 0.06f)
      }
    val surface =
      when {
        isAmoled -> 0xFF0B0C12L
        isLight -> 0xFFFFFFFFL
        else -> hslToColorLong(baseHue, 0.30f, 0.12f)
      }
    val onSurface = if (isLight) 0xFF0F172AL else 0xFFF8FAFC
    val contrast = computeContrastRatio(onSurface, surface)

    // Combine with existing structural/wallpaper/glass rules from LocalAiThemeEngine
    val structuralReport = LocalAiThemeEngine.generateFromPrompt(clean, tier, formFactor)
    val themeName =
      matchedAnchors.firstOrNull()?.let { anchor ->
        val suffix =
          when {
            isAmoled -> "AMOLED"
            isNeon -> "Neon"
            isLight -> "Porcelain"
            else -> "Glass"
          }
        "${anchor.label} $suffix"
      } ?: structuralReport.theme.name

    val theme =
      structuralReport.theme.copy(
        id = "gen_${System.currentTimeMillis()}",
        name = themeName,
        description = "Algorithmic ($harmonyName): $clean",
        isBuiltIn = false,
        promptSource = clean,
        palette =
          ThemePalette(
            primaryHex = primary,
            secondaryHex = secondary,
            tertiaryHex = tertiary,
            backgroundHex = background,
            surfaceHex = surface,
            accentGlowHex = primary,
            onSurfaceHex = onSurface,
            isAmoledDark = isAmoled,
          ),
      )

    return GeneratedPaletteResult(
      theme = theme,
      primaryHex = primary,
      secondaryHex = secondary,
      tertiaryHex = tertiary,
      backgroundHex = background,
      surfaceHex = surface,
      accentGlowHex = primary,
      onSurfaceHex = onSurface,
      harmonyStrategy = harmonyName,
      contrastRatio = contrast,
      keywordsMatched = matchedAnchors.map { it.label }.ifEmpty { listOf("Hash-Derived Hue") },
    )
  }

  /**
   * Samples dominant & accent hues directly from any wallpaper bitmap offline
   * to power ColorOS-style wallpaper-based colour themes.
   */
  fun extractPaletteFromBitmap(bitmap: Bitmap, baseTheme: ThemeSpec): ThemeSpec {
    val stepX = max(1, bitmap.width / 20)
    val stepY = max(1, bitmap.height / 20)
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0L
    var bestSat = -1f
    var vibrantColor = baseTheme.palette.primaryHex.toInt()
    val hsv = FloatArray(3)

    var y = 0
    while (y < bitmap.height) {
      var x = 0
      while (x < bitmap.width) {
        val px = bitmap.getPixel(x, y)
        val r = Color.red(px)
        val g = Color.green(px)
        val b = Color.blue(px)
        rSum += r
        gSum += g
        bSum += b
        count++
        Color.colorToHSV(px, hsv)
        if (hsv[1] > bestSat && hsv[2] > 0.25f) {
          bestSat = hsv[1]
          vibrantColor = px
        }
        x += stepX
      }
      y += stepY
    }

    if (count == 0L) return baseTheme
    Color.colorToHSV(vibrantColor, hsv)
    val primaryHue = hsv[0]
    val primary = hslToColorLong(primaryHue, max(0.55f, hsv[1]), 0.60f)
    val secondary = hslToColorLong((primaryHue + 42f) % 360f, 0.75f, 0.62f)
    val tertiary = hslToColorLong((primaryHue + 165f) % 360f, 0.70f, 0.58f)
    val avgLuminance =
      ((rSum / count) * 0.299f + (gSum / count) * 0.587f + (bSum / count) * 0.114f) / 255f
    val isDark = avgLuminance < 0.65f

    return baseTheme.copy(
      id = "wp_sync_${System.currentTimeMillis()}",
      name = "Wallpaper Harmony",
      isBuiltIn = false,
      palette =
        ThemePalette(
          primaryHex = primary,
          secondaryHex = secondary,
          tertiaryHex = tertiary,
          backgroundHex = if (isDark) hslToColorLong(primaryHue, 0.30f, 0.06f) else 0xFFF4F6FBL,
          surfaceHex = if (isDark) hslToColorLong(primaryHue, 0.26f, 0.13f) else 0xFFFFFFFFL,
          accentGlowHex = primary,
          onSurfaceHex = if (isDark) 0xFFF8FAFCL else 0xFF0F172AL,
          isAmoledDark = isDark,
        ),
    )
  }

  private fun hslToColorLong(hDeg: Float, s: Float, l: Float): Long {
    val h = ((hDeg % 360f) + 360f) % 360f
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) =
      when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
      }
    val r = ((r1 + m) * 255f).toInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255f).toInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255f).toInt().coerceIn(0, 255)
    return (0xFF000000L) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
  }

  fun computeContrastRatio(fgHex: Long, bgHex: Long): Float {
    val l1 = relativeLuminance(fgHex)
    val l2 = relativeLuminance(bgHex)
    val bright = max(l1, l2)
    val dark = min(l1, l2)
    return (bright + 0.05f) / (dark + 0.05f)
  }

  private fun relativeLuminance(hex: Long): Float {
    val r = ((hex shr 16) and 0xFF) / 255f
    val g = ((hex shr 8) and 0xFF) / 255f
    val b = (hex and 0xFF) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
  }
}
