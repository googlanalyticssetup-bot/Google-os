package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.PerformanceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore by preferencesDataStore(name = "theme_studio_prefs")

class PreferencesDataStore(private val context: Context) {

  private object Keys {
    val ACTIVE_THEME_ID = stringPreferencesKey("active_theme_id")
    val PERFORMANCE_MODE = stringPreferencesKey("performance_mode")
    val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
    val LAST_WALLPAPER_TARGET = stringPreferencesKey("last_wallpaper_target")
  }

  val activeThemeId: Flow<String> =
    context.studioDataStore.data.map { prefs ->
      prefs[Keys.ACTIVE_THEME_ID] ?: "builtin_premium_glass"
    }

  val performanceMode: Flow<PerformanceMode> =
    context.studioDataStore.data.map { prefs ->
      val raw = prefs[Keys.PERFORMANCE_MODE] ?: PerformanceMode.AUTO.name
      PerformanceMode.entries.firstOrNull { it.name == raw } ?: PerformanceMode.AUTO
    }

  val animationsEnabled: Flow<Boolean> =
    context.studioDataStore.data.map { prefs -> prefs[Keys.ANIMATIONS_ENABLED] ?: true }

  val lastWallpaperTarget: Flow<String> =
    context.studioDataStore.data.map { prefs -> prefs[Keys.LAST_WALLPAPER_TARGET] ?: "Not applied yet" }

  suspend fun setActiveThemeId(id: String) {
    context.studioDataStore.edit { it[Keys.ACTIVE_THEME_ID] = id }
  }

  suspend fun setPerformanceMode(mode: PerformanceMode) {
    context.studioDataStore.edit { it[Keys.PERFORMANCE_MODE] = mode.name }
  }

  suspend fun setAnimationsEnabled(enabled: Boolean) {
    context.studioDataStore.edit { it[Keys.ANIMATIONS_ENABLED] = enabled }
  }

  suspend fun setLastWallpaperTarget(label: String) {
    context.studioDataStore.edit { it[Keys.LAST_WALLPAPER_TARGET] = label }
  }
}
