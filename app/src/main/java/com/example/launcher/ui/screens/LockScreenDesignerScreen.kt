package com.example.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.engine.WallpaperTarget
import com.example.launcher.ui.LauncherUiState
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.PreviewSurfaceMode
import com.example.ui.components.ThemeDevicePreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreenDesignerScreen(
  state: LauncherUiState,
  onUpdateTheme: ((ThemeSpec) -> ThemeSpec) -> Unit,
  onApplyLockWallpaper: (WallpaperTarget) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onBack() }

  val theme = state.activeTheme
  val primary = Color(theme.palette.primaryHex)
  var fingerPulseCount by remember { mutableIntStateOf(0) }

  Scaffold(
    containerColor = Color.Transparent,
    topBar = {
      TopAppBar(
        title = { Text("Lock Screen & AOD Designer") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("lock_designer_back_btn")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        modifier
          .fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .testTag("lock_designer_screen"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ThemeDevicePreview(
        theme = theme,
        previewBitmap = state.wallpaperBitmap,
        rules = state.renderRules,
        isAppForeground = state.isForeground,
        initialMode = PreviewSurfaceMode.LOCK,
      )

      GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Lock Screen Clock Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ClockStyle.entries.forEach { cs ->
              FilterChip(
                selected = theme.lockScreen.clockStyle == cs,
                onClick = {
                  onUpdateTheme { t -> t.copy(lockScreen = t.lockScreen.copy(clockStyle = cs)) }
                },
                label = { Text(cs.label, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }

          SettingsSwitchRow("Show Clock Seconds", theme.lockScreen.showSeconds) { show ->
            onUpdateTheme { t -> t.copy(lockScreen = t.lockScreen.copy(showSeconds = show)) }
          }

          Text("Always-On Display (AOD) Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            AodStyle.entries.forEach { aod ->
              FilterChip(
                selected = theme.lockScreen.aodStyle == aod,
                onClick = {
                  onUpdateTheme { t -> t.copy(lockScreen = t.lockScreen.copy(aodStyle = aod)) }
                },
                label = { Text(aod.label, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
        }
      }

      GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Fingerprint & Charging Visual Studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FingerprintEffect.entries.forEach { fp ->
              FilterChip(
                selected = theme.lockScreen.fingerprintEffect == fp,
                onClick = {
                  onUpdateTheme { t -> t.copy(lockScreen = t.lockScreen.copy(fingerprintEffect = fp)) }
                },
                label = { Text(fp.label, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }

          // Interactive Fingerprint Ripple Preview Pad
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier
                  .size(72.dp)
                  .clip(CircleShape)
                  .background(primary.copy(alpha = 0.16f))
                  .pointerInput(Unit) {
                    detectTapGestures(onTap = { fingerPulseCount++ })
                  },
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeColor = if (fingerPulseCount % 2 == 0) primary else Color.White
                drawCircle(color = strokeColor.copy(alpha = 0.65f), style = Stroke(3.dp.toPx()))
              }
              Icon(Icons.Rounded.Fingerprint, contentDescription = "Test fingerprint ripple", tint = primary, modifier = Modifier.size(34.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("Tap Pad to Preview Ripple", style = MaterialTheme.typography.titleSmall)
              Text(
                text = "${theme.lockScreen.fingerprintEffect.label} • Taps: $fingerPulseCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Text("Charging Animation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ChargingAnimation.entries.forEach { ca ->
              FilterChip(
                selected = theme.lockScreen.chargingAnimation == ca,
                onClick = {
                  onUpdateTheme { t -> t.copy(lockScreen = t.lockScreen.copy(chargingAnimation = ca)) }
                },
                label = { Text(ca.label, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
          onClick = { onApplyLockWallpaper(WallpaperTarget.LOCK) },
          modifier = Modifier.weight(1f).heightIn(min = 50.dp).testTag("apply_lock_wp_btn"),
        ) {
          Icon(Icons.Rounded.Lock, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text("Apply Lock Wallpaper")
        }
        OutlinedButton(
          onClick = { onApplyLockWallpaper(WallpaperTarget.BOTH) },
          modifier = Modifier.weight(1f).heightIn(min = 50.dp),
        ) {
          Text("Home & Lock")
        }
      }

      Spacer(Modifier.height(20.dp))
    }
  }
}
