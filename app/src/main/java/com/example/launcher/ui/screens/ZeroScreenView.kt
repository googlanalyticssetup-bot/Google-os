package com.example.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.launcher.model.InstalledAppInfo
import com.example.launcher.model.IsolatedWebApp
import com.example.launcher.system.ControlCenterSystemController
import com.example.launcher.ui.LauncherUiState
import com.example.launcher.ui.components.LauncherAppIconTile
import com.example.ui.components.GlassSurfaceCard
import java.util.Locale

@Composable
fun ZeroScreenView(
  state: LauncherUiState,
  onClose: () -> Unit,
  onOpenSearch: () -> Unit,
  onOpenAiArchitect: () -> Unit,
  onAskQuickAi: (String) -> Unit,
  onOpenClonerVault: () -> Unit,
  onLaunchApp: (InstalledAppInfo) -> Unit,
  onOpenWebApp: (IsolatedWebApp) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onClose() }

  val context = LocalContext.current
  val theme = state.activeTheme
  val primary = Color(theme.palette.primaryHex)
  val secondary = Color(theme.palette.secondaryHex)
  val onSurface = Color(theme.palette.onSurfaceHex)
  val snap = state.controlCenterSnapshot

  var aiQuickInput by remember { mutableStateOf("") }

  val topApps =
    remember(state.installedApps, state.settings.hiddenPackages) {
      state.installedApps
        .filter { it.packageName !in state.settings.hiddenPackages }
        .sortedByDescending { it.launchCount * 1000L + it.lastLaunchedAt }
        .take(5)
    }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              Color(theme.palette.backgroundHex).copy(alpha = 0.90f),
              Color(theme.palette.surfaceHex).copy(alpha = 0.86f),
            ),
          ),
        )
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .testTag("zero_screen_view"),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header + Search Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("Zero Workspace", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = onSurface)
          Text("Smart Aether Feed • Real Telemetry", style = MaterialTheme.typography.labelSmall, color = primary)
        }
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp).testTag("zero_close_btn")) {
          Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Return to Home", tint = onSurface)
        }
      }

      Surface(
        onClick = onOpenSearch,
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = primary)
            Spacer(Modifier.width(10.dp))
            Text("Search apps, settings & contacts…", style = MaterialTheme.typography.bodyMedium, color = onSurface.copy(alpha = 0.8f))
          }
          Icon(Icons.Rounded.Mic, contentDescription = "Voice search", tint = secondary)
        }
      }

      // AI Architect Quick Card
      GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = primary)
              Text("Aether AI Architect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onSurface)
            }
            TextButton(onClick = onOpenAiArchitect) {
              Text("Open Studio")
            }
          }
          val latestReply = state.aiArchitect.chatTurns.lastOrNull { !it.isUser }?.text
            ?: state.aiArchitect.overviewMessage
            ?: "Ask Gemini 3.8 Flash about theme ideas, battery-friendly Glass FX, or generate palettes offline."
          Text(latestReply, style = MaterialTheme.typography.bodySmall, color = onSurface.copy(alpha = 0.82f), maxLines = 3, overflow = TextOverflow.Ellipsis)

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = aiQuickInput,
              onValueChange = { aiQuickInput = it },
              placeholder = { Text("Ask AI Architect…") },
              singleLine = true,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(
              onClick = {
                val q = aiQuickInput.trim()
                if (q.isNotBlank()) {
                  onAskQuickAi(q)
                  aiQuickInput = ""
                  onOpenAiArchitect()
                }
              },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(Icons.Rounded.Send, contentDescription = "Ask AI Architect")
            }
          }
        }
      }

      // Digital Wellness & Device Storage Telemetry
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.weight(1f)) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Rounded.Storage, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
            Text("Storage", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = onSurface)
            val usedStr = String.format(Locale.US, "%.1f / %.0f GB", snap.storageUsedGb, snap.storageTotalGb)
            Text(usedStr, style = MaterialTheme.typography.bodySmall, color = onSurface.copy(alpha = 0.8f))
            LinearProgressIndicator(
              progress = { (snap.storageUsedGb / snap.storageTotalGb.coerceAtLeast(1f)).coerceIn(0f, 1f) },
              color = primary,
              trackColor = onSurface.copy(alpha = 0.15f),
              modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
            )
          }
        }

        GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.weight(1f)) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = secondary, modifier = Modifier.size(20.dp))
            Text("Wellness", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = onSurface)
            val mins = snap.todayScreenMinutes
            if (mins != null) {
              Text("${mins / 60}h ${mins % 60}m screen time", style = MaterialTheme.typography.bodySmall, color = onSurface.copy(alpha = 0.8f))
            } else {
              val totalLaunches = state.installedApps.sumOf { it.launchCount }
              Text("$totalLaunches launcher opens", style = MaterialTheme.typography.bodySmall, color = onSurface.copy(alpha = 0.8f))
              Text(
                text = "Enable Usage Access",
                style = MaterialTheme.typography.labelSmall,
                color = primary,
                modifier = Modifier.clickable { ControlCenterSystemController.openUsageAccessSettings(context) },
              )
            }
          }
        }
      }

      // Cloner Vault & Isolated Web Apps Card
      GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Rounded.Security, contentDescription = null, tint = primary)
              Column {
                Text("Cloner Vault & Web Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = onSurface)
                Text(
                  "${state.settings.lockedPackages.size} locked • ${state.settings.isolatedWebApps.size} web apps • ${if (snap.hasWorkProfile) "Work Profile Ready" else "Work Optional"}",
                  style = MaterialTheme.typography.labelSmall,
                  color = onSurface.copy(alpha = 0.75f),
                )
              }
            }
            FilledTonalButton(onClick = onOpenClonerVault) {
              Text("Vault")
            }
          }
          if (state.settings.isolatedWebApps.isNotEmpty()) {
            Row(
              modifier = Modifier.horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              state.settings.isolatedWebApps.forEach { webApp ->
                AssistChip(
                  onClick = { onOpenWebApp(webApp) },
                  leadingIcon = { Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(16.dp)) },
                  label = { Text(webApp.title) },
                )
              }
            }
          }
        }
      }

      // Recent & Frequent Apps Row
      if (topApps.isNotEmpty()) {
        GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Suggested Apps", style = MaterialTheme.typography.labelLarge, color = primary, fontWeight = FontWeight.SemiBold)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              topApps.forEach { app ->
                LauncherAppIconTile(
                  app = app,
                  settings = state.settings,
                  theme = theme,
                  rules = state.renderRules,
                  iconSizeDp = 44,
                  onClick = { onLaunchApp(app) },
                  onLongClick = {},
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }

      Spacer(Modifier.height(16.dp))
    }
  }
}
