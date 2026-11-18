package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.engine.WallpaperTarget
import com.example.ui.ThemeStudioUiState
import com.example.ui.components.*

enum class StudioSection(val label: String) {
  WALLPAPER("Wallpaper"),
  GLASS("Glass"),
  ICONS("Icons"),
  LOCK_CLOCK("Clock & AOD"),
  CONTROL_NOTIFS("Control & Notifs"),
  FX_FONT_SOUND("FX, Font & Sound"),
}

private val AccentColorChoices =
  listOf(
    0xFFA855F7L,
    0xFF38BDF8L,
    0xFF10B981L,
    0xFFF59E0BL,
    0xFFF472B6L,
    0xFFEF4444L,
    0xFFE2E8F0L,
  )

@Composable
fun DesignStudiosScreen(
  state: ThemeStudioUiState,
  onUpdateDraft: ((ThemeSpec) -> ThemeSpec) -> Unit,
  onSaveToLibrary: () -> Unit,
  onActivateTheme: () -> Unit,
  onApplyWallpaper: (WallpaperTarget) -> Unit,
  onPreviewSound: (SoundProfile) -> Unit,
  onToggleMasterAnimations: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedSection by remember { mutableStateOf(StudioSection.WALLPAPER) }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWide = maxWidth >= 720.dp

    if (isWide) {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        Column(
          modifier = Modifier.weight(1.1f).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          StudioHeaderAndTabs(
            state = state,
            selectedSection = selectedSection,
            onSelectSection = { selectedSection = it },
          )
          StudioControlsContent(
            selectedSection = selectedSection,
            state = state,
            onUpdateDraft = onUpdateDraft,
            onApplyWallpaper = onApplyWallpaper,
            onPreviewSound = onPreviewSound,
            onToggleMasterAnimations = onToggleMasterAnimations,
          )
        }
        Column(
          modifier = Modifier.weight(0.9f).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          ThemeDevicePreview(
            theme = state.draftTheme,
            previewBitmap = state.previewBitmap,
            rules = state.renderRules,
            isAppForeground = state.isAppForeground,
          )
          StudioFooterActions(
            onSaveToLibrary = onSaveToLibrary,
            onActivateTheme = onActivateTheme,
          )
        }
      }
    } else {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        StudioHeaderAndTabs(
          state = state,
          selectedSection = selectedSection,
          onSelectSection = { selectedSection = it },
        )
        ThemeDevicePreview(
          theme = state.draftTheme,
          previewBitmap = state.previewBitmap,
          rules = state.renderRules,
          isAppForeground = state.isAppForeground,
        )
        StudioControlsContent(
          selectedSection = selectedSection,
          state = state,
          onUpdateDraft = onUpdateDraft,
          onApplyWallpaper = onApplyWallpaper,
          onPreviewSound = onPreviewSound,
          onToggleMasterAnimations = onToggleMasterAnimations,
        )
        StudioFooterActions(
          onSaveToLibrary = onSaveToLibrary,
          onActivateTheme = onActivateTheme,
        )
        Spacer(Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun StudioHeaderAndTabs(
  state: ThemeStudioUiState,
  selectedSection: StudioSection,
  onSelectSection: (StudioSection) -> Unit,
) {
  StudioTopHeader(
    title = "Design Studios",
    subtitle = "Editing: ${state.draftTheme.name}",
    rules = state.renderRules,
  )

  Row(
    modifier = Modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    StudioSection.entries.forEach { sec ->
      FilterChip(
        selected = selectedSection == sec,
        onClick = { onSelectSection(sec) },
        label = { Text(sec.label) },
        modifier = Modifier.testTag("studio_tab_${sec.name.lowercase()}"),
      )
    }
  }
}

@Composable
private fun StudioControlsContent(
  selectedSection: StudioSection,
  state: ThemeStudioUiState,
  onUpdateDraft: ((ThemeSpec) -> ThemeSpec) -> Unit,
  onApplyWallpaper: (WallpaperTarget) -> Unit,
  onPreviewSound: (SoundProfile) -> Unit,
  onToggleMasterAnimations: (Boolean) -> Unit,
) {
  val theme = state.draftTheme
  when (selectedSection) {
    StudioSection.WALLPAPER -> {
      GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
        Text("Procedural Wallpaper Studio", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          WallpaperAspectMode.entries.forEach { aspect ->
            FilterChip(
              selected = theme.wallpaper.aspectMode == aspect,
              onClick = {
                onUpdateDraft { it.copy(wallpaper = it.wallpaper.copy(aspectMode = aspect)) }
              },
              label = { Text(aspect.label) },
              modifier = Modifier.weight(1f),
            )
          }
        }

        Spacer(Modifier.height(8.dp))
        WallpaperStyle.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { style ->
              FilterChip(
                selected = theme.wallpaper.style == style,
                onClick = {
                  onUpdateDraft { it.copy(wallpaper = it.wallpaper.copy(style = style)) }
                },
                label = { Text(style.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f).testTag("wp_style_${style.name.lowercase()}"),
              )
            }
          }
        }

        Spacer(Modifier.height(8.dp))
        Text("Primary Accent Colour", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          AccentColorChoices.forEach { hex ->
            val selected = theme.palette.primaryHex == hex
            Box(
              modifier =
                Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(Color(hex))
                  .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.35f),
                    shape = CircleShape,
                  )
                  .clickable {
                    onUpdateDraft {
                      it.copy(palette = it.palette.copy(primaryHex = hex, accentGlowHex = hex))
                    }
                  },
            )
          }
        }

        Spacer(Modifier.height(10.dp))
        Text("Particles (${theme.wallpaper.particleCount} • cap ${state.renderRules.maxParticleCount})", style = MaterialTheme.typography.bodyMedium)
        Slider(
          value = theme.wallpaper.particleCount.toFloat(),
          onValueChange = { v ->
            onUpdateDraft { it.copy(wallpaper = it.wallpaper.copy(particleCount = v.toInt())) }
          },
          valueRange = 0f..96f,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              onUpdateDraft {
                it.copy(wallpaper = it.wallpaper.copy(seed = System.currentTimeMillis() % 99999L))
              }
            },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Remix Seed")
          }
          Button(
            onClick = { onApplyWallpaper(WallpaperTarget.BOTH) },
            enabled = !state.isApplyingWallpaper,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("studio_apply_wp_btn"),
          ) {
            Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Apply Both")
          }
        }
      }
    }

    StudioSection.GLASS -> {
      GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
        Text("Offline Glass Effect Engine", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        GlassStyle.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { gs ->
              FilterChip(
                selected = theme.glass.style == gs,
                onClick = { onUpdateDraft { it.copy(glass = it.glass.copy(style = gs)) } },
                label = { Text(gs.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f).testTag("glass_${gs.name.lowercase()}"),
              )
            }
          }
        }
        Spacer(Modifier.height(8.dp))
        Text("Transparency (${(theme.glass.transparency * 100).toInt()}%)")
        Slider(
          value = theme.glass.transparency,
          onValueChange = { v -> onUpdateDraft { it.copy(glass = it.glass.copy(transparency = v)) } },
          valueRange = 0.08f..0.75f,
        )
        Text("Blur Depth (${(theme.glass.blurStrength * 100).toInt()}%)")
        Slider(
          value = theme.glass.blurStrength,
          onValueChange = { v -> onUpdateDraft { it.copy(glass = it.glass.copy(blurStrength = v)) } },
          valueRange = 0f..1f,
        )
        Text("Glow & Reflection (${(theme.glass.glowIntensity * 100).toInt()}%)")
        Slider(
          value = theme.glass.glowIntensity,
          onValueChange = { v ->
            onUpdateDraft { it.copy(glass = it.glass.copy(glowIntensity = v, reflectionOpacity = v)) }
          },
          valueRange = 0f..1f,
        )
        Text("Corner Radius (${theme.glass.cornerRadiusDp} dp)")
        Slider(
          value = theme.glass.cornerRadiusDp.toFloat(),
          onValueChange = { v -> onUpdateDraft { it.copy(glass = it.glass.copy(cornerRadiusDp = v.toInt())) } },
          valueRange = 8f..32f,
        )
      }
    }

    StudioSection.ICONS -> {
      GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
        Text("Local Vector App Icon Studio", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
          SampleStudioIcons.take(4).forEach { app ->
            ThemedVectorAppIcon(app, theme.icons, theme.palette, state.renderRules, size = 46.dp)
          }
        }
        Spacer(Modifier.height(12.dp))
        IconStyle.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { st ->
              FilterChip(
                selected = theme.icons.style == st,
                onClick = { onUpdateDraft { it.copy(icons = it.icons.copy(style = st)) } },
                label = { Text(st.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f).testTag("icon_style_${st.name.lowercase()}"),
              )
            }
          }
        }
        Spacer(Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Icon Outer Glow")
          Switch(
            checked = theme.icons.glowEnabled,
            onCheckedChange = { c -> onUpdateDraft { it.copy(icons = it.icons.copy(glowEnabled = c)) } },
          )
        }
        Text("Icon Corner Radius (${theme.icons.cornerRadiusDp} dp)")
        Slider(
          value = theme.icons.cornerRadiusDp.toFloat(),
          onValueChange = { v -> onUpdateDraft { it.copy(icons = it.icons.copy(cornerRadiusDp = v.toInt())) } },
          valueRange = 6f..28f,
        )
      }
    }

    StudioSection.LOCK_CLOCK -> {
      GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
        Text("Lock Screen, Clock & AOD Studio", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ClockStyle.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { cs ->
              FilterChip(
                selected = theme.lockScreen.clockStyle == cs,
                onClick = { onUpdateDraft { it.copy(lockScreen = it.lockScreen.copy(clockStyle = cs)) } },
                label = { Text(cs.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f).testTag("clock_${cs.name.lowercase()}"),
              )
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Show Seconds on Lock Clock")
          Switch(
            checked = theme.lockScreen.showSeconds,
            onCheckedChange = { c -> onUpdateDraft { it.copy(lockScreen = it.lockScreen.copy(showSeconds = c)) } },
          )
        }
        Spacer(Modifier.height(8.dp))
        Text("Always-On Display (AOD) Style", style = MaterialTheme.typography.titleSmall)
        AodStyle.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { aod ->
              FilterChip(
                selected = theme.lockScreen.aodStyle == aod,
                onClick = { onUpdateDraft { it.copy(lockScreen = it.lockScreen.copy(aodStyle = aod)) } },
                label = { Text(aod.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f).testTag("aod_${aod.name.lowercase()}"),
              )
            }
          }
        }
      }
    }

    StudioSection.CONTROL_NOTIFS -> {
      GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
        Text("Control Centre & Notification Studio", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Control Tile Geometry", style = MaterialTheme.typography.labelLarge)
        TileShape.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { shape ->
              FilterChip(
                selected = theme.controlCenter.tileShape == shape,
                onClick = { onUpdateDraft { it.copy(controlCenter = it.controlCenter.copy(tileShape = shape)) } },
                label = { Text(shape.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
        Spacer(Modifier.height(8.dp))
        Text("Notification Card Style", style = MaterialTheme.typography.labelLarge)
        NotificationStyle.entries.chunked(2).forEach { pair ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pair.forEach { ns ->
              FilterChip(
                selected = theme.controlCenter.notificationStyle == ns,
                onClick = { onUpdateDraft { it.copy(controlCenter = it.controlCenter.copy(notificationStyle = ns)) } },
                label = { Text(ns.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    }

    StudioSection.FX_FONT_SOUND -> {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FingerprintAndChargingStudioSection(state = state, onUpdateDraft = onUpdateDraft)
        FontSoundAndMotionStudioSection(
          state = state,
          onUpdateDraft = onUpdateDraft,
          onPreviewSound = onPreviewSound,
          onToggleMasterAnimations = onToggleMasterAnimations,
        )
      }
    }
  }
}

@Composable
private fun StudioFooterActions(
  onSaveToLibrary: () -> Unit,
  onActivateTheme: () -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    OutlinedButton(
      onClick = onSaveToLibrary,
      modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("studio_save_btn"),
    ) {
      Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text("Save Custom")
    }
    Button(
      onClick = onActivateTheme,
      modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("studio_activate_btn"),
    ) {
      Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text("Activate & Sync")
    }
  }
}
