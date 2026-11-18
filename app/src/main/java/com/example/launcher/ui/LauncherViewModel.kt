package com.example.launcher.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AetherAiArchitectRepository
import com.example.ai.AiArchitectChatTurn
import com.example.ai.AiGenerationResult
import com.example.domain.model.*
import com.example.engine.*
import com.example.launcher.data.AppUsageEntity
import com.example.launcher.data.LauncherRepository
import com.example.launcher.engine.GeneratedPaletteResult
import com.example.launcher.engine.ThemeGeneratorEngine
import com.example.launcher.model.*
import com.example.launcher.system.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "LauncherViewModel"

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

  private val repo = LauncherRepository(application)
  private val aiRepo = AetherAiArchitectRepository()
  private val initialCaps = HardwareCapabilityEngine.inspectDevice(application)
  private val initialRules =
    HardwareCapabilityEngine.resolveRenderRules(PerformanceMode.AUTO, initialCaps, true)

  private val _isInitialized = MutableStateFlow(false)
  private val _destination = MutableStateFlow(LauncherDestination.HOME)
  private val _settingsSection = MutableStateFlow<SettingsSection?>(null)
  private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
  private val _searchQuery = MutableStateFlow("")
  private val _contactMatches = MutableStateFlow<List<ContactSearchMatch>>(emptyList())
  private val _isDrawerOpen = MutableStateFlow(false)
  private val _isControlCenterOpen = MutableStateFlow(false)
  private val _isZeroScreenActive = MutableStateFlow(false)
  private val _isEditMode = MutableStateFlow(false)
  private val _activeFolderId = MutableStateFlow<String?>(null)
  private val _pendingUnlockApp = MutableStateFlow<InstalledAppInfo?>(null)
  private val _activeWebApp = MutableStateFlow<IsolatedWebApp?>(null)
  private val _currentPage = MutableStateFlow(0)
  private val _wallpaperBitmap = MutableStateFlow<Bitmap?>(null)
  private val _capabilities = MutableStateFlow(initialCaps)
  private val _ccSnapshot =
    MutableStateFlow(ControlCenterSystemController.inspectSnapshot(application))
  private val _torchEnabled = MutableStateFlow(false)
  private val _capsuleState = MutableStateFlow(DynamicCapsuleState())
  private val _isForeground = MutableStateFlow(true)
  private val _themeGenPrompt =
    MutableStateFlow("Aether liquid glass with warm obsidian highlights and electric cyan accents")
  private val _themeGenResult = MutableStateFlow<GeneratedPaletteResult?>(null)
  private val _aiArchitect = MutableStateFlow(AiArchitectUiState())
  private val _statusMessage = MutableStateFlow<String?>(null)
  private val _isDefaultHome =
    MutableStateFlow(LauncherSystemBridge.isDefaultHomeLauncher(application))
  private val _isDeviceAdminActive =
    MutableStateFlow(LauncherSystemBridge.isDeviceAdminActive(application))

  private var wallpaperLoadJob: Job? = null
  private var contactSearchJob: Job? = null
  private var stopwatchJob: Job? = null
  private var aiJob: Job? = null

  private val torchCallback =
    object : CameraManager.TorchCallback() {
      override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
        _torchEnabled.value = enabled
      }
    }

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
      _isControlCenterOpen,
      _isEditMode,
      _activeFolderId,
      _pendingUnlockApp,
      _activeWebApp,
      _isZeroScreenActive,
    ) { vals ->
      @Suppress("UNCHECKED_CAST")
      InteractionBundle(
        destination = vals[0] as LauncherDestination,
        settingsSection = vals[1] as SettingsSection?,
        apps = vals[2] as List<InstalledAppInfo>,
        query = vals[3] as String,
        contacts = vals[4] as List<ContactSearchMatch>,
        drawerOpen = vals[5] as Boolean,
        ccOpen = vals[6] as Boolean,
        editMode = vals[7] as Boolean,
        folderId = vals[8] as String?,
        pendingUnlockApp = vals[9] as InstalledAppInfo?,
        activeWebApp = vals[10] as IsolatedWebApp?,
        zeroScreenActive = vals[11] as Boolean,
      )
    }

  private val systemBundleFlow =
    combine(
      _isInitialized,
      _currentPage,
      _wallpaperBitmap,
      _capabilities,
      _ccSnapshot,
      _torchEnabled,
      _capsuleState,
      _isForeground,
      _themeGenPrompt,
      _themeGenResult,
      _aiArchitect,
      _statusMessage,
      _isDefaultHome,
      _isDeviceAdminActive,
      NotificationStreamHub.notifications,
      NotificationStreamHub.activeMedia,
    ) { vals ->
      @Suppress("UNCHECKED_CAST")
      SystemBundle(
        initialized = vals[0] as Boolean,
        page = vals[1] as Int,
        wpBitmap = vals[2] as Bitmap?,
        caps = vals[3] as DeviceCapabilities,
        ccSnapshot = vals[4] as ControlCenterSystemSnapshot,
        torchEnabled = vals[5] as Boolean,
        capsule = vals[6] as DynamicCapsuleState,
        fg = vals[7] as Boolean,
        genPrompt = vals[8] as String,
        genResult = vals[9] as GeneratedPaletteResult?,
        aiArchitect = vals[10] as AiArchitectUiState,
        status = vals[11] as String?,
        isDefaultHome = vals[12] as Boolean,
        isDeviceAdmin = vals[13] as Boolean,
        notifications = vals[14] as List<LiveNotificationItem>,
        activeMedia = vals[15] as ActiveMediaTrack?,
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
      val mergedTheme = applyGlassAndAccentOverrides(data.activeTheme, data.settings)
      val ctx = getApplication<Application>()

      LauncherUiState(
        isInitialized = sys.initialized,
        destination = inter.destination,
        settingsSection = inter.settingsSection,
        settings = data.settings,
        activeTheme = mergedTheme,
        allThemes = data.allThemes,
        installedApps = enrichedApps,
        homeItems = data.homeItems,
        searchHistory = data.history,
        searchQuery = inter.query,
        contactMatches = inter.contacts,
        isDrawerOpen = inter.drawerOpen,
        isControlCenterOpen = inter.ccOpen,
        isZeroScreenActive = inter.zeroScreenActive,
        isEditMode = inter.editMode,
        activeFolderId = inter.folderId,
        pendingUnlockApp = inter.pendingUnlockApp,
        activeWebApp = inter.activeWebApp,
        currentPage = sys.page,
        wallpaperBitmap = sys.wpBitmap,
        deviceCapabilities = sys.caps,
        renderRules = rules,
        controlCenterSnapshot = sys.ccSnapshot,
        torchEnabled = sys.torchEnabled,
        liveNotifications = sys.notifications,
        activeMedia = sys.activeMedia,
        capsuleState = sys.capsule,
        isDefaultHome = sys.isDefaultHome,
        isDeviceAdminActive = sys.isDeviceAdmin,
        isSpeechAvailable = LauncherSystemBridge.isSpeechRecognitionAvailable(ctx),
        isForeground = sys.fg,
        themeGenPrompt = sys.genPrompt,
        themeGenResult = sys.genResult,
        aiArchitect = sys.aiArchitect,
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
    runCatching {
      val cm = application.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
      cm?.registerTorchCallback(torchCallback, null)
    }

    viewModelScope.launch {
      val curSettings = repo.settings.first()
      if (!curSettings.onboardingCompleted) {
        _destination.value = LauncherDestination.ONBOARDING
      }
      val apps = repo.loadApps()
      _installedApps.value = apps
      repo.initializeDefaultsIfNeeded(apps)
      generateThemePaletteFromText(_themeGenPrompt.value, silent = true)
      val curTheme = repo.activeTheme.first()
      val rules =
        HardwareCapabilityEngine.resolveRenderRules(
          curSettings.performanceMode,
          initialCaps,
          curSettings.animationsEnabled,
        )
      refreshWallpaperBitmap(curTheme, curSettings.customWallpaperUri, rules)
      _isInitialized.value = true
    }
  }

  override fun onCleared() {
    super.onCleared()
    runCatching {
      val cm = getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as? CameraManager
      cm?.unregisterTorchCallback(torchCallback)
    }
  }

  private fun applyGlassAndAccentOverrides(theme: ThemeSpec, s: LauncherSettings): ThemeSpec {
    val accent = s.customAccentHex
    val palette =
      if (accent != null) {
        theme.palette.copy(primaryHex = accent, accentGlowHex = accent)
      } else {
        theme.palette
      }
    return theme.copy(
      palette = palette,
      glass =
        theme.glass.copy(
          blurStrength = s.glassBlurStrength,
          transparency = s.glassTransparency,
          reflectionOpacity = s.glassBorderReflection,
          shadowDepth = s.glassShadowDepth,
        ),
      fontStyle = s.fontStyle,
    )
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
      _ccSnapshot.value = ControlCenterSystemController.inspectSnapshot(ctx)
      _installedApps.value = repo.loadApps()
    }
  }

  fun completeOnboarding() {
    viewModelScope.launch {
      repo.updateSettings { it.copy(onboardingCompleted = true) }
      _destination.value = LauncherDestination.HOME
    }
  }

  fun replayOnboarding() {
    _isDrawerOpen.value = false
    _isControlCenterOpen.value = false
    _destination.value = LauncherDestination.ONBOARDING
  }

  fun navigateTo(dest: LauncherDestination, section: SettingsSection? = null) {
    _destination.value = dest
    _settingsSection.value = section
    if (dest != LauncherDestination.HOME) {
      _isDrawerOpen.value = false
      _isControlCenterOpen.value = false
      _isZeroScreenActive.value = false
      _isEditMode.value = false
      _activeFolderId.value = null
    }
  }

  fun selectSettingsSection(section: SettingsSection?) {
    _settingsSection.value = section
  }

  fun openDrawer(open: Boolean) {
    _isDrawerOpen.value = open
    if (open) _isControlCenterOpen.value = false
    if (!open) _searchQuery.value = ""
  }

  fun openControlCenter(open: Boolean) {
    _isControlCenterOpen.value = open
    if (open) {
      _isDrawerOpen.value = false
      _ccSnapshot.value = ControlCenterSystemController.inspectSnapshot(getApplication())
    }
  }

  fun openZeroScreen(open: Boolean) {
    _isZeroScreenActive.value = open
    if (open) {
      _isDrawerOpen.value = false
      _isControlCenterOpen.value = false
      _ccSnapshot.value = ControlCenterSystemController.inspectSnapshot(getApplication())
    }
  }

  fun setEditMode(edit: Boolean) {
    _isEditMode.value = edit
    if (edit) {
      _isDrawerOpen.value = false
      _isControlCenterOpen.value = false
    }
  }

  fun openFolder(folderId: String?) {
    _activeFolderId.value = folderId
  }

  fun setCurrentPage(page: Int) {
    _currentPage.value = page
  }

  fun toggleCapsuleExpanded() {
    _capsuleState.update { it.copy(expanded = !it.expanded) }
  }

  fun startOrPauseStopwatch() {
    val currentlyRunning = _capsuleState.value.stopwatchRunning
    if (currentlyRunning) {
      stopwatchJob?.cancel()
      _capsuleState.update { it.copy(stopwatchRunning = false) }
    } else {
      _capsuleState.update { it.copy(stopwatchRunning = true) }
      stopwatchJob?.cancel()
      stopwatchJob =
        viewModelScope.launch {
          while (true) {
            delay(1_000L)
            _capsuleState.update { it.copy(stopwatchElapsedMs = it.stopwatchElapsedMs + 1_000L) }
          }
        }
    }
  }

  fun resetStopwatch() {
    stopwatchJob?.cancel()
    _capsuleState.update { it.copy(stopwatchRunning = false, stopwatchElapsedMs = 0L) }
  }

  // --- Control Center System Actions ---

  fun refreshControlCenterSnapshot() {
    _ccSnapshot.value = ControlCenterSystemController.inspectSnapshot(getApplication())
  }

  fun toggleTorch() {
    val ctx = getApplication<Application>()
    if (!_ccSnapshot.value.torchAvailable) {
      _statusMessage.value = "Camera flash hardware unavailable on this device"
      return
    }
    val next = !_torchEnabled.value
    val ok = ControlCenterSystemController.setTorchEnabled(ctx, next)
    if (ok) _torchEnabled.value = next
    else _statusMessage.value = "Unable to toggle flashlight"
  }

  fun setSystemBrightness(percent: Int) {
    val ctx = getApplication<Application>()
    if (ControlCenterSystemController.setSystemBrightnessPercent(ctx, percent)) {
      _ccSnapshot.value = ControlCenterSystemController.inspectSnapshot(ctx)
    } else {
      ControlCenterSystemController.openWriteSettingsPermission(ctx)
      _statusMessage.value = "Grant 'Modify system settings' to control device brightness"
    }
  }

  fun setMediaVolume(index: Int) {
    val ctx = getApplication<Application>()
    ControlCenterSystemController.setMediaVolume(ctx, index)
    _ccSnapshot.value = ControlCenterSystemController.inspectSnapshot(ctx)
  }

  fun toggleAutoRotate() {
    val ctx = getApplication<Application>()
    val target = !_ccSnapshot.value.autoRotateOn
    if (ControlCenterSystemController.setAutoRotate(ctx, target)) {
      _ccSnapshot.value = ControlCenterSystemController.inspectSnapshot(ctx)
    } else {
      ControlCenterSystemController.openWriteSettingsPermission(ctx)
      _statusMessage.value = "Grant 'Modify system settings' or toggle in Display Settings"
    }
  }

  fun cycleRingerMode() {
    val ctx = getApplication<Application>()
    ControlCenterSystemController.cycleRingerMode(ctx)
    _ccSnapshot.value = ControlCenterSystemController.inspectSnapshot(ctx)
  }

  // --- Search ---

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
    viewModelScope.launch { repo.addSearchHistory(query) }
  }

  fun clearSearchHistory() {
    viewModelScope.launch {
      repo.clearSearchHistory()
      _statusMessage.value = "Local search history cleared"
    }
  }

  // --- App Launch & Cloner Vault App Lock ---

  fun launchApp(app: InstalledAppInfo) {
    val settings = uiState.value.settings
    if (app.packageName in settings.lockedPackages) {
      _pendingUnlockApp.value = app
    } else {
      performDirectLaunch(app)
    }
  }

  fun cancelUnlockPrompt() {
    _pendingUnlockApp.value = null
  }

  fun completeUnlockedLaunch() {
    val app = _pendingUnlockApp.value ?: return
    _pendingUnlockApp.value = null
    performDirectLaunch(app)
  }

  private fun performDirectLaunch(app: InstalledAppInfo) {
    val launched = AppCatalogLoader.launchInstalledApp(getApplication(), app)
    if (launched) {
      viewModelScope.launch { repo.recordAppLaunch(app.packageName) }
      _isDrawerOpen.value = false
      _activeFolderId.value = null
      _isZeroScreenActive.value = false
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

  fun toggleLockedPackage(packageName: String, locked: Boolean) {
    updateSettings { s ->
      val next = s.lockedPackages.toMutableSet()
      if (locked) next.add(packageName) else next.remove(packageName)
      s.copy(lockedPackages = next)
    }
    _statusMessage.value =
      if (locked) "App added to Cloner Vault launcher lock"
      else "App lock removed"
  }

  fun setVaultPin(pin: String?) {
    val hashed = pin?.takeIf { it.length >= 4 }?.let { VaultSecurityHelper.hashPin(it) }
    updateSettings { it.copy(vaultPinHash = hashed) }
    _statusMessage.value = if (hashed != null) "Cloner Vault PIN saved" else "Cloner Vault PIN cleared"
  }

  fun addIsolatedWebApp(title: String, rawUrl: String, isolateCookies: Boolean) {
    val safeUrl = VaultSecurityHelper.sanitizeHttpsUrl(rawUrl)
    if (safeUrl == null) {
      _statusMessage.value = "Enter a valid https:// or http:// web app URL"
      return
    }
    val item =
      IsolatedWebApp(
        id = "webapp_${System.currentTimeMillis()}",
        title = title.trim().ifBlank { Uri.parse(safeUrl).host ?: "Web App" },
        url = safeUrl,
        isolateCookies = isolateCookies,
      )
    updateSettings { s -> s.copy(isolatedWebApps = s.isolatedWebApps + item) }
    _statusMessage.value = "Added isolated web app: ${item.title}"
  }

  fun removeIsolatedWebApp(id: String) {
    updateSettings { s -> s.copy(isolatedWebApps = s.isolatedWebApps.filterNot { it.id == id }) }
  }

  fun openIsolatedWebApp(webApp: IsolatedWebApp?) {
    _activeWebApp.value = webApp
  }

  // --- Settings, Home Grid & Folders ---

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

  fun updateThemeLockAndStyle(transform: (ThemeSpec) -> ThemeSpec) {
    viewModelScope.launch {
      val current = repo.activeTheme.first()
      val updated = transform(current).copy(isBuiltIn = false)
      repo.saveAndActivateTheme(updated)
      refreshWallpaperBitmap(updated, uiState.value.settings.customWallpaperUri, uiState.value.renderRules)
      _statusMessage.value = "Updated theme studio preset"
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

  fun createFolderOnHome(title: String, apps: List<InstalledAppInfo>, page: Int = _currentPage.value) {
    if (apps.isEmpty()) return
    viewModelScope.launch {
      val s = uiState.value.settings
      val folder =
        HomeItem(
          id = "folder_${System.currentTimeMillis()}",
          type = HomeItemType.FOLDER,
          page = page.coerceIn(0, (s.pageCount - 1).coerceAtLeast(0)),
          row = 0,
          col = 0,
          folderTitle = title.trim().ifBlank { "Folder" },
          folderColorHex = s.defaultFolderColorHex,
          folderStyle = s.defaultFolderStyle,
          folderLayout = s.defaultFolderLayout,
          folderAppPackages = apps.map { it.componentKey }.distinct(),
        )
      repo.upsertHomeItem(folder)
      _statusMessage.value = "Created folder \"${folder.folderTitle}\" on Page ${folder.page + 1}"
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

  // --- AI Architect (Gemini AI Logic + Onboard Engine) ---

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

  fun generateGeminiRecommendations(promptOverride: String? = null) {
    val prompt = (promptOverride ?: _themeGenPrompt.value).trim()
    if (prompt.isNotBlank()) _themeGenPrompt.value = prompt
    aiJob?.cancel()
    aiJob =
      viewModelScope.launch {
        _aiArchitect.update {
          it.copy(isGeneratingThemes = true, errorMessage = null, overviewMessage = null)
        }
        when (
          val res =
            aiRepo.generateGeminiThemeRecommendations(
              prompt = prompt.ifBlank { "Aether liquid glass with warm obsidian highlights" },
              tier = uiState.value.renderRules.tier,
              formFactor = uiState.value.deviceCapabilities.formFactor,
            )
        ) {
          is AiGenerationResult.Success -> {
            _aiArchitect.update {
              it.copy(
                isGeneratingThemes = false,
                recommendations = res.recommendations,
                overviewMessage = res.overview,
                errorMessage = null,
              )
            }
          }
          is AiGenerationResult.Error -> {
            _aiArchitect.update {
              it.copy(
                isGeneratingThemes = false,
                errorMessage = res.message,
              )
            }
          }
        }
      }
  }

  fun generateOfflineRecommendations(promptOverride: String? = null) {
    val prompt = (promptOverride ?: _themeGenPrompt.value).trim()
    val recs =
      aiRepo.generateOfflineFallbackRecommendations(
        prompt = prompt,
        tier = uiState.value.renderRules.tier,
        formFactor = uiState.value.deviceCapabilities.formFactor,
      )
    generateThemePaletteFromText(prompt, silent = false)
    _aiArchitect.update {
      it.copy(
        recommendations = recs,
        overviewMessage = "Generated 3 palettes locally using the Onboard HSL & Contrast Engine.",
        errorMessage = null,
      )
    }
  }

  fun askAiQuestion(question: String) {
    val trimmed = question.trim()
    if (trimmed.isBlank()) return
    aiJob?.cancel()
    val userTurn = AiArchitectChatTurn("u_${System.currentTimeMillis()}", true, trimmed)
    val replyId = "a_${System.currentTimeMillis()}"
    _aiArchitect.update {
      it.copy(
        isStreamingAnswer = true,
        errorMessage = null,
        chatTurns = it.chatTurns + userTurn + AiArchitectChatTurn(replyId, false, "Thinking…"),
      )
    }
    aiJob =
      viewModelScope.launch {
        try {
          aiRepo.streamQuestion(
            question = trimmed,
            settings = uiState.value.settings,
            activeTheme = uiState.value.activeTheme,
            capabilities = uiState.value.deviceCapabilities,
          ).collect { partial ->
            _aiArchitect.update { cur ->
              cur.copy(
                chatTurns = cur.chatTurns.map { t -> if (t.id == replyId) t.copy(text = partial) else t },
              )
            }
          }
          _aiArchitect.update { it.copy(isStreamingAnswer = false) }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "AI Architect Q&A failed", e)
          val msg = e.localizedMessage ?: "Gemini AI Logic is unavailable offline."
          _aiArchitect.update { cur ->
            cur.copy(
              isStreamingAnswer = false,
              errorMessage = msg,
              chatTurns =
                cur.chatTurns.map { t ->
                  if (t.id == replyId) t.copy(text = "Gemini error: $msg", isError = true)
                  else t
                },
            )
          }
        }
      }
  }

  fun applyGeneratedTheme(setWallpaperToo: Boolean = false) {
    val generated = _themeGenResult.value ?: return
    applyThemeSpec(generated.theme, setWallpaperToo)
  }

  fun applyThemeSpec(theme: ThemeSpec, setWallpaperToo: Boolean = false) {
    viewModelScope.launch {
      repo.saveAndActivateTheme(theme)
      repo.updateSettings {
        it.copy(
          customWallpaperUri = null,
          customAccentHex = null,
          glassBlurStrength = theme.glass.blurStrength,
          glassTransparency = theme.glass.transparency,
          glassBorderReflection = theme.glass.reflectionOpacity,
          glassShadowDepth = theme.glass.shadowDepth,
        )
      }
      refreshWallpaperBitmap(theme, null, uiState.value.renderRules)
      if (setWallpaperToo) {
        ProceduralWallpaperRenderer.applyToDeviceWallpaper(
          context = getApplication(),
          theme = theme,
          target = WallpaperTarget.BOTH,
          rules = uiState.value.renderRules,
        )
      }
      _statusMessage.value =
        if (setWallpaperToo) "Applied \"${theme.name}\" & system wallpaper"
        else "Applied \"${theme.name}\" to Aether Glass OS"
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
    val ccOpen: Boolean,
    val editMode: Boolean,
    val folderId: String?,
    val pendingUnlockApp: InstalledAppInfo?,
    val activeWebApp: IsolatedWebApp?,
    val zeroScreenActive: Boolean,
  )

  private data class SystemBundle(
    val initialized: Boolean,
    val page: Int,
    val wpBitmap: Bitmap?,
    val caps: DeviceCapabilities,
    val ccSnapshot: ControlCenterSystemSnapshot,
    val torchEnabled: Boolean,
    val capsule: DynamicCapsuleState,
    val fg: Boolean,
    val genPrompt: String,
    val genResult: GeneratedPaletteResult?,
    val aiArchitect: AiArchitectUiState,
    val status: String?,
    val isDefaultHome: Boolean,
    val isDeviceAdmin: Boolean,
    val notifications: List<LiveNotificationItem>,
    val activeMedia: ActiveMediaTrack?,
  )
}
