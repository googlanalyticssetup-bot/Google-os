package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.domain.model.*
import kotlin.math.roundToInt

object HardwareCapabilityEngine {

  fun inspectDevice(context: Context): DeviceCapabilities {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am?.getMemoryInfo(memInfo)

    val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(512L)
    val availRamMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(128L)
    val isLowRam = am?.isLowRamDevice == true || totalRamMb <= 3072L

    val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    val dm = DisplayMetrics()
    @Suppress("DEPRECATION")
    wm?.defaultDisplay?.getRealMetrics(dm)
    val widthPx = dm.widthPixels.takeIf { it > 0 } ?: context.resources.displayMetrics.widthPixels
    val heightPx = dm.heightPixels.takeIf { it > 0 } ?: context.resources.displayMetrics.heightPixels
    val densityDpi = dm.densityDpi.takeIf { it > 0 } ?: context.resources.displayMetrics.densityDpi

    @Suppress("DEPRECATION")
    val refreshRate =
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          context.display?.refreshRate?.roundToInt() ?: 60
        } else {
          wm?.defaultDisplay?.refreshRate?.roundToInt() ?: 60
        }
      } catch (_: Exception) {
        60
      }

    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val batteryPct = if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt() else 85
    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging =
      status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isPowerSave = pm?.isPowerSaveMode == true

    val config: Configuration = context.resources.configuration
    val smallestDp = config.smallestScreenWidthDp
    val formFactor =
      when {
        smallestDp >= 720 -> DeviceFormFactor.TABLET
        smallestDp >= 600 -> DeviceFormFactor.FOLDABLE
        else -> DeviceFormFactor.PHONE
      }

    val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
    val cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isLowRam

    val gpuHint =
      buildString {
        append(Build.HARDWARE.ifBlank { "SoC" })
        append(" • ")
        append(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "RenderEffect GPU" else "Canvas HW Accel")
      }

    val recommendedTier =
      when {
        isLowRam || totalRamMb <= 3500L || availRamMb < 650L || isPowerSave || batteryPct < 15 ->
          ResolvedTier.LITE
        totalRamMb >= 7500L && cpuCores >= 8 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
          ResolvedTier.PREMIUM
        else -> ResolvedTier.BALANCED
      }

    return DeviceCapabilities(
      manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
      brand = Build.BRAND.replaceFirstChar { it.uppercase() },
      model = Build.MODEL,
      androidVersion = Build.VERSION.RELEASE ?: "14",
      sdkInt = Build.VERSION.SDK_INT,
      totalRamMb = totalRamMb,
      availableRamMb = availRamMb,
      isLowRamDevice = isLowRam,
      cpuCores = cpuCores,
      cpuAbi = cpuAbi,
      screenWidthPx = widthPx,
      screenHeightPx = heightPx,
      densityDpi = densityDpi,
      smallestScreenWidthDp = smallestDp,
      refreshRateHz = refreshRate,
      supportsHardwareBlur = supportsBlur,
      gpuHint = gpuHint,
      batteryPercent = batteryPct,
      isCharging = isCharging,
      isPowerSaveMode = isPowerSave,
      formFactor = formFactor,
      recommendedTier = recommendedTier,
    )
  }

  fun resolveRenderRules(
    mode: PerformanceMode,
    caps: DeviceCapabilities,
    animationsMasterEnabled: Boolean,
  ): EffectiveRenderRules {
    val tier =
      when (mode) {
        PerformanceMode.AUTO -> caps.recommendedTier
        PerformanceMode.LITE -> ResolvedTier.LITE
        PerformanceMode.BALANCED -> ResolvedTier.BALANCED
        PerformanceMode.PREMIUM -> ResolvedTier.PREMIUM
      }

    return when (tier) {
      ResolvedTier.LITE ->
        EffectiveRenderRules(
          tier = ResolvedTier.LITE,
          enableBlur = false,
          enableReflections = false,
          enableGlow = false,
          maxWallpaperLongSidePx = 1080,
          maxParticleCount = 18,
          animationScale = 0f,
          useRgb565Thumbnails = true,
        )
      ResolvedTier.BALANCED ->
        EffectiveRenderRules(
          tier = ResolvedTier.BALANCED,
          enableBlur = caps.supportsHardwareBlur,
          enableReflections = true,
          enableGlow = true,
          maxWallpaperLongSidePx = 1920,
          maxParticleCount = 48,
          animationScale = if (animationsMasterEnabled) 0.85f else 0f,
          useRgb565Thumbnails = false,
        )
      ResolvedTier.PREMIUM ->
        EffectiveRenderRules(
          tier = ResolvedTier.PREMIUM,
          enableBlur = caps.supportsHardwareBlur,
          enableReflections = true,
          enableGlow = true,
          maxWallpaperLongSidePx = 2400,
          maxParticleCount = 96,
          animationScale = if (animationsMasterEnabled) 1.0f else 0f,
          useRgb565Thumbnails = false,
        )
    }
  }

  val oemMatrix: List<OemCompatibilityProfile> =
    listOf(
      OemCompatibilityProfile("OPPO", "ColorOS", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "Custom launcher needed for system-wide icon packs; Theme Store handles OEM fonts/AOD.", "System Control Centre & fingerprint UI remain protected by ColorOS."),
      OemCompatibilityProfile("OnePlus", "OxygenOS", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "OxygenOS native launcher supports third-party icon styles via Launcher Settings.", "System AOD & charging overlays require OxygenOS system privileges."),
      OemCompatibilityProfile("Samsung", "One UI", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "Use Samsung Theme Park / Good Lock or custom launcher for system icon packs.", "One UI Quick Panel & LockStar require Good Lock / system signing."),
      OemCompatibilityProfile("Xiaomi / Redmi / POCO", "HyperOS / MIUI", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "Custom launcher recommended for universal icon packs; Themes app handles system icons.", "HyperOS Control Centre & fingerprint effects are OEM-signed."),
      OemCompatibilityProfile("Realme", "realme UI", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "Supports standard WallpaperManager + widgets; custom launcher enables icon packs.", "System lock screen & charging overlays require realme system permissions."),
      OemCompatibilityProfile("Vivo / iQOO", "Funtouch OS / OriginOS", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "Full wallpaper & widget support; iTheme or custom launcher handles icon packs.", "OriginOS/Funtouch fingerprint animations are system-level."),
      OemCompatibilityProfile("Google Pixel", "Pixel UI", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "Full WallpaperManager + Material You widgets; Lawnchair/Nova for custom icons.", "Pixel Lock Screen & Quick Settings follow AOSP Monet."),
      OemCompatibilityProfile("Nothing", "Nothing OS", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "Nothing Launcher natively supports custom icon packs & monochrome styling.", "Glyph & system lock overlays are firmware-controlled."),
      OemCompatibilityProfile("Motorola", "Hello UI / MyUX", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "Standard WallpaperManager + widgets; icon shapes via Moto Personalize.", "System status shade follows stock Android."),
      OemCompatibilityProfile("ASUS", "ZenUI", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "ZenUI Launcher supports icon packs natively alongside Theme Studio wallpapers.", "Armoury/system overlays remain OEM-managed."),
      OemCompatibilityProfile("Sony", "Xperia UI", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "Full Android WallpaperManager + App Widgets; custom launcher for icon packs.", "Near-stock AOSP lock & notification shade."),
      OemCompatibilityProfile("Lenovo Tablets", "ZUI Tablet", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "Tablet-wide procedural wallpapers & resizable glass widgets supported natively.", "Multi-window & taskbar follow Lenovo ZUI."),
      OemCompatibilityProfile("Samsung Tablets", "One UI Tab", SupportLevel.UNIVERSAL, SupportLevel.UNIVERSAL, "Wide tablet wallpapers + DeX/large-screen widgets work out of the box.", "System-wide themes managed via Galaxy Themes / Good Lock."),
    )
}
