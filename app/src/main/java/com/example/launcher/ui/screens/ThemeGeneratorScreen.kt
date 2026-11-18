package com.example.launcher.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.window.Dialog
import com.example.ai.AiRecommendedTheme
import com.example.domain.model.ThemeSpec
import com.example.launcher.ui.LauncherUiState
import com.example.ui.components.*
import java.util.Locale

private val ThemeGeneratorInspirations =
  listOf(
    "Aether liquid obsidian glass with warm amber highlights and frost sheen",
    "ColorOS 7 frosted cyan and luminous violet glass with deep obsidian dock",
    "Emerald forest aurora with liquid glass icons and calm pastel accents",
    "Cyberpunk neon rose and electric cyan high-contrast grid",
    "Minimal porcelain white daylight with crisp cobalt highlights",
  )

private val QuickAiQuestions =
  listOf(
    "Recommend a battery-friendly liquid glass setup for my device",
    "How do Blur, Transparency & Shadows interact in Glass FX?",
    "Suggest a crisp daytime pastel layout vs AMOLED night look",
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeGeneratorScreen(
  state: LauncherUiState,
  onPromptChange: (String) -> Unit,
  onGeneratePalette: (String?) -> Unit,
  onGenerateGeminiRecommendations: (String?) -> Unit,
  onGenerateOfflineRecommendations: (String?) -> Unit,
  onAskAiQuestion: (String) -> Unit,
  onApplyThemeSpec: (ThemeSpec, Boolean) -> Unit,
  onApplyPalette: (Boolean) -> Unit,
  onExtractFromWallpaper: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onBack() }

  val generated = state.themeGenResult
  val previewTheme = generated?.theme ?: state.activeTheme
  val ai = state.aiArchitect

  var questionDraft by remember { mutableStateOf("") }
  var previewingRecommendation by remember { mutableStateOf<AiRecommendedTheme?>(null) }

  Scaffold(
    containerColor = Color.Transparent,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Aether AI Architect")
            Text(
              text = "Gemini 3.8 Flash • Offline Algorithmic Engine",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        },
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

      // 1. Gemini Theme Recommendations & Onboard Theme Synthesis Card
      GlassSurfaceCard(
        glass = previewTheme.glass,
        palette = previewTheme.palette,
        rules = state.renderRules,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("AI Theme Designer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
          text =
            "Generate 3 personalized Liquid Glass themes via Gemini 3.8 Flash (Firebase AI Logic) or synthesize palettes instantly offline.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
          value = state.themeGenPrompt,
          onValueChange = onPromptChange,
          label = { Text("Theme Description or Vibe") },
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
            enabled = !ai.isGeneratingThemes,
            onClick = { onGenerateGeminiRecommendations(null) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("ai_gemini_recs_btn"),
          ) {
            if (ai.isGeneratingThemes) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
              Spacer(Modifier.width(8.dp))
              Text("Designing…")
            } else {
              Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Ask Gemini (3)")
            }
          }
          FilledTonalButton(
            onClick = { onGenerateOfflineRecommendations(null) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("themegen_generate_btn"),
          ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Offline 3×")
          }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
          onClick = onExtractFromWallpaper,
          modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("themegen_from_wp_btn"),
        ) {
          Icon(Icons.Rounded.Colorize, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Extract Harmony Palette from Current Wallpaper")
        }

        ai.errorMessage?.let { err ->
          Spacer(Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(err, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
              TextButton(onClick = { onGenerateOfflineRecommendations(null) }) {
                Text("Use Offline")
              }
            }
          }
        }
      }

      // 2. AI Recommended Themes List (Gemini or Offline Fallback)
      if (ai.recommendations.isNotEmpty()) {
        ai.overviewMessage?.let { summary ->
          Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp),
          )
        }
        ai.recommendations.forEachIndexed { idx, rec ->
          GlassSurfaceCard(
            glass = rec.theme.glass,
            palette = rec.theme.palette,
            rules = state.renderRules,
            modifier = Modifier.fillMaxWidth().testTag("ai_rec_card_$idx"),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(rec.theme.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  Text(rec.source, style = MaterialTheme.typography.labelSmall, color = Color(rec.theme.palette.primaryHex))
                }
                PaletteSwatchRow(rec.theme.palette)
              }
              Text(rec.rationale, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                  onClick = { previewingRecommendation = rec },
                  modifier = Modifier.heightIn(min = 44.dp),
                ) {
                  Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Preview")
                }
                Button(
                  onClick = { onApplyThemeSpec(rec.theme, false) },
                  modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                ) {
                  Text("Apply Theme")
                }
                FilledTonalButton(
                  onClick = { onApplyThemeSpec(rec.theme, true) },
                  modifier = Modifier.heightIn(min = 44.dp),
                ) {
                  Text("+ WP")
                }
              }
            }
          }
        }
      }

      // 3. Natural-Language AI Architect Q&A (Streaming Gemini 3.8 Flash)
      GlassSurfaceCard(
        glass = previewTheme.glass,
        palette = previewTheme.palette,
        rules = state.renderRules,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Ask AI Architect (Q&A & Design Advice)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            QuickAiQuestions.forEach { q ->
              AssistChip(
                onClick = { onAskAiQuestion(q) },
                label = { Text(q.take(28) + "…", style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
              value = questionDraft,
              onValueChange = { questionDraft = it },
              placeholder = { Text("Ask about themes, gestures, battery, Glass FX…") },
              singleLine = true,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.weight(1f).testTag("ai_question_input"),
            )
            Button(
              enabled = questionDraft.isNotBlank() && !ai.isStreamingAnswer,
              onClick = {
                onAskAiQuestion(questionDraft)
                questionDraft = ""
              },
              modifier = Modifier.heightIn(min = 48.dp).testTag("ai_ask_btn"),
            ) {
              Icon(Icons.Rounded.Send, contentDescription = "Ask Gemini")
            }
          }

          if (ai.chatTurns.isNotEmpty()) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            ai.chatTurns.takeLast(6).forEach { turn ->
              Surface(
                shape = RoundedCornerShape(14.dp),
                color =
                  if (turn.isUser) Color(previewTheme.palette.primaryHex).copy(alpha = 0.18f)
                  else if (turn.isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                  else Color.White.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = if (turn.isUser) "You" else "Aether AI (${turn.sourceLabel})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(previewTheme.palette.primaryHex),
                    fontWeight = FontWeight.Bold,
                  )
                  Text(turn.text, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }
      }

      // 4. Instant Algorithmic Palette Preview Card
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
              Text(previewTheme.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PaletteTokenSwatch("Primary", generated.primaryHex)
            PaletteTokenSwatch("Secondary", generated.secondaryHex)
            PaletteTokenSwatch("Tertiary", generated.tertiaryHex)
            PaletteTokenSwatch("Surface", generated.surfaceHex)
            PaletteTokenSwatch("Base", generated.backgroundHex)
          }

          Spacer(Modifier.height(14.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
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
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = { onApplyPalette(false) },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("themegen_apply_btn"),
            ) {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Apply Palette")
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

  previewingRecommendation?.let { rec ->
    Dialog(onDismissRequest = { previewingRecommendation = null }) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(rec.theme.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          ThemeDevicePreview(
            theme = rec.theme,
            previewBitmap = null,
            rules = state.renderRules,
            isAppForeground = state.isForeground,
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { previewingRecommendation = null }, modifier = Modifier.weight(1f)) {
              Text("Close")
            }
            Button(
              onClick = {
                onApplyThemeSpec(rec.theme, false)
                previewingRecommendation = null
              },
              modifier = Modifier.weight(1f),
            ) {
              Text("Apply Theme")
            }
          }
        }
      }
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
      text = String.format(Locale.US, "#%06X", (hex and 0xFFFFFF)),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
