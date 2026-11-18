package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.PerformanceMode
import com.example.domain.model.ResolvedTier
import com.example.domain.model.SupportLevel
import com.example.engine.HardwareCapabilityEngine
import com.example.ui.ThemeStudioUiState
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.StudioTopHeader
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.WarmAmber

@Composable
fun DeviceEngineScreen(
  state: ThemeStudioUiState,
  onSelectPerformanceMode: (PerformanceMode) -> Unit,
  onToggleMasterAnimations: (Boolean) -> Unit,
  onRefreshTelemetry: () -> Unit,
  onClearBitmapCache: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val caps = state.deviceCapabilities
  val rules = state.renderRules
  var oemSearch by remember { mutableStateOf("") }

  val filteredOems =
    remember(oemSearch) {
      HardwareCapabilityEngine.oemMatrix.filter {
        oemSearch.isBlank() ||
          it.brand.contains(oemSearch, ignoreCase = true) ||
          it.uiSkin.contains(oemSearch, ignoreCase = true)
      }
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    StudioTopHeader(
      title = "Optimisation & Device Engine",
      subtitle = "Low-RAM protection • Battery efficiency • Universal OEM Matrix",
      rules = rules,
    )

    // Performance Mode Switcher
    GlassSurfaceCard(
      glass = state.draftTheme.glass,
      palette = state.draftTheme.palette,
      rules = rules,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Performance Mode", style = MaterialTheme.typography.titleMedium)
          Text(
            "Resolved Tier: ${rules.tier.label} (Recommended: ${caps.recommendedTier.label})",
            style = MaterialTheme.typography.labelSmall,
            color =
              when (rules.tier) {
                ResolvedTier.LITE -> EmeraldMint
                ResolvedTier.BALANCED -> ElectricCyan
                ResolvedTier.PREMIUM -> LuminousViolet
              },
          )
        }
        IconButton(onClick = onRefreshTelemetry, modifier = Modifier.size(48.dp)) {
          Icon(Icons.Rounded.Refresh, contentDescription = "Refresh hardware telemetry")
        }
      }

      Spacer(Modifier.height(8.dp))
      PerformanceMode.entries.forEach { mode ->
        val selected = state.performanceMode == mode
        Surface(
          onClick = { onSelectPerformanceMode(mode) },
          shape = RoundedCornerShape(14.dp),
          color =
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            else Color.Transparent,
          modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).testTag("perf_mode_${mode.name.lowercase()}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(mode.label, style = MaterialTheme.typography.titleSmall)
              Text(
                mode.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      Spacer(Modifier.height(8.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline)
      Spacer(Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Decorative Animations", style = MaterialTheme.typography.bodyMedium)
          Text(
            "Auto-paused when app is backgrounded or screen off",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = state.animationsMasterEnabled,
          onCheckedChange = onToggleMasterAnimations,
          modifier = Modifier.testTag("engine_anim_switch"),
        )
      }

      Spacer(Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onClearBitmapCache,
          modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("clear_cache_btn"),
        ) {
          Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Trim Bitmap Cache")
        }
      }
    }

    // Real Hardware Capabilities Card
    GlassSurfaceCard(
      glass = state.draftTheme.glass,
      palette = state.draftTheme.palette,
      rules = rules,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Detected Device Hardware", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(8.dp))
      TelemetryRow("Device", "${caps.manufacturer} ${caps.model} (${caps.formFactor.label})")
      TelemetryRow("Android OS", "Android ${caps.androidVersion} (API ${caps.sdkInt})")
      TelemetryRow(
        "Memory (RAM)",
        "${caps.availableRamMb} MB free / ${caps.totalRamMb} MB total" +
          if (caps.isLowRamDevice) " (Low-RAM Flag)" else "",
      )
      TelemetryRow("CPU & ABI", "${caps.cpuCores} cores • ${caps.cpuAbi}")
      TelemetryRow(
        "Display",
        "${caps.screenWidthPx}×${caps.screenHeightPx} • ${caps.densityDpi} dpi • ${caps.refreshRateHz} Hz",
      )
      TelemetryRow("Graphics", caps.gpuHint)
      TelemetryRow(
        "Battery",
        "${caps.batteryPercent}% • ${if (caps.isCharging) "Charging" else "On Battery"}" +
          if (caps.isPowerSaveMode) " • Power Saver ON" else "",
      )
      TelemetryRow(
        "Active Render Budget",
        "Max ${rules.maxWallpaperLongSidePx}px • Blur=${rules.enableBlur} • Particles≤${rules.maxParticleCount}",
      )
    }

    // Universal OEM Compatibility Matrix
    GlassSurfaceCard(
      glass = state.draftTheme.glass,
      palette = state.draftTheme.palette,
      rules = rules,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Universal Device Compatibility Engine", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(4.dp))
      Text(
        "Standard Android APIs (Wallpapers, App Widgets, JSON Packs, Offline Studios) work across all phones & tablets. OS-level overlays requiring OEM system privileges are transparently labeled below.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(10.dp))
      OutlinedTextField(
        value = oemSearch,
        onValueChange = { oemSearch = it },
        placeholder = { Text("Filter brand (OPPO, Samsung, Xiaomi, Pixel, Tablet...)") },
        leadingIcon = { Icon(Icons.Rounded.Devices, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("oem_search_input"),
      )
      Spacer(Modifier.height(10.dp))

      filteredOems.forEach { oem ->
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${oem.brand} (${oem.uiSkin})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Surface(
                shape = RoundedCornerShape(50),
                color = EmeraldMint.copy(alpha = 0.16f),
              ) {
                Text(
                  text = if (oem.wallpaperSupport == SupportLevel.UNIVERSAL) "Wallpaper & Widgets ✓" else oem.wallpaperSupport.label,
                  style = MaterialTheme.typography.labelSmall,
                  color = EmeraldMint,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
            Text(
              text = "Icons: ${oem.iconPackNote}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "OEM System Scope: ${oem.systemOverlayNote}",
              style = MaterialTheme.typography.labelSmall,
              color = WarmAmber,
            )
          }
        }
      }
    }

    Spacer(Modifier.height(16.dp))
  }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(0.38f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(0.62f),
    )
  }
}
