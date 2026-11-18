package com.example.launcher.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.launcher.ui.LauncherUiState
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.OfflinePrivacyPill
import com.example.ui.components.SampleStudioIcons
import com.example.ui.components.ThemedVectorAppIcon
import java.util.Locale

private val ThemeGeneratorInspirations =
  listOf(
    "ColorOS 7 frosted cyan and luminous violet glass with deep obsidian dock",
    "Sunset amber gold luxury with warm charcoal AMOLED background",
    "Emerald forest aurora with liquid glass icons and calm pastel accents",
    "Cyberpunk neon rose and electric cyan high-contrast grid",
    "Minimal porcelain white daylight with crisp cobalt highlights",
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeGeneratorScreen(
  state: LauncherUiState,
  onPromptChange: (String) -> Unit,
  onGeneratePalette: (String?) -> Unit,
  onApplyPalette: (Boolean) -> Unit,
  onExtractFromWallpaper: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val generated = state.themeGenResult
  val previewTheme = generated?.theme ?: state.activeTheme

  Scaffold(
    containerColor = Color.Transparent,
    topBar = {
      TopAppBar(
        title = { Text("ThemeGenerator") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("themegen_back_btn")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Home")
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
          .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      OfflinePrivacyPill()

      GlassSurfaceCard(
        glass = previewTheme.glass,
        palette = previewTheme.palette,
        rules = state.renderRules,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = "Onboard Algorithmic Palette Generator",
          style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text =
            "Describe a mood, colour combination, or visual style. Our local HSL color-harmony & contrast engine synthesizes a complete launcher palette offline in milliseconds.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
          value = state.themeGenPrompt,
          onValueChange = onPromptChange,
          label = { Text("Theme Description") },
          placeholder = { Text("e.g. Emerald aurora glass with dark AMOLED dock...") },
          minLines = 2,
          maxLines = 4,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().testTag("themegen_prompt_input"),
        )

        Spacer(Modifier.height(8.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ThemeGeneratorInspirations.forEachIndexed { idx, prompt ->
            AssistChip(
              onClick = { onGeneratePalette(prompt) },
              label = { Text(prompt.take(26) + "…") },
              modifier = Modifier.testTag("themegen_chip_$idx"),
            )
          }
        }

        Spacer(Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { onGeneratePalette(null) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("themegen_generate_btn"),
          ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Generate Palette")
          }
          OutlinedButton(
            onClick = onExtractFromWallpaper,
            modifier = Modifier.heightIn(min = 48.dp).testTag("themegen_from_wp_btn"),
          ) {
            Icon(Icons.Rounded.Colorize, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("From Wallpaper")
          }
        }
      }

      if (generated != null) {
        GlassSurfaceCard(
          glass = previewTheme.glass,
          palette = previewTheme.palette,
          rules = state.renderRules,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = previewTheme.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text =
                  "${generated.harmonyStrategy} • Contrast ${String.format(Locale.US, "%.1f", generated.contrastRatio)}:1",
                style = MaterialTheme.typography.labelSmall,
                color = Color(generated.primaryHex),
              )
            }
            Surface(
              shape = RoundedCornerShape(50),
              color = Color(generated.primaryHex).copy(alpha = 0.2f),
              border = BorderStroke(1.dp, Color(generated.primaryHex).copy(alpha = 0.5f)),
            ) {
              Text(
                text = generated.keywordsMatched.joinToString(", "),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }

          Spacer(Modifier.height(12.dp))

          // Generated 6-token Palette Swatches
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            PaletteTokenSwatch("Primary", generated.primaryHex)
            PaletteTokenSwatch("Secondary", generated.secondaryHex)
            PaletteTokenSwatch("Tertiary", generated.tertiaryHex)
            PaletteTokenSwatch("Surface", generated.surfaceHex)
            PaletteTokenSwatch("Base", generated.backgroundHex)
          }

          Spacer(Modifier.height(14.dp))

          // Live Dock & Icon Style Preview inside ThemeGenerator
          Text("Launcher Icon & Glass Preview", style = MaterialTheme.typography.labelLarge)
          Spacer(Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            SampleStudioIcons.take(4).forEach { sample ->
              ThemedVectorAppIcon(
                appIcon = sample,
                config = previewTheme.icons,
                palette = previewTheme.palette,
                rules = state.renderRules,
                size = 44.dp,
              )
            }
          }

          Spacer(Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = { onApplyPalette(false) },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("themegen_apply_btn"),
            ) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Apply to Launcher")
            }
            OutlinedButton(
              onClick = { onApplyPalette(true) },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("themegen_apply_wp_btn"),
            ) {
              Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Apply + Wallpaper")
            }
          }
        }
      }

      Spacer(Modifier.height(24.dp))
    }
  }
}

@Composable
private fun PaletteTokenSwatch(label: String, hex: Long) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier =
        Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(Color(hex))
          .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape),
    )
    Spacer(Modifier.height(4.dp))
    Text(label, style = MaterialTheme.typography.labelSmall)
    Text(
      text = String.format("#%06X", (hex and 0xFFFFFF)),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
