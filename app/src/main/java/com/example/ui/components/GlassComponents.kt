package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.theme.*

@Composable
fun GlassSurfaceCard(
  glass: GlassConfig,
  palette: ThemePalette,
  rules: EffectiveRenderRules,
  modifier: Modifier = Modifier,
  cornerRadius: Dp = glass.cornerRadiusDp.dp,
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
      glass.transparency.coerceIn(0.10f, 0.85f)
    }

  val borderBrush =
    if (rules.enableReflections) {
      Brush.linearGradient(
        colors =
          listOf(
            Color.White.copy(alpha = (0.18f + glass.reflectionOpacity * 0.45f).coerceIn(0.1f, 0.7f)),
            primary.copy(alpha = (0.15f + glass.glowIntensity * 0.35f).coerceIn(0.1f, 0.55f)),
            secondary.copy(alpha = 0.12f),
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
            primary.copy(alpha = effectiveAlpha * 0.45f),
            secondary.copy(alpha = effectiveAlpha * 0.35f),
          ),
        )
      GlassStyle.CRYSTAL ->
        Brush.linearGradient(
          listOf(
            surface.copy(alpha = effectiveAlpha * 0.85f),
            Color.White.copy(alpha = 0.08f),
            surface.copy(alpha = effectiveAlpha),
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
    if (rules.tier == ResolvedTier.PREMIUM && glass.shadowDepth > 0.25f) {
      Modifier.shadow(
        elevation = (glass.shadowDepth * 14f).dp,
        shape = shape,
        ambientColor = primary.copy(alpha = 0.45f),
        spotColor = primary.copy(alpha = 0.45f),
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
      modifier = Modifier.padding(16.dp),
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
          text = "${rules.tier.label} • Offline",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
fun OfflinePrivacyPill(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = EmeraldMint.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, EmeraldMint.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Rounded.Lock,
        contentDescription = null,
        tint = EmeraldMint,
        modifier = Modifier.size(14.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "No API Key • On-Device AI • Local Storage",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = EmeraldMint,
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
