package com.example.launcher.ui.components

import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TileShape
import com.example.launcher.model.*
import com.example.launcher.system.ControlCenterSystemController
import com.example.launcher.system.LiveNotificationItem
import com.example.launcher.system.NotificationStreamHub
import com.example.launcher.ui.LauncherUiState
import com.example.ui.components.GlassSurfaceCard
import java.util.Locale

private val CcAccentOptions =
  listOf(
    null to "Theme",
    0xFF38BDF8L to "Cyan",
    0xFF8B5CF6L to "Violet",
    0xFF10B981L to "Emerald",
    0xFFF59E0BL to "Amber",
    0xFFF43F5EL to "Rose",
  )

@Composable
fun ControlCenterOverlay(
  state: LauncherUiState,
  onClose: () -> Unit,
  onToggleTorch: () -> Unit,
  onToggleAutoRotate: () -> Unit,
  onCycleRingerMode: () -> Unit,
  onSetBrightness: (Int) -> Unit,
  onSetMediaVolume: (Int) -> Unit,
  onRefreshSystem: () -> Unit,
  onUpdateSettings: ((LauncherSettings) -> LauncherSettings) -> Unit,
  onOpenSettingsSection: (SettingsSection?) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onClose() }

  val context = LocalContext.current
  val settings = state.settings
  val theme = state.activeTheme
  val snap = state.controlCenterSnapshot
  val accentColor = Color(settings.controlCenterAccentHex ?: theme.palette.primaryHex)
  val onSurface = Color(theme.palette.onSurfaceHex)

  var customizingTiles by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    onRefreshSystem()
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              Color(theme.palette.backgroundHex).copy(alpha = 0.88f),
              Color(theme.palette.surfaceHex).copy(alpha = 0.82f),
            ),
          ),
        )
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .testTag("control_center_overlay"),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Top Control Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("Control Center", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = onSurface)
          Text(
            text = "Battery ${snap.batteryPercent}% • ${settings.weatherCityNote}",
            style = MaterialTheme.typography.labelSmall,
            color = onSurface.copy(alpha = 0.78f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          FilledTonalIconButton(
            onClick = { customizingTiles = !customizingTiles },
            modifier = Modifier.size(44.dp).testTag("cc_customize_tiles_btn"),
          ) {
            Icon(
              imageVector = if (customizingTiles) Icons.Rounded.Done else Icons.Rounded.Edit,
              contentDescription = if (customizingTiles) "Finish tile customization" else "Customize tiles",
              tint = accentColor,
            )
          }
          FilledTonalIconButton(
            onClick = { onOpenSettingsSection(SettingsSection.GESTURES) },
            modifier = Modifier.size(44.dp),
          ) {
            Icon(Icons.Rounded.Settings, contentDescription = "Control Center & Gesture Settings")
          }
          IconButton(onClick = onClose, modifier = Modifier.size(44.dp).testTag("cc_close_btn")) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Close Control Center", tint = onSurface)
          }
        }
      }

      if (customizingTiles) {
        ControlCenterTileCustomizerCard(
          settings = settings,
          theme = theme,
          rules = state.renderRules,
          accentColor = accentColor,
          onUpdateSettings = onUpdateSettings,
        )
      }

      // Quick Settings Tiles Grid (Honoring Order, Visibility, 1x1 / 2x1 Size, Shape & Accent)
      val visibleTiles = settings.controlCenterTiles.filter { it.visible }
      val tileCorner =
        when (settings.controlCenterTileShape) {
          TileShape.PILL -> 28.dp
          TileShape.SQUIRCLE -> 18.dp
          TileShape.CIRCLE -> 50.dp
          TileShape.SHARP_GLASS -> 8.dp
        }

      val rows = buildTileRows(visibleTiles)
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { rowConfigs ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            rowConfigs.forEach { cfg ->
              val spec = resolveTileSpec(context, cfg.id, state, snap)
              val active = spec.active
              Surface(
                onClick = {
                  when (cfg.id) {
                    ControlCenterTileId.WIFI -> ControlCenterSystemController.openWifiPanel(context)
                    ControlCenterTileId.MOBILE_DATA -> ControlCenterSystemController.openMobileDataSettings(context)
                    ControlCenterTileId.BLUETOOTH -> ControlCenterSystemController.openBluetoothSettings(context)
                    ControlCenterTileId.AIRPLANE -> ControlCenterSystemController.openAirplaneSettings(context)
                    ControlCenterTileId.TORCH -> onToggleTorch()
                    ControlCenterTileId.AUTO_ROTATE -> onToggleAutoRotate()
                    ControlCenterTileId.EYE_COMFORT -> ControlCenterSystemController.openEyeComfortSettings(context)
                    ControlCenterTileId.DARK_MODE -> ControlCenterSystemController.openDisplaySettings(context)
                    ControlCenterTileId.POWER_SAVER -> ControlCenterSystemController.openBatterySaverSettings(context)
                    ControlCenterTileId.SOUND_MODE -> onCycleRingerMode()
                  }
                },
                shape = RoundedCornerShape(tileCorner),
                color =
                  if (active) accentColor.copy(alpha = 0.30f)
                  else Color(theme.palette.surfaceHex).copy(alpha = 0.56f),
                border =
                  BorderStroke(
                    width = 1.dp,
                    color = if (active) accentColor.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.22f),
                  ),
                modifier =
                  Modifier
                    .weight(cfg.size.weightCols.toFloat())
                    .heightIn(min = 68.dp)
                    .testTag("cc_tile_${cfg.id.name.lowercase()}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Surface(
                    shape = CircleShape,
                    color = if (active) accentColor else Color.White.copy(alpha = 0.14f),
                    modifier = Modifier.size(34.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = spec.icon,
                        contentDescription = cfg.id.label,
                        tint = if (active) Color.Black else onSurface,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = cfg.id.label,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.SemiBold,
                      color = onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = spec.statusLabel,
                      style = MaterialTheme.typography.labelSmall,
                      color = if (active) accentColor else onSurface.copy(alpha = 0.70f),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Display Brightness & Media Volume Sliders
      GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Rounded.BrightnessHigh, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
              Text("Brightness (${snap.brightnessPercent}%)", style = MaterialTheme.typography.labelLarge, color = onSurface)
            }
            if (!snap.canWriteSystemSettings) {
              TextButton(
                onClick = { ControlCenterSystemController.openWriteSettingsPermission(context) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
              ) {
                Text("Grant Write", fontSize = 11.sp)
              }
            }
          }
          var sliderBrightness by remember(snap.brightnessPercent) { mutableFloatStateOf(snap.brightnessPercent.toFloat()) }
          Slider(
            value = sliderBrightness,
            onValueChange = { sliderBrightness = it },
            onValueChangeFinished = { onSetBrightness(sliderBrightness.toInt()) },
            valueRange = 5f..100f,
          )

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Text("Media Volume (${snap.mediaVolume}/${snap.mediaMaxVolume})", style = MaterialTheme.typography.labelLarge, color = onSurface)
          }
          var sliderVolume by remember(snap.mediaVolume) { mutableFloatStateOf(snap.mediaVolume.toFloat()) }
          Slider(
            value = sliderVolume,
            onValueChange = {
              sliderVolume = it
              onSetMediaVolume(it.toInt())
            },
            valueRange = 0f..snap.mediaMaxVolume.toFloat().coerceAtLeast(1f),
          )
        }
      }

      // Live Android Notification Stream
      LiveNotificationSection(
        state = state,
        accentColor = accentColor,
        onSurface = onSurface,
      )

      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
private fun ControlCenterTileCustomizerCard(
  settings: LauncherSettings,
  theme: com.example.domain.model.ThemeSpec,
  rules: com.example.domain.model.EffectiveRenderRules,
  accentColor: Color,
  onUpdateSettings: ((LauncherSettings) -> LauncherSettings) -> Unit,
) {
  GlassSurfaceCard(theme.glass, theme.palette, rules, Modifier.fillMaxWidth()) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text("Customize Tiles, Shape & Accent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TileShape.entries.forEach { ts ->
          FilterChip(
            selected = settings.controlCenterTileShape == ts,
            onClick = { onUpdateSettings { it.copy(controlCenterTileShape = ts) } },
            label = { Text(ts.label, style = MaterialTheme.typography.labelSmall) },
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Accent:", style = MaterialTheme.typography.labelMedium)
        CcAccentOptions.forEach { (hex, label) ->
          val selected = settings.controlCenterAccentHex == hex
          Box(
            modifier =
              Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (hex != null) Color(hex) else Color(theme.palette.primaryHex))
                .border(if (selected) 2.5.dp else 1.dp, Color.White, CircleShape)
                .clickable { onUpdateSettings { it.copy(controlCenterAccentHex = hex) } },
          )
        }
      }

      HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
      settings.controlCenterTiles.forEachIndexed { idx, tile ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Checkbox(
              checked = tile.visible,
              onCheckedChange = { c ->
                onUpdateSettings { s ->
                  s.copy(
                    controlCenterTiles =
                      s.controlCenterTiles.mapIndexed { i, t -> if (i == idx) t.copy(visible = c) else t },
                  )
                }
              },
            )
            Text(tile.id.label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
              onClick = {
                val nextSize =
                  if (tile.size == ControlCenterTileSize.COMPACT_1X1) ControlCenterTileSize.WIDE_2X1
                  else ControlCenterTileSize.COMPACT_1X1
                onUpdateSettings { s ->
                  s.copy(
                    controlCenterTiles =
                      s.controlCenterTiles.mapIndexed { i, t -> if (i == idx) t.copy(size = nextSize) else t },
                  )
                }
              },
            ) {
              Text(tile.size.label, color = accentColor)
            }
            IconButton(
              enabled = idx > 0,
              onClick = {
                onUpdateSettings { s ->
                  val mutable = s.controlCenterTiles.toMutableList()
                  val item = mutable.removeAt(idx)
                  mutable.add(idx - 1, item)
                  s.copy(controlCenterTiles = mutable)
                }
              },
            ) {
              Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move tile up", modifier = Modifier.size(16.dp))
            }
            IconButton(
              enabled = idx < settings.controlCenterTiles.lastIndex,
              onClick = {
                onUpdateSettings { s ->
                  val mutable = s.controlCenterTiles.toMutableList()
                  val item = mutable.removeAt(idx)
                  mutable.add(idx + 1, item)
                  s.copy(controlCenterTiles = mutable)
                }
              },
            ) {
              Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move tile down", modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LiveNotificationSection(
  state: LauncherUiState,
  accentColor: Color,
  onSurface: Color,
) {
  val context = LocalContext.current
  val granted = state.controlCenterSnapshot.notificationAccessGranted

  GlassSurfaceCard(state.activeTheme.glass, state.activeTheme.palette, state.renderRules, Modifier.fillMaxWidth()) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Notifications (${state.liveNotifications.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = onSurface)
        if (granted && state.liveNotifications.any { it.isClearable }) {
          TextButton(onClick = { NotificationStreamHub.clearClearableNotifications() }) {
            Text("Clear All", color = accentColor)
          }
        }
      }

      if (!granted) {
        Text(
          "Notification Access is an optional system permission. Grant it in Android Settings to read, open, and dismiss real notifications here.",
          style = MaterialTheme.typography.bodySmall,
          color = onSurface.copy(alpha = 0.78f),
        )
        Button(
          onClick = { NotificationStreamHub.openNotificationAccessSettings(context) },
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cc_grant_notif_btn"),
        ) {
          Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Grant Notification Access")
        }
      } else if (state.liveNotifications.isEmpty()) {
        Text(
          "All caught up — no active notifications.",
          style = MaterialTheme.typography.bodySmall,
          color = onSurface.copy(alpha = 0.75f),
        )
      } else {
        state.liveNotifications.take(8).forEach { item: LiveNotificationItem ->
          Surface(
            onClick = { runCatching { item.contentIntent?.send() } },
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${item.appName} • ${item.title}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.text.isNotBlank()) {
                  Text(item.text, style = MaterialTheme.typography.bodySmall, color = onSurface.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
              }
              if (item.isClearable) {
                IconButton(onClick = { NotificationStreamHub.dismissNotification(item.key) }) {
                  Icon(Icons.Rounded.Close, contentDescription = "Dismiss notification", modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

private data class ResolvedTileSpec(
  val icon: ImageVector,
  val active: Boolean,
  val statusLabel: String,
)

private fun resolveTileSpec(
  context: android.content.Context,
  id: ControlCenterTileId,
  state: LauncherUiState,
  snap: com.example.launcher.system.ControlCenterSystemSnapshot,
): ResolvedTileSpec =
  when (id) {
    ControlCenterTileId.WIFI ->
      ResolvedTileSpec(Icons.Rounded.Wifi, snap.wifiConnected, if (snap.wifiConnected) "Connected • Panel" else "Open Panel")
    ControlCenterTileId.MOBILE_DATA ->
      ResolvedTileSpec(Icons.Rounded.CellTower, snap.cellularConnected, if (snap.cellularConnected) "Active • Panel" else "Network Panel")
    ControlCenterTileId.BLUETOOTH ->
      ResolvedTileSpec(
        Icons.Rounded.Bluetooth,
        snap.bluetoothEnabled == true,
        when (snap.bluetoothEnabled) {
          true -> "On • Settings"
          false -> "Off • Settings"
          null -> "Settings"
        },
      )
    ControlCenterTileId.AIRPLANE ->
      ResolvedTileSpec(Icons.Rounded.AirplanemodeActive, snap.airplaneModeOn, if (snap.airplaneModeOn) "On • System" else "Off • System")
    ControlCenterTileId.TORCH ->
      ResolvedTileSpec(
        Icons.Rounded.FlashlightOn,
        state.torchEnabled,
        if (!snap.torchAvailable) "Unavailable" else if (state.torchEnabled) "On" else "Off",
      )
    ControlCenterTileId.AUTO_ROTATE ->
      ResolvedTileSpec(Icons.Rounded.ScreenRotation, snap.autoRotateOn, if (snap.autoRotateOn) "On" else "Locked")
    ControlCenterTileId.EYE_COMFORT ->
      ResolvedTileSpec(Icons.Rounded.RemoveRedEye, false, "Display Panel")
    ControlCenterTileId.DARK_MODE ->
      ResolvedTileSpec(Icons.Rounded.DarkMode, snap.darkModeOn, if (snap.darkModeOn) "Dark On • Settings" else "Light • Settings")
    ControlCenterTileId.POWER_SAVER ->
      ResolvedTileSpec(Icons.Rounded.BatterySaver, snap.powerSaverOn, if (snap.powerSaverOn) "Active • Settings" else "${snap.batteryPercent}% • Settings")
    ControlCenterTileId.SOUND_MODE -> {
      val label =
        when (snap.ringerMode) {
          AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
          AudioManager.RINGER_MODE_SILENT -> "Silent"
          else -> "Ring"
        }
      ResolvedTileSpec(Icons.Rounded.Notifications, snap.ringerMode == AudioManager.RINGER_MODE_NORMAL, label)
    }
  }

private fun buildTileRows(tiles: List<ControlCenterTileConfig>): List<List<ControlCenterTileConfig>> {
  val rows = mutableListOf<MutableList<ControlCenterTileConfig>>()
  var curRow = mutableListOf<ControlCenterTileConfig>()
  var curWeight = 0
  tiles.forEach { t ->
    val w = t.size.weightCols.coerceIn(1, 2)
    if (curWeight + w > 4) {
      if (curRow.isNotEmpty()) rows.add(curRow)
      curRow = mutableListOf(t)
      curWeight = w
    } else {
      curRow.add(t)
      curWeight += w
    }
  }
  if (curRow.isNotEmpty()) rows.add(curRow)
  return rows
}
