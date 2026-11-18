package com.example.domain.model

enum class PerformanceMode(val label: String, val summary: String) {
  AUTO("Auto (Adaptive)", "Automatically matches device RAM, GPU, and battery state"),
  LITE("Lite Mode", "Low-RAM friendly: disables blur, caps particles, lowers bitmap resolution"),
  BALANCED("Balanced Mode", "Mid-range sweet spot: moderate glass translucency and 1080p wallpapers"),
  PREMIUM("Premium Mode", "Full glass reflections, high-res shaders, and crisp spring motion"),
}

enum class ResolvedTier(val label: String) {
  LITE("Lite"),
  BALANCED("Balanced"),
  PREMIUM("Premium"),
}

enum class DeviceFormFactor(val label: String) {
  PHONE("Smartphone"),
  TABLET("Tablet"),
  FOLDABLE("Foldable / Large Screen"),
}

data class DeviceCapabilities(
  val manufacturer: String,
  val brand: String,
  val model: String,
  val androidVersion: String,
  val sdkInt: Int,
  val totalRamMb: Long,
  val availableRamMb: Long,
  val isLowRamDevice: Boolean,
  val cpuCores: Int,
  val cpuAbi: String,
  val screenWidthPx: Int,
  val screenHeightPx: Int,
  val densityDpi: Int,
  val smallestScreenWidthDp: Int,
  val refreshRateHz: Int,
  val supportsHardwareBlur: Boolean,
  val gpuHint: String,
  val batteryPercent: Int,
  val isCharging: Boolean,
  val isPowerSaveMode: Boolean,
  val formFactor: DeviceFormFactor,
  val recommendedTier: ResolvedTier,
)

enum class SupportLevel(val label: String) {
  UNIVERSAL("Standard Android API"),
  OEM_ASSISTED("Launcher / Theme Store Assisted"),
  SYSTEM_RESTRICTED("Requires OEM System Access"),
}

data class OemCompatibilityProfile(
  val brand: String,
  val uiSkin: String,
  val wallpaperSupport: SupportLevel,
  val widgetSupport: SupportLevel,
  val iconPackNote: String,
  val systemOverlayNote: String,
)

data class EffectiveRenderRules(
  val tier: ResolvedTier,
  val enableBlur: Boolean,
  val enableReflections: Boolean,
  val enableGlow: Boolean,
  val maxWallpaperLongSidePx: Int,
  val maxParticleCount: Int,
  val animationScale: Float,
  val useRgb565Thumbnails: Boolean,
)
