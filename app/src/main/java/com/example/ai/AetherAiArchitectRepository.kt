package com.example.ai

import android.util.Log
import com.example.domain.model.*
import com.example.engine.LocalAiThemeEngine
import com.example.launcher.engine.ThemeGeneratorEngine
import com.example.launcher.model.LauncherSettings
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AetherAiArchitect"

data class AiRecommendedTheme(
  val theme: ThemeSpec,
  val rationale: String,
  val source: String,
)

data class AiArchitectChatTurn(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val isError: Boolean = false,
  val sourceLabel: String = "Gemini",
)

sealed interface AiGenerationResult {
  data class Success(
    val recommendations: List<AiRecommendedTheme>,
    val overview: String,
  ) : AiGenerationResult

  data class Error(val message: String) : AiGenerationResult
}

class AetherAiArchitectRepository {

  private val recommendationSchema =
    Schema.obj(
      properties =
        mapOf(
          "architectSummary" to
            Schema.string("Helpful explanation of how these themes fit the request and device."),
          "recommendations" to
            Schema.array(
              items =
                Schema.obj(
                  properties =
                    mapOf(
                      "name" to Schema.string("Short evocative 2-3 word theme name"),
                      "rationale" to Schema.string("1-2 sentence contextual design explanation"),
                      "primaryHex" to Schema.string("Hex color like #38BDF8"),
                      "secondaryHex" to Schema.string("Hex color like #A855F7"),
                      "tertiaryHex" to Schema.string("Hex color like #F59E0B"),
                      "backgroundHex" to Schema.string("Deep obsidian/dark or light hex like #060810"),
                      "surfaceHex" to Schema.string("Glass card surface hex like #12172A"),
                      "isAmoledDark" to Schema.boolean("True for dark/AMOLED themes, false for light"),
                      "wallpaperStyle" to
                        Schema.enumeration(WallpaperStyle.entries.map { it.name }),
                      "glassStyle" to Schema.enumeration(GlassStyle.entries.map { it.name }),
                      "iconStyle" to Schema.enumeration(IconStyle.entries.map { it.name }),
                      "clockStyle" to Schema.enumeration(ClockStyle.entries.map { it.name }),
                      "fontStyle" to Schema.enumeration(FontStyleOption.entries.map { it.name }),
                    ),
                ),
              description = "2 or 3 distinct curated Aether Glass OS theme recommendations",
            ),
        ),
    )

  private val chatModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      systemInstruction =
        content {
          text(
            "You are Aether AI Architect inside Aether Glass OS, an Android Liquid Glass launcher. " +
              "Answer questions clearly and concisely about launcher setup, themes, glassmorphism FX, " +
              "battery-conscious animations, gestures, Control Center customization, and privacy. " +
              "Do not invent unsupported Android permissions or claim root capabilities.",
          )
        },
    )
  }

  private val recommendationModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = recommendationSchema
        },
    )
  }

  fun streamQuestion(
    question: String,
    settings: LauncherSettings,
    activeTheme: ThemeSpec,
    capabilities: DeviceCapabilities,
  ): Flow<String> = flow {
    val contextPrompt =
      buildString {
        appendLine("Current Launcher Context:")
        appendLine("- Active Theme: ${activeTheme.name} (${activeTheme.category.label})")
        appendLine("- Hardware Tier: ${capabilities.recommendedTier.label}, ${capabilities.availableRamMb}/${capabilities.totalRamMb} MB RAM, ${capabilities.refreshRateHz}Hz, ${capabilities.formFactor.label}")
        appendLine("- Grid: ${settings.gridColumns}x${settings.gridRows}, Dock: ${settings.dockStyle.label}, Icon Style: ${settings.iconStyle.label}")
        appendLine("- Glass FX: blur=${(settings.glassBlurStrength * 100).toInt()}%, opacity=${(settings.glassTransparency * 100).toInt()}%")
        appendLine()
        append("User Question: $question")
      }

    val accumulated = StringBuilder()
    withTimeout(60_000L) {
      chatModel.generateContentStream(contextPrompt).collect { chunk ->
        val part = chunk.text.orEmpty()
        if (part.isNotEmpty()) {
          accumulated.append(part)
          emit(accumulated.toString())
        }
      }
    }
    if (accumulated.isBlank()) {
      throw IllegalStateException("Gemini returned an empty response.")
    }
  }

  suspend fun generateGeminiThemeRecommendations(
    prompt: String,
    tier: ResolvedTier,
    formFactor: DeviceFormFactor,
  ): AiGenerationResult {
    return try {
      val instruction =
        "Design 3 distinct, cohesive Aether Glass OS themes inspired by: \"$prompt\". " +
          "Target device tier: ${tier.label}, form factor: ${formFactor.label}. " +
          "Ensure strong text contrast on surfaceHex and harmonious Liquid Glass accents."
      val response =
        withTimeout(60_000L) {
          recommendationModel.generateContent(instruction)
        }
      val jsonStr = response.text ?: throw IllegalStateException("Model returned empty response.")
      val root = JSONObject(jsonStr)
      val summary = root.optString("architectSummary", "Curated themes synthesized by Gemini.")
      val recsArray = root.getJSONArray("recommendations")
      val baseTimestamp = System.currentTimeMillis()
      val recs =
        (0 until recsArray.length()).map { idx ->
          val obj = recsArray.getJSONObject(idx)
          val name = obj.optString("name", "Aether Glass ${idx + 1}").trim().ifBlank { "Aether Glass ${idx + 1}" }
          val rationale = obj.optString("rationale", "Balanced contrast and translucent depth.")
          val primary = parseHexColor(obj.optString("primaryHex"), 0xFF38BDF8L)
          val secondary = parseHexColor(obj.optString("secondaryHex"), 0xFFA855F7L)
          val tertiary = parseHexColor(obj.optString("tertiaryHex"), 0xFFF59E0BL)
          val isDark = obj.optBoolean("isAmoledDark", true)
          val bg = parseHexColor(obj.optString("backgroundHex"), if (isDark) 0xFF05070FL else 0xFFF4F6FBL)
          val surface = parseHexColor(obj.optString("surfaceHex"), if (isDark) 0xFF101526L else 0xFFFFFFFFL)
          val onSurface = if (isDark) 0xFFF8FAFCL else 0xFF0F172AL

          val wpStyle = enumOr(obj.optString("wallpaperStyle"), WallpaperStyle.GLASS_RIBBON)
          val glassStyle = enumOr(obj.optString("glassStyle"), GlassStyle.LIQUID)
          val iconStyle = enumOr(obj.optString("iconStyle"), IconStyle.GLASS)
          val clockStyle = enumOr(obj.optString("clockStyle"), ClockStyle.MINIMAL_DIGITAL)
          val fontStyle = enumOr(obj.optString("fontStyle"), FontStyleOption.SPACE_GROTESK)

          val baseSpec =
            ThemeSpec(
              id = "gemini_${baseTimestamp}_$idx",
              name = name,
              description = rationale,
              category = if (isDark) ThemeCategory.GLASS else ThemeCategory.MINIMAL,
              isBuiltIn = false,
              promptSource = prompt,
              palette =
                ThemePalette(
                  primaryHex = primary,
                  secondaryHex = secondary,
                  tertiaryHex = tertiary,
                  backgroundHex = bg,
                  surfaceHex = surface,
                  accentGlowHex = primary,
                  onSurfaceHex = onSurface,
                  isAmoledDark = isDark,
                ),
              wallpaper =
                WallpaperConfig(
                  style = wpStyle,
                  aspectMode =
                    if (formFactor == DeviceFormFactor.TABLET) WallpaperAspectMode.TABLET_WIDE
                    else WallpaperAspectMode.PORTRAIT,
                  seed = baseTimestamp + idx * 97L,
                ),
              glass =
                GlassConfig(
                  style = glassStyle,
                  transparency = if (isDark) 0.24f else 0.62f,
                  blurStrength = 0.72f,
                  glowIntensity = 0.60f,
                  shadowDepth = 0.55f,
                  reflectionOpacity = 0.45f,
                ),
              icons =
                IconPackConfig(
                  style = iconStyle,
                  glowEnabled = isDark,
                ),
              lockScreen = LockScreenConfig(clockStyle = clockStyle),
              controlCenter = ControlCenterConfig(),
              widget = WidgetConfig(),
              fontStyle = fontStyle,
            )
          val optimized = LocalAiThemeEngine.optimizeThemeForDevice(baseSpec, tier)
          AiRecommendedTheme(
            theme = optimized,
            rationale = rationale,
            source = "Gemini 3.8 Flash",
          )
        }
      if (recs.isEmpty()) {
        AiGenerationResult.Error("Gemini returned no theme recommendations.")
      } else {
        AiGenerationResult.Success(recommendations = recs, overview = summary)
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Gemini recommendation timed out", e)
      AiGenerationResult.Error("Gemini request timed out after 60s. Check connectivity or use Onboard Engine.")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Gemini recommendation failed", e)
      AiGenerationResult.Error(e.localizedMessage ?: "Unable to reach Gemini AI Logic right now.")
    }
  }

  fun generateOfflineFallbackRecommendations(
    prompt: String,
    tier: ResolvedTier,
    formFactor: DeviceFormFactor,
  ): List<AiRecommendedTheme> {
    val clean = prompt.trim().ifEmpty { "Deep obsidian liquid glass with warm amber glow" }
    val variants =
      listOf(
        clean to "Primary algorithmic HSL harmony matching your prompt cues.",
        "$clean frosted minimal porcelain glass" to "High-legibility balanced glass variant.",
        "$clean AMOLED obsidian neon glow" to "Battery-conscious true black AMOLED contrast variant.",
      )
    return variants.mapIndexed { idx, (desc, note) ->
      val res = ThemeGeneratorEngine.generatePaletteFromDescription(desc, tier, formFactor)
      AiRecommendedTheme(
        theme = res.theme.copy(id = "offline_${System.currentTimeMillis()}_$idx"),
        rationale = "$note (${res.harmonyStrategy}, contrast ${String.format(Locale.US, "%.1f", res.contrastRatio)}:1)",
        source = "Onboard Engine (Offline)",
      )
    }
  }

  private fun parseHexColor(raw: String, fallback: Long): Long {
    val clean = raw.trim().removePrefix("#").uppercase(Locale.US)
    return runCatching {
      when (clean.length) {
        6 -> (0xFF000000L or clean.toLong(16))
        8 -> clean.toLong(16)
        else -> fallback
      }
    }.getOrDefault(fallback)
  }

  private inline fun <reified T : Enum<T>> enumOr(raw: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: fallback
}
