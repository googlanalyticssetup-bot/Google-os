package com.example.launcher.ui.screens

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DeviceFormFactor
import com.example.domain.model.ResolvedTier
import com.example.launcher.model.*
import com.example.launcher.system.LauncherSystemBridge
import com.example.launcher.system.LauncherWidgetHostHolder
import com.example.launcher.ui.LauncherUiState
import com.example.launcher.ui.components.*
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun HomeScreenView(
  state: LauncherUiState,
  widgetHostHolder: LauncherWidgetHostHolder,
  onOpenDrawer: () -> Unit,
  onSetEditMode: (Boolean) -> Unit,
  onPageChanged: (Int) -> Unit,
  onTriggerGesture: (GestureAction) -> Unit,
  onLaunchApp: (InstalledAppInfo) -> Unit,
  onOpenFolder: (String) -> Unit,
  onLongPressApp: (InstalledAppInfo, HomeItem?) -> Unit,
  onAddCustomWidget: (CustomWidgetKind) -> Unit,
  onAddSystemWidget: (Int) -> Unit,
  onUpdateHomeItem: (HomeItem) -> Unit,
  onRemoveHomeItem: (HomeItem) -> Unit,
  onCreateFolderFromTwo: (HomeItem, HomeItem) -> Unit,
  onAddPage: () -> Unit,
  onRemoveLastPage: () -> Unit,
  onOpenThemeGenerator: () -> Unit,
  onOpenSettings: (SettingsSection?) -> Unit,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val settings = state.settings
  val theme = state.activeTheme
  val primary = Color(theme.palette.primaryHex)
  val onSurface = Color(theme.palette.onSurfaceHex)

  var showWidgetPicker by remember { mutableStateOf(false) }
  var showFolderPairHint by remember { mutableStateOf<HomeItem?>(null) }
  var pendingBindWidgetId by remember { mutableIntStateOf(-1) }

  val bindWidgetLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
      if (res.resultCode == Activity.RESULT_OK && pendingBindWidgetId != -1) {
        onAddSystemWidget(pendingBindWidgetId)
      } else if (pendingBindWidgetId != -1) {
        runCatching { widgetHostHolder.appWidgetHost.deleteAppWidgetId(pendingBindWidgetId) }
      }
      pendingBindWidgetId = -1
    }

  if (state.isEditMode) {
    BackHandler {
      showFolderPairHint = null
      onSetEditMode(false)
    }
  }

  val pagerState = rememberPagerState(initialPage = state.currentPage.coerceIn(0, (settings.pageCount - 1).coerceAtLeast(0)), pageCount = { settings.pageCount.coerceAtLeast(1) })

  LaunchedEffect(pagerState.currentPage) {
    onPageChanged(pagerState.currentPage)
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isLargeScreen = maxWidth >= 600.dp || state.deviceCapabilities.formFactor != DeviceFormFactor.PHONE
    val effectiveCols =
      if (isLargeScreen && settings.autoAdaptLargeScreen) {
        (settings.gridColumns + settings.tabletColumnBoost).coerceAtMost(8)
      } else {
        settings.gridColumns
      }

    // Wallpaper Backdrop with optional parallax
    val wpBmp = state.wallpaperBitmap
    if (wpBmp != null && !wpBmp.isRecycled) {
      val parallaxPx =
        if (settings.wallpaperParallax && state.renderRules.tier != ResolvedTier.LITE) {
          -(pagerState.currentPage + pagerState.currentPageOffsetFraction) * 24f
        } else 0f
      Image(
        bitmap = wpBmp.asImageBitmap(),
        contentDescription = "Launcher Wallpaper",
        contentScale = ContentScale.Crop,
        modifier =
          Modifier
            .fillMaxSize()
            .graphicsLayer {
              scaleX = if (settings.wallpaperParallax) 1.08f else 1f
              scaleY = if (settings.wallpaperParallax) 1.08f else 1f
              translationX = parallaxPx
            },
      )
    } else {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(Color(theme.palette.backgroundHex), Color(theme.palette.surfaceHex)),
              ),
            ),
      )
    }

    // Gesture & Content Host
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .pointerInput(settings.swipeUpAction, settings.swipeDownAction, settings.doubleTapAction) {
            detectTapGestures(
              onDoubleTap = { onTriggerGesture(settings.doubleTapAction) },
              onLongPress = { onTriggerGesture(settings.longPressAction) },
            )
          }
          .pointerInput(settings.swipeUpAction, settings.swipeDownAction) {
            var accumulatedDrag = 0f
            detectVerticalDragGestures(
              onDragStart = { accumulatedDrag = 0f },
              onVerticalDrag = { _, dragAmount -> accumulatedDrag += dragAmount },
              onDragEnd = {
                if (accumulatedDrag < -80f) {
                  onTriggerGesture(settings.swipeUpAction)
                } else if (accumulatedDrag > 90f) {
                  onTriggerGesture(settings.swipeDownAction)
                }
              },
            )
          },
    ) {
      // Optional top Home Search Bar
      if (settings.searchBarStyle != SearchBarStyle.HIDDEN && !state.isEditMode) {
        HomeTopSearchBar(
          style = settings.searchBarStyle,
          theme = theme,
          isDefaultHome = state.isDefaultHome,
          onOpenSearch = { onTriggerGesture(GestureAction.OPEN_SEARCH) },
          onOpenThemeGen = onOpenThemeGenerator,
          onOpenSettings = { onOpenSettings(null) },
        )
      }

      // Edit Mode top header
      if (state.isEditMode) {
        Surface(
          color = Color.Black.copy(alpha = 0.62f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, primary.copy(alpha = 0.55f)),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                if (showFolderPairHint != null) "Tap another app to group into Folder"
                else "Edit Mode • Page ${pagerState.currentPage + 1}/${settings.pageCount}",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              IconButton(onClick = onAddPage) {
                Icon(Icons.Rounded.Add, contentDescription = "Add home page", tint = Color.White)
              }
              if (settings.pageCount > 1) {
                IconButton(onClick = onRemoveLastPage) {
                  Icon(Icons.Rounded.Remove, contentDescription = "Remove last home page", tint = Color.White)
                }
              }
              TextButton(
                onClick = {
                  showFolderPairHint = null
                  onSetEditMode(false)
                },
                modifier = Modifier.testTag("edit_done_btn"),
              ) {
                Text("Done", color = primary, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Paged Home Grid
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.weight(1f).fillMaxWidth(),
      ) { pageIdx ->
        val pageOffset = (pagerState.currentPage - pageIdx) + pagerState.currentPageOffsetFraction
        val pageItems =
          remember(state.homeItems, pageIdx) {
            state.homeItems.filter { it.page == pageIdx }
          }
        val widgets = pageItems.filter { it.type == HomeItemType.CUSTOM_WIDGET || it.type == HomeItemType.SYSTEM_WIDGET }
        val iconsAndFolders = pageItems.filter { it.type == HomeItemType.APP || it.type == HomeItemType.FOLDER }

        Column(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(horizontal = 14.dp, vertical = 8.dp)
              .graphicsLayer {
                if (state.renderRules.animationScale > 0f) {
                  when (settings.pageTransition) {
                    PageTransitionEffect.DEPTH_CARD -> {
                      val scale = 1f - (abs(pageOffset) * 0.12f).coerceAtMost(0.18f)
                      scaleX = scale
                      scaleY = scale
                      alpha = 1f - abs(pageOffset).coerceIn(0f, 0.45f)
                    }
                    PageTransitionEffect.CUBE_TILT -> {
                      rotationY = pageOffset * -18f
                    }
                    PageTransitionEffect.PARALLAX_FADE -> {
                      alpha = 1f - abs(pageOffset).coerceIn(0f, 0.5f)
                    }
                    else -> {}
                  }
                }
              },
          verticalArrangement = Arrangement.spacedBy(settings.iconSpacingDp.dp),
        ) {
          // Placed Widgets on this page
          widgets.forEach { widgetItem ->
            val widgetHeight = if (widgetItem.spanY >= 2) 136.dp else 86.dp
            LauncherHomeWidgetCard(
              item = widgetItem,
              theme = theme,
              settings = settings,
              capabilities = state.deviceCapabilities,
              rules = state.renderRules,
              isEditMode = state.isEditMode,
              widgetHostHolder = widgetHostHolder,
              onOpenSearch = { onTriggerGesture(GestureAction.OPEN_SEARCH) },
              onOpenThemeGenerator = onOpenThemeGenerator,
              onUpdateItem = onUpdateHomeItem,
              onRemoveItem = { onRemoveHomeItem(widgetItem) },
              modifier =
                Modifier
                  .fillMaxWidth(
                    fraction = (widgetItem.spanX.toFloat() / effectiveCols.toFloat()).coerceIn(0.45f, 1f),
                  )
                  .height(widgetHeight),
            )
          }

          // Placed Apps & Folders Grid
          val rows = iconsAndFolders.chunked(effectiveCols)
          rows.forEach { rowItems ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(settings.iconSpacingDp.dp),
            ) {
              for (colIdx in 0 until effectiveCols) {
                val item = rowItems.getOrNull(colIdx)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                  if (item != null) {
                    when (item.type) {
                      HomeItemType.APP -> {
                        val app =
                          state.installedApps.firstOrNull {
                            it.packageName == item.packageName && it.activityName == item.activityName
                          } ?: state.installedApps.firstOrNull { it.packageName == item.packageName }
                        if (app != null) {
                          Box {
                            LauncherAppIconTile(
                              app = app,
                              settings = settings,
                              theme = theme,
                              rules = state.renderRules,
                              overrideStyle = item.customIconStyle,
                              onClick = {
                                val first = showFolderPairHint
                                if (state.isEditMode && first != null && first.id != item.id) {
                                  onCreateFolderFromTwo(first, item)
                                  showFolderPairHint = null
                                } else if (state.isEditMode) {
                                  showFolderPairHint = item
                                } else {
                                  onLaunchApp(app)
                                }
                              },
                              onLongClick = { onLongPressApp(app, item) },
                            )
                            if (state.isEditMode) {
                              IconButton(
                                onClick = { onRemoveHomeItem(item) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp),
                              ) {
                                Icon(
                                  Icons.Rounded.Cancel,
                                  contentDescription = "Remove from home",
                                  tint = Color.White,
                                  modifier = Modifier.size(16.dp),
                                )
                              }
                            }
                          }
                        }
                      }
                      HomeItemType.FOLDER -> {
                        LauncherFolderTile(
                          folder = item,
                          allApps = state.installedApps,
                          settings = settings,
                          theme = theme,
                          rules = state.renderRules,
                          onClick = {
                            val first = showFolderPairHint
                            if (state.isEditMode && first != null && first.id != item.id) {
                              onCreateFolderFromTwo(item, first)
                              showFolderPairHint = null
                            } else {
                              onOpenFolder(item.id)
                            }
                          },
                          onLongClick = { onOpenFolder(item.id) },
                        )
                      }
                      else -> {}
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.weight(1f))
        }
      }

      // Page Indicator Dots
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        repeat(settings.pageCount) { idx ->
          val active = idx == pagerState.currentPage
          Box(
            modifier =
              Modifier
                .padding(horizontal = 4.dp)
                .size(if (active) 8.dp else 6.dp)
                .clip(CircleShape)
                .background(if (active) primary else onSurface.copy(alpha = 0.35f))
                .clickable { scope.launch { pagerState.animateScrollToPage(idx) } },
          )
        }
      }

      // Edit Mode Action Dock vs Normal Home Dock
      if (state.isEditMode) {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = Color.Black.copy(alpha = 0.68f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            EditToolChip("Widgets", Icons.Rounded.Widgets) { showWidgetPicker = true }
            EditToolChip("ThemeGen", Icons.Rounded.AutoAwesome, onOpenThemeGenerator)
            EditToolChip("Wallpaper", Icons.Rounded.Wallpaper) { onOpenSettings(SettingsSection.WALLPAPERS) }
            EditToolChip("Icons", Icons.Rounded.Apps) { onOpenSettings(SettingsSection.ICONS) }
            EditToolChip("Grid & Dock", Icons.Rounded.GridView) { onOpenSettings(SettingsSection.HOME_SCREEN) }
            EditToolChip("Transitions", Icons.Rounded.Animation) { onOpenSettings(SettingsSection.ANIMATIONS) }
            EditToolChip("Settings", Icons.Rounded.Settings) { onOpenSettings(null) }
          }
        }
      } else if (settings.dockStyle != DockStyle.HIDDEN) {
        HomeDockBar(
          state = state,
          onLaunchApp = onLaunchApp,
          onLongPressApp = { app -> onLongPressApp(app, null) },
          onOpenDrawer = onOpenDrawer,
          onOpenThemeGenerator = onOpenThemeGenerator,
        )
      }
    }
  }

  // Widget Picker Modal (Built-in Glass Widgets + Real Android App Widgets)
  if (showWidgetPicker) {
    val systemProviders = remember { LauncherSystemBridge.getInstalledAppWidgetProviders(context) }
    AlertDialog(
      onDismissRequest = { showWidgetPicker = false },
      title = { Text("Add Widget to Page ${pagerState.currentPage + 1}") },
      text = {
        LazyColumn(
          modifier = Modifier.heightIn(max = 360.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          item {
            Text("Built-in Glass & Transparent Widgets", style = MaterialTheme.typography.labelLarge, color = primary)
          }
          items(CustomWidgetKind.entries) { kind ->
            Surface(
              onClick = {
                onAddCustomWidget(kind)
                showWidgetPicker = false
              },
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(kind.label, style = MaterialTheme.typography.bodyMedium)
                Text("${kind.defaultSpanX}×${kind.defaultSpanY}", style = MaterialTheme.typography.labelSmall)
              }
            }
          }

          if (systemProviders.isNotEmpty()) {
            item {
              Spacer(Modifier.height(6.dp))
              Text("Installed Android App Widgets", style = MaterialTheme.typography.labelLarge, color = primary)
            }
            items(systemProviders.take(20)) { info: AppWidgetProviderInfo ->
              val label = info.loadLabel(context.packageManager)
              Surface(
                onClick = {
                  val appWidgetId = widgetHostHolder.appWidgetHost.allocateAppWidgetId()
                  val allowed = widgetHostHolder.appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
                  if (allowed) {
                    onAddSystemWidget(appWidgetId)
                    showWidgetPicker = false
                  } else {
                    pendingBindWidgetId = appWidgetId
                    showWidgetPicker = false
                    val intent =
                      Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                      }
                    runCatching { bindWidgetLauncher.launch(intent) }
                      .onFailure {
                        onShowMessage("System widget binding cancelled or unsupported on this OEM")
                      }
                  }
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                  Text(info.provider.packageName.substringAfterLast('.'), style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showWidgetPicker = false }) { Text("Close") }
      },
    )
  }
}

@Composable
private fun HomeTopSearchBar(
  style: SearchBarStyle,
  theme: com.example.domain.model.ThemeSpec,
  isDefaultHome: Boolean,
  onOpenSearch: () -> Unit,
  onOpenThemeGen: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val primary = Color(theme.palette.primaryHex)
  val onSurface = Color(theme.palette.onSurfaceHex)
  val bgColor =
    when (style) {
      SearchBarStyle.GLASS_PILL -> Color(theme.palette.surfaceHex).copy(alpha = 0.42f)
      SearchBarStyle.TRANSPARENT_LINE -> Color.Transparent
      SearchBarStyle.MINIMAL_SOLID -> Color(theme.palette.surfaceHex).copy(alpha = 0.88f)
      SearchBarStyle.HIDDEN -> Color.Transparent
    }

  Surface(
    shape = RoundedCornerShape(50),
    color = bgColor,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 40.dp).clickable { onOpenSearch() },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Rounded.Search, contentDescription = "Search", tint = primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          text = if (isDefaultHome) "Search apps & launcher…" else "Search • Tap Settings to set Default Home",
          fontSize = 12.sp,
          color = onSurface.copy(alpha = 0.8f),
          maxLines = 1,
        )
      }
      IconButton(onClick = onOpenThemeGen, modifier = Modifier.size(38.dp).testTag("home_themegen_btn")) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = "Open ThemeGenerator", tint = primary, modifier = Modifier.size(19.dp))
      }
      IconButton(onClick = onOpenSettings, modifier = Modifier.size(38.dp).testTag("home_settings_btn")) {
        Icon(Icons.Rounded.Settings, contentDescription = "Launcher Settings", tint = onSurface, modifier = Modifier.size(19.dp))
      }
    }
  }
}

@Composable
private fun HomeDockBar(
  state: LauncherUiState,
  onLaunchApp: (InstalledAppInfo) -> Unit,
  onLongPressApp: (InstalledAppInfo) -> Unit,
  onOpenDrawer: () -> Unit,
  onOpenThemeGenerator: () -> Unit,
) {
  val settings = state.settings
  val theme = state.activeTheme
  val primary = Color(theme.palette.primaryHex)
  val dockApps =
    remember(settings.dockPackages, state.installedApps) {
      settings.dockPackages.mapNotNull { key ->
        state.installedApps.firstOrNull { it.componentKey == key }
      }.take(settings.dockMaxIcons)
    }

  val dockColor =
    when (settings.dockStyle) {
      DockStyle.GLASS -> Color(theme.palette.surfaceHex).copy(alpha = 0.44f)
      DockStyle.TRANSPARENT -> Color.White.copy(alpha = 0.08f)
      DockStyle.SOLID -> Color(theme.palette.surfaceHex).copy(alpha = 0.92f)
      DockStyle.HIDDEN -> Color.Transparent
    }

  Surface(
    shape = RoundedCornerShape(28.dp),
    color = dockColor,
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp).testTag("home_dock_bar"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      dockApps.forEach { app ->
        LauncherAppIconTile(
          app = app,
          settings = settings,
          theme = theme,
          rules = state.renderRules,
          showLabel = settings.showDockLabels,
          iconSizeDp = (settings.iconSizeDp - 4).coerceAtLeast(42),
          onClick = { onLaunchApp(app) },
          onLongClick = { onLongPressApp(app) },
          modifier = Modifier.weight(1f),
        )
      }

      // Dedicated App Drawer handle button
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
          Modifier
            .weight(1f)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable { onOpenDrawer() }
            .testTag("dock_drawer_btn"),
      ) {
        Surface(
          shape = CircleShape,
          color = primary.copy(alpha = 0.25f),
          border = BorderStroke(1.dp, primary.copy(alpha = 0.6f)),
          modifier = Modifier.size((settings.iconSizeDp - 4).coerceAtLeast(42).dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Apps, contentDescription = "Open App Drawer", tint = Color.White)
          }
        }
        if (settings.showDockLabels) {
          Spacer(Modifier.height(4.dp))
          Text("Apps", fontSize = 10.sp, color = Color(theme.palette.onSurfaceHex))
        }
      }

      // Quick ThemeGenerator button on dock if dock has space
      if (dockApps.size < 4) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier =
            Modifier
              .weight(1f)
              .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
              .clickable { onOpenThemeGenerator() }
              .testTag("dock_themegen_btn"),
        ) {
          Surface(
            shape = CircleShape,
            color = Color(theme.palette.secondaryHex).copy(alpha = 0.24f),
            modifier = Modifier.size((settings.iconSizeDp - 4).coerceAtLeast(42).dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Rounded.AutoAwesome, contentDescription = "ThemeGenerator", tint = Color.White)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EditToolChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
) {
  FilledTonalButton(
    onClick = onClick,
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    modifier = Modifier.heightIn(min = 44.dp),
  ) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(6.dp))
    Text(label, style = MaterialTheme.typography.labelSmall)
  }
}
