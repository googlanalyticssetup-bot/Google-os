package com.example.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.FontStyleOption
import com.example.domain.model.ThemeSpec
import com.example.domain.model.WallpaperStyle
import com.example.engine.WallpaperTarget
import com.example.launcher.model.*
import com.example.launcher.system.LauncherSystemBridge
import com.example.launcher.ui.LauncherUiState
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.PaletteSwatchRow
import java.util.Locale

private val GlassAccentSwatches =
  listOf(
    null to "Theme",
    0xFF38BDF8L to "Cyan",
    0xFF8B5CF6L to "Violet",
    0xFF10B981L to "Mint",
    0xFFF59E0BL to "Amber",
    0xFFF43F5EL to "Rose",
  )

@Composable
fun SettingsSectionDetailContent(
  section: SettingsSection,
  state: LauncherUiState,
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
  onExportBackup: () -> Unit,
  onImportBackup: () -> Unit,
  onTrimCaches: () -> Unit,
) {
  val context = LocalContext.current
  val settings = state.settings
  val theme = state.activeTheme
  val rules = state.renderRules

  val wpPhotoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        try {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        } catch (_: SecurityException) {
        }
        onPickWallpaperPhoto(uri)
      }
    }

  GlassSurfaceCard(theme.glass, theme.palette, rules, Modifier.fillMaxWidth()) {
    Text("${section.number}. ${section.title}", style = MaterialTheme.typography.titleLarge)
    Text(
      section.subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(12.dp))

    when (section) {
      SettingsSection.HOME_SCREEN -> {
        Button(
          onClick = {
            runCatching { context.startActivity(LauncherSystemBridge.buildDefaultHomeIntent(context)) }
          },
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("set_default_home_btn"),
        ) {
          Icon(Icons.Rounded.Home, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(if (state.isDefaultHome) "Active Default Launcher ✓" else "Set as Default HOME Launcher")
        }
        Spacer(Modifier.height(10.dp))
        SettingsSwitchRow("Enable Dynamic Capsule Bar", settings.dynamicCapsuleEnabled) { c ->
          onUpdateSettings { it.copy(dynamicCapsuleEnabled = c) }
        }
        SettingsSwitchRow("Enable Swipe-Left Zero Screen", settings.zeroScreenEnabled) { c ->
          onUpdateSettings { it.copy(zeroScreenEnabled = c) }
        }
        Text("Grid Columns (${settings.gridColumns}) • Tablets adapt dynamically")
        Slider(
          value = settings.gridColumns.toFloat(),
          onValueChange = { v -> onUpdateSettings { it.copy(gridColumns = v.toInt()) } },
          valueRange = 3f..7f,
        )
        Text("Grid Rows (${settings.gridRows})")
        Slider(
          value = settings.gridRows.toFloat(),
          onValueChange = { v -> onUpdateSettings { it.copy(gridRows = v.toInt()) } },
          valueRange = 4f..8f,
        )
        Text("Icon Size (${settings.iconSizeDp} dp)")
        Slider(
          value = settings.iconSizeDp.toFloat(),
          onValueChange = { v -> onUpdateSettings { it.copy(iconSizeDp = v.toInt()) } },
          valueRange = 40f..68f,
        )
        Text("Icon Spacing (${settings.iconSpacingDp} dp)")
        Slider(
          value = settings.iconSpacingDp.toFloat(),
          onValueChange = { v -> onUpdateSettings { it.copy(iconSpacingDp = v.toInt()) } },
          valueRange = 4f..22f,
        )
        SettingsSwitchRow("Show Home Icon Labels", settings.showHomeLabels) { c ->
          onUpdateSettings { it.copy(showHomeLabels = c) }
        }
        SettingsSwitchRow("Wallpaper Parallax", settings.wallpaperParallax) { c ->
          onUpdateSettings { it.copy(wallpaperParallax = c) }
        }
        Text("Launcher Font Family", style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FontStyleOption.entries.forEach { f ->
            FilterChip(
              selected = settings.fontStyle == f,
              onClick = { onUpdateSettings { it.copy(fontStyle = f) } },
              label = { Text(f.label) },
            )
          }
        }
      }

      SettingsSection.APP_DRAWER -> {
        Text("Scroll Direction", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          DrawerScrollMode.entries.forEach { m ->
            FilterChip(
              selected = settings.drawerScrollMode == m,
              onClick = { onUpdateSettings { it.copy(drawerScrollMode = m) } },
              label = { Text(m.label) },
              modifier = Modifier.weight(1f),
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        Text("App Sorting", style = MaterialTheme.typography.labelLarge)
        DrawerSortMode.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { sm ->
              FilterChip(
                selected = settings.drawerSortMode == sm,
                onClick = { onUpdateSettings { it.copy(drawerSortMode = sm) } },
                label = { Text(sm.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
        Text("Drawer Columns (${settings.drawerColumns})")
        Slider(
          value = settings.drawerColumns.toFloat(),
          onValueChange = { v -> onUpdateSettings { it.copy(drawerColumns = v.toInt()) } },
          valueRange = 3f..7f,
        )
        Text("Drawer Background Opacity (${(settings.drawerBackgroundAlpha * 100).toInt()}%)")
        Slider(
          value = settings.drawerBackgroundAlpha,
          onValueChange = { v -> onUpdateSettings { it.copy(drawerBackgroundAlpha = v) } },
          valueRange = 0.35f..0.96f,
        )
        SettingsSwitchRow("Glass Drawer Backdrop", settings.drawerGlassEffect) { c ->
          onUpdateSettings { it.copy(drawerGlassEffect = c) }
        }
        SettingsSwitchRow("Show Recently & Frequently Used", settings.drawerShowRecentFrequent) { c ->
          onUpdateSettings { it.copy(drawerShowRecentFrequent = c) }
        }
        SettingsSwitchRow("Show App Categories", settings.drawerShowCategories) { c ->
          onUpdateSettings { it.copy(drawerShowCategories = c) }
        }
      }

      SettingsSection.ICONS -> {
        Text("11 Launcher Icon Styles", style = MaterialTheme.typography.labelLarge)
        LauncherIconStyle.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { st ->
              FilterChip(
                selected = settings.iconStyle == st,
                onClick = { onUpdateSettings { it.copy(iconStyle = st) } },
                label = { Text(st.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f).testTag("launcher_icon_${st.name.lowercase()}"),
              )
            }
          }
        }
        Text("Shape Corner Radius (${settings.iconCornerPercent}%)")
        Slider(
          value = settings.iconCornerPercent.toFloat(),
          onValueChange = { v -> onUpdateSettings { it.copy(iconCornerPercent = v.toInt()) } },
          valueRange = 10f..50f,
        )
        SettingsSwitchRow("Icon Drop Shadows", settings.iconShadowEnabled) { c ->
          onUpdateSettings { it.copy(iconShadowEnabled = c) }
        }
        SettingsSwitchRow("Show Drawer Icon Labels", settings.showDrawerLabels) { c ->
          onUpdateSettings { it.copy(showDrawerLabels = c) }
        }
        Text(
          "Tip: Long-press any app icon on Home or Drawer to replace its individual icon style or label.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.secondary,
        )
      }

      SettingsSection.WIDGETS -> {
        SettingsSwitchRow("Glass Style for New Widgets", settings.defaultWidgetsGlass) { c ->
          onUpdateSettings { it.copy(defaultWidgetsGlass = c) }
        }
        SettingsSwitchRow("Transparent Widget Mode", settings.defaultWidgetsTransparent) { c ->
          onUpdateSettings { it.copy(defaultWidgetsTransparent = c) }
        }
        OutlinedTextField(
          value = settings.weatherCityNote,
          onValueChange = { txt -> onUpdateSettings { it.copy(weatherCityNote = txt) } },
          label = { Text("Weather Widget City / Climate Note") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text("Quick-Add Built-in Atomic Widget (${CustomWidgetKind.entries.size} Total)", style = MaterialTheme.typography.labelLarge)
        CustomWidgetKind.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { kind ->
              OutlinedButton(
                onClick = { onAddCustomWidget(kind) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
              ) {
                Text("+ ${kind.label}", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }

      SettingsSection.FOLDERS -> {
        Text("Default Folder Style", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FolderStyle.entries.forEach { fs ->
            FilterChip(
              selected = settings.defaultFolderStyle == fs,
              onClick = { onUpdateSettings { it.copy(defaultFolderStyle = fs) } },
              label = { Text(fs.label) },
              modifier = Modifier.weight(1f),
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        Text("Default Folder Layout", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FolderLayout.entries.forEach { fl ->
            FilterChip(
              selected = settings.defaultFolderLayout == fl,
              onClick = { onUpdateSettings { it.copy(defaultFolderLayout = fl) } },
              label = { Text(fl.label) },
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      SettingsSection.DOCK -> {
        Text("Dock Style", style = MaterialTheme.typography.labelLarge)
        DockStyle.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { ds ->
              FilterChip(
                selected = settings.dockStyle == ds,
                onClick = { onUpdateSettings { it.copy(dockStyle = ds) } },
                label = { Text(ds.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
        Text("Dock Icon Slots (${settings.dockMaxIcons})")
        Slider(
          value = settings.dockMaxIcons.toFloat(),
          onValueChange = { v -> onUpdateSettings { it.copy(dockMaxIcons = v.toInt()) } },
          valueRange = 3f..7f,
        )
        SettingsSwitchRow("Show Dock App Labels", settings.showDockLabels) { c ->
          onUpdateSettings { it.copy(showDockLabels = c) }
        }
      }

      SettingsSection.ANIMATIONS -> {
        SettingsSwitchRow("Enable Launcher Animations", settings.animationsEnabled) { c ->
          onUpdateSettings { it.copy(animationsEnabled = c) }
        }
        Text("Page Transition Effect", style = MaterialTheme.typography.labelLarge)
        PageTransitionEffect.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { pt ->
              FilterChip(
                selected = settings.pageTransition == pt,
                onClick = { onUpdateSettings { it.copy(pageTransition = pt) } },
                label = { Text(pt.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
        Text("Animation Speed (${String.format(Locale.US, "%.1f", settings.animationSpeed)}×)")
        Slider(
          value = settings.animationSpeed,
          onValueChange = { v -> onUpdateSettings { it.copy(animationSpeed = v) } },
          valueRange = 0.5f..1.6f,
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
        Spacer(Modifier.height(6.dp))
        Text("Glass FX Studio Sliders", style = MaterialTheme.typography.titleSmall)
        Text("Blur Strength (${(settings.glassBlurStrength * 100).toInt()}%)")
        Slider(
          value = settings.glassBlurStrength,
          onValueChange = { v -> onUpdateSettings { it.copy(glassBlurStrength = v) } },
          valueRange = 0f..1f,
        )
        Text("Surface Transparency (${(settings.glassTransparency * 100).toInt()}%)")
        Slider(
          value = settings.glassTransparency,
          onValueChange = { v -> onUpdateSettings { it.copy(glassTransparency = v) } },
          valueRange = 0.08f..0.75f,
        )
        Text("Specular Edge Reflection (${(settings.glassBorderReflection * 100).toInt()}%)")
        Slider(
          value = settings.glassBorderReflection,
          onValueChange = { v -> onUpdateSettings { it.copy(glassBorderReflection = v) } },
          valueRange = 0f..1f,
        )
        Text("Elevation & Shadow Depth (${(settings.glassShadowDepth * 100).toInt()}%)")
        Slider(
          value = settings.glassShadowDepth,
          onValueChange = { v -> onUpdateSettings { it.copy(glassShadowDepth = v) } },
          valueRange = 0f..1f,
        )
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Accent:", style = MaterialTheme.typography.labelLarge)
          GlassAccentSwatches.forEach { (hex, _) ->
            val selected = settings.customAccentHex == hex
            Box(
              modifier =
                Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(if (hex != null) Color(hex) else Color(theme.palette.primaryHex))
                  .border(if (selected) 2.5.dp else 1.dp, Color.White, CircleShape)
                  .clickable { onUpdateSettings { it.copy(customAccentHex = hex) } },
            )
          }
        }
      }

      SettingsSection.WALLPAPERS -> {
        Button(
          onClick = onOpenLockDesigner,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_open_lock_designer_btn"),
        ) {
          Icon(Icons.Rounded.Lock, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Open Lock Screen & AOD Designer")
        }
        Spacer(Modifier.height(10.dp))
        Text("Procedural Wallpaper Presets", style = MaterialTheme.typography.labelLarge)
        WallpaperStyle.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { ws ->
              FilterChip(
                selected = theme.wallpaper.style == ws && settings.customWallpaperUri == null,
                onClick = {
                  onUpdateSettings { it.copy(customWallpaperUri = null) }
                  onActivateTheme(
                    theme.copy(
                      id = "custom_wp_${ws.name.lowercase()}",
                      isBuiltIn = false,
                      wallpaper = theme.wallpaper.copy(style = ws),
                    ),
                  )
                },
                label = { Text(ws.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              wpPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
          ) {
            Text("Pick Photo")
          }
          Button(
            onClick = { onApplyWallpaperToSystem(WallpaperTarget.BOTH) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
          ) {
            Text("Set System WP")
          }
        }
        Spacer(Modifier.height(6.dp))
        SettingsSwitchRow("Auto Wallpaper-Based Colour Theme", settings.wallpaperBasedPalette) { c ->
          onUpdateSettings { it.copy(wallpaperBasedPalette = c) }
        }
        TextButton(onClick = onExtractThemeFromWallpaper) {
          Text("Extract Colour Palette from Current Wallpaper Now")
        }
      }

      SettingsSection.THEMES -> {
        Button(
          onClick = onOpenThemeGenerator,
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("settings_open_themegen_btn"),
        ) {
          Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Open Aether AI Architect")
        }
        Spacer(Modifier.height(10.dp))
        Text("Built-In & Saved Themes (${state.allThemes.size})", style = MaterialTheme.typography.labelLarge)
        state.allThemes.forEach { itemTheme ->
          val active = itemTheme.id == theme.id
          Surface(
            onClick = { onActivateTheme(itemTheme) },
            shape = RoundedCornerShape(14.dp),
            color = if (active) Color(itemTheme.palette.primaryHex).copy(alpha = 0.2f) else Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(itemTheme.name, style = MaterialTheme.typography.titleSmall)
                Text(itemTheme.category.label, style = MaterialTheme.typography.labelSmall)
              }
              PaletteSwatchRow(itemTheme.palette)
            }
          }
        }
      }

      else -> {
        SystemAndInteractionSectionContent(
          section = section,
          state = state,
          onUpdateSettings = onUpdateSettings,
          onToggleHiddenApp = onToggleHiddenApp,
          onOpenControlCenter = onOpenControlCenter,
          onOpenClonerVault = onOpenClonerVault,
          onReplayOnboarding = onReplayOnboarding,
          onClearSearchHistory = onClearSearchHistory,
          onExportBackup = onExportBackup,
          onImportBackup = onImportBackup,
          onTrimCaches = onTrimCaches,
        )
      }
    }
  }
}

@Composable
internal fun SettingsSwitchRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
internal fun GesturePickerRow(
  gestureName: String,
  current: GestureAction,
  onSelect: (GestureAction) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    OutlinedButton(
      onClick = { expanded = true },
      modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(gestureName)
        Text(current.label, color = MaterialTheme.colorScheme.primary)
      }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      GestureAction.entries.forEach { action ->
        DropdownMenuItem(
          text = { Text(action.label) },
          onClick = {
            onSelect(action)
            expanded = false
          },
        )
      }
    }
  }
}
