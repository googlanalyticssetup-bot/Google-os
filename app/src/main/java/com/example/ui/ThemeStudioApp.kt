package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.ThemeStudioTheme

enum class StudioDestination(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  AI_STUDIO("ai_studio", "AI Studio", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
  LIBRARY("library", "Library", Icons.Rounded.Palette, Icons.Outlined.Palette),
  STUDIOS("studios", "Studios", Icons.Rounded.Tune, Icons.Outlined.Tune),
  WIDGETS("widgets", "Widgets", Icons.Rounded.Widgets, Icons.Outlined.Widgets),
  DEVICE_ENGINE("device_engine", "Engine", Icons.Rounded.Memory, Icons.Outlined.Memory),
}

@Composable
fun ThemeStudioApp(viewModel: ThemeStudioViewModel) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(state.statusMessage) {
    val msg = state.statusMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  val draftPrimary = Color(state.draftTheme.palette.primaryHex)
  val draftSecondary = Color(state.draftTheme.palette.secondaryHex)
  val isLightTheme = !state.draftTheme.palette.isAmoledDark && state.draftTheme.palette.backgroundHex == 0xFFF1F5F9L

  ThemeStudioTheme(
    darkTheme = !isLightTheme,
    accentOverride = draftPrimary,
    secondaryOverride = draftSecondary,
  ) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navigateTo: (StudioDestination) -> Unit = { dest ->
      navController.navigate(dest.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
      }
    }

    BoxWithConstraints(
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
          ),
    ) {
      val useNavRail = maxWidth >= 600.dp

      Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (!useNavRail) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
              windowInsets = WindowInsets.navigationBars,
            ) {
              StudioDestination.entries.forEach { dest ->
                val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateTo(dest) },
                  icon = {
                    Icon(
                      imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                      contentDescription = dest.label,
                    )
                  },
                  label = { Text(dest.label, maxLines = 1) },
                  modifier = Modifier.testTag("nav_${dest.route}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (useNavRail) {
            NavigationRail(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            ) {
              Spacer(Modifier.height(12.dp))
              StudioDestination.entries.forEach { dest ->
                val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                NavigationRailItem(
                  selected = selected,
                  onClick = { navigateTo(dest) },
                  icon = {
                    Icon(
                      imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                      contentDescription = dest.label,
                    )
                  },
                  label = { Text(dest.label) },
                  modifier = Modifier.testTag("rail_${dest.route}"),
                )
              }
            }
          }

          NavHost(
            navController = navController,
            startDestination = StudioDestination.AI_STUDIO.route,
            modifier = Modifier.weight(1f).fillMaxHeight(),
          ) {
            composable(StudioDestination.AI_STUDIO.route) {
              AiStudioScreen(
                state = state,
                onPromptChanged = viewModel::updateAiPrompt,
                onGenerate = viewModel::generateThemeFromPrompt,
                onCancelGenerate = viewModel::cancelGeneration,
                onSaveToLibrary = viewModel::saveDraftToLibrary,
                onActivateTheme = { viewModel.activateTheme(state.draftTheme) },
                onApplyWallpaper = viewModel::applyDraftWallpaper,
                onOptimizeForDevice = viewModel::optimizeDraftForCurrentDevice,
                onOpenStudios = { navigateTo(StudioDestination.STUDIOS) },
              )
            }
            composable(StudioDestination.LIBRARY.route) {
              LibraryScreen(
                state = state,
                onSelectForStudio = { theme ->
                  viewModel.selectThemeIntoStudio(theme)
                  navigateTo(StudioDestination.STUDIOS)
                },
                onActivateTheme = viewModel::activateTheme,
                onDeleteTheme = viewModel::deleteCustomTheme,
                onExportJson = viewModel::exportActiveThemeJson,
                onImportJson = viewModel::importThemeJson,
              )
            }
            composable(StudioDestination.STUDIOS.route) {
              DesignStudiosScreen(
                state = state,
                onUpdateDraft = viewModel::updateDraftTheme,
                onSaveToLibrary = viewModel::saveDraftToLibrary,
                onActivateTheme = { viewModel.activateTheme(state.draftTheme) },
                onApplyWallpaper = viewModel::applyDraftWallpaper,
                onPreviewSound = viewModel::previewSound,
                onToggleMasterAnimations = viewModel::setAnimationsMasterEnabled,
              )
            }
            composable(StudioDestination.WIDGETS.route) {
              WidgetsStudioScreen(
                state = state,
                onUpdateDraft = viewModel::updateDraftTheme,
                onActivateAndSyncWidget = { viewModel.activateTheme(state.draftTheme) },
              )
            }
            composable(StudioDestination.DEVICE_ENGINE.route) {
              DeviceEngineScreen(
                state = state,
                onSelectPerformanceMode = viewModel::setPerformanceMode,
                onToggleMasterAnimations = viewModel::setAnimationsMasterEnabled,
                onRefreshTelemetry = viewModel::refreshDeviceCapabilities,
                onClearBitmapCache = viewModel::onTrimMemory,
              )
            }
          }
        }
      }
    }
  }
}
