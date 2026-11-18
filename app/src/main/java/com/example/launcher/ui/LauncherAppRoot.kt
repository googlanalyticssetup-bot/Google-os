package com.example.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher.model.GestureAction
import com.example.launcher.model.HomeItem
import com.example.launcher.model.InstalledAppInfo
import com.example.launcher.model.SettingsSection
import com.example.launcher.system.LauncherSystemBridge
import com.example.launcher.system.LauncherWidgetHostHolder
import com.example.launcher.ui.components.*
import com.example.launcher.ui.screens.*
import com.example.ui.components.FluidMotionSpecs
import com.example.ui.theme.ThemeStudioTheme

@Composable
fun LauncherAppRoot(
  viewModel: LauncherViewModel,
  widgetHostHolder: LauncherWidgetHostHolder,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  var actionTarget by remember { mutableStateOf<Pair<InstalledAppInfo, HomeItem?>?>(null) }

  LaunchedEffect(state.statusMessage) {
    val msg = state.statusMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  val isLightTheme =
    !state.activeTheme.palette.isAmoledDark && state.activeTheme.palette.backgroundHex >= 0xFFE0E0E0L

  val executeGesture: (GestureAction) -> Unit = { action ->
    when (action) {
      GestureAction.OPEN_DRAWER -> viewModel.openDrawer(true)
      GestureAction.OPEN_CONTROL_CENTER -> viewModel.openControlCenter(true)
      GestureAction.OPEN_ZERO_SCREEN -> {
        if (state.settings.zeroScreenEnabled) viewModel.openZeroScreen(true)
        else viewModel.openDrawer(true)
      }
      GestureAction.OPEN_NOTIFICATIONS -> {
        val ok = LauncherSystemBridge.tryExpandNotificationShade(context)
        if (!ok) viewModel.openControlCenter(true)
      }
      GestureAction.LOCK_SCREEN -> {
        val locked = LauncherSystemBridge.tryLockScreen(context)
        if (!locked) {
          viewModel.navigateTo(LauncherDestination.SETTINGS, SettingsSection.GESTURES)
        }
      }
      GestureAction.OPEN_EDIT_MODE -> viewModel.setEditMode(true)
      GestureAction.OPEN_SEARCH -> viewModel.openDrawer(true)
      GestureAction.OPEN_THEME_GENERATOR -> viewModel.navigateTo(LauncherDestination.THEME_GENERATOR)
      GestureAction.OPEN_CLONER_VAULT -> viewModel.navigateTo(LauncherDestination.CLONER_VAULT)
      GestureAction.OPEN_LOCK_DESIGNER -> viewModel.navigateTo(LauncherDestination.LOCK_DESIGNER)
      GestureAction.OPEN_SETTINGS -> viewModel.navigateTo(LauncherDestination.SETTINGS, null)
      GestureAction.NEXT_PAGE -> {
        val next = (state.currentPage + 1) % state.settings.pageCount.coerceAtLeast(1)
        viewModel.setCurrentPage(next)
      }
      GestureAction.NONE -> {}
    }
  }

  ThemeStudioTheme(
    darkTheme = !isLightTheme,
    accentOverride = Color(state.activeTheme.palette.primaryHex),
    secondaryOverride = Color(state.activeTheme.palette.secondaryHex),
  ) {
    Scaffold(
      containerColor = Color.Transparent,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(
                  MaterialTheme.colorScheme.background,
                  MaterialTheme.colorScheme.surface,
                ),
              ),
            )
            .padding(
              bottom =
                if (state.destination == LauncherDestination.HOME ||
                  state.destination == LauncherDestination.ONBOARDING
                ) {
                  0.dp
                } else {
                  innerPadding.calculateBottomPadding()
                },
            ),
      ) {
        when (state.destination) {
          LauncherDestination.ONBOARDING -> {
            OnboardingScreen(
              state = state,
              onActivateTheme = viewModel::activateExistingTheme,
              onUpdateSettings = viewModel::updateSettings,
              onRefreshSystemStatus = viewModel::refreshCapabilitiesAndApps,
              onComplete = viewModel::completeOnboarding,
            )
          }

          LauncherDestination.HOME -> {
            HomeScreenView(
              state = state,
              widgetHostHolder = widgetHostHolder,
              onOpenDrawer = { viewModel.openDrawer(true) },
              onSetEditMode = viewModel::setEditMode,
              onPageChanged = viewModel::setCurrentPage,
              onTriggerGesture = executeGesture,
              onLaunchApp = viewModel::launchApp,
              onOpenFolder = { id -> viewModel.openFolder(id) },
              onLongPressApp = { app, item -> actionTarget = app to item },
              onAddCustomWidget = { kind -> viewModel.addCustomWidgetToHome(kind) },
              onAddSystemWidget = { id -> viewModel.addSystemAppWidgetToHome(id) },
              onUpdateHomeItem = viewModel::updateHomeItem,
              onRemoveHomeItem = { item ->
                if (item.appWidgetId != -1) {
                  runCatching { widgetHostHolder.appWidgetHost.deleteAppWidgetId(item.appWidgetId) }
                }
                viewModel.removeHomeItem(item)
              },
              onCreateFolderFromTwo = viewModel::createFolderFromTwoApps,
              onAddPage = viewModel::addHomePage,
              onRemoveLastPage = viewModel::removeLastHomePage,
              onOpenThemeGenerator = { viewModel.navigateTo(LauncherDestination.THEME_GENERATOR) },
              onOpenSettings = { sec -> viewModel.navigateTo(LauncherDestination.SETTINGS, sec) },
              onOpenControlCenter = { viewModel.openControlCenter(true) },
              onOpenZeroScreen = { viewModel.openZeroScreen(true) },
              onOpenClonerVault = { viewModel.navigateTo(LauncherDestination.CLONER_VAULT) },
              onToggleCapsuleExpand = viewModel::toggleCapsuleExpanded,
              onToggleStopwatch = viewModel::startOrPauseStopwatch,
              onResetStopwatch = viewModel::resetStopwatch,
              onShowMessage = { /* snackbar via viewModel */ },
            )
          }

          LauncherDestination.THEME_GENERATOR -> {
            ThemeGeneratorScreen(
              state = state,
              onPromptChange = viewModel::updateThemeGenPrompt,
              onGeneratePalette = { p -> viewModel.generateThemePaletteFromText(p) },
              onGenerateGeminiRecommendations = { p -> viewModel.generateGeminiRecommendations(p) },
              onGenerateOfflineRecommendations = { p -> viewModel.generateOfflineRecommendations(p) },
              onAskAiQuestion = viewModel::askAiQuestion,
              onApplyThemeSpec = viewModel::applyThemeSpec,
              onApplyPalette = viewModel::applyGeneratedTheme,
              onExtractFromWallpaper = viewModel::extractThemeFromCurrentWallpaper,
              onBack = { viewModel.navigateTo(LauncherDestination.HOME) },
            )
          }

          LauncherDestination.SETTINGS -> {
            LauncherSettingsScreen(
              state = state,
              onSelectSection = viewModel::selectSettingsSection,
              onUpdateSettings = viewModel::updateSettings,
              onActivateTheme = viewModel::activateExistingTheme,
              onOpenThemeGenerator = { viewModel.navigateTo(LauncherDestination.THEME_GENERATOR) },
              onOpenLockDesigner = { viewModel.navigateTo(LauncherDestination.LOCK_DESIGNER) },
              onOpenClonerVault = { viewModel.navigateTo(LauncherDestination.CLONER_VAULT) },
              onOpenControlCenter = {
                viewModel.navigateTo(LauncherDestination.HOME)
                viewModel.openControlCenter(true)
              },
              onReplayOnboarding = viewModel::replayOnboarding,
              onPickWallpaperPhoto = viewModel::setCustomPhotoWallpaper,
              onApplyWallpaperToSystem = viewModel::applyCurrentWallpaperToSystem,
              onExtractThemeFromWallpaper = viewModel::extractThemeFromCurrentWallpaper,
              onAddCustomWidget = { kind ->
                viewModel.addCustomWidgetToHome(kind)
                viewModel.navigateTo(LauncherDestination.HOME)
              },
              onToggleHiddenApp = viewModel::setAppHidden,
              onClearSearchHistory = viewModel::clearSearchHistory,
              onExportBackupSuspend = viewModel::exportBackupJson,
              onImportBackup = viewModel::importBackupJson,
              onTrimCaches = viewModel::onTrimMemory,
              onBack = { viewModel.navigateTo(LauncherDestination.HOME) },
            )
          }

          LauncherDestination.CLONER_VAULT -> {
            ClonerVaultScreen(
              state = state,
              onToggleLockPackage = viewModel::toggleLockedPackage,
              onSetVaultPin = viewModel::setVaultPin,
              onToggleHiddenPackage = viewModel::setAppHidden,
              onLaunchApp = viewModel::launchApp,
              onAddIsolatedWebApp = viewModel::addIsolatedWebApp,
              onRemoveIsolatedWebApp = viewModel::removeIsolatedWebApp,
              onOpenIsolatedWebApp = viewModel::openIsolatedWebApp,
              onBack = { viewModel.navigateTo(LauncherDestination.HOME) },
            )
          }

          LauncherDestination.LOCK_DESIGNER -> {
            LockScreenDesignerScreen(
              state = state,
              onUpdateTheme = viewModel::updateThemeLockAndStyle,
              onApplyLockWallpaper = viewModel::applyCurrentWallpaperToSystem,
              onBack = { viewModel.navigateTo(LauncherDestination.HOME) },
            )
          }
        }

        val animEnabled = state.renderRules.animationScale > 0f && state.isForeground
        val speed = state.settings.animationSpeed

        // Animated App Drawer Overlay (Swipe Up)
        AnimatedVisibility(
          visible = state.isDrawerOpen && state.destination == LauncherDestination.HOME,
          enter =
            if (animEnabled) {
              slideInVertically(FluidMotionSpecs.offsetSpring(speed)) { it / 3 } + fadeIn()
            } else {
              fadeIn()
            },
          exit =
            if (animEnabled) {
              slideOutVertically(FluidMotionSpecs.offsetSpring(speed)) { it / 3 } + fadeOut()
            } else {
              fadeOut()
            },
        ) {
          AppDrawerOverlay(
            state = state,
            onClose = { viewModel.openDrawer(false) },
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSubmitSearch = viewModel::submitSearchQuery,
            onLaunchApp = viewModel::launchApp,
            onLongPressApp = { app -> actionTarget = app to null },
            onOpenSettingsSection = { sec ->
              viewModel.openDrawer(false)
              viewModel.navigateTo(LauncherDestination.SETTINGS, sec)
            },
            onOpenThemeGenerator = {
              viewModel.openDrawer(false)
              viewModel.navigateTo(LauncherDestination.THEME_GENERATOR)
            },
            onCreateFolderFromDrawer = { title, apps ->
              viewModel.createFolderOnHome(title, apps, state.currentPage)
            },
            onOpenClonerVault = {
              viewModel.openDrawer(false)
              viewModel.navigateTo(LauncherDestination.CLONER_VAULT)
            },
          )
        }

        // Animated Control Center Overlay (Swipe Down)
        AnimatedVisibility(
          visible = state.isControlCenterOpen && state.destination == LauncherDestination.HOME,
          enter =
            if (animEnabled) {
              slideInVertically(FluidMotionSpecs.offsetSpring(speed)) { -it } + fadeIn()
            } else {
              fadeIn()
            },
          exit =
            if (animEnabled) {
              slideOutVertically(FluidMotionSpecs.offsetSpring(speed)) { -it } + fadeOut()
            } else {
              fadeOut()
            },
        ) {
          ControlCenterOverlay(
            state = state,
            onClose = { viewModel.openControlCenter(false) },
            onToggleTorch = viewModel::toggleTorch,
            onToggleAutoRotate = viewModel::toggleAutoRotate,
            onCycleRingerMode = viewModel::cycleRingerMode,
            onSetBrightness = viewModel::setSystemBrightness,
            onSetMediaVolume = viewModel::setMediaVolume,
            onRefreshSystem = viewModel::refreshControlCenterSnapshot,
            onUpdateSettings = viewModel::updateSettings,
            onOpenSettingsSection = { sec ->
              viewModel.openControlCenter(false)
              viewModel.navigateTo(LauncherDestination.SETTINGS, sec)
            },
          )
        }

        // Animated Zero Screen Workspace (Swipe Left / Minus-One)
        AnimatedVisibility(
          visible = state.isZeroScreenActive && state.destination == LauncherDestination.HOME,
          enter =
            if (animEnabled) {
              slideInHorizontally(FluidMotionSpecs.offsetSpring(speed)) { it } + fadeIn()
            } else {
              fadeIn()
            },
          exit =
            if (animEnabled) {
              slideOutHorizontally(FluidMotionSpecs.offsetSpring(speed)) { it } + fadeOut()
            } else {
              fadeOut()
            },
        ) {
          ZeroScreenView(
            state = state,
            onClose = { viewModel.openZeroScreen(false) },
            onOpenSearch = {
              viewModel.openZeroScreen(false)
              viewModel.openDrawer(true)
            },
            onOpenAiArchitect = {
              viewModel.openZeroScreen(false)
              viewModel.navigateTo(LauncherDestination.THEME_GENERATOR)
            },
            onAskQuickAi = { q -> viewModel.askAiQuestion(q) },
            onOpenClonerVault = {
              viewModel.openZeroScreen(false)
              viewModel.navigateTo(LauncherDestination.CLONER_VAULT)
            },
            onLaunchApp = viewModel::launchApp,
            onOpenWebApp = { webApp -> viewModel.openIsolatedWebApp(webApp) },
          )
        }
      }

      // Active Folder Modal
      val openFolder = state.homeItems.firstOrNull { it.id == state.activeFolderId }
      if (openFolder != null) {
        FolderModalDialog(
          folder = openFolder,
          allApps = state.installedApps,
          settings = state.settings,
          theme = state.activeTheme,
          rules = state.renderRules,
          onDismiss = { viewModel.openFolder(null) },
          onLaunchApp = viewModel::launchApp,
          onUpdateFolder = viewModel::updateHomeItem,
          onDeleteFolder = {
            viewModel.removeHomeItem(openFolder)
            viewModel.openFolder(null)
          },
        )
      }

      // App Long-Press Action Dialog
      actionTarget?.let { (app, homeItem) ->
        AppQuickActionDialog(
          app = app,
          homeItem = homeItem,
          settings = state.settings,
          onDismiss = { actionTarget = null },
          onLaunch = { viewModel.launchApp(app) },
          onAddToHome = { viewModel.addAppToHome(app) },
          onRemoveFromHome = homeItem?.let { item -> { viewModel.removeHomeItem(item) } },
          onMoveHomeItem = homeItem?.let { item -> { viewModel.moveHomeItemToNextSlot(item) } },
          onToggleDock = { viewModel.toggleDockApp(app.componentKey) },
          onToggleHidden = {
            val isHidden = app.packageName in state.settings.hiddenPackages
            viewModel.setAppHidden(app.packageName, !isHidden)
          },
          onSetCustomLabel = { lbl -> viewModel.setCustomAppLabel(app.componentKey, lbl) },
          onSetCustomIconStyle = { st -> viewModel.setCustomAppIconStyle(app.componentKey, st) },
          onOpenAppInfo = { viewModel.openAppInfo(app.packageName) },
        )
      }

      // Cloner Vault App Unlock Prompt
      state.pendingUnlockApp?.let { lockedApp ->
        AppUnlockDialog(
          app = lockedApp,
          vaultPinHash = state.settings.vaultPinHash,
          onDismiss = viewModel::cancelUnlockPrompt,
          onUnlocked = viewModel::completeUnlockedLaunch,
        )
      }

      // Isolated Web App Sandbox Dialog
      state.activeWebApp?.let { webApp ->
        IsolatedWebAppDialog(
          webApp = webApp,
          onDismiss = { viewModel.openIsolatedWebApp(null) },
        )
      }
    }
  }
}
