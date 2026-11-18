package com.example.launcher.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.engine.*
import com.example.launcher.data.AppUsageEntity
import com.example.launcher.data.LauncherRepository
import com.example.launcher.engine.GeneratedPaletteResult
import com.example.launcher.engine.ThemeGeneratorEngine
import com.example.launcher.model.*
import com.example.launcher.system.AppCatalogLoader
import com.example.launcher.system.ContactSearchMatch
import com.example.launcher.system.LauncherSystemBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LauncherDestination {
  HOME,
  THEME_GENERATOR,
  SETTINGS,
}

data class LauncherUiState(
  val destination: LauncherDestination = LauncherDestination.HOME,
  val settingsSection: SettingsSection? = null,
  val settings: LauncherSettings = LauncherSettings(),
  val activeTheme: ThemeSpec = BuiltInThemes.all.first(),
  val allThemes: List<ThemeSpec> = BuiltInThemes.all,
  val installedApps: List<InstalledAppInfo> = emptyList(),
  val homeItems: List<HomeItem> = emptyList(),
  val searchHistory: List<String> = emptyList(),
  val searchQuery: String = "",
  val contactMatches: List<ContactSearchMatch> = emptyList(),
  val isDrawerOpen: Boolean = false,
  val isEditMode: Boolean = false,
  val activeFolderId: String? = null,
  val currentPage: Int = 0,
  val wallpaperBitmap: Bitmap? = null,
  val deviceCapabilities: DeviceCapabilities,
  val renderRules: EffectiveRenderRules,
  val isDefaultHome: Boolean = false,
  val isDeviceAdminActive: Boolean = false,
  val isSpeechAvailable: Boolean = false,
  val isForeground: Boolean = true,
  val themeGenPrompt: String =
    "ColorOS 7 frosted cyan and luminous violet glass with deep obsidian dock",
  val themeGenResult: GeneratedPaletteResult? = null,
  val statusMessage: String? = null,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

  private val repo = LauncherRepository(application)
  private val initialCaps = HardwareCapabilityEngine.inspectDevice(application)
  private val initialRules =
    HardwareCapabilityEngine.resolveRenderRules(PerformanceMode.AUTO, initialCaps, true)

  private val _destination = MutableStateFlow(LauncherDestination.HOME)
  private val _settingsSection = MutableStateFlow<SettingsSection?>(null)
  private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
  private val _searchQuery = MutableStateFlow("")
  private val _contactMatches = MutableStateFlow<List<ContactSearchMatch>>(emptyList())
  private val _isDrawerOpen = MutableStateFlow(false)
  private val _isEditMode = MutableStateFlow(false)
  private val _activeFolderId = MutableStateFlow<String?>(null)
  private val _currentPage = MutableStateFlow(0)
  private val _wallpaperBitmap = MutableStateFlow<Bitmap?>(null)
  private val _capabilities = MutableStateFlow(initialCaps)
  private val _isForeground = MutableStateFlow(true)
  private val _themeGenPrompt =
    MutableStateFlow("ColorOS 7 frosted cyan and luminous violet glass with deep obsidian dock")
  private val _themeGenResult = MutableStateFlow<GeneratedPaletteResult?>(null)
  private val _statusMessage = MutableStateFlow<String?>(null)
  private val _isDefaultHome =
    MutableStateFlow(LauncherSystemBridge.isDefaultHomeLauncher(application))
  private val _isDeviceAdminActive =
    MutableStateFlow(LauncherSystemBridge.isDeviceAdminActive(application))

  private var wallpaperLoadJob: Job? = null
  private var contactSearchJob: Job? = null

  private val dataBundleFlow =
    combine(
      repo.settings,
      repo.activeTheme,
      repo.allThemes,
      repo.homeItems,
      repo.appUsage,
      repo.searchHistory,
    ) { vals ->
      @Suppress("UNCHECKED_CAST")
      DataBundle(
        settings = vals[0] as LauncherSettings,
        activeTheme = vals[1] as ThemeSpec,
        allThemes = vals[2] as List<ThemeSpec>,
        homeItems = vals[3] as List<HomeItem>,
        usageMap = vals[4] as Map<String, AppUsageEntity>,
        history = vals[5] as List<String>,
      )
    }

  private val interactionFlow =
    combine(
      _destination,
      _settingsSection,
      _installedApps,
      _searchQuery,
      _contactMatches,
      _isDrawerOpen,
      _isEditMode,
      _activeFolderId,
    ) { vals ->
      @Suppress("UNCHECKED_CAST")
      InteractionBundle(
        destination = vals[0] as LauncherDestination,
        settingsSection = vals[1] as SettingsSection?,
        apps = vals[2] as List<InstalledAppInfo>,
        query = vals[3] as String,
        contacts = vals[4] as List<ContactSearchMatch>,
        drawerOpen = vals[5] as Boolean,
        editMode = vals[6] as Boolean,
        folderId = vals[7] as String?,
      )
    }

  private val systemBundleFlow =
    combine(
      _currentPage,
      _wallpaperBitmap,
      _capabilities,
      _isForeground,
      _themeGenPrompt,
      _themeGenResult,
      _statusMessage,
      _isDefaultHome,
      _isDeviceAdminActive,
    ) { vals ->
      SystemBundle(
        page = vals[0] as Int,
        wpBitmap = vals[1] as Bitmap?,
        caps = vals[2] as DeviceCapabilities,
        fg = vals[3] as Boolean,
        genPrompt = vals[4] as String,
        genResult = vals[5] as GeneratedPaletteResult?,
        status = vals[6] as String?,
        isDefaultHome = vals[7] as Boolean,
        isDeviceAdmin = vals[8] as Boolean,
      )
    }

  val uiState: StateFlow<LauncherUiState> =
    combine(dataBundleFlow, interactionFlow, systemBundleFlow) { data, inter, sys ->
      val enrichedApps =
        inter.apps.map { app ->
          val u = data.usageMap[app.packageName]
          val customLbl = data.settings.customAppLabels[app.componentKey]
          app.copy(
            label = customLbl ?: app.label,
            launchCount = u?.launchCount ?: 0,
            lastLaunchedAt = u?.lastLaunchedAt ?: 0L,
          )
        }
      val rules =
        HardwareCapabilityEngine.resolveRenderRules(
          mode = data.settings.performanceMode,
          caps = sys.caps,
          animationsMasterEnabled = data.settings.animationsEnabled,
        )
      val ctx = getApplication<Application>()

      LauncherUiState(
        destination = inter.destination,
        settingsSection = inter.settingsSection,
        settings = data.settings,
        activeTheme = data.activeTheme,
        allThemes = data.allThemes,
        installedApps = enrichedApps,
        homeItems = data.homeItems,
        searchHistory = data.history,
        searchQuery = inter.query,
        contactMatches = inter.contacts,
        isDrawerOpen = inter.drawerOpen,
        isEditMode = inter.editMode,
        activeFolderId = inter.folderId,
        currentPage = sys.page,
        wallpaperBitmap = sys.wpBitmap,
        deviceCapabilities = sys.caps,
        renderRules = rules,
        isDefaultHome = sys.isDefaultHome,
        isDeviceAdminActive = sys.isDeviceAdmin,
        isSpeechAvailable = LauncherSystemBridge.isSpeechRecognitionAvailable(ctx),
        isForeground = sys.fg,
        themeGenPrompt = sys.genPrompt,
        themeGenResult = sys.genResult,
        statusMessage = sys.status,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue =
        LauncherUiState(
          deviceCapabilities = initialCaps,
          renderRules = initialRules,
        ),
    )

  init {
    viewModelScope.launch {
      val apps = repo.loadApps()
      _installedApps.value = apps
      repo.initializeDefaultsIfNeeded(apps)
      generateThemePaletteFromText(_themeGenPrompt.value, silent = true)
      val curSettings = repo.settings.first()
      val curTheme = repo.activeTheme.first()
      val rules =
        HardwareCapabilityEngine.resolveRenderRules(
          curSettings.performanceMode,
          initialCaps,
          curSettings.animationsEnabled,
        )
      refreshWallpaperBitmap(curTheme, curSettings.customWallpaperUri, rules)
    }
  }

  fun setForegroundState(inForeground: Boolean) {
    _isForeground.value = inForeground
    if (inForeground) {
      refreshCapabilitiesAndApps()
    } else {
      contactSearchJob?.cancel()
    }
  }

  fun onTrimMemory() {
    AppCatalogLoader.trimIconCache()
    ProceduralWallpaperRenderer.trimMemory()
  }

  fun refreshCapabilitiesAndApps() {
    viewModelScope.launch {
      val ctx = getApplication<Application>()
      _capabilities.value = HardwareCapabilityEngine.inspectDevice(ctx)
      _isDefaultHome.value = LauncherSystemBridge.isDefaultHomeLauncher(ctx)
      _isDeviceAdminActive.value = LauncherSystemBridge.isDeviceAdminActive(ctx)
      _installedApps.value = repo.loadApps()
    }
  }

  fun navigateTo(dest: LauncherDestination, section: SettingsSection? = null) {
    _destination.value = dest
    _settingsSection.value = section
    if (dest != LauncherDestination.HOME) {
      _isDrawerOpen.value = false
      _isEditMode.value = false
      _activeFolderId.value = null
    }
  }

  fun selectSettingsSection(section: SettingsSection?) {
    _settingsSection.value = section
  }

  fun openDrawer(open: Boolean) {
    _isDrawerOpen.value = open
    if (!open) _searchQuery.value = ""
  }

  fun setEditMode(edit: Boolean) {
    _isEditMode.value = edit
    if (edit) _isDrawerOpen.value = false
  }

  fun openFolder(folderId: String?) {
    _activeFolderId.value = folderId
  }

  fun setCurrentPage(page: Int) {
    _currentPage.value = page
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
    contactSearchJob?.cancel()
    if (uiState.value.settings.searchContactsEnabled && query.length >= 2) {
      contactSearchJob =
        viewModelScope.launch {
          _contactMatches.value = AppCatalogLoader.searchContactsIfPermitted(getApplication(), query)
        }
    } else {
      _contactMatches.value = emptyList()
    }
  }

  fun submitSearchQuery(query: String) {
    viewModelScope.launch {
      repo.addSearchHistory(query)
    }
  }

  fun clearSearchHistory() {
    viewModelScope.launch {
      repo.clearSearchHistory()
      _statusMessage.value = "Local search history cleared"
    }
  }

  fun launchApp(app: InstalledAppInfo) {
    val launched = AppCatalogLoader.launchApp(getApplication(), app.packageName, app.activityName)
    if (launched) {
      viewModelScope.launch {
        repo.recordAppLaunch(app.packageName)
      }
      _isDrawerOpen.value = false
      _activeFolderId.value = null
    } else {
      _statusMessage.value = "Unable to launch ${app.label}"
    }
  }

  fun launchAppByComponentKey(componentKey: String) {
    val app = uiState.value.installedApps.firstOrNull { it.componentKey == componentKey }
    if (app != null) launchApp(app)
  }

  fun openAppInfo(packageName: String) {
    AppCatalogLoader.openAppInfo(getApplication(), packageName)
  }

  fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
    viewModelScope.launch {
      repo.updateSettings(transform)
      val s = repo.settings.first()
      val t = repo.activeTheme.first()
      val rules =
        HardwareCapabilityEngine.resolveRenderRules(
          s.performanceMode,
          _capabilities.value,
          s.animationsEnabled,
        )
      refreshWallpaperBitmap(t, s.customWallpaperUri, rules)
    }
  }

  fun setAppHidden(packageName: String, hidden: Boolean) {
    updateSettings { s ->
      val next = s.hiddenPackages.toMutableSet()
      if (hidden) next.add(packageName) else next.remove(packageName)
      s.copy(hiddenPackages = next)
    }
    _statusMessage.value =
      if (hidden) "App hidden from launcher view (not encrypted)" else "App restored to drawer"
  }

  fun setCustomAppLabel(componentKey: String, customLabel: String?) {
    updateSettings { s ->
      val map = s.customAppLabels.toMutableMap()
      if (customLabel.isNullOrBlank()) map.remove(componentKey) else map[componentKey] = customLabel.trim()
      s.copy(customAppLabels = map)
    }
  }

  fun setCustomAppIconStyle(componentKey: String, style: LauncherIconStyle?) {
    updateSettings { s ->
      val map = s.customAppIconStyles.toMutableMap()
      if (style == null) map.remove(componentKey) else map[componentKey] = style
      s.copy(customAppIconStyles = map)
    }
  }

  fun toggleDockApp(componentKey: String) {
    updateSettings { s ->
      val list = s.dockPackages.toMutableList()
      if (componentKey in list) {
        list.remove(componentKey)
      } else if (list.size < s.dockMaxIcons) {
        list.add(componentKey)
      }
      s.copy(dockPackages = list)
    }
  }

  fun addAppToHome(app: InstalledAppInfo, page: Int = _currentPage.value) {
    viewModelScope.launch {
      val s = uiState.value.settings
      val existing = uiState.value.homeItems.filter { it.page == page }
      val occupied = mutableSetOf<Pair<Int, Int>>()
      existing.forEach { item ->
        for (r in item.row until (item.row + item.spanY)) {
          for (c in item.col until (item.col + item.spanX)) {
            occupied.add(r to c)
          }
        }
      }
      var placedRow = -1
      var placedCol = -1
      outer@ for (r in 0 until s.gridRows) {
        for (c in 0 until s.gridColumns) {
          if ((r to c) !in occupied) {
            placedRow = r
            placedCol = c
            break@outer
          }
        }
      }
      val targetPage = if (placedRow >= 0) page else (page + 1).coerceAtMost(6)
      if (placedRow < 0) {
        placedRow = 0
        placedCol = 0
        if (targetPage >= s.pageCount) {
          repo.updateSettings { it.copy(pageCount = targetPage + 1) }
        }
      }
      repo.upsertHomeItem(
        HomeItem(
          id = "app_${System.currentTimeMillis()}",
          type = HomeItemType.APP,
          page = targetPage,
          row = placedRow,
          col = placedCol,
          packageName = app.packageName,
          activityName = app.activityName,
        ),
      )
      _statusMessage.value = "Added ${app.label} to Page ${targetPage + 1}"
    }
  }

  fun addCustomWidgetToHome(kind: CustomWidgetKind, page: Int = _currentPage.value) {
    viewModelScope.launch {
      val s = uiState.value.settings
      val item =
        HomeItem(
          id = "cwidget_${kind.name.lowercase()}_${System.currentTimeMillis()}",
          type = HomeItemType.CUSTOM_WIDGET,
          page = page,
          row = 0,
          col = 0,
          spanX = kind.defaultSpanX.coerceAtMost(s.gridColumns),
          spanY = kind.defaultSpanY.coerceAtMost(s.gridRows),
          customWidgetKind = kind,
          widgetGlass = s.defaultWidgetsGlass,
          widgetTransparent = s.defaultWidgetsTransparent,
        )
      repo.upsertHomeItem(item)
      _statusMessage.value = "Added ${kind.label} widget to Home"
    }
  }

  fun addSystemAppWidgetToHome(appWidgetId: Int, page: Int = _currentPage.value) {
    viewModelScope.launch {
      val item =
        HomeItem(
          id = "syswidget_${appWidgetId}_${System.currentTimeMillis()}",
          type = HomeItemType.SYSTEM_WIDGET,
          page = page,
          row = 0,
          col = 0,
          spanX = 4.coerceAtMost(uiState.value.settings.gridColumns),
          spanY = 2,
          appWidgetId = appWidgetId,
        )
      repo.upsertHomeItem(item)
      _statusMessage.value = "Added Android App Widget to Page ${page + 1}"
    }
  }

  fun createFolderFromTwoApps(first: HomeItem, second: HomeItem) {
    viewModelScope.launch {
      val s = uiState.value.settings
      val pkgs = buildList {
        if (first.type == HomeItemType.APP) add("${first.packageName}/${first.activityName}")
        if (first.type == HomeItemType.FOLDER) addAll(first.folderAppPackages)
        if (second.type == HomeItemType.APP) add("${second.packageName}/${second.activityName}")
        if (second.type == HomeItemType.FOLDER) addAll(second.folderAppPackages)
      }.distinct()

      val folder =
        HomeItem(
          id = if (first.type == HomeItemType.FOLDER) first.id else "folder_${System.currentTimeMillis()}",
          type = HomeItemType.FOLDER,
          page = first.page,
          row = first.row,
          col = first.col,
          folderTitle = if (first.type == HomeItemType.FOLDER) first.folderTitle else "New Folder",
          folderColorHex = s.defaultFolderColorHex,
          folderStyle = s.defaultFolderStyle,
          folderLayout = s.defaultFolderLayout,
          folderAppPackages = pkgs,
        )
      if (first.id != folder.id) repo.deleteHomeItem(first.id)
      repo.deleteHomeItem(second.id)
      repo.upsertHomeItem(folder)
      _statusMessage.value = "Updated folder (${pkgs.size} apps)"
    }
  }

  fun updateHomeItem(item: HomeItem) {
    viewModelScope.launch { repo.upsertHomeItem(item) }
  }

  fun removeHomeItem(item: HomeItem) {
    viewModelScope.launch {
      repo.deleteHomeItem(item.id)
      _statusMessage.value = "Removed item from Home Screen"
    }
  }

  fun moveHomeItemToNextSlot(item: HomeItem) {
    viewModelScope.launch {
      val s = uiState.value.settings
      var nextCol = item.col + 1
      var nextRow = item.row
      var nextPage = item.page
      if (nextCol + item.spanX > s.gridColumns) {
        nextCol = 0
        nextRow++
      }
      if (nextRow + item.spanY > s.gridRows) {
        nextRow = 0
        nextPage = (nextPage + 1) % s.pageCount
      }
      repo.upsertHomeItem(item.copy(page = nextPage, row = nextRow, col = nextCol))
    }
  }

  fun addHomePage() {
    updateSettings { s -> s.copy(pageCount = (s.pageCount + 1).coerceAtMost(7)) }
  }

  fun removeLastHomePage() {
    viewModelScope.launch {
      val s = uiState.value.settings
      if (s.pageCount <= 1) return@launch
      val lastPage = s.pageCount - 1
      uiState.value.homeItems.filter { it.page == lastPage }.forEach { repo.deleteHomeItem(it.id) }
      repo.updateSettings { it.copy(pageCount = lastPage) }
      if (_currentPage.value >= lastPage) _currentPage.value = lastPage - 1
    }
  }

  fun updateThemeGenPrompt(text: String) {
    _themeGenPrompt.value = text
  }

  fun generateThemePaletteFromText(description: String? = null, silent: Boolean = false) {
    val text = description ?: _themeGenPrompt.value
    _themeGenPrompt.value = text
    val result =
      ThemeGeneratorEngine.generatePaletteFromDescription(
        description = text,
        tier = uiState.value.renderRules.tier,
        formFactor = uiState.value.deviceCapabilities.formFactor,
      )
    _themeGenResult.value = result
    if (!silent) {
      viewModelScope.launch {
        OfflineSoundEngine.playPreviewTone(result.theme.motionAndSound.soundProfile)
      }
    }
  }

  fun applyGeneratedTheme(setWallpaperToo: Boolean = false) {
    val generated = _themeGenResult.value ?: return
    viewModelScope.launch {
      repo.saveAndActivateTheme(generated.theme)
      repo.updateSettings { it.copy(customWallpaperUri = null) }
      refreshWallpaperBitmap(generated.theme, null, uiState.value.renderRules)
      if (setWallpaperToo) {
        ProceduralWallpaperRenderer.applyToDeviceWallpaper(
          context = getApplication(),
          theme = generated.theme,
          target = WallpaperTarget.BOTH,
          rules = uiState.value.renderRules,
        )
      }
      _statusMessage.value =
        if (setWallpaperToo) "Applied ${generated.theme.name} palette & system wallpaper"
        else "Applied ${generated.theme.name} palette to Launcher"
    }
  }

  fun activateExistingTheme(theme: ThemeSpec) {
    viewModelScope.launch {
      repo.saveAndActivateTheme(theme)
      refreshWallpaperBitmap(theme, uiState.value.settings.customWallpaperUri, uiState.value.renderRules)
      _statusMessage.value = "Applied \"${theme.name}\" theme"
    }
  }

  fun setCustomPhotoWallpaper(uri: Uri) {
    viewModelScope.launch {
      val s = uiState.value.settings
      val uriStr = uri.toString()
      repo.updateSettings { it.copy(customWallpaperUri = uriStr) }
      val bmp = decodeUriBitmap(getApplication(), uriStr)
      if (bmp != null) {
        _wallpaperBitmap.value = bmp
        if (s.wallpaperBasedPalette) {
          val extracted = ThemeGeneratorEngine.extractPaletteFromBitmap(bmp, uiState.value.activeTheme)
          repo.saveAndActivateTheme(extracted)
        }
        _statusMessage.value = "Custom photo wallpaper updated"
      }
    }
  }

  fun extractThemeFromCurrentWallpaper() {
    val bmp = _wallpaperBitmap.value ?: return
    viewModelScope.launch {
      val extracted = ThemeGeneratorEngine.extractPaletteFromBitmap(bmp, uiState.value.activeTheme)
      repo.saveAndActivateTheme(extracted)
      _statusMessage.value = "Extracted colour theme from current wallpaper"
    }
  }

  fun applyCurrentWallpaperToSystem(target: WallpaperTarget) {
    viewModelScope.launch {
      val res =
        ProceduralWallpaperRenderer.applyToDeviceWallpaper(
          context = getApplication(),
          theme = uiState.value.activeTheme,
          target = target,
          rules = uiState.value.renderRules,
        )
      res.onSuccess { msg -> _statusMessage.value = msg }
        .onFailure { _statusMessage.value = "Wallpaper apply failed: ${it.message}" }
    }
  }

  suspend fun exportBackupJson(): String = repo.exportBackupJson()

  fun importBackupJson(rawJson: String) {
    viewModelScope.launch {
      repo.importBackupJson(rawJson)
        .onSuccess { msg ->
          _statusMessage.value = msg
          val s = repo.settings.first()
          val t = repo.activeTheme.first()
          refreshWallpaperBitmap(t, s.customWallpaperUri, uiState.value.renderRules)
        }
        .onFailure { _statusMessage.value = "Invalid launcher backup file" }
    }
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  private fun refreshWallpaperBitmap(
    theme: ThemeSpec,
    customUri: String?,
    rules: EffectiveRenderRules,
  ) {
    wallpaperLoadJob?.cancel()
    wallpaperLoadJob =
      viewModelScope.launch {
        val customBmp = customUri?.let { decodeUriBitmap(getApplication(), it) }
        _wallpaperBitmap.value =
          customBmp ?: ProceduralWallpaperRenderer.getOrRenderPreview(theme, rules)
      }
  }

  private suspend fun decodeUriBitmap(context: Context, uriStr: String): Bitmap? =
    withContext(Dispatchers.IO) {
      runCatching {
        context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
          val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
          BitmapFactory.decodeStream(stream, null, opts)
        }
      }.getOrNull()
    }

  private data class DataBundle(
    val settings: LauncherSettings,
    val activeTheme: ThemeSpec,
    val allThemes: List<ThemeSpec>,
    val homeItems: List<HomeItem>,
    val usageMap: Map<String, AppUsageEntity>,
    val history: List<String>,
  )

  private data class InteractionBundle(
    val destination: LauncherDestination,
    val settingsSection: SettingsSection?,
    val apps: List<InstalledAppInfo>,
    val query: String,
    val contacts: List<ContactSearchMatch>,
    val drawerOpen: Boolean,
    val editMode: Boolean,
    val folderId: String?,
  )

  private data class SystemBundle(
    val page: Int,
    val wpBitmap: Bitmap?,
    val caps: DeviceCapabilities,
    val fg: Boolean,
    val genPrompt: String,
    val genResult: GeneratedPaletteResult?,
    val status: String?,
    val isDefaultHome: Boolean,
    val isDeviceAdmin: Boolean,
  )
}
