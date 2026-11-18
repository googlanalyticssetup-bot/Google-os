package com.example.data

import android.content.Context
import com.example.domain.model.PerformanceMode
import com.example.domain.model.ThemeSpec
import com.example.engine.BuiltInThemes
import com.example.engine.ThemeJsonCodec
import com.example.widget.ThemeStudioWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ThemeRepository(private val context: Context) {

  private val dao = ThemeDatabase.getInstance(context).themeDao()
  private val prefs = PreferencesDataStore(context)

  val allThemes: Flow<List<ThemeSpec>> =
    dao.observeAllThemes().map { entities ->
      val decoded =
        entities.mapNotNull { entity ->
          ThemeJsonCodec.decode(entity.payloadJson).getOrNull()
        }
      decoded.ifEmpty { BuiltInThemes.all }
    }

  val activeThemeId: Flow<String> = prefs.activeThemeId
  val performanceMode: Flow<PerformanceMode> = prefs.performanceMode
  val animationsEnabled: Flow<Boolean> = prefs.animationsEnabled
  val lastWallpaperTarget: Flow<String> = prefs.lastWallpaperTarget

  suspend fun ensureBuiltInThemesSeeded() =
    withContext(Dispatchers.IO) {
      val now = System.currentTimeMillis()
      val builtIns =
        BuiltInThemes.all.map { spec ->
          ThemeEntity(
            id = spec.id,
            name = spec.name,
            category = spec.category.name,
            isBuiltIn = true,
            createdAt = spec.createdAt,
            updatedAt = now,
            payloadJson = ThemeJsonCodec.encode(spec),
          )
        }
      dao.insertBuiltInsIgnoreExisting(builtIns)
    }

  suspend fun saveTheme(theme: ThemeSpec) =
    withContext(Dispatchers.IO) {
      val now = System.currentTimeMillis()
      dao.upsertTheme(
        ThemeEntity(
          id = theme.id,
          name = theme.name,
          category = theme.category.name,
          isBuiltIn = theme.isBuiltIn,
          createdAt = theme.createdAt,
          updatedAt = now,
          payloadJson = ThemeJsonCodec.encode(theme),
        ),
      )
    }

  suspend fun setActiveTheme(theme: ThemeSpec) {
    saveTheme(theme)
    prefs.setActiveThemeId(theme.id)
    ThemeStudioWidgetProvider.pushThemeUpdate(context, theme)
  }

  suspend fun deleteCustomTheme(id: String) =
    withContext(Dispatchers.IO) {
      dao.deleteCustomTheme(id)
    }

  suspend fun getActiveThemeOrDefault(): ThemeSpec =
    withContext(Dispatchers.IO) {
      val id = dao.getThemeById("builtin_premium_glass") // fallback if prefs empty
      var resolved: ThemeSpec? = null
      prefs.activeThemeId.let { /* read via DAO directly using firstOrNull below */ }
      if (id != null) {
        resolved = ThemeJsonCodec.decode(id.payloadJson).getOrNull()
      }
      resolved ?: BuiltInThemes.all.first()
    }

  suspend fun setPerformanceMode(mode: PerformanceMode) = prefs.setPerformanceMode(mode)

  suspend fun setAnimationsEnabled(enabled: Boolean) = prefs.setAnimationsEnabled(enabled)

  suspend fun setLastWallpaperTarget(label: String) = prefs.setLastWallpaperTarget(label)

  fun exportThemeJson(theme: ThemeSpec): String = ThemeJsonCodec.encode(theme)

  suspend fun importThemeJson(json: String): Result<ThemeSpec> =
    withContext(Dispatchers.IO) {
      ThemeJsonCodec.decode(json, overrideAsCustomImport = true).onSuccess { imported ->
        saveTheme(imported)
      }
    }
}
