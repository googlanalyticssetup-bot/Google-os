package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import com.example.engine.WallpaperTarget
import com.example.ui.ThemeStudioUiState
import com.example.ui.components.*

private val PromptPresets =
  listOf(
    "Purple glass AMOLED black glowing icons transparent widgets",
    "Luxury black and gold editorial serif 3D glass",
    "Cyberpunk neon cyan magenta grid futuristic HUD",
    "Pastel lavender frosted minimal tablet theme",
    "Emerald nature aurora liquid glass calm animations",
    "ColorOS 7 inspired teal rounded glass minimal",
  )

@Composable
fun AiStudioScreen(
  state: ThemeStudioUiState,
  onPromptChanged: (String) -> Unit,
  onGenerate: (String?) -> Unit,
  onCancelGenerate: () -> Unit,
  onSaveToLibrary: () -> Unit,
  onActivateTheme: () -> Unit,
  onApplyWallpaper: (WallpaperTarget) -> Unit,
  onOptimizeForDevice: () -> Unit,
  onOpenStudios: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showWallpaperPicker by remember { mutableStateOf(false) }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWide = maxWidth >= 720.dp

    if (isWide) {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        Column(
          modifier = Modifier.weight(1.05f).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          AiPromptSection(
            state = state,
            onPromptChanged = onPromptChanged,
            onGenerate = onGenerate,
            onCancelGenerate = onCancelGenerate,
            onOptimizeForDevice = onOptimizeForDevice,
          )
        }
        Column(
          modifier = Modifier.weight(0.95f).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          AiPreviewAndActionsSection(
            state = state,
            onSaveToLibrary = onSaveToLibrary,
            onActivateTheme = onActivateTheme,
            onOpenWallpaperSheet = { showWallpaperPicker = true },
            onOpenStudios = onOpenStudios,
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        AiPromptSection(
          state = state,
          onPromptChanged = onPromptChanged,
          onGenerate = onGenerate,
          onCancelGenerate = onCancelGenerate,
          onOptimizeForDevice = onOptimizeForDevice,
        )
        AiPreviewAndActionsSection(
          state = state,
          onSaveToLibrary = onSaveToLibrary,
          onActivateTheme = onActivateTheme,
          onOpenWallpaperSheet = { showWallpaperPicker = true },
          onOpenStudios = onOpenStudios,
        )
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  if (showWallpaperPicker) {
    AlertDialog(
      onDismissRequest = { showWallpaperPicker = false },
      title = { Text("Apply Procedural Wallpaper") },
      text = {
        Text(
          "Generates a ${state.renderRules.maxWallpaperLongSidePx}px ${state.draftTheme.wallpaper.style.label} wallpaper locally and applies it via Android WallpaperManager.",
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showWallpaperPicker = false
            onApplyWallpaper(WallpaperTarget.BOTH)
          },
          modifier = Modifier.testTag("apply_wp_both_btn"),
        ) {
          Text("Home & Lock")
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TextButton(
            onClick = {
              showWallpaperPicker = false
              onApplyWallpaper(WallpaperTarget.HOME)
            },
          ) {
            Text("Home Only")
          }
          TextButton(
            onClick = {
              showWallpaperPicker = false
              onApplyWallpaper(WallpaperTarget.LOCK)
            },
          ) {
            Text("Lock Only")
          }
        }
      },
    )
  }
}

@Composable
private fun AiPromptSection(
  state: ThemeStudioUiState,
  onPromptChanged: (String) -> Unit,
  onGenerate: (String?) -> Unit,
  onCancelGenerate: () -> Unit,
  onOptimizeForDevice: () -> Unit,
) {
  StudioTopHeader(
    title = "Universal AI Studio",
    subtitle = "On-device prompt interpreter • Phone & Tablet",
    rules = state.renderRules,
  )

  OfflinePrivacyPill()

  GlassSurfaceCard(
    glass = state.draftTheme.glass,
    palette = state.draftTheme.palette,
    rules = state.renderRules,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = "Describe your complete theme",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = state.aiPrompt,
      onValueChange = onPromptChanged,
      placeholder = {
        Text("e.g. Purple glass theme with AMOLED black wallpaper and glowing icons...")
      },
      minLines = 3,
      maxLines = 5,
      modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input"),
      shape = RoundedCornerShape(16.dp),
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      PromptPresets.forEachIndexed { index, preset ->
        AssistChip(
          onClick = { onGenerate(preset) },
          label = { Text(preset.take(30) + if (preset.length > 30) "…" else "") },
          modifier = Modifier.testTag("prompt_chip_$index"),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Button(
        onClick = { onGenerate(null) },
        enabled = !state.isGenerating,
        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("generate_theme_btn"),
      ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (state.isGenerating) "Synthesizing..." else "Generate Offline")
      }
      if (state.isGenerating) {
        OutlinedButton(
          onClick = onCancelGenerate,
          modifier = Modifier.heightIn(min = 48.dp).testTag("cancel_generate_btn"),
        ) {
          Text("Cancel")
        }
      } else {
        OutlinedButton(
          onClick = onOptimizeForDevice,
          modifier = Modifier.heightIn(min = 48.dp).testTag("optimize_theme_btn"),
        ) {
          Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Optimise")
        }
      }
    }
  }

  AnimatedVisibility(visible = state.lastAiReport != null) {
    state.lastAiReport?.let { report ->
      GlassSurfaceCard(
        glass = state.draftTheme.glass,
        palette = state.draftTheme.palette,
        rules = state.renderRules,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Local AI Design Breakdown",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          PaletteSwatchRow(report.theme.palette)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Styles: ${report.detectedStyles.joinToString(" • ")}",
          style = MaterialTheme.typography.labelLarge,
          color = Color(report.theme.palette.primaryHex),
        )
        Spacer(modifier = Modifier.height(6.dp))
        report.rationale.forEach { bullet ->
          Text(
            text = "• $bullet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = report.optimizationNote,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.secondary,
        )
      }
    }
  }
}

@Composable
private fun AiPreviewAndActionsSection(
  state: ThemeStudioUiState,
  onSaveToLibrary: () -> Unit,
  onActivateTheme: () -> Unit,
  onOpenWallpaperSheet: () -> Unit,
  onOpenStudios: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = state.draftTheme.name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text =
          "${state.draftTheme.glass.style.label} • ${state.draftTheme.icons.style.label} Icons • ${state.draftTheme.fontStyle.label}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    PaletteSwatchRow(state.draftTheme.palette)
  }

  ThemeDevicePreview(
    theme = state.draftTheme,
    previewBitmap = state.previewBitmap,
    rules = state.renderRules,
    isAppForeground = state.isAppForeground,
  )

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Button(
      onClick = onActivateTheme,
      modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("activate_theme_btn"),
    ) {
      Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text("Activate")
    }
    OutlinedButton(
      onClick = onOpenWallpaperSheet,
      enabled = !state.isApplyingWallpaper,
      modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("apply_wallpaper_btn"),
    ) {
      Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text(if (state.isApplyingWallpaper) "Applying..." else "Set Wallpaper")
    }
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    OutlinedButton(
      onClick = onSaveToLibrary,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("save_library_btn"),
    ) {
      Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text("Save Offline")
    }
    OutlinedButton(
      onClick = onOpenStudios,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("open_studios_btn"),
    ) {
      Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text("Fine-Tune")
    }
  }
}
