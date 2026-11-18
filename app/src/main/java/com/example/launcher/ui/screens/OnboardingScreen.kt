package com.example.launcher.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemeSpec
import com.example.launcher.model.LauncherIconStyle
import com.example.launcher.model.LauncherSettings
import com.example.launcher.system.ControlCenterSystemController
import com.example.launcher.system.LauncherSystemBridge
import com.example.launcher.system.NotificationStreamHub
import com.example.launcher.ui.LauncherUiState
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.PaletteSwatchRow

private enum class OnboardingStep(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
) {
  INTRO("Welcome to Aether Glass OS", "Liquid glass materials, fluid spring motion & real Android controls.", Icons.Rounded.AutoAwesome),
  THEME("Choose Signature Surface", "Pick an instant starting look — customize everything later in Settings.", Icons.Rounded.Palette),
  LAYOUT("Home Grid & Icon Material", "Tailor density, Liquid Glass icons, Zero Screen & Dynamic Capsule.", Icons.Rounded.GridView),
  DEFAULT_HOME("Default HOME Role", "Set Aether Glass as your system Home Screen replacement.", Icons.Rounded.Home),
  PERMISSIONS("Optional System Integrations", "Grant only the Android capabilities you want — every feature stays honest.", Icons.Rounded.AdminPanelSettings),
  READY("Workspace Ready", "Swipe up for App Drawer, down for Control Center, left for Zero Screen.", Icons.Rounded.RocketLaunch),
}

@Composable
fun OnboardingScreen(
  state: LauncherUiState,
  onActivateTheme: (ThemeSpec) -> Unit,
  onUpdateSettings: ((LauncherSettings) -> LauncherSettings) -> Unit,
  onRefreshSystemStatus: () -> Unit,
  onComplete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val steps = OnboardingStep.entries
  var stepIndex by remember { mutableIntStateOf(0) }
  val step = steps[stepIndex]

  val theme = state.activeTheme
  val primary = Color(theme.palette.primaryHex)
  val secondary = Color(theme.palette.secondaryHex)
  val onSurface = Color(theme.palette.onSurfaceHex)

  LaunchedEffect(stepIndex) {
    onRefreshSystemStatus()
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              Color(theme.palette.backgroundHex),
              primary.copy(alpha = 0.24f),
              Color(theme.palette.surfaceHex),
            ),
          ),
        )
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .testTag("onboarding_screen"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Progress Bar & Skip
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Step ${stepIndex + 1} of ${steps.size}",
            style = MaterialTheme.typography.labelLarge,
            color = primary,
          )
          TextButton(
            onClick = onComplete,
            modifier = Modifier.heightIn(min = 48.dp).testTag("onboarding_skip_btn"),
          ) {
            Text("Skip Setup", color = onSurface.copy(alpha = 0.8f))
          }
        }
        LinearProgressIndicator(
          progress = { (stepIndex + 1).toFloat() / steps.size.toFloat() },
          color = primary,
          trackColor = onSurface.copy(alpha = 0.14f),
          modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
        )
      }

      // Main Animated Step Body
      AnimatedContent(
        targetState = step,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        label = "onboarding_step",
      ) { cur ->
        Column(
          modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = primary.copy(alpha = 0.2f),
              border = BorderStroke(1.dp, primary.copy(alpha = 0.5f)),
              modifier = Modifier.size(52.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(cur.icon, contentDescription = null, tint = primary, modifier = Modifier.size(26.dp))
              }
            }
            Column {
              Text(cur.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onSurface)
              Text(cur.subtitle, style = MaterialTheme.typography.bodySmall, color = onSurface.copy(alpha = 0.78f))
            }
          }

          when (cur) {
            OnboardingStep.INTRO -> {
              GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  GestureHintRow(Icons.Rounded.SwipeUp, "Swipe Up — App Drawer", "Instant search, categories, folders & Manage Apps.")
                  GestureHintRow(Icons.Rounded.SwipeDown, "Swipe Down — Control Center", "10 customizable glass tiles, brightness, volume & live notifications.")
                  GestureHintRow(Icons.Rounded.SwipeLeft, "Swipe Left — Zero Screen", "AI Architect workspace, screen wellness, storage & isolated web apps.")
                  GestureHintRow(Icons.Rounded.AutoAwesome, "Gemini AI Architect", "Chat Q&A & theme recommendations backed by Firebase AI Logic + offline fallback.")
                }
              }
            }

            OnboardingStep.THEME -> {
              state.allThemes.take(6).forEach { candidate ->
                val active = candidate.id == theme.id
                Surface(
                  onClick = { onActivateTheme(candidate) },
                  shape = RoundedCornerShape(18.dp),
                  color = if (active) primary.copy(alpha = 0.22f) else Color(theme.palette.surfaceHex).copy(alpha = 0.55f),
                  border = BorderStroke(if (active) 2.dp else 1.dp, if (active) primary else Color.White.copy(alpha = 0.2f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(candidate.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = onSurface)
                      Text(candidate.description, style = MaterialTheme.typography.bodySmall, color = onSurface.copy(alpha = 0.75f), maxLines = 2)
                    }
                    Spacer(Modifier.width(8.dp))
                    PaletteSwatchRow(candidate.palette)
                  }
                }
              }
            }

            OnboardingStep.LAYOUT -> {
              GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Text("Home Grid Columns (${state.settings.gridColumns})", style = MaterialTheme.typography.labelLarge, color = onSurface)
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4, 5, 6).forEach { cols ->
                      FilterChip(
                        selected = state.settings.gridColumns == cols,
                        onClick = { onUpdateSettings { it.copy(gridColumns = cols) } },
                        label = { Text("$cols Cols") },
                        modifier = Modifier.weight(1f),
                      )
                    }
                  }
                  Text("Icon Material Style", style = MaterialTheme.typography.labelLarge, color = onSurface)
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                      LauncherIconStyle.LIQUID_GLASS,
                      LauncherIconStyle.COLOROS,
                      LauncherIconStyle.GLASS,
                    ).forEach { st ->
                      FilterChip(
                        selected = state.settings.iconStyle == st,
                        onClick = { onUpdateSettings { it.copy(iconStyle = st) } },
                        label = { Text(st.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f),
                      )
                    }
                  }
                  SettingsSwitchRow("Enable Dynamic Capsule Bar", state.settings.dynamicCapsuleEnabled) { c ->
                    onUpdateSettings { it.copy(dynamicCapsuleEnabled = c) }
                  }
                  SettingsSwitchRow("Enable Swipe-Left Zero Screen", state.settings.zeroScreenEnabled) { c ->
                    onUpdateSettings { it.copy(zeroScreenEnabled = c) }
                  }
                }
              }
            }

            OnboardingStep.DEFAULT_HOME -> {
              GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Text(
                    text =
                      if (state.isDefaultHome) "Aether Glass is currently set as your Default HOME launcher ✓"
                      else "Press Home cleanly by setting Aether Glass as your Default HOME app. You can also preview everything first without changing it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurface,
                  )
                  Button(
                    onClick = {
                      runCatching { context.startActivity(LauncherSystemBridge.buildDefaultHomeIntent(context)) }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                  ) {
                    Icon(Icons.Rounded.Home, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isDefaultHome) "Default HOME Active ✓" else "Choose Default HOME App")
                  }
                }
              }
            }

            OnboardingStep.PERMISSIONS -> {
              val snap = state.controlCenterSnapshot
              GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  PermissionOptInRow(
                    title = "Notification & Media Stream",
                    desc = "Shows real Android notifications & active music inside Control Center and Dynamic Capsule.",
                    granted = snap.notificationAccessGranted,
                    onRequest = { NotificationStreamHub.openNotificationAccessSettings(context) },
                  )
                  HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                  PermissionOptInRow(
                    title = "Modify System Settings",
                    desc = "Lets Control Center adjust screen brightness and auto-rotate directly.",
                    granted = snap.canWriteSystemSettings,
                    onRequest = { ControlCenterSystemController.openWriteSettingsPermission(context) },
                  )
                  HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                  PermissionOptInRow(
                    title = "Digital Wellness Usage Access",
                    desc = "Displays daily device screen-time summary in Zero Screen & Wellness widget.",
                    granted = snap.usageStatsGranted,
                    onRequest = { ControlCenterSystemController.openUsageAccessSettings(context) },
                  )
                }
              }
            }

            OnboardingStep.READY -> {
              GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text("Active Theme: ${theme.name}", style = MaterialTheme.typography.titleMedium, color = primary, fontWeight = FontWeight.Bold)
                  Text("Grid: ${state.settings.gridColumns}×${state.settings.gridRows} • Icons: ${state.settings.iconStyle.label}", style = MaterialTheme.typography.bodyMedium, color = onSurface)
                  Text("Tier: ${state.renderRules.tier.label} • Form Factor: ${state.deviceCapabilities.formFactor.label}", style = MaterialTheme.typography.bodySmall, color = onSurface.copy(alpha = 0.78f))
                  Spacer(Modifier.height(4.dp))
                  Text("Tip: You can revisit this Welcome Setup anytime from Section 17 (About Aether Glass OS).", style = MaterialTheme.typography.labelSmall, color = secondary)
                }
              }
            }
          }
        }
      }

      // Bottom Stepper Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (stepIndex > 0) {
          OutlinedButton(
            onClick = { stepIndex-- },
            modifier = Modifier.weight(1f).heightIn(min = 50.dp),
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Back")
          }
        }
        Button(
          onClick = {
            if (stepIndex < steps.lastIndex) stepIndex++ else onComplete()
          },
          modifier = Modifier.weight(1.4f).heightIn(min = 50.dp).testTag("onboarding_next_btn"),
        ) {
          Text(if (stepIndex < steps.lastIndex) "Continue" else "Enter Aether Glass OS")
          Spacer(Modifier.width(6.dp))
          Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
        }
      }
    }
  }
}

@Composable
private fun GestureHintRow(icon: ImageVector, title: String, detail: String) {
  Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    Column {
      Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun PermissionOptInRow(
  title: String,
  desc: String,
  granted: Boolean,
  onRequest: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (granted) {
      AssistChip(
        onClick = {},
        label = { Text("Active ✓") },
      )
    } else {
      FilledTonalButton(onClick = onRequest, modifier = Modifier.heightIn(min = 44.dp)) {
        Text("Grant")
      }
    }
  }
}
