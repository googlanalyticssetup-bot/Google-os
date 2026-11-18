package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*

data class SampleAppIcon(
  val label: String,
  val vector: ImageVector,
  val tintOffset: Int,
)

val SampleStudioIcons =
  listOf(
    SampleAppIcon("Phone", Icons.Rounded.Call, 0),
    SampleAppIcon("Messages", Icons.Rounded.ChatBubble, 1),
    SampleAppIcon("Camera", Icons.Rounded.PhotoCamera, 2),
    SampleAppIcon("Music", Icons.Rounded.MusicNote, 0),
    SampleAppIcon("Browser", Icons.Rounded.Public, 1),
    SampleAppIcon("Gallery", Icons.Rounded.PhotoLibrary, 2),
    SampleAppIcon("Clock", Icons.Rounded.Schedule, 0),
    SampleAppIcon("Settings", Icons.Rounded.Settings, 1),
  )

@Composable
fun ThemedVectorAppIcon(
  appIcon: SampleAppIcon,
  config: IconPackConfig,
  palette: ThemePalette,
  rules: EffectiveRenderRules,
  size: Dp = 48.dp,
  showLabel: Boolean = true,
) {
  val baseColor =
    config.customTintHex?.let { Color(it) }
      ?: when (appIcon.tintOffset % 3) {
        0 -> Color(palette.primaryHex)
        1 -> Color(palette.secondaryHex)
        else -> Color(palette.tertiaryHex)
      }
  val shape = RoundedCornerShape(config.cornerRadiusDp.dp)
  val glowActive = config.glowEnabled && rules.enableGlow

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.width(size + 10.dp),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier
          .size(size)
          .then(
            if (glowActive && rules.tier == ResolvedTier.PREMIUM) {
              Modifier.shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = baseColor,
                spotColor = baseColor,
              )
            } else {
              Modifier
            },
          )
          .clip(shape),
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = CornerRadius(config.cornerRadiusDp.dp.toPx(), config.cornerRadiusDp.dp.toPx())
        when (config.style) {
          IconStyle.GLASS -> {
            drawRoundRect(
              brush =
                Brush.linearGradient(
                  listOf(baseColor.copy(alpha = 0.42f), Color.White.copy(alpha = 0.14f)),
                ),
              cornerRadius = radius,
            )
            drawRoundRect(
              color = Color.White.copy(alpha = 0.48f),
              cornerRadius = radius,
              style = Stroke(width = 1.5.dp.toPx()),
            )
          }
          IconStyle.NEON -> {
            drawRoundRect(color = Color(0xFF070812), cornerRadius = radius)
            drawRoundRect(
              color = baseColor,
              cornerRadius = radius,
              style = Stroke(width = if (glowActive) 2.2.dp.toPx() else 1.5.dp.toPx()),
            )
          }
          IconStyle.ROUNDED -> {
            drawRoundRect(color = baseColor.copy(alpha = 0.9f), cornerRadius = radius)
          }
          IconStyle.MINIMAL -> {
            drawRoundRect(color = Color(palette.surfaceHex).copy(alpha = 0.85f), cornerRadius = radius)
            drawRoundRect(
              color = baseColor.copy(alpha = 0.35f),
              cornerRadius = radius,
              style = Stroke(width = 1.dp.toPx()),
            )
          }
          IconStyle.THREE_D -> {
            drawRoundRect(
              brush =
                Brush.linearGradient(
                  colors = listOf(baseColor.copy(alpha = 0.95f), baseColor.copy(alpha = 0.45f)),
                  start = Offset.Zero,
                  end = Offset(this.size.width, this.size.height),
                ),
              cornerRadius = radius,
            )
            drawRoundRect(
              brush =
                Brush.verticalGradient(
                  listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                ),
              cornerRadius = radius,
              style = Stroke(width = 2.dp.toPx()),
            )
          }
          IconStyle.GRADIENT -> {
            drawRoundRect(
              brush =
                Brush.linearGradient(
                  listOf(Color(palette.primaryHex), Color(palette.secondaryHex)),
                ),
              cornerRadius = radius,
            )
          }
          IconStyle.TRANSPARENT -> {
            drawRoundRect(color = Color.White.copy(alpha = 0.08f), cornerRadius = radius)
            drawRoundRect(
              color = baseColor.copy(alpha = 0.65f),
              cornerRadius = radius,
              style = Stroke(width = 1.2.dp.toPx()),
            )
          }
          IconStyle.AMOLED -> {
            drawRoundRect(color = Color.Black, cornerRadius = radius)
            drawRoundRect(
              color = baseColor.copy(alpha = 0.55f),
              cornerRadius = radius,
              style = Stroke(width = 1.2.dp.toPx()),
            )
          }
        }
      }

      val glyphTint =
        when (config.style) {
          IconStyle.ROUNDED, IconStyle.GRADIENT, IconStyle.THREE_D -> Color.White
          IconStyle.NEON, IconStyle.AMOLED, IconStyle.TRANSPARENT, IconStyle.MINIMAL -> baseColor
          IconStyle.GLASS -> Color.White
        }

      Icon(
        imageVector = appIcon.vector,
        contentDescription = "${appIcon.label} themed icon",
        tint = glyphTint,
        modifier = Modifier.size(size * 0.48f * config.innerScale),
      )
    }

    if (showLabel) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = appIcon.label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = Color(palette.onSurfaceHex).copy(alpha = 0.9f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
