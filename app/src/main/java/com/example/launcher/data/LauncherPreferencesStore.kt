package com.example.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.FontStyleOption
import com.example.domain.model.PerformanceMode
import com.example.launcher.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.launcherSettingsStore by preferencesDataStore(name = "launcher_settings_store")

class LauncherPreferencesStore(private val context: Context) {

  private val KEY_SETTINGS_JSON = stringPreferencesKey("launcher_settings_json")

  val settings: Flow<LauncherSettings> =
    context.launcherSettingsStore.data.map { prefs ->
      val raw = prefs[KEY_SETTINGS_JSON]
      if (raw.isNullOrBlank()) LauncherSettings() else decodeSettings(raw)
    }

  suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
    context.launcherSettingsStore.edit { prefs ->
      val current =
        prefs[KEY_SETTINGS_JSON]?.let { decodeSettings(it) } ?: LauncherSettings()
      prefs[KEY_SETTINGS_JSON] = encodeSettings(transform(current))
    }
  }

  suspend fun replaceSettings(newSettings: LauncherSettings) {
    context.launcherSettingsStore.edit { prefs ->
      prefs[KEY_SETTINGS_JSON] = encodeSettings(newSettings)
    }
  }

  companion object {
    fun encodeSettings(s: LauncherSettings): String {
      val o = JSONObject()
      o.put("gridColumns", s.gridColumns)
      o.put("gridRows", s.gridRows)
      o.put("pageCount", s.pageCount)
      o.put("iconSizeDp", s.iconSizeDp)
      o.put("iconSpacingDp", s.iconSpacingDp)
      o.put("showHomeLabels", s.showHomeLabels)
      o.put("fontStyle", s.fontStyle.name)
      o.put("wallpaperParallax", s.wallpaperParallax)

      o.put("drawerScrollMode", s.drawerScrollMode.name)
      o.put("drawerSortMode", s.drawerSortMode.name)
      o.put("drawerColumns", s.drawerColumns)
      o.put("drawerBackgroundAlpha", s.drawerBackgroundAlpha.toDouble())
      o.put("drawerGlassEffect", s.drawerGlassEffect)
      o.put("drawerShowRecentFrequent", s.drawerShowRecentFrequent)
      o.put("drawerShowCategories", s.drawerShowCategories)

      o.put("iconStyle", s.iconStyle.name)
      o.put("iconCornerPercent", s.iconCornerPercent)
      o.put("iconShadowEnabled", s.iconShadowEnabled)
      o.put("showDrawerLabels", s.showDrawerLabels)

      o.put("defaultWidgetsGlass", s.defaultWidgetsGlass)
      o.put("defaultWidgetsTransparent", s.defaultWidgetsTransparent)

      o.put("defaultFolderStyle", s.defaultFolderStyle.name)
      o.put("defaultFolderLayout", s.defaultFolderLayout.name)
      o.put("defaultFolderColorHex", s.defaultFolderColorHex)

      o.put("swipeUpAction", s.swipeUpAction.name)
      o.put("swipeDownAction", s.swipeDownAction.name)
      o.put("doubleTapAction", s.doubleTapAction.name)
      o.put("twoFingerSwipeAction", s.twoFingerSwipeAction.name)
      o.put("pinchAction", s.pinchAction.name)
      o.put("longPressAction", s.longPressAction.name)

      o.put("animationsEnabled", s.animationsEnabled)
      o.put("pageTransition", s.pageTransition.name)
      o.put("animationSpeed", s.animationSpeed.toDouble())

      if (s.customWallpaperUri != null) o.put("customWallpaperUri", s.customWallpaperUri)

      o.put("instantSearch", s.instantSearch)
      o.put("showSearchSuggestions", s.showSearchSuggestions)
      o.put("showSearchHistory", s.showSearchHistory)
      o.put("searchContactsEnabled", s.searchContactsEnabled)
      o.put("voiceSearchEnabled", s.voiceSearchEnabled)
      o.put("searchBarStyle", s.searchBarStyle.name)

      o.put("dockStyle", s.dockStyle.name)
      o.put("dockMaxIcons", s.dockMaxIcons)
      o.put("showDockLabels", s.showDockLabels)
      o.put("dockPackages", JSONArray(s.dockPackages))

      o.put("activeThemeId", s.activeThemeId)
      o.put("wallpaperBasedPalette", s.wallpaperBasedPalette)
      o.put("performanceMode", s.performanceMode.name)

      o.put("hiddenPackages", JSONArray(s.hiddenPackages.toList()))
      o.put("customAppLabels", JSONObject(s.customAppLabels))
      o.put(
        "customAppIconStyles",
        JSONObject(s.customAppIconStyles.mapValues { it.value.name }),
      )
      o.put("customSortOrder", JSONArray(s.customSortOrder))
      o.put("autoAdaptLargeScreen", s.autoAdaptLargeScreen)
      o.put("tabletColumnBoost", s.tabletColumnBoost)
      o.put("tabletDrawerColumnBoost", s.tabletDrawerColumnBoost)
      return o.toString()
    }

    fun decodeSettings(json: String): LauncherSettings =
      runCatching {
        val o = JSONObject(json)
        val dockArr = o.optJSONArray("dockPackages") ?: JSONArray()
        val hiddenArr = o.optJSONArray("hiddenPackages") ?: JSONArray()
        val sortArr = o.optJSONArray("customSortOrder") ?: JSONArray()
        val labelsObj = o.optJSONObject("customAppLabels") ?: JSONObject()
        val iconStylesObj = o.optJSONObject("customAppIconStyles") ?: JSONObject()

        val labelsMap = mutableMapOf<String, String>()
        labelsObj.keys().forEach { k -> labelsMap[k] = labelsObj.getString(k) }

        val stylesMap = mutableMapOf<String, LauncherIconStyle>()
        iconStylesObj.keys().forEach { k ->
          val v = iconStylesObj.getString(k)
          LauncherIconStyle.entries.firstOrNull { it.name == v }?.let { stylesMap[k] = it }
        }

        LauncherSettings(
          gridColumns = o.optInt("gridColumns", 4).coerceIn(3, 8),
          gridRows = o.optInt("gridRows", 5).coerceIn(4, 8),
          pageCount = o.optInt("pageCount", 2).coerceIn(1, 7),
          iconSizeDp = o.optInt("iconSizeDp", 54).coerceIn(38, 72),
          iconSpacingDp = o.optInt("iconSpacingDp", 10).coerceIn(4, 24),
          showHomeLabels = o.optBoolean("showHomeLabels", true),
          fontStyle = enumOr(o.optString("fontStyle"), FontStyleOption.PLUS_JAKARTA),
          wallpaperParallax = o.optBoolean("wallpaperParallax", true),
          drawerScrollMode = enumOr(o.optString("drawerScrollMode"), DrawerScrollMode.VERTICAL),
          drawerSortMode = enumOr(o.optString("drawerSortMode"), DrawerSortMode.ALPHABETICAL),
          drawerColumns = o.optInt("drawerColumns", 4).coerceIn(3, 7),
          drawerBackgroundAlpha = o.optDouble("drawerBackgroundAlpha", 0.72).toFloat(),
          drawerGlassEffect = o.optBoolean("drawerGlassEffect", true),
          drawerShowRecentFrequent = o.optBoolean("drawerShowRecentFrequent", true),
          drawerShowCategories = o.optBoolean("drawerShowCategories", true),
          iconStyle = enumOr(o.optString("iconStyle"), LauncherIconStyle.COLOROS),
          iconCornerPercent = o.optInt("iconCornerPercent", 34).coerceIn(8, 50),
          iconShadowEnabled = o.optBoolean("iconShadowEnabled", true),
          showDrawerLabels = o.optBoolean("showDrawerLabels", true),
          defaultWidgetsGlass = o.optBoolean("defaultWidgetsGlass", true),
          defaultWidgetsTransparent = o.optBoolean("defaultWidgetsTransparent", false),
          defaultFolderStyle = enumOr(o.optString("defaultFolderStyle"), FolderStyle.GLASS),
          defaultFolderLayout = enumOr(o.optString("defaultFolderLayout"), FolderLayout.GRID),
          defaultFolderColorHex = o.optLong("defaultFolderColorHex", 0xFF8B5CF6),
          swipeUpAction = enumOr(o.optString("swipeUpAction"), GestureAction.OPEN_DRAWER),
          swipeDownAction = enumOr(o.optString("swipeDownAction"), GestureAction.OPEN_NOTIFICATIONS),
          doubleTapAction = enumOr(o.optString("doubleTapAction"), GestureAction.LOCK_SCREEN),
          twoFingerSwipeAction = enumOr(o.optString("twoFingerSwipeAction"), GestureAction.OPEN_THEME_GENERATOR),
          pinchAction = enumOr(o.optString("pinchAction"), GestureAction.OPEN_EDIT_MODE),
          longPressAction = enumOr(o.optString("longPressAction"), GestureAction.OPEN_EDIT_MODE),
          animationsEnabled = o.optBoolean("animationsEnabled", true),
          pageTransition = enumOr(o.optString("pageTransition"), PageTransitionEffect.SILK_SLIDE),
          animationSpeed = o.optDouble("animationSpeed", 1.0).toFloat(),
          customWallpaperUri = o.optString("customWallpaperUri", "").ifBlank { null },
          instantSearch = o.optBoolean("instantSearch", true),
          showSearchSuggestions = o.optBoolean("showSearchSuggestions", true),
          showSearchHistory = o.optBoolean("showSearchHistory", true),
          searchContactsEnabled = o.optBoolean("searchContactsEnabled", false),
          voiceSearchEnabled = o.optBoolean("voiceSearchEnabled", true),
          searchBarStyle = enumOr(o.optString("searchBarStyle"), SearchBarStyle.GLASS_PILL),
          dockStyle = enumOr(o.optString("dockStyle"), DockStyle.GLASS),
          dockMaxIcons = o.optInt("dockMaxIcons", 5).coerceIn(3, 7),
          showDockLabels = o.optBoolean("showDockLabels", false),
          dockPackages = (0 until dockArr.length()).map { dockArr.getString(it) },
          activeThemeId = o.optString("activeThemeId", "builtin_coloros7"),
          wallpaperBasedPalette = o.optBoolean("wallpaperBasedPalette", false),
          performanceMode = enumOr(o.optString("performanceMode"), PerformanceMode.AUTO),
          hiddenPackages = (0 until hiddenArr.length()).map { hiddenArr.getString(it) }.toSet(),
          customAppLabels = labelsMap,
          customAppIconStyles = stylesMap,
          customSortOrder = (0 until sortArr.length()).map { sortArr.getString(it) },
          autoAdaptLargeScreen = o.optBoolean("autoAdaptLargeScreen", true),
          tabletColumnBoost = o.optInt("tabletColumnBoost", 2).coerceIn(0, 3),
          tabletDrawerColumnBoost = o.optInt("tabletDrawerColumnBoost", 2).coerceIn(0, 3),
        )
      }.getOrElse { LauncherSettings() }

    private inline fun <reified T : Enum<T>> enumOr(raw: String, fallback: T): T =
      enumValues<T>().firstOrNull { it.name == raw } ?: fallback
  }
}
