package com.example.launcher.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.model.DeviceFormFactor
import com.example.domain.model.PerformanceMode
import com.example.engine.HardwareCapabilityEngine
import com.example.launcher.model.*
import com.example.launcher.system.LauncherSystemBridge
import com.example.launcher.ui.LauncherUiState

@Composable
internal fun SystemAndInteractionSectionContent(
  section: SettingsSection,
  state: LauncherUiState,
  onUpdateSettings: ((LauncherSettings) -> LauncherSettings) -> Unit,
  onToggleHiddenApp: (String, Boolean) -> Unit,
  onClearSearchHistory: () -> Unit,
  onExportBackup: () -> Unit,
  onImportBackup: () -> Unit,
  onTrimCaches: () -> Unit,
) {
  val context = LocalContext.current
  val settings = state.settings
  val caps = state.deviceCapabilities

  val contactsPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      onUpdateSettings { it.copy(searchContactsEnabled = granted) }
    }

  when (section) {
    SettingsSection.GESTURES -> {
      GesturePickerRow("Swipe Up", settings.swipeUpAction) { a ->
        onUpdateSettings { it.copy(swipeUpAction = a) }
      }
      GesturePickerRow("Swipe Down", settings.swipeDownAction) { a ->
        onUpdateSettings { it.copy(swipeDownAction = a) }
      }
      GesturePickerRow("Double Tap", settings.doubleTapAction) { a ->
        onUpdateSettings { it.copy(doubleTapAction = a) }
      }
      GesturePickerRow("Two-Finger Swipe", settings.twoFingerSwipeAction) { a ->
        onUpdateSettings { it.copy(twoFingerSwipeAction = a) }
      }
      GesturePickerRow("Long Press Empty Area", settings.longPressAction) { a ->
        onUpdateSettings { it.copy(longPressAction = a) }
      }
      Spacer(Modifier.height(8.dp))
      OutlinedButton(
        onClick = {
          runCatching { context.startActivity(LauncherSystemBridge.buildEnableDeviceAdminIntent(context)) }
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
      ) {
        Text(
          if (state.isDeviceAdminActive) "Double-Tap Screen Lock Admin Active ✓"
          else "Enable Optional Lock-Screen Admin",
        )
      }
      Spacer(Modifier.height(4.dp))
      Text(
        "Note: Notification Shade expansion or instant lock may be restricted by some OEMs when not granted.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    SettingsSection.SEARCH -> {
      SettingsSwitchRow("Instant Search as You Type", settings.instantSearch) { c ->
        onUpdateSettings { it.copy(instantSearch = c) }
      }
      SettingsSwitchRow("Show Local Search History", settings.showSearchHistory) { c ->
        onUpdateSettings { it.copy(showSearchHistory = c) }
      }
      SettingsSwitchRow("Voice Search (Android Speech API)", settings.voiceSearchEnabled) { c ->
        onUpdateSettings { it.copy(voiceSearchEnabled = c) }
      }
      SettingsSwitchRow("Search Local Contacts (Permission Opt-In)", settings.searchContactsEnabled) { want ->
        if (want) {
          val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
              PackageManager.PERMISSION_GRANTED
          if (granted) onUpdateSettings { it.copy(searchContactsEnabled = true) }
          else contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
          onUpdateSettings { it.copy(searchContactsEnabled = false) }
        }
      }
      Text("Home Search Bar Style", style = MaterialTheme.typography.labelLarge)
      SearchBarStyle.entries.chunked(2).forEach { pair ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          pair.forEach { sbs ->
            FilterChip(
              selected = settings.searchBarStyle == sbs,
              onClick = { onUpdateSettings { it.copy(searchBarStyle = sbs) } },
              label = { Text(sbs.label, style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
      TextButton(onClick = onClearSearchHistory) { Text("Clear Local Search History") }
    }

    SettingsSection.BACKUP_RESTORE -> {
      Text(
        "Export or restore your complete Home Screen pages, folders, widgets, icon styles, gestures, and active theme as an offline JSON file.",
        style = MaterialTheme.typography.bodyMedium,
      )
      Spacer(Modifier.height(10.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = onExportBackup,
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("backup_export_btn"),
        ) {
          Text("Export Backup")
        }
        OutlinedButton(
          onClick = onImportBackup,
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("backup_import_btn"),
        ) {
          Text("Restore Backup")
        }
      }
    }

    SettingsSection.PERFORMANCE -> {
      Text(
        "Detected Tier: ${caps.recommendedTier.label} • RAM ${caps.availableRamMb}/${caps.totalRamMb} MB • ${caps.refreshRateHz}Hz",
        style = MaterialTheme.typography.labelSmall,
      )
      Spacer(Modifier.height(8.dp))
      PerformanceMode.entries.forEach { pm ->
        val selected = settings.performanceMode == pm
        Surface(
          onClick = { onUpdateSettings { it.copy(performanceMode = pm) } },
          shape = RoundedCornerShape(12.dp),
          color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
          modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        ) {
          Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(8.dp))
            Column {
              Text(pm.label, style = MaterialTheme.typography.titleSmall)
              Text(pm.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
      Spacer(Modifier.height(8.dp))
      OutlinedButton(
        onClick = onTrimCaches,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
      ) {
        Text("Release Unused Icon & Wallpaper Caches")
      }
    }

    SettingsSection.PRIVACY -> {
      Text(
        "• Zero API keys, zero cloud accounts, zero telemetry.\n" +
          "• All layouts, usage counts, search history, and themes stay on this device.\n" +
          "• Hiding an app removes it from the launcher view only—it does NOT encrypt, lock, or uninstall the app.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(10.dp))
      Text("Hidden Apps (${settings.hiddenPackages.size})", style = MaterialTheme.typography.labelLarge)
      if (settings.hiddenPackages.isEmpty()) {
        Text(
          "No apps hidden. Long-press any app in the Drawer to hide it from launcher view.",
          style = MaterialTheme.typography.bodySmall,
        )
      } else {
        settings.hiddenPackages.forEach { pkg ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(pkg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { onToggleHiddenApp(pkg, false) }) { Text("Unhide") }
          }
        }
      }
    }

    SettingsSection.TABLET_FOLDABLE -> {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            "Detected Form Factor: ${caps.formFactor.label}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            "Smallest Width: ${caps.smallestScreenWidthDp}dp • Resolution: ${caps.screenWidthPx}×${caps.screenHeightPx}px",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          val effectiveHomeCols =
            if (caps.formFactor != DeviceFormFactor.PHONE && settings.autoAdaptLargeScreen) {
              (settings.gridColumns + settings.tabletColumnBoost).coerceAtMost(8)
            } else {
              settings.gridColumns
            }
          Text(
            "Active Home Grid Columns: $effectiveHomeCols (Base ${settings.gridColumns})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
      Spacer(Modifier.height(10.dp))
      SettingsSwitchRow(
        label = "Auto-Expand Columns on Tablets & Unfolded Screens",
        checked = settings.autoAdaptLargeScreen,
      ) { c ->
        onUpdateSettings { it.copy(autoAdaptLargeScreen = c) }
      }
      Text("Large-Screen Home Column Boost (+${settings.tabletColumnBoost} cols)")
      Slider(
        value = settings.tabletColumnBoost.toFloat(),
        onValueChange = { v -> onUpdateSettings { it.copy(tabletColumnBoost = v.toInt()) } },
        valueRange = 0f..3f,
        steps = 2,
        enabled = settings.autoAdaptLargeScreen,
      )
      Text("Large-Screen App Drawer Boost (+${settings.tabletDrawerColumnBoost} cols)")
      Slider(
        value = settings.tabletDrawerColumnBoost.toFloat(),
        onValueChange = { v -> onUpdateSettings { it.copy(tabletDrawerColumnBoost = v.toInt()) } },
        valueRange = 0f..3f,
        steps = 2,
        enabled = settings.autoAdaptLargeScreen,
      )
      Text(
        "Split-pane List-Detail settings & container-based layouts activate automatically whenever available width reaches 600dp–680dp.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    SettingsSection.DEFAULT_HOME_COMPAT -> {
      var oemFilter by remember { mutableStateOf("") }
      Button(
        onClick = {
          runCatching { context.startActivity(LauncherSystemBridge.buildDefaultHomeIntent(context)) }
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("compat_set_default_home_btn"),
      ) {
        Icon(Icons.Rounded.Home, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (state.isDefaultHome) "Active Default HOME Launcher ✓" else "Choose Default HOME App")
      }
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = oemFilter,
        onValueChange = { oemFilter = it },
        label = { Text("Filter OEMs (OPPO, Samsung, Pixel, Xiaomi…)") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))
      val filteredOems =
        remember(oemFilter) {
          if (oemFilter.isBlank()) HardwareCapabilityEngine.oemMatrix
          else HardwareCapabilityEngine.oemMatrix.filter {
            it.brand.contains(oemFilter, ignoreCase = true) ||
              it.uiSkin.contains(oemFilter, ignoreCase = true)
          }
        }
      Text(
        "OEM Compatibility Matrix (${filteredOems.size} of ${HardwareCapabilityEngine.oemMatrix.size} profiles)",
        style = MaterialTheme.typography.labelLarge,
      )
      Spacer(Modifier.height(4.dp))
      filteredOems.forEach { oem ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("${oem.brand} • ${oem.uiSkin}", style = MaterialTheme.typography.titleSmall)
              Text(
                "${oem.wallpaperSupport.label} WP/Widget",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Text(oem.iconPackNote, style = MaterialTheme.typography.bodySmall)
            Text(
              oem.systemOverlayNote,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    SettingsSection.ABOUT -> {
      Text("Theme Studio — ColorOS 7 Inspired Launcher", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(4.dp))
      Text(
        "100% Offline-First • Zero API Keys • Zero Cloud Dependencies • Zero Telemetry",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(10.dp))
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Hardware & Display Telemetry", style = MaterialTheme.typography.labelLarge)
          Text("Device: ${caps.manufacturer} ${caps.model}", style = MaterialTheme.typography.bodySmall)
          Text("Android: ${caps.androidVersion} (API ${caps.sdkInt})", style = MaterialTheme.typography.bodySmall)
          Text("Form Factor: ${caps.formFactor.label} (${caps.smallestScreenWidthDp}dp min width)", style = MaterialTheme.typography.bodySmall)
          Text("CPU / ABI: ${caps.cpuCores} cores (${caps.cpuAbi})", style = MaterialTheme.typography.bodySmall)
          Text("Memory: ${caps.availableRamMb} / ${caps.totalRamMb} MB available", style = MaterialTheme.typography.bodySmall)
          Text("Display: ${caps.screenWidthPx}×${caps.screenHeightPx} @ ${caps.refreshRateHz}Hz", style = MaterialTheme.typography.bodySmall)
          Text("Graphics: ${caps.gpuHint}", style = MaterialTheme.typography.bodySmall)
        }
      }
    }

    else -> {}
  }
}
