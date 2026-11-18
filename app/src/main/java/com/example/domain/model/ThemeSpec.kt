package com.example.domain.model

enum class ThemeCategory(val label: String) {
  GLASS("Glass"),
  AMOLED("AMOLED"),
  NEON("Neon"),
  MINIMAL("Minimal"),
  LUXURY("Luxury"),
  NATURE("Nature"),
  PASTEL("Pastel"),
  FUTURISTIC("Futuristic"),
  CUSTOM("Custom"),
}

enum class WallpaperStyle(val label: String) {
  AMOLED_GLOW("AMOLED Glow"),
  GLASS_RIBBON("Glass Ribbon"),
  GALAXY_NEBULA("Galaxy Nebula"),
  ABSTRACT_PRISM("Abstract Prism"),
  MINIMAL_GRADIENT("Minimal Gradient"),
  NATURE_AURORA("Nature Aurora"),
  CYBER_GRID("Cyber Grid"),
  FRACTAL_WAVE("Fractal Wave"),
}

enum class WallpaperAspectMode(val label: String) {
  PORTRAIT("Phone Portrait"),
  TABLET_WIDE("Tablet / Foldable"),
}

enum class GlassStyle(val label: String) {
  GLASSMORPHISM("Glassmorphism"),
  FROSTED("Frosted Glass"),
  LIQUID("Liquid Glass"),
  TRANSPARENT("Transparent"),
  CRYSTAL("Crystal Prism"),
  THREE_D("3D Floating Glass"),
}

enum class IconStyle(val label: String) {
  GLASS("Glass"),
  NEON("Neon"),
  ROUNDED("Rounded"),
  MINIMAL("Minimal"),
  THREE_D("3D-Look"),
  GRADIENT("Gradient"),
  TRANSPARENT("Transparent"),
  AMOLED("AMOLED"),
}

enum class ClockStyle(val label: String) {
  MINIMAL_DIGITAL("Minimal Digital"),
  EDITORIAL_SERIF("Editorial Serif"),
  NEON_SPLIT("Neon Split"),
  ANALOG_CRYSTAL("Analog Crystal"),
  GLASS_PILL("Glass Pill"),
}

enum class AodStyle(val label: String) {
  SUBTLE_EDGE("Subtle Edge Glow"),
  MINIMAL_CLOCK("Minimal Clock"),
  ORBIT_HALO("Orbit Halo"),
  MONOCHROME_MONO("Pure AMOLED Mono"),
}

enum class FingerprintEffect(val label: String) {
  PULSE_RING("Pulse Ring"),
  CRYSTAL_HEX("Crystal Prism"),
  NEON_WAVE("Neon Ripple"),
  LIQUID_DROP("Liquid Ripple"),
  SUBTLE_DOT("Minimal Dot"),
}

enum class ChargingAnimation(val label: String) {
  ORBIT_RING("Orbit Energy Ring"),
  LIQUID_FILL("Liquid Glass Fill"),
  PLASMA_BOLT("Plasma Arc"),
  MINIMAL_BAR("Minimal Pill"),
}

enum class TileShape(val label: String) {
  PILL("Capsule Pill"),
  SQUIRCLE("Soft Squircle"),
  CIRCLE("Circle"),
  SHARP_GLASS("Faceted Glass"),
}

enum class NotificationStyle(val label: String) {
  FROSTED_STACK("Frosted Stack"),
  TRANSPARENT_OUTLINE("Transparent Outline"),
  AMOLED_ACCENT("AMOLED Accent Strip"),
  FLOATING_PILL("Floating Glass Pill"),
}

enum class WidgetLayout(val label: String) {
  GLASS_CLOCK_WEATHER("Glass Clock & Status"),
  COMPACT_STATUS("Compact Status"),
  CONTROL_PILL("Control Pill"),
  MINIMAL_DATE("Minimal Editorial Date"),
}

enum class FontStyleOption(val label: String, val previewTag: String) {
  PLUS_JAKARTA("Plus Jakarta Sans", "Clean Modern"),
  SPACE_GROTESK("Space Grotesk", "Geometric Tech"),
  PLAYFAIR_DISPLAY("Playfair Display", "Luxury Editorial"),
  JETBRAINS_MONO("JetBrains Mono", "Cyber Monospace"),
}

enum class AnimationPreset(val label: String, val durationMultiplier: Float) {
  SILK_SPRING("Silk Spring", 1.0f),
  LIQUID_MORPH("Liquid Morph", 1.2f),
  CYBER_SNAP("Cyber Snap", 0.7f),
  REDUCED_STATIC("Reduced / Static", 0f),
}

enum class SoundProfile(val label: String) {
  CRYSTAL_CHIME("Crystal Chime"),
  MINIMAL_TAP("Minimal Tap"),
  CYBER_SYNTH("Cyber Synth"),
  SILENT("Silent"),
}

data class ThemePalette(
  val primaryHex: Long,
  val secondaryHex: Long,
  val tertiaryHex: Long,
  val backgroundHex: Long,
  val surfaceHex: Long,
  val accentGlowHex: Long,
  val onSurfaceHex: Long,
  val isAmoledDark: Boolean = true,
)

data class WallpaperConfig(
  val style: WallpaperStyle,
  val aspectMode: WallpaperAspectMode = WallpaperAspectMode.PORTRAIT,
  val patternDensity: Float = 0.65f,
  val noiseIntensity: Float = 0.25f,
  val particleCount: Int = 48,
  val seed: Long = 104729L,
)

data class GlassConfig(
  val style: GlassStyle,
  val transparency: Float = 0.24f,
  val blurStrength: Float = 0.65f,
  val glowIntensity: Float = 0.55f,
  val shadowDepth: Float = 0.50f,
  val reflectionOpacity: Float = 0.35f,
  val cornerRadiusDp: Int = 22,
)

data class IconPackConfig(
  val style: IconStyle,
  val cornerRadiusDp: Int = 18,
  val glowEnabled: Boolean = true,
  val customTintHex: Long? = null,
  val innerScale: Float = 0.85f,
)

data class LockScreenConfig(
  val clockStyle: ClockStyle,
  val showSeconds: Boolean = false,
  val aodStyle: AodStyle = AodStyle.SUBTLE_EDGE,
  val fingerprintEffect: FingerprintEffect = FingerprintEffect.PULSE_RING,
  val chargingAnimation: ChargingAnimation = ChargingAnimation.ORBIT_RING,
)

data class ControlCenterConfig(
  val tileShape: TileShape = TileShape.SQUIRCLE,
  val blurTiles: Boolean = true,
  val notificationStyle: NotificationStyle = NotificationStyle.FROSTED_STACK,
)

data class WidgetConfig(
  val layout: WidgetLayout = WidgetLayout.GLASS_CLOCK_WEATHER,
  val showBattery: Boolean = true,
  val showWeatherPlaceholder: Boolean = true,
  val glassOpacity: Float = 0.30f,
)

data class MotionSoundConfig(
  val animationPreset: AnimationPreset = AnimationPreset.SILK_SPRING,
  val animationsEnabled: Boolean = true,
  val soundProfile: SoundProfile = SoundProfile.CRYSTAL_CHIME,
)

data class ThemeSpec(
  val id: String,
  val name: String,
  val description: String,
  val category: ThemeCategory,
  val isBuiltIn: Boolean = false,
  val promptSource: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val palette: ThemePalette,
  val wallpaper: WallpaperConfig,
  val glass: GlassConfig,
  val icons: IconPackConfig,
  val lockScreen: LockScreenConfig,
  val controlCenter: ControlCenterConfig,
  val widget: WidgetConfig,
  val fontStyle: FontStyleOption = FontStyleOption.SPACE_GROTESK,
  val motionAndSound: MotionSoundConfig = MotionSoundConfig(),
)
