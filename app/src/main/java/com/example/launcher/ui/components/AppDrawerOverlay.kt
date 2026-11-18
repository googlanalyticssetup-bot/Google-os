package com.example.launcher.ui.components

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.launcher.model.*
import com.example.launcher.ui.LauncherUiState
import kotlin.math.ceil

@Composable
fun AppDrawerOverlay(
  state: LauncherUiState,
  onClose: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSubmitSearch: (String) -> Unit,
  onLaunchApp: (InstalledAppInfo) -> Unit,
  onLongPressApp: (InstalledAppInfo) -> Unit,
  onOpenSettingsSection: (SettingsSection?) -> Unit,
  onOpenThemeGenerator: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onClose() }

  val context = LocalContext.current
  val settings = state.settings
  val theme = state.activeTheme
  val primary = Color(theme.palette.primaryHex)
  val onSurface = Color(theme.palette.onSurfaceHex)

  var selectedCategory by remember { mutableStateOf(AppCategory.ALL) }
  var selectedLetter by remember { mutableStateOf<Char?>(null) }

  val voiceLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
      if (res.resultCode == Activity.RESULT_OK) {
        val spoken =
          res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
          onSearchQueryChange(spoken)
          onSubmitSearch(spoken)
        }
      }
    }

  val visibleApps =
    remember(state.installedApps, settings.hiddenPackages, settings.drawerSortMode, settings.customSortOrder) {
      val base = state.installedApps.filter { it.packageName !in settings.hiddenPackages }
      when (settings.drawerSortMode) {
        DrawerSortMode.ALPHABETICAL -> base.sortedBy { it.label.lowercase() }
        DrawerSortMode.FREQUENT -> base.sortedByDescending { it.launchCount }
        DrawerSortMode.RECENT -> base.sortedByDescending { it.lastLaunchedAt }
        DrawerSortMode.CUSTOM -> {
          val orderMap = settings.customSortOrder.withIndex().associate { it.value to it.index }
          base.sortedBy { orderMap[it.packageName] ?: Int.MAX_VALUE }
        }
      }
    }

  val filteredApps =
    remember(visibleApps, state.searchQuery, selectedCategory, selectedLetter) {
      visibleApps.filter { app ->
        val matchesQuery =
          state.searchQuery.isBlank() ||
            app.label.contains(state.searchQuery, ignoreCase = true) ||
            app.packageName.contains(state.searchQuery, ignoreCase = true)
        val matchesCat = selectedCategory == AppCategory.ALL || app.category == selectedCategory
        val matchesLetter =
          selectedLetter == null ||
            app.label.firstOrNull()?.uppercaseChar() == selectedLetter
        matchesQuery && matchesCat && matchesLetter
      }
    }

  val recentFrequentApps =
    remember(visibleApps) {
      visibleApps.filter { it.launchCount > 0 || it.lastLaunchedAt > 0L }
        .sortedByDescending { it.lastLaunchedAt + it.launchCount * 1000L }
        .take(4)
    }

  val matchingSettings =
    remember(state.searchQuery) {
      if (state.searchQuery.isBlank()) emptyList()
      else SettingsSection.entries.filter {
        it.title.contains(state.searchQuery, ignoreCase = true) ||
          it.subtitle.contains(state.searchQuery, ignoreCase = true)
      }.take(4)
    }

  val effectiveAlpha =
    if (state.renderRules.tier == com.example.domain.model.ResolvedTier.LITE) {
      0.94f
    } else {
      settings.drawerBackgroundAlpha.coerceIn(0.35f, 0.96f)
    }

  val bgBrush =
    if (settings.drawerGlassEffect) {
      Brush.verticalGradient(
        listOf(
          Color(theme.palette.surfaceHex).copy(alpha = effectiveAlpha),
          Color(theme.palette.backgroundHex).copy(alpha = (effectiveAlpha + 0.1f).coerceAtMost(0.98f)),
        ),
      )
    } else {
      Brush.verticalGradient(
        listOf(
          Color(theme.palette.backgroundHex).copy(alpha = effectiveAlpha),
          Color(theme.palette.backgroundHex).copy(alpha = effectiveAlpha),
        ),
      )
    }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(bgBrush)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .testTag("app_drawer_overlay"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Top Bar: Search + ThemeGenerator + Settings + Close
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = state.searchQuery,
          onValueChange = { q ->
            onSearchQueryChange(q)
            if (settings.instantSearch && q.length >= 2) onSubmitSearch(q)
          },
          placeholder = { Text("Search apps, settings, contacts…") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = primary) },
          trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (state.searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                  Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                }
              } else if (settings.voiceSearchEnabled && state.isSpeechAvailable) {
                IconButton(
                  onClick = {
                    val intent =
                      Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                          RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                          RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                        )
                      }
                    voiceLauncher.launch(intent)
                  },
                ) {
                  Icon(Icons.Rounded.Mic, contentDescription = "Voice search")
                }
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier.weight(1f).testTag("drawer_search_input"),
        )

        FilledTonalIconButton(
          onClick = onOpenThemeGenerator,
          modifier = Modifier.size(48.dp).testTag("drawer_themegen_btn"),
        ) {
          Icon(Icons.Rounded.AutoAwesome, contentDescription = "Open ThemeGenerator")
        }

        FilledTonalIconButton(
          onClick = { onOpenSettingsSection(null) },
          modifier = Modifier.size(48.dp).testTag("drawer_settings_btn"),
        ) {
          Icon(Icons.Rounded.Settings, contentDescription = "Open Launcher Settings")
        }

        IconButton(
          onClick = onClose,
          modifier = Modifier.size(48.dp).testTag("drawer_close_btn"),
        ) {
          Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close App Drawer", tint = onSurface)
        }
      }

      // Recent search history pills when search is empty or active
      if (settings.showSearchHistory && state.searchHistory.isNotEmpty() && state.searchQuery.isBlank()) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          state.searchHistory.take(5).forEach { hist ->
            AssistChip(
              onClick = { onSearchQueryChange(hist) },
              leadingIcon = { Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(14.dp)) },
              label = { Text(hist, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
      }

      // Category chips
      if (settings.drawerShowCategories && state.searchQuery.isBlank()) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          AppCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat; selectedLetter = null },
              label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
      }

      // Instant Settings & Contacts search results
      if (matchingSettings.isNotEmpty() || state.contactMatches.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          matchingSettings.forEach { sec ->
            Surface(
              onClick = { onOpenSettingsSection(sec) },
              shape = RoundedCornerShape(14.dp),
              color = primary.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, primary.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("${sec.title} Setting", style = MaterialTheme.typography.labelLarge, color = onSurface)
              }
            }
          }
          state.contactMatches.forEach { contact ->
            Surface(
              onClick = {
                contact.lookupUri?.let { uri ->
                  context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
              },
              shape = RoundedCornerShape(14.dp),
              color = Color.White.copy(alpha = 0.08f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(contact.displayName, style = MaterialTheme.typography.bodyMedium, color = onSurface)
              }
            }
          }
        }
      }

      // A-Z quick index filter bar
      if (state.searchQuery.isBlank() && settings.drawerSortMode == DrawerSortMode.ALPHABETICAL) {
        val letters = remember(filteredApps, visibleApps) {
          visibleApps.mapNotNull { it.label.firstOrNull()?.uppercaseChar() }.distinct().sorted()
        }
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          FilterChip(
            selected = selectedLetter == null,
            onClick = { selectedLetter = null },
            label = { Text("A–Z", style = MaterialTheme.typography.labelSmall) },
          )
          letters.forEach { ch ->
            FilterChip(
              selected = selectedLetter == ch,
              onClick = { selectedLetter = if (selectedLetter == ch) null else ch },
              label = { Text(ch.toString(), style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
      }

      val isLargeScreen =
        state.deviceCapabilities.formFactor != com.example.domain.model.DeviceFormFactor.PHONE
      val cols =
        if (isLargeScreen && settings.autoAdaptLargeScreen) {
          (settings.drawerColumns + settings.tabletDrawerColumnBoost).coerceIn(3, 8)
        } else {
          settings.drawerColumns.coerceIn(3, 7)
        }

      if (settings.drawerScrollMode == DrawerScrollMode.HORIZONTAL && state.searchQuery.isBlank()) {
        val perPage = cols * 4
        val pageCount = ceil(filteredApps.size / perPage.toFloat()).toInt().coerceAtLeast(1)
        val pagerState = rememberPagerState(pageCount = { pageCount })
        Column(modifier = Modifier.weight(1f)) {
          HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIdx ->
            val slice = filteredApps.drop(pageIdx * perPage).take(perPage)
            LazyVerticalGrid(
              columns = GridCells.Fixed(cols),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxSize(),
            ) {
              items(slice, key = { it.componentKey }) { app ->
                LauncherAppIconTile(
                  app = app,
                  settings = settings,
                  theme = theme,
                  rules = state.renderRules,
                  showLabel = settings.showDrawerLabels,
                  onClick = { onLaunchApp(app) },
                  onLongClick = { onLongPressApp(app) },
                )
              }
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
          ) {
            repeat(pageCount) { idx ->
              Text(
                text = if (idx == pagerState.currentPage) "● " else "○ ",
                color = if (idx == pagerState.currentPage) primary else onSurface.copy(alpha = 0.4f),
              )
            }
          }
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Fixed(cols),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          if (settings.drawerShowRecentFrequent && recentFrequentApps.isNotEmpty() && state.searchQuery.isBlank()) {
            item(span = { GridItemSpan(cols) }) {
              Text(
                text = "Recently & Frequently Used",
                style = MaterialTheme.typography.labelLarge,
                color = primary,
                fontWeight = FontWeight.SemiBold,
              )
            }
            items(recentFrequentApps, key = { "recent_${it.componentKey}" }) { app ->
              LauncherAppIconTile(
                app = app,
                settings = settings,
                theme = theme,
                rules = state.renderRules,
                showLabel = settings.showDrawerLabels,
                onClick = { onLaunchApp(app) },
                onLongClick = { onLongPressApp(app) },
              )
            }
            item(span = { GridItemSpan(cols) }) {
              HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            }
          }

          items(filteredApps, key = { it.componentKey }) { app ->
            LauncherAppIconTile(
              app = app,
              settings = settings,
              theme = theme,
              rules = state.renderRules,
              showLabel = settings.showDrawerLabels,
              onClick = { onLaunchApp(app) },
              onLongClick = { onLongPressApp(app) },
            )
          }
        }
      }
    }
  }
}
