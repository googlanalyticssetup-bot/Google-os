package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.theme.*

object FluidMotionSpecs {
  fun <T> silkySpring(speedMultiplier: Float = 1.0f) =
    spring<T>(
      dampingRatio = 0.78f,
      stiffness = (340f * speedMultiplier.coerceIn(0.5f, 1.8f)),
    )

  fun <T> crispBounceSpring(speedMultiplier: Float = 1.0f) =
    spring<T>(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = (420f * speedMultiplier.coerceIn(0.5f, 1.8f)),
    )

  fun offsetSpring(speedMultiplier: Float = 1.0f) =
    spring<IntOffset>(
      dampingRatio = 0.82f,
      stiffness = (360f * speedMultiplier.coerceIn(0.5f, 1.8f)),
    )
}

@Composable
fun GlassSurfaceCard(
  glass: GlassConfig,
  palette: ThemePalette,
  rules: EffectiveRenderRules,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = glass.cornerRadiusDp.dp,
  contentPadding: Dp = 16.dp,
  content: @Composable ColumnScope.() -> Unit,
) {
  val primary = Color(palette.primaryHex)
  val secondary = Color(palette.secondaryHex)
  val surface = Color(palette.surfaceHex)
  val shape = RoundedCornerShape(cornerRadius)

  val effectiveAlpha =
    if (rules.tier == ResolvedTier.LITE) {
      (glass.transparency + 0.45f).coerceIn(0.65f, 0.92f)
    } else {
      glass.transparency.coerceIn(0.08f, 0.88f)
    }

  val borderBrush =
    if (rules.enableReflections) {
      Brush.linearGradient(
        colors =
          listOf(
            Color.White.copy(alpha = (0.16f + glass.reflectionOpacity * 0.48f).coerceIn(0.1f, 0.72f)),
            primary.copy(alpha = (0.14f + glass.glowIntensity * 0.36f).coerceIn(0.1f, 0.58f)),
            secondary.copy(alpha = 0.14f),
          ),
      )
    } else {
      Brush.linearGradient(listOf(primary.copy(alpha = 0.28f), primary.copy(alpha = 0.14f)))
    }

  val bgBrush =
    when (glass.style) {
      GlassStyle.LIQUID ->
        Brush.linearGradient(
          listOf(
            surface.copy(alpha = effectiveAlpha),
            primary.copy(alpha = effectiveAlpha * 0.40f),
            secondary.copy(alpha = effectiveAlpha * 0.30f),
          ),
        )
      GlassStyle.CRYSTAL ->
        Brush.linearGradient(
          listOf(
            surface.copy(alpha = effectiveAlpha * 0.82f),
            Color.White.copy(alpha = 0.09f),
            surface.copy(alpha = effectiveAlpha),
          ),
        )
      GlassStyle.TRANSPARENT ->
        Brush.verticalGradient(
          listOf(
            Color.White.copy(alpha = (effectiveAlpha * 0.45f).coerceIn(0.05f, 0.28f)),
            surface.copy(alpha = (effectiveAlpha * 0.65f).coerceIn(0.08f, 0.38f)),
          ),
        )
      else ->
        Brush.verticalGradient(
          listOf(
            surface.copy(alpha = (effectiveAlpha + 0.08f).coerceAtMost(0.95f)),
            surface.copy(alpha = effectiveAlpha),
          ),
        )
    }

  val shadowModifier =
    if (rules.tier != ResolvedTier.LITE && glass.shadowDepth > 0.20f) {
      Modifier.shadow(
        elevation = (glass.shadowDepth * 14f).dp,
        shape = shape,
        ambientColor = primary.copy(alpha = 0.42f),
        spotColor = primary.copy(alpha = 0.42f),
      )
    } else {
      Modifier
    }

  Box(
    modifier =
      modifier
        .then(shadowModifier)
        .clip(shape)
        .background(bgBrush)
        .border(width = 1.dp, brush = borderBrush, shape = shape),
  ) {
    if (rules.enableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && glass.blurStrength > 0.2f) {
      Box(
        modifier =
          Modifier
            .matchParentSize()
            .blur((glass.blurStrength * 12f).dp)
            .background(primary.copy(alpha = 0.03f)),
      )
    }
    Column(
      modifier = Modifier.padding(contentPadding),
      content = content,
    )
  }
}

@Composable
fun StudioTopHeader(
  title: String,
  subtitle: String,
  rules: EffectiveRenderRules,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Surface(
      shape = RoundedCornerShape(50),
      color = MaterialTheme.colorScheme.surfaceContainer,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Rounded.Bolt,
          contentDescription = null,
          tint =
            when (rules.tier) {
              ResolvedTier.LITE -> EmeraldMint
              ResolvedTier.BALANCED -> ElectricCyan
              ResolvedTier.PREMIUM -> LuminousViolet
            },
          modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "${rules.tier.label} • Aether",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
fun OfflinePrivacyPill(
  modifier: Modifier = Modifier,
  label: String = "Gemini AI Logic • Onboard Fallback • Zero Exposed Keys",
  isOnlineReady: Boolean = true,
) {
  val accent = if (isOnlineReady) ElectricCyan else EmeraldMint
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = accent.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = if (isOnlineReady) Icons.Rounded.AutoAwesome else Icons.Rounded.Lock,
        contentDescription = null,
        tint = accent,
        modifier = Modifier.size(14.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = accent,
      )
    }
  }
}

@Composable
fun PaletteSwatchRow(palette: ThemePalette, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    listOf(
      palette.primaryHex,
      palette.secondaryHex,
      palette.tertiaryHex,
      palette.backgroundHex,
    ).forEach { hex ->
      Box(
        modifier =
          Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(Color(hex))
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
      )
    }
  }
}
