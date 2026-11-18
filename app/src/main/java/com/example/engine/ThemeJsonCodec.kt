package com.example.engine

import com.example.domain.model.*
import org.json.JSONObject

object ThemeJsonCodec {

  private const val SCHEMA_VERSION = 1

  fun encode(theme: ThemeSpec): String {
    val root = JSONObject()
    root.put("schemaVersion", SCHEMA_VERSION)
    root.put("id", theme.id)
    root.put("name", theme.name)
    root.put("description", theme.description)
    root.put("category", theme.category.name)
    root.put("isBuiltIn", theme.isBuiltIn)
    root.put("promptSource", theme.promptSource)
    root.put("createdAt", theme.createdAt)
    root.put("fontStyle", theme.fontStyle.name)

    root.put(
      "palette",
      JSONObject().apply {
        put("primaryHex", theme.palette.primaryHex)
        put("secondaryHex", theme.palette.secondaryHex)
        put("tertiaryHex", theme.palette.tertiaryHex)
        put("backgroundHex", theme.palette.backgroundHex)
        put("surfaceHex", theme.palette.surfaceHex)
        put("accentGlowHex", theme.palette.accentGlowHex)
        put("onSurfaceHex", theme.palette.onSurfaceHex)
        put("isAmoledDark", theme.palette.isAmoledDark)
      },
    )

    root.put(
      "wallpaper",
      JSONObject().apply {
        put("style", theme.wallpaper.style.name)
        put("aspectMode", theme.wallpaper.aspectMode.name)
        put("patternDensity", theme.wallpaper.patternDensity.toDouble())
        put("noiseIntensity", theme.wallpaper.noiseIntensity.toDouble())
        put("particleCount", theme.wallpaper.particleCount)
        put("seed", theme.wallpaper.seed)
      },
    )

    root.put(
      "glass",
      JSONObject().apply {
        put("style", theme.glass.style.name)
        put("transparency", theme.glass.transparency.toDouble())
        put("blurStrength", theme.glass.blurStrength.toDouble())
        put("glowIntensity", theme.glass.glowIntensity.toDouble())
        put("shadowDepth", theme.glass.shadowDepth.toDouble())
        put("reflectionOpacity", theme.glass.reflectionOpacity.toDouble())
        put("cornerRadiusDp", theme.glass.cornerRadiusDp)
      },
    )

    root.put(
      "icons",
      JSONObject().apply {
        put("style", theme.icons.style.name)
        put("cornerRadiusDp", theme.icons.cornerRadiusDp)
        put("glowEnabled", theme.icons.glowEnabled)
        if (theme.icons.customTintHex != null) {
          put("customTintHex", theme.icons.customTintHex)
        }
        put("innerScale", theme.icons.innerScale.toDouble())
      },
    )

    root.put(
      "lockScreen",
      JSONObject().apply {
        put("clockStyle", theme.lockScreen.clockStyle.name)
        put("showSeconds", theme.lockScreen.showSeconds)
        put("aodStyle", theme.lockScreen.aodStyle.name)
        put("fingerprintEffect", theme.lockScreen.fingerprintEffect.name)
        put("chargingAnimation", theme.lockScreen.chargingAnimation.name)
      },
    )

    root.put(
      "controlCenter",
      JSONObject().apply {
        put("tileShape", theme.controlCenter.tileShape.name)
        put("blurTiles", theme.controlCenter.blurTiles)
        put("notificationStyle", theme.controlCenter.notificationStyle.name)
      },
    )

    root.put(
      "widget",
      JSONObject().apply {
        put("layout", theme.widget.layout.name)
        put("showBattery", theme.widget.showBattery)
        put("showWeatherPlaceholder", theme.widget.showWeatherPlaceholder)
        put("glassOpacity", theme.widget.glassOpacity.toDouble())
      },
    )

    root.put(
      "motionAndSound",
      JSONObject().apply {
        put("animationPreset", theme.motionAndSound.animationPreset.name)
        put("animationsEnabled", theme.motionAndSound.animationsEnabled)
        put("soundProfile", theme.motionAndSound.soundProfile.name)
      },
    )

    return root.toString(2)
  }

  fun decode(json: String, overrideAsCustomImport: Boolean = false): Result<ThemeSpec> = runCatching {
    val root = JSONObject(json)
    val palObj = root.getJSONObject("palette")
    val wpObj = root.getJSONObject("wallpaper")
    val glObj = root.getJSONObject("glass")
    val icObj = root.getJSONObject("icons")
    val lsObj = root.getJSONObject("lockScreen")
    val ccObj = root.getJSONObject("controlCenter")
    val wgObj = root.getJSONObject("widget")
    val msObj = root.getJSONObject("motionAndSound")

    val id =
      if (overrideAsCustomImport) "import_${System.currentTimeMillis()}"
      else root.getString("id")

    ThemeSpec(
      id = id,
      name = root.getString("name"),
      description = root.optString("description", "Imported offline theme"),
      category = enumOrDefault(root.optString("category"), ThemeCategory.CUSTOM),
      isBuiltIn = if (overrideAsCustomImport) false else root.optBoolean("isBuiltIn", false),
      promptSource = root.optString("promptSource", ""),
      createdAt =
        if (overrideAsCustomImport) System.currentTimeMillis()
        else root.optLong("createdAt", System.currentTimeMillis()),
      palette =
        ThemePalette(
          primaryHex = palObj.getLong("primaryHex"),
          secondaryHex = palObj.getLong("secondaryHex"),
          tertiaryHex = palObj.getLong("tertiaryHex"),
          backgroundHex = palObj.getLong("backgroundHex"),
          surfaceHex = palObj.getLong("surfaceHex"),
          accentGlowHex = palObj.getLong("accentGlowHex"),
          onSurfaceHex = palObj.getLong("onSurfaceHex"),
          isAmoledDark = palObj.optBoolean("isAmoledDark", true),
        ),
      wallpaper =
        WallpaperConfig(
          style = enumOrDefault(wpObj.optString("style"), WallpaperStyle.GLASS_RIBBON),
          aspectMode = enumOrDefault(wpObj.optString("aspectMode"), WallpaperAspectMode.PORTRAIT),
          patternDensity = wpObj.optDouble("patternDensity", 0.65).toFloat(),
          noiseIntensity = wpObj.optDouble("noiseIntensity", 0.2).toFloat(),
          particleCount = wpObj.optInt("particleCount", 40),
          seed = wpObj.optLong("seed", 104729L),
        ),
      glass =
        GlassConfig(
          style = enumOrDefault(glObj.optString("style"), GlassStyle.GLASSMORPHISM),
          transparency = glObj.optDouble("transparency", 0.24).toFloat(),
          blurStrength = glObj.optDouble("blurStrength", 0.65).toFloat(),
          glowIntensity = glObj.optDouble("glowIntensity", 0.55).toFloat(),
          shadowDepth = glObj.optDouble("shadowDepth", 0.5).toFloat(),
          reflectionOpacity = glObj.optDouble("reflectionOpacity", 0.35).toFloat(),
          cornerRadiusDp = glObj.optInt("cornerRadiusDp", 22),
        ),
      icons =
        IconPackConfig(
          style = enumOrDefault(icObj.optString("style"), IconStyle.GLASS),
          cornerRadiusDp = icObj.optInt("cornerRadiusDp", 18),
          glowEnabled = icObj.optBoolean("glowEnabled", true),
          customTintHex = if (icObj.has("customTintHex")) icObj.getLong("customTintHex") else null,
          innerScale = icObj.optDouble("innerScale", 0.85).toFloat(),
        ),
      lockScreen =
        LockScreenConfig(
          clockStyle = enumOrDefault(lsObj.optString("clockStyle"), ClockStyle.MINIMAL_DIGITAL),
          showSeconds = lsObj.optBoolean("showSeconds", false),
          aodStyle = enumOrDefault(lsObj.optString("aodStyle"), AodStyle.SUBTLE_EDGE),
          fingerprintEffect =
            enumOrDefault(lsObj.optString("fingerprintEffect"), FingerprintEffect.PULSE_RING),
          chargingAnimation =
            enumOrDefault(lsObj.optString("chargingAnimation"), ChargingAnimation.ORBIT_RING),
        ),
      controlCenter =
        ControlCenterConfig(
          tileShape = enumOrDefault(ccObj.optString("tileShape"), TileShape.SQUIRCLE),
          blurTiles = ccObj.optBoolean("blurTiles", true),
          notificationStyle =
            enumOrDefault(ccObj.optString("notificationStyle"), NotificationStyle.FROSTED_STACK),
        ),
      widget =
        WidgetConfig(
          layout = enumOrDefault(wgObj.optString("layout"), WidgetLayout.GLASS_CLOCK_WEATHER),
          showBattery = wgObj.optBoolean("showBattery", true),
          showWeatherPlaceholder = wgObj.optBoolean("showWeatherPlaceholder", true),
          glassOpacity = wgObj.optDouble("glassOpacity", 0.3).toFloat(),
        ),
      fontStyle = enumOrDefault(root.optString("fontStyle"), FontStyleOption.SPACE_GROTESK),
      motionAndSound =
        MotionSoundConfig(
          animationPreset =
            enumOrDefault(msObj.optString("animationPreset"), AnimationPreset.SILK_SPRING),
          animationsEnabled = msObj.optBoolean("animationsEnabled", true),
          soundProfile = enumOrDefault(msObj.optString("soundProfile"), SoundProfile.CRYSTAL_CHIME),
        ),
    )
  }

  private inline fun <reified T : Enum<T>> enumOrDefault(raw: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: fallback
}
