package com.example.engine

import com.example.domain.model.*
import kotlin.math.abs

data class AiGenerationReport(
  val theme: ThemeSpec,
  val detectedColors: List<String>,
  val detectedStyles: List<String>,
  val rationale: List<String>,
  val optimizationNote: String,
)

object LocalAiThemeEngine {

  private data class ColorRule(
    val keywords: List<String>,
    val label: String,
    val primary: Long,
    val secondary: Long,
    val tertiary: Long,
    val glow: Long,
  )

  private val colorRules =
    listOf(
      ColorRule(listOf("purple", "violet", "amethyst", "lavender", "indigo"), "Violet", 0xFFA855F7, 0xFF38BDF8, 0xFFE879F9, 0xFFC084FC),
      ColorRule(listOf("cyan", "blue", "azure", "cobalt", "ice", "sapphire"), "Electric Blue", 0xFF38BDF8, 0xFF6366F1, 0xFF22D3EE, 0xFF00E5FF),
      ColorRule(listOf("emerald", "green", "jade", "mint", "forest", "nature"), "Emerald", 0xFF10B981, 0xFF34D399, 0xFF2DD4BF, 0xFF34D399),
      ColorRule(listOf("gold", "amber", "champagne", "luxury", "royal"), "24k Gold", 0xFFF59E0B, 0xFFFDE68A, 0xFFD97706, 0xFFFBBF24),
      ColorRule(listOf("pink", "rose", "magenta", "blush", "coral"), "Rose", 0xFFF472B6, 0xFFC084FC, 0xFFFB7185, 0xFFFF2A85),
      ColorRule(listOf("red", "crimson", "ruby", "scarlet"), "Crimson", 0xFFEF4444, 0xFFF97316, 0xFFF43F5E, 0xFFFF3B5C),
      ColorRule(listOf("orange", "sunset", "copper", "warm"), "Amber Sunset", 0xFFF97316, 0xFFFACC15, 0xFFFB7185, 0xFFFB923C),
      ColorRule(listOf("silver", "white", "ivory", "porcelain", "clean"), "Porcelain", 0xFF1E293B, 0xFF0EA5E9, 0xFF64748B, 0xFF38BDF8),
    )

  fun generateFromPrompt(
    rawPrompt: String,
    tier: ResolvedTier,
    formFactor: DeviceFormFactor,
  ): AiGenerationReport {
    val prompt = rawPrompt.trim().ifEmpty { "Premium glass theme with AMOLED black wallpaper and glowing icons" }
    val lower = prompt.lowercase()
    val tokens = lower.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()

    val matchedColors = colorRules.filter { rule -> rule.keywords.any { kw -> kw in lower } }
    val primaryRule = matchedColors.firstOrNull() ?: colorRules[0] // default Violet

    val isAmoled = listOf("amoled", "black", "obsidian", "oled", "pitch", "dark").any { it in lower }
    val isLightMinimal =
      !isAmoled && listOf("white", "light", "ivory", "porcelain", "bright").any { it in lower }

    val isGlass = listOf("glass", "frosted", "blur", "crystal", "acrylic").any { it in lower }
    val isLiquid = listOf("liquid", "fluid", "water", "drop").any { it in lower }
    val isTransparent = listOf("transparent", "clear", "translucent", "see-through").any { it in lower }
    val isNeon = listOf("neon", "glow", "glowing", "cyber", "cyberpunk", "laser").any { it in lower }
    val isLuxury = listOf("luxury", "premium", "elegant", "gold", "royal", "editorial").any { it in lower }
    val isMinimal = listOf("minimal", "clean", "simple", "modern", "coloros").any { it in lower }
    val isPastel = listOf("pastel", "soft", "dream", "cute").any { it in lower }
    val isGalaxy = listOf("galaxy", "space", "cosmic", "star", "nebula", "universe").any { it in lower }
    val isNature = listOf("nature", "forest", "aurora", "mountain", "leaf", "organic").any { it in lower }
    val is3D = listOf("3d", "depth", "floating", "sculpted").any { it in lower }
    val isFuturistic = listOf("futuristic", "hud", "sci-fi", "scifi", "holo", "tech").any { it in lower }
    val isReducedMotion = listOf("static", "no animation", "calm", "battery", "lite").any { it in lower }

    val bgHex =
      when {
        isAmoled -> 0xFF000000
        isLightMinimal -> 0xFFF4F6FB
        isPastel -> 0xFF1A1428
        else -> 0xFF070914
      }
    val surfaceHex =
      when {
        isAmoled -> 0xFF0A0A0F
        isLightMinimal -> 0xFFFFFFFF
        isPastel -> 0xFF281F3D
        else -> 0xFF13172B
      }
    val onSurfaceHex = if (isLightMinimal) 0xFF0F172A else 0xFFF8FAFC

    val wallpaperStyle =
      when {
        isGalaxy -> WallpaperStyle.GALAXY_NEBULA
        isNeon && ("cyber" in lower || "grid" in lower) -> WallpaperStyle.CYBER_GRID
        isNature -> WallpaperStyle.NATURE_AURORA
        isLiquid -> WallpaperStyle.FRACTAL_WAVE
        isAmoled -> WallpaperStyle.AMOLED_GLOW
        isGlass || isTransparent -> WallpaperStyle.ABSTRACT_PRISM
        isMinimal || isPastel -> WallpaperStyle.MINIMAL_GRADIENT
        else -> WallpaperStyle.GLASS_RIBBON
      }

    val glassStyle =
      when {
        isLiquid -> GlassStyle.LIQUID
        isGlass -> GlassStyle.GLASSMORPHISM
        isTransparent -> GlassStyle.TRANSPARENT
        is3D -> GlassStyle.THREE_D
        isFuturistic || isNeon -> GlassStyle.CRYSTAL
        else -> GlassStyle.FROSTED
      }

    val iconStyle =
      when {
        "transparent icon" in lower -> IconStyle.TRANSPARENT
        isNeon -> IconStyle.NEON
        is3D -> IconStyle.THREE_D
        isGlass -> IconStyle.GLASS
        isTransparent -> IconStyle.TRANSPARENT
        isAmoled -> IconStyle.AMOLED
        isPastel -> IconStyle.GRADIENT
        isMinimal -> IconStyle.MINIMAL
        else -> IconStyle.ROUNDED
      }

    val clockStyle =
      when {
        isLuxury || "elegant" in lower || "serif" in lower -> ClockStyle.EDITORIAL_SERIF
        isNeon || isFuturistic -> ClockStyle.NEON_SPLIT
        isLiquid || isPastel -> ClockStyle.ANALOG_CRYSTAL
        isGlass || isTransparent -> ClockStyle.GLASS_PILL
        else -> ClockStyle.MINIMAL_DIGITAL
      }

    val fontStyle =
      when {
        isLuxury || "elegant" in lower || "serif" in lower -> FontStyleOption.PLAYFAIR_DISPLAY
        isNeon || isFuturistic || "mono" in lower -> FontStyleOption.JETBRAINS_MONO
        isGlass || is3D -> FontStyleOption.SPACE_GROTESK
        else -> FontStyleOption.PLUS_JAKARTA
      }

    val animPreset =
      when {
        isReducedMotion || tier == ResolvedTier.LITE -> AnimationPreset.REDUCED_STATIC
        isLiquid -> AnimationPreset.LIQUID_MORPH
        isNeon || isFuturistic -> AnimationPreset.CYBER_SNAP
        else -> AnimationPreset.SILK_SPRING
      }

    val category =
      when {
        isAmoled -> ThemeCategory.AMOLED
        isNeon -> ThemeCategory.NEON
        isGlass || isLiquid || isTransparent -> ThemeCategory.GLASS
        isLuxury -> ThemeCategory.LUXURY
        isNature -> ThemeCategory.NATURE
        isPastel -> ThemeCategory.PASTEL
        isFuturistic -> ThemeCategory.FUTURISTIC
        isMinimal -> ThemeCategory.MINIMAL
        else -> ThemeCategory.CUSTOM
      }

    val aspectMode =
      if (formFactor == DeviceFormFactor.TABLET || "tablet" in lower || "foldable" in lower) {
        WallpaperAspectMode.TABLET_WIDE
      } else {
        WallpaperAspectMode.PORTRAIT
      }

    val seed = abs(prompt.hashCode().toLong()) + 1337L

    val rawTheme =
      ThemeSpec(
        id = "ai_${System.currentTimeMillis()}",
        name = buildThemeTitle(primaryRule.label, category, isAmoled, isGlass, isNeon),
        description = "Generated offline from prompt: \"$prompt\"",
        category = category,
        isBuiltIn = false,
        promptSource = prompt,
        palette =
          ThemePalette(
            primaryHex = primaryRule.primary,
            secondaryHex = matchedColors.getOrNull(1)?.primary ?: primaryRule.secondary,
            tertiaryHex = primaryRule.tertiary,
            backgroundHex = bgHex,
            surfaceHex = surfaceHex,
            accentGlowHex = primaryRule.glow,
            onSurfaceHex = onSurfaceHex,
            isAmoledDark = isAmoled,
          ),
        wallpaper =
          WallpaperConfig(
            style = wallpaperStyle,
            aspectMode = aspectMode,
            patternDensity = if (isMinimal) 0.45f else 0.75f,
            noiseIntensity = if (isAmoled) 0.1f else 0.22f,
            particleCount = if (isGalaxy || isNeon) 72 else 36,
            seed = seed,
          ),
        glass =
          GlassConfig(
            style = glassStyle,
            transparency = if (isTransparent) 0.14f else if (isLightMinimal) 0.62f else 0.24f,
            blurStrength = if (isGlass || isLiquid) 0.78f else 0.5f,
            glowIntensity = if (isNeon) 0.85f else if (isGlass || isLuxury) 0.58f else 0.25f,
            shadowDepth = if (is3D || isLuxury) 0.7f else 0.45f,
            reflectionOpacity = if (isGlass || isLiquid || isTransparent) 0.48f else 0.25f,
            cornerRadiusDp = if (isPastel || isLiquid) 26 else if (isFuturistic || isNeon) 16 else 22,
          ),
        icons =
          IconPackConfig(
            style = iconStyle,
            cornerRadiusDp = if (isPastel) 22 else 18,
            glowEnabled = isNeon || isGlass || isAmoled,
          ),
        lockScreen =
          LockScreenConfig(
            clockStyle = clockStyle,
            showSeconds = isFuturistic || "seconds" in lower,
            aodStyle = if (isAmoled) AodStyle.MONOCHROME_MONO else AodStyle.SUBTLE_EDGE,
            fingerprintEffect =
              when {
                isNeon -> FingerprintEffect.NEON_WAVE
                isLiquid -> FingerprintEffect.LIQUID_DROP
                isGlass || isLuxury -> FingerprintEffect.CRYSTAL_HEX
                else -> FingerprintEffect.PULSE_RING
              },
            chargingAnimation =
              when {
                isNeon || isFuturistic -> ChargingAnimation.PLASMA_BOLT
                isLiquid || isNature -> ChargingAnimation.LIQUID_FILL
                isMinimal -> ChargingAnimation.MINIMAL_BAR
                else -> ChargingAnimation.ORBIT_RING
              },
          ),
        controlCenter =
          ControlCenterConfig(
            tileShape =
              when {
                isLiquid || isTransparent -> TileShape.PILL
                isFuturistic || isNeon -> TileShape.SHARP_GLASS
                isPastel -> TileShape.CIRCLE
                else -> TileShape.SQUIRCLE
              },
            blurTiles = !isTransparent,
            notificationStyle =
              when {
                isTransparent -> NotificationStyle.TRANSPARENT_OUTLINE
                isAmoled -> NotificationStyle.AMOLED_ACCENT
                isLiquid -> NotificationStyle.FLOATING_PILL
                else -> NotificationStyle.FROSTED_STACK
              },
          ),
        widget =
          WidgetConfig(
            layout =
              when {
                isLuxury -> WidgetLayout.MINIMAL_DATE
                isFuturistic -> WidgetLayout.CONTROL_PILL
                isMinimal -> WidgetLayout.COMPACT_STATUS
                else -> WidgetLayout.GLASS_CLOCK_WEATHER
              },
            glassOpacity = if (isTransparent) 0.16f else 0.30f,
          ),
        fontStyle = fontStyle,
        motionAndSound =
          MotionSoundConfig(
            animationPreset = animPreset,
            animationsEnabled = tier != ResolvedTier.LITE && !isReducedMotion,
            soundProfile =
              when {
                isNeon || isFuturistic -> SoundProfile.CYBER_SYNTH
                isMinimal -> SoundProfile.MINIMAL_TAP
                else -> SoundProfile.CRYSTAL_CHIME
              },
          ),
      )

    val optimized = optimizeThemeForDevice(rawTheme, tier)

    val detectedStyleLabels = buildList {
      if (isAmoled) add("AMOLED Black")
      if (isGlass) add("Glassmorphism")
      if (isLiquid) add("Liquid Glass")
      if (isTransparent) add("Transparent")
      if (isNeon) add("Neon Glow")
      if (isLuxury) add("Luxury Editorial")
      if (isMinimal) add("Minimal")
      if (isGalaxy) add("Cosmic Galaxy")
      if (isNature) add("Nature Aurora")
      if (isPastel) add("Pastel")
      if (isFuturistic) add("Futuristic HUD")
      if (isEmpty()) add("Balanced Modern Glass")
    }

    val rationale =
      listOf(
        "Palette: ${primaryRule.label} primary paired on ${if (isAmoled) "true #000000 AMOLED" else if (isLightMinimal) "light porcelain" else "deep obsidian"} base.",
        "Wallpaper: ${wallpaperStyle.label} (${aspectMode.label}) rendered procedurally on-device.",
        "Icons & Glass: ${iconStyle.label} vector icons + ${glassStyle.label} cards (${(optimized.glass.transparency * 100).toInt()}% tint).",
        "Clock & Motion: ${clockStyle.label} in ${fontStyle.label} with ${animPreset.label}.",
      )

    val optimizationNote =
      when (tier) {
        ResolvedTier.LITE ->
          "Lite Mode active: blur & heavy reflections scaled down, particles capped at ${optimized.wallpaper.particleCount} for smooth low-RAM performance."
        ResolvedTier.BALANCED ->
          "Balanced Mode active: crisp 1080p procedural wallpaper & moderate glass translucency for steady frame pacing."
        ResolvedTier.PREMIUM ->
          "Premium Mode active: full specular reflections, high-res shaders, and spring animations enabled."
      }

    return AiGenerationReport(
      theme = optimized,
      detectedColors = matchedColors.ifEmpty { listOf(primaryRule) }.map { it.label },
      detectedStyles = detectedStyleLabels,
      rationale = rationale,
      optimizationNote = optimizationNote,
    )
  }

  fun optimizeThemeForDevice(theme: ThemeSpec, tier: ResolvedTier): ThemeSpec =
    when (tier) {
      ResolvedTier.LITE ->
        theme.copy(
          wallpaper =
            theme.wallpaper.copy(
              particleCount = theme.wallpaper.particleCount.coerceAtMost(18),
              noiseIntensity = 0f,
            ),
          glass =
            theme.glass.copy(
              blurStrength = 0f,
              reflectionOpacity = 0.12f,
              glowIntensity = theme.glass.glowIntensity.coerceAtMost(0.3f),
            ),
          controlCenter = theme.controlCenter.copy(blurTiles = false),
          motionAndSound =
            theme.motionAndSound.copy(
              animationPreset = AnimationPreset.REDUCED_STATIC,
              animationsEnabled = false,
            ),
        )
      ResolvedTier.BALANCED ->
        theme.copy(
          wallpaper =
            theme.wallpaper.copy(
              particleCount = theme.wallpaper.particleCount.coerceAtMost(48),
            ),
          glass =
            theme.glass.copy(
              blurStrength = theme.glass.blurStrength.coerceAtMost(0.65f),
            ),
        )
      ResolvedTier.PREMIUM -> theme
    }

  private fun buildThemeTitle(
    colorLabel: String,
    category: ThemeCategory,
    isAmoled: Boolean,
    isGlass: Boolean,
    isNeon: Boolean,
  ): String {
    val prefix = if (isAmoled && colorLabel != "Porcelain") "AMOLED " else ""
    val suffix =
      when {
        isNeon -> "Neon"
        isGlass -> "Glass"
        else -> category.label
      }
    return "$prefix$colorLabel $suffix".trim()
  }
}
