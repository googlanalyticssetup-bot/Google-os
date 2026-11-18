package com.example.launcher.data

import android.content.Context
import com.example.data.ThemeRepository
import com.example.domain.model.ThemeSpec
import com.example.launcher.model.*
import com.example.launcher.system.AppCatalogLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LauncherRepository(private val context: Context) {

  private val dao = LauncherDatabase.getInstance(context).launcherDao()
  private val prefs = LauncherPreferencesStore(context)
  val themeRepository = ThemeRepository(context)

  val settings: Flow<LauncherSettings> = prefs.settings
  val homeItems: Flow<List<HomeItem>> = dao.observeHomeItems().map { list -> list.map { it.toDomain() } }
  val appUsage: Flow<Map<String, AppUsageEntity>> =
    dao.observeAppUsage().map { list -> list.associateBy { it.packageName } }
  val searchHistory: Flow<List<String>> =
    dao.observeSearchHistory().map { list -> list.map { it.query } }
  val allThemes: Flow<List<ThemeSpec>> = themeRepository.allThemes

  val activeTheme: Flow<ThemeSpec> =
    combine(themeRepository.allThemes, prefs.settings) { themes, s ->
      themes.firstOrNull { it.id == s.activeThemeId } ?: themes.first()
    }

  suspend fun initializeDefaultsIfNeeded(installedApps: List<InstalledAppInfo>) =
    withContext(Dispatchers.IO) {
      themeRepository.ensureBuiltInThemesSeeded()
      if (dao.countHomeItems() > 0) return@withContext

      val initialItems = mutableListOf<HomeItem>()
      // Page 0: ColorOS 7 Clock Widget + Search Pill + Starter Apps + Productivity Folder
      initialItems +=
        HomeItem(
          id = "widget_clock_0",
          type = HomeItemType.CUSTOM_WIDGET,
          page = 0,
          row = 0,
          col = 0,
          spanX = 4,
          spanY = 1,
          customWidgetKind = CustomWidgetKind.CLOCK,
          widgetGlass = true,
        )
      initialItems +=
        HomeItem(
          id = "widget_search_0",
          type = HomeItemType.CUSTOM_WIDGET,
          page = 0,
          row = 1,
          col = 0,
          spanX = 4,
          spanY = 1,
          customWidgetKind = CustomWidgetKind.SEARCH,
          widgetGlass = true,
        )

      val starterApps = installedApps.take(10)
      var r = 2
      var c = 0
      starterApps.take(6).forEachIndexed { idx, app ->
        initialItems +=
          HomeItem(
            id = "app_init_${idx}_${app.packageName}",
            type = HomeItemType.APP,
            page = 0,
            row = r,
            col = c,
            packageName = app.packageName,
            activityName = app.activityName,
          )
        c++
        if (c >= 4) {
          c = 0
          r++
        }
      }

      if (starterApps.size >= 4) {
        initialItems +=
          HomeItem(
            id = "folder_starter_tools",
            type = HomeItemType.FOLDER,
            page = 0,
            row = r.coerceAtMost(4),
            col = c.coerceAtMost(3),
            folderTitle = "Essentials",
            folderColorHex = 0xFF38BDF8,
            folderStyle = FolderStyle.GLASS,
            folderLayout = FolderLayout.GRID,
            folderAppPackages = starterApps.drop(6).take(4).map { it.componentKey },
          )
      }

      // Page 1: Battery & Notes Widgets + Weather
      initialItems +=
        HomeItem(
          id = "widget_weather_1",
          type = HomeItemType.CUSTOM_WIDGET,
          page = 1,
          row = 0,
          col = 0,
          spanX = 2,
          spanY = 1,
          customWidgetKind = CustomWidgetKind.WEATHER,
          widgetGlass = true,
        )
      initialItems +=
        HomeItem(
          id = "widget_battery_1",
          type = HomeItemType.CUSTOM_WIDGET,
          page = 1,
          row = 0,
          col = 2,
          spanX = 2,
          spanY = 1,
          customWidgetKind = CustomWidgetKind.BATTERY,
          widgetGlass = true,
        )
      initialItems +=
        HomeItem(
          id = "widget_notes_1",
          type = HomeItemType.CUSTOM_WIDGET,
          page = 1,
          row = 1,
          col = 0,
          spanX = 4,
          spanY = 2,
          customWidgetKind = CustomWidgetKind.NOTES,
          widgetGlass = true,
          noteText = "Offline ColorOS 7 Launcher ready. Pinch or long-press Home to edit.",
        )

      dao.upsertHomeItems(initialItems.map { it.toEntity() })

      // Seed default Dock if empty
      val currentSettings = prefs.settings.first()
      if (currentSettings.dockPackages.isEmpty() && installedApps.isNotEmpty()) {
        val dockPkgs = installedApps.take(4).map { it.componentKey }
        prefs.updateSettings { it.copy(dockPackages = dockPkgs) }
      }
    }

  suspend fun loadApps(): List<InstalledAppInfo> = AppCatalogLoader.loadInstalledApps(context)

  suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
    prefs.updateSettings(transform)
  }

  suspend fun upsertHomeItem(item: HomeItem) = withContext(Dispatchers.IO) {
    dao.upsertHomeItem(item.toEntity())
  }

  suspend fun upsertHomeItems(items: List<HomeItem>) = withContext(Dispatchers.IO) {
    dao.upsertHomeItems(items.map { it.toEntity() })
  }

  suspend fun deleteHomeItem(id: String) = withContext(Dispatchers.IO) {
    dao.deleteHomeItem(id)
  }

  suspend fun recordAppLaunch(packageName: String) = withContext(Dispatchers.IO) {
    val prev = dao.getUsage(packageName)
    dao.upsertUsage(
      AppUsageEntity(
        packageName = packageName,
        launchCount = (prev?.launchCount ?: 0) + 1,
        lastLaunchedAt = System.currentTimeMillis(),
      ),
    )
  }

  suspend fun addSearchHistory(query: String) = withContext(Dispatchers.IO) {
    val trimmed = query.trim()
    if (trimmed.isNotEmpty()) {
      dao.insertSearchHistory(SearchHistoryEntity(trimmed, System.currentTimeMillis()))
    }
  }

  suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
    dao.clearSearchHistory()
  }

  suspend fun saveAndActivateTheme(theme: ThemeSpec) {
    themeRepository.setActiveTheme(theme)
    prefs.updateSettings { it.copy(activeThemeId = theme.id, fontStyle = theme.fontStyle) }
  }

  suspend fun exportBackupJson(): String {
    val s = prefs.settings.first()
    val items = dao.observeHomeItems().first().map { it.toDomain() }
    val theme = activeTheme.first()
    return LauncherBackupCodec.encodeBackup(s, items, theme)
  }

  suspend fun importBackupJson(rawJson: String): Result<String> =
    withContext(Dispatchers.IO) {
      LauncherBackupCodec.decodeBackup(rawJson).map { payload ->
        prefs.replaceSettings(payload.settings)
        dao.clearAllHomeItems()
        dao.upsertHomeItems(payload.homeItems.map { it.toEntity() })
        payload.activeTheme?.let { theme ->
          themeRepository.setActiveTheme(theme)
        }
        "Restored ${payload.homeItems.size} home items and launcher settings"
      }
    }
}
