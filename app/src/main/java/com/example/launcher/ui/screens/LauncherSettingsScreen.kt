package com.example.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.model.ThemeSpec
import com.example.engine.WallpaperTarget
import com.example.launcher.model.CustomWidgetKind
import com.example.launcher.model.LauncherSettings
import com.example.launcher.model.SettingsGroup
import com.example.launcher.model.SettingsSection
import com.example.launcher.ui.LauncherUiState
import kotlinx.coroutines.launch

private const val ROUTE_SETTINGS_DASHBOARD = "settings_dashboard"
private const val ROUTE_SETTINGS_DETAIL = "settings_detail/{sectionName}"

private fun settingsDetailRoute(section: SettingsSection): String = "settings_detail/${section.name}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherSettingsScreen(
  state: LauncherUiState,
  onSelectSection: (SettingsSection?) -> Unit,
  onUpdateSettings: ((LauncherSettings) -> LauncherSettings) -> Unit,
  onActivateTheme: (ThemeSpec) -> Unit,
  onOpenThemeGenerator: () -> Unit,
  onOpenLockDesigner: () -> Unit,
  onOpenClonerVault: () -> Unit,
  onOpenControlCenter: () -> Unit,
  onReplayOnboarding: () -> Unit,
  onPickWallpaperPhoto: (Uri) -> Unit,
  onApplyWallpaperToSystem: (WallpaperTarget) -> Unit,
  onExtractThemeFromWallpaper: () -> Unit,
  onAddCustomWidget: (CustomWidgetKind) -> Unit,
  onToggleHiddenApp: (String, Boolean) -> Unit,
  onClearSearchHistory: () -> Unit,
  onExportBackupSuspend: suspend () -> String,
  onImportBackup: (String) -> Unit,
  onTrimCaches: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val exportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) {
        scope.launch {
          val json = onExportBackupSuspend()
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
          }
        }
      }
    }

  val importLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        try {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        } catch (_: SecurityException) {
        }
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (!text.isNullOrBlank()) onImportBackup(text)
      }
    }

  var searchQuery by remember { mutableStateOf("") }
  val expandedGroups = remember {
    mutableStateMapOf<SettingsGroup, Boolean>().apply {
      SettingsGroup.entries.forEach { put(it, true) }
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWide = maxWidth >= 680.dp

    if (isWide) {
      BackHandler { onBack() }
      val initialSection = state.settingsSection ?: SettingsSection.HOME_SCREEN
      val tabletNavController = rememberNavController()
      val backEntry by tabletNavController.currentBackStackEntryAsState()
      val activeSectionName = backEntry?.arguments?.getString("sectionName") ?: initialSection.name
      val activeSection =
        SettingsSection.entries.firstOrNull { it.name == activeSectionName } ?: initialSection

      LaunchedEffect(state.settingsSection) {
        val target = state.settingsSection ?: return@LaunchedEffect
        if (activeSection != target) {
          tabletNavController.navigate(settingsDetailRoute(target)) {
            launchSingleTop = true
          }
        }
      }

      Scaffold(
        containerColor = Color.Transparent,
        topBar = {
          TopAppBar(
            title = { Text("Launcher Settings • 17 Sections") },
            navigationIcon = {
              IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Home")
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
          )
        },
      ) { innerPadding ->
        Row(
          modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          HierarchicalSettingsDashboardList(
            state = state,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            expandedGroups = expandedGroups,
            selectedSection = activeSection,
            onToggleGroup = { g -> expandedGroups[g] = !(expandedGroups[g] ?: true) },
            onSectionClick = { sec ->
              onSelectSection(sec)
              tabletNavController.navigate(settingsDetailRoute(sec)) {
                popUpTo(tabletNavController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
              }
            },
            modifier = Modifier.weight(0.42f).fillMaxHeight(),
          )

          Box(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
            NavHost(
              navController = tabletNavController,
              startDestination = settingsDetailRoute(initialSection),
            ) {
              composable(
                route = ROUTE_SETTINGS_DETAIL,
                arguments = listOf(navArgument("sectionName") { type = NavType.StringType }),
              ) { entry ->
                val secName = entry.arguments?.getString("sectionName")
                val sec =
                  SettingsSection.entries.firstOrNull { it.name == secName } ?: SettingsSection.HOME_SCREEN
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                  SettingsSectionDetailContent(
                    section = sec,
                    state = state,
                    onUpdateSettings = onUpdateSettings,
                    onActivateTheme = onActivateTheme,
                    onOpenThemeGenerator = onOpenThemeGenerator,
                    onOpenLockDesigner = onOpenLockDesigner,
                    onOpenClonerVault = onOpenClonerVault,
                    onOpenControlCenter = onOpenControlCenter,
                    onReplayOnboarding = onReplayOnboarding,
                    onPickWallpaperPhoto = onPickWallpaperPhoto,
                    onApplyWallpaperToSystem = onApplyWallpaperToSystem,
                    onExtractThemeFromWallpaper = onExtractThemeFromWallpaper,
                    onAddCustomWidget = onAddCustomWidget,
                    onToggleHiddenApp = onToggleHiddenApp,
                    onClearSearchHistory = onClearSearchHistory,
                    onExportBackup = { exportLauncher.launch("aether_glass_backup.json") },
                    onImportBackup = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                    onTrimCaches = onTrimCaches,
                  )
                  Spacer(Modifier.height(10.dp))
                  SectionStepperFooter(
                    current = sec,
                    onNavigateSection = { next ->
                      onSelectSection(next)
                      tabletNavController.navigate(settingsDetailRoute(next)) {
                        popUpTo(tabletNavController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                      }
                    },
                  )
                  Spacer(Modifier.height(20.dp))
                }
              }
            }
          }
        }
      }
    } else {
      val phoneNavController = rememberNavController()
      val backEntry by phoneNavController.currentBackStackEntryAsState()
      val currentRoute = backEntry?.destination?.route
      val isOnDashboard = currentRoute == null || currentRoute == ROUTE_SETTINGS_DASHBOARD

      var lastDeepLinked by remember { mutableStateOf<SettingsSection?>(null) }
      var hasVisitedDetail by remember { mutableStateOf(false) }

      LaunchedEffect(state.settingsSection) {
        val target = state.settingsSection
        if (target != null && target != lastDeepLinked) {
          lastDeepLinked = target
          phoneNavController.navigate(settingsDetailRoute(target)) {
            launchSingleTop = true
          }
        }
      }

      LaunchedEffect(currentRoute) {
        if (currentRoute?.startsWith("settings_detail") == true) {
          hasVisitedDetail = true
        } else if (currentRoute == ROUTE_SETTINGS_DASHBOARD && hasVisitedDetail) {
          hasVisitedDetail = false
          lastDeepLinked = null
          onSelectSection(null)
        }
      }

      BackHandler(enabled = isOnDashboard) { onBack() }

      NavHost(
        navController = phoneNavController,
        startDestination = ROUTE_SETTINGS_DASHBOARD,
        modifier = Modifier.fillMaxSize(),
      ) {
        composable(ROUTE_SETTINGS_DASHBOARD) {
          Scaffold(
            containerColor = Color.Transparent,
            topBar = {
              TopAppBar(
                title = {
                  Column {
                    Text("Launcher Settings")
                    Text(
                      "${SettingsGroup.entries.size} Categories • ${SettingsSection.entries.size} Sections",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                },
                navigationIcon = {
                  IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Home")
                  }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
              )
            },
          ) { innerPadding ->
            HierarchicalSettingsDashboardList(
              state = state,
              searchQuery = searchQuery,
              onSearchQueryChange = { searchQuery = it },
              expandedGroups = expandedGroups,
              selectedSection = null,
              onToggleGroup = { g -> expandedGroups[g] = !(expandedGroups[g] ?: true) },
              onSectionClick = { sec ->
                lastDeepLinked = sec
                onSelectSection(sec)
                phoneNavController.navigate(settingsDetailRoute(sec)) {
                  launchSingleTop = true
                }
              },
              modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            )
          }
        }

        composable(
          route = ROUTE_SETTINGS_DETAIL,
          arguments = listOf(navArgument("sectionName") { type = NavType.StringType }),
        ) { entry ->
          val secName = entry.arguments?.getString("sectionName")
          val sec =
            SettingsSection.entries.firstOrNull { it.name == secName } ?: SettingsSection.HOME_SCREEN

          Scaffold(
            containerColor = Color.Transparent,
            topBar = {
              TopAppBar(
                title = {
                  Column {
                    Text("${sec.number}. ${sec.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                      sec.group.title,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                },
                navigationIcon = {
                  IconButton(
                    onClick = { phoneNavController.popBackStack() },
                    modifier = Modifier.testTag("settings_detail_back_btn"),
                  ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Settings Dashboard")
                  }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
              )
            },
          ) { innerPadding ->
            Column(
              modifier =
                Modifier
                  .fillMaxSize()
                  .padding(innerPadding)
                  .verticalScroll(rememberScrollState())
                  .padding(horizontal = 16.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              SettingsSectionDetailContent(
                section = sec,
                state = state,
                onUpdateSettings = onUpdateSettings,
                onActivateTheme = onActivateTheme,
                onOpenThemeGenerator = onOpenThemeGenerator,
                onOpenLockDesigner = onOpenLockDesigner,
                onOpenClonerVault = onOpenClonerVault,
                onOpenControlCenter = onOpenControlCenter,
                onReplayOnboarding = onReplayOnboarding,
                onPickWallpaperPhoto = onPickWallpaperPhoto,
                onApplyWallpaperToSystem = onApplyWallpaperToSystem,
                onExtractThemeFromWallpaper = onExtractThemeFromWallpaper,
                onAddCustomWidget = onAddCustomWidget,
                onToggleHiddenApp = onToggleHiddenApp,
                onClearSearchHistory = onClearSearchHistory,
                onExportBackup = { exportLauncher.launch("aether_glass_backup.json") },
                onImportBackup = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                onTrimCaches = onTrimCaches,
              )
              SectionStepperFooter(
                current = sec,
                onNavigateSection = { next ->
                  lastDeepLinked = next
                  onSelectSection(next)
                  phoneNavController.navigate(settingsDetailRoute(next)) {
                    popUpTo(ROUTE_SETTINGS_DASHBOARD)
                    launchSingleTop = true
                  }
                },
              )
              Spacer(Modifier.height(20.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HierarchicalSettingsDashboardList(
  state: LauncherUiState,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  expandedGroups: Map<SettingsGroup, Boolean>,
  selectedSection: SettingsSection?,
  onToggleGroup: (SettingsGroup) -> Unit,
  onSectionClick: (SettingsSection) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isFiltering = searchQuery.isNotBlank()
  val groupedSections =
    remember(searchQuery) {
      SettingsGroup.entries.mapNotNull { group ->
        val children =
          SettingsSection.entries.filter { sec ->
            sec.group == group &&
              (!isFiltering ||
                sec.title.contains(searchQuery, ignoreCase = true) ||
                sec.subtitle.contains(searchQuery, ignoreCase = true) ||
                group.title.contains(searchQuery, ignoreCase = true))
          }
        if (children.isEmpty()) null else group to children
      }
    }

  LazyColumn(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(10.dp),
    contentPadding = PaddingValues(bottom = 24.dp),
  ) {
    item {
      SettingsOverviewBanner(state = state)
    }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search all 17 settings sections…") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(Icons.Rounded.Clear, contentDescription = "Clear filter")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("settings_filter_input"),
      )
    }

    if (groupedSections.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Rounded.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("No settings match \"$searchQuery\"", style = MaterialTheme.typography.titleSmall)
          }
        }
      }
    } else {
      groupedSections.forEach { (group, sections) ->
        item(key = "group_${group.name}") {
          val isExpanded = isFiltering || (expandedGroups[group] ?: true)
          SettingsGroupHierarchyCard(
            group = group,
            sections = sections,
            state = state,
            isExpanded = isExpanded,
            selectedSection = selectedSection,
            onToggleExpand = { onToggleGroup(group) },
            onSectionClick = onSectionClick,
          )
        }
      }
    }
  }
}

@Composable
private fun SettingsOverviewBanner(state: LauncherUiState) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "Theme: ${state.activeTheme.name}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text =
            "${state.renderRules.tier.label} Engine • ${state.deviceCapabilities.formFactor.label} • " +
              if (state.isDefaultHome) "Default HOME ✓" else "Preview Mode",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
      ) {
        Text(
          text = "Offline",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }
  }
}

@Composable
private fun SettingsGroupHierarchyCard(
  group: SettingsGroup,
  sections: List<SettingsSection>,
  state: LauncherUiState,
  isExpanded: Boolean,
  selectedSection: SettingsSection?,
  onToggleExpand: () -> Unit,
  onSectionClick: (SettingsSection) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Surface(
        onClick = onToggleExpand,
        color = Color.Transparent,
        modifier =
          Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("settings_group_${group.name.lowercase()}"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier =
              Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = group.icon(),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = group.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = group.subtitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Text(
              text = "${sections.size}",
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
          Icon(
            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (isExpanded) "Collapse ${group.title}" else "Expand ${group.title}",
          )
        }
      }

      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
          sections.forEachIndexed { idx, sec ->
            if (idx > 0) {
              HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                modifier = Modifier.padding(horizontal = 8.dp),
              )
            }
            SettingsSectionTile(
              section = sec,
              summary = sec.liveSummary(state),
              selected = sec == selectedSection,
              onClick = { onSectionClick(sec) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsSectionTile(
  section: SettingsSection,
  summary: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .testTag("settings_tile_${section.name.lowercase()}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        modifier = Modifier.size(34.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = section.icon(),
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${section.number}. ${section.title}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = summary,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
          )
        }
        Text(
          text = section.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = "Open ${section.title}",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SectionStepperFooter(
  current: SettingsSection,
  onNavigateSection: (SettingsSection) -> Unit,
) {
  val all = SettingsSection.entries
  val idx = all.indexOf(current)
  val prev = all.getOrNull(idx - 1)
  val next = all.getOrNull(idx + 1)

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (prev != null) {
      OutlinedButton(
        onClick = { onNavigateSection(prev) },
        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
      ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("${prev.number}. ${prev.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    } else {
      Spacer(Modifier.weight(1f))
    }
    if (next != null) {
      FilledTonalButton(
        onClick = { onNavigateSection(next) },
        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
      ) {
        Text("${next.number}. ${next.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
      }
    } else {
      Spacer(Modifier.weight(1f))
    }
  }
}

private fun SettingsGroup.icon(): ImageVector =
  when (this) {
    SettingsGroup.WORKSPACE -> Icons.Rounded.DashboardCustomize
    SettingsGroup.VISUALS -> Icons.Rounded.Palette
    SettingsGroup.INTERACTION -> Icons.Rounded.TouchApp
    SettingsGroup.SYSTEM -> Icons.Rounded.Tune
  }

private fun SettingsSection.icon(): ImageVector =
  when (this) {
    SettingsSection.HOME_SCREEN -> Icons.Rounded.Home
    SettingsSection.APP_DRAWER -> Icons.Rounded.Apps
    SettingsSection.ICONS -> Icons.Rounded.Interests
    SettingsSection.WIDGETS -> Icons.Rounded.Widgets
    SettingsSection.FOLDERS -> Icons.Rounded.Folder
    SettingsSection.DOCK -> Icons.Rounded.ViewCarousel
    SettingsSection.ANIMATIONS -> Icons.Rounded.Animation
    SettingsSection.WALLPAPERS -> Icons.Rounded.Wallpaper
    SettingsSection.THEMES -> Icons.Rounded.AutoAwesome
    SettingsSection.GESTURES -> Icons.Rounded.Gesture
    SettingsSection.SEARCH -> Icons.Rounded.Search
    SettingsSection.BACKUP_RESTORE -> Icons.Rounded.SaveAlt
    SettingsSection.PERFORMANCE -> Icons.Rounded.Speed
    SettingsSection.PRIVACY -> Icons.Rounded.PrivacyTip
    SettingsSection.TABLET_FOLDABLE -> Icons.Rounded.TabletAndroid
    SettingsSection.DEFAULT_HOME_COMPAT -> Icons.Rounded.Devices
    SettingsSection.ABOUT -> Icons.Rounded.Info
  }

private fun SettingsSection.liveSummary(state: LauncherUiState): String {
  val s = state.settings
  return when (this) {
    SettingsSection.HOME_SCREEN -> "${s.gridColumns}×${s.gridRows} • ${s.iconSizeDp}dp"
    SettingsSection.APP_DRAWER -> "${s.drawerColumns} cols • ${s.drawerScrollMode.label.substringBefore(" ")}"
    SettingsSection.ICONS -> s.iconStyle.label
    SettingsSection.WIDGETS -> if (s.defaultWidgetsGlass) "Glass" else "Solid"
    SettingsSection.FOLDERS -> "${s.defaultFolderStyle.label} ${s.defaultFolderLayout.label}"
    SettingsSection.DOCK -> s.dockStyle.label
    SettingsSection.ANIMATIONS -> if (s.animationsEnabled) s.pageTransition.label else "Off"
    SettingsSection.WALLPAPERS -> if (s.customWallpaperUri != null) "Photo" else state.activeTheme.wallpaper.style.label
    SettingsSection.THEMES -> state.activeTheme.name
    SettingsSection.GESTURES -> "6 triggers"
    SettingsSection.SEARCH -> s.searchBarStyle.label
    SettingsSection.BACKUP_RESTORE -> "Offline JSON"
    SettingsSection.PERFORMANCE -> state.renderRules.tier.label
    SettingsSection.PRIVACY -> "${s.hiddenPackages.size} hidden"
    SettingsSection.TABLET_FOLDABLE -> if (s.autoAdaptLargeScreen) "+${s.tabletColumnBoost} cols" else "Fixed"
    SettingsSection.DEFAULT_HOME_COMPAT -> if (state.isDefaultHome) "Active ✓" else "13 OEMs"
    SettingsSection.ABOUT -> "API ${state.deviceCapabilities.sdkInt}"
  }
}
