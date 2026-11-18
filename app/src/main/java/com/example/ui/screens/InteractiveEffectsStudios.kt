package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.ui.ThemeStudioUiState
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.fontStyleToFamily

@Composable
fun FingerprintAndChargingStudioSection(
  state: ThemeStudioUiState,
  onUpdateDraft: ((ThemeSpec) -> ThemeSpec) -> Unit,
) {
  val theme = state.draftTheme
  val primary = Color(theme.palette.primaryHex)
  val secondary = Color(theme.palette.secondaryHex)
  var triggerCount by remember { mutableIntStateOf(0) }
  var simulateCharging by remember { mutableStateOf(true) }

  val animate =
    state.isAppForeground &&
      theme.motionAndSound.animationsEnabled &&
      state.renderRules.animationScale > 0f &&
      theme.motionAndSound.animationPreset != AnimationPreset.REDUCED_STATIC

  val rippleAnim = remember { Animatable(0f) }
  LaunchedEffect(triggerCount) {
    if (triggerCount > 0) {
      rippleAnim.snapTo(0f)
      rippleAnim.animateTo(1f, animationSpec = tween(durationMillis = 650))
    }
  }

  val infinite = rememberInfiniteTransition(label = "charging_anim")
  val sweepAngle by
    if (animate && simulateCharging) {
      infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "orbit_sweep",
      )
    } else {
      remember { mutableFloatStateOf(120f) }
    }

  GlassSurfaceCard(
    glass = theme.glass,
    palette = theme.palette,
    rules = state.renderRules,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text("Fingerprint Visual Effects (Tap Pad to Test)", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.5.dp, primary.copy(alpha = 0.55f), CircleShape)
            .clickable { triggerCount++ }
            .testTag("fingerprint_test_pad"),
      ) {
        val progress = rippleAnim.value
        Canvas(modifier = Modifier.fillMaxSize()) {
          val maxR = size.minDimension * 0.48f
          if (progress > 0f && progress < 1f) {
            when (theme.lockScreen.fingerprintEffect) {
              FingerprintEffect.CRYSTAL_HEX -> {
                drawRect(
                  color = primary.copy(alpha = 1f - progress),
                  style = Stroke(width = 3.dp.toPx()),
                )
              }
              FingerprintEffect.NEON_WAVE -> {
                drawCircle(
                  color = secondary.copy(alpha = 1f - progress),
                  radius = maxR * progress,
                  style = Stroke(width = 4.dp.toPx()),
                )
                drawCircle(
                  color = primary.copy(alpha = (1f - progress) * 0.7f),
                  radius = maxR * progress * 0.65f,
                  style = Stroke(width = 2.dp.toPx()),
                )
              }
              else -> {
                drawCircle(
                  color = primary.copy(alpha = (1f - progress) * 0.8f),
                  radius = maxR * progress,
                )
              }
            }
          }
        }
        Icon(
          imageVector = Icons.Rounded.Fingerprint,
          contentDescription = "Tap to preview fingerprint visual effect",
          tint = primary,
          modifier = Modifier.size(44.dp),
        )
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FingerprintEffect.entries.forEach { effect ->
          FilterChip(
            selected = theme.lockScreen.fingerprintEffect == effect,
            onClick = {
              triggerCount++
              onUpdateDraft { it.copy(lockScreen = it.lockScreen.copy(fingerprintEffect = effect)) }
            },
            label = { Text(effect.label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.fillMaxWidth().testTag("fp_${effect.name.lowercase()}"),
          )
        }
      }
    }
  }

  GlassSurfaceCard(
    glass = theme.glass,
    palette = theme.palette,
    rules = state.renderRules,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("Charging Animation Studio", style = MaterialTheme.typography.titleMedium)
      Switch(
        checked = simulateCharging,
        onCheckedChange = { simulateCharging = it },
        modifier = Modifier.testTag("charging_preview_switch"),
      )
    }
    Spacer(Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.55f)),
      ) {
        Canvas(modifier = Modifier.size(84.dp)) {
          val r = size.minDimension * 0.44f
          drawCircle(color = primary.copy(alpha = 0.2f), radius = r, style = Stroke(5.dp.toPx()))
          if (simulateCharging) {
            drawArc(
              color = if (theme.lockScreen.chargingAnimation == ChargingAnimation.PLASMA_BOLT) secondary else primary,
              startAngle = sweepAngle,
              sweepAngle = 110f,
              useCenter = false,
              style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
          }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Rounded.Bolt, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
          Text(
            text = "${state.deviceCapabilities.batteryPercent}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ChargingAnimation.entries.forEach { anim ->
          FilterChip(
            selected = theme.lockScreen.chargingAnimation == anim,
            onClick = {
              onUpdateDraft { it.copy(lockScreen = it.lockScreen.copy(chargingAnimation = anim)) }
            },
            label = { Text(anim.label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.fillMaxWidth().testTag("charging_${anim.name.lowercase()}"),
          )
        }
      }
    }
  }
}

@Composable
fun FontSoundAndMotionStudioSection(
  state: ThemeStudioUiState,
  onUpdateDraft: ((ThemeSpec) -> ThemeSpec) -> Unit,
  onPreviewSound: (SoundProfile) -> Unit,
  onToggleMasterAnimations: (Boolean) -> Unit,
) {
  val theme = state.draftTheme
  val primary = Color(theme.palette.primaryHex)

  GlassSurfaceCard(
    glass = theme.glass,
    palette = theme.palette,
    rules = state.renderRules,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text("Font Studio (Bundled Offline .ttf)", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    FontStyleOption.entries.forEach { fontOpt ->
      val selected = theme.fontStyle == fontOpt
      val family = fontStyleToFamily(fontOpt)
      Surface(
        onClick = { onUpdateDraft { it.copy(fontStyle = fontOpt) } },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) primary.copy(alpha = 0.18f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).testTag("font_${fontOpt.name.lowercase()}"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = fontOpt.label,
              fontFamily = family,
              fontSize = 17.sp,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "${fontOpt.previewTag} • 11:09 AM • Aa Bb Cc 0123",
              fontFamily = family,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          RadioButton(selected = selected, onClick = null)
        }
      }
    }
  }

  GlassSurfaceCard(
    glass = theme.glass,
    palette = theme.palette,
    rules = state.renderRules,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text("Sound Studio (Procedural Offline AudioTrack)", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SoundProfile.entries.forEach { profile ->
        FilterChip(
          selected = theme.motionAndSound.soundProfile == profile,
          onClick = {
            onUpdateDraft {
              it.copy(motionAndSound = it.motionAndSound.copy(soundProfile = profile))
            }
            onPreviewSound(profile)
          },
          label = { Text(profile.label, style = MaterialTheme.typography.labelSmall) },
          modifier = Modifier.weight(1f).testTag("sound_${profile.name.lowercase()}"),
        )
      }
    }
  }

  GlassSurfaceCard(
    glass = theme.glass,
    palette = theme.palette,
    rules = state.renderRules,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text("Animation Engine", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Enable Decorative Animations", style = MaterialTheme.typography.bodyMedium)
        Text(
          "Automatically pauses when backgrounded or in Lite Mode",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Switch(
        checked = state.animationsMasterEnabled,
        onCheckedChange = onToggleMasterAnimations,
        modifier = Modifier.testTag("master_animations_switch"),
      )
    }

    Spacer(Modifier.height(8.dp))
    AnimationPreset.entries.chunked(2).forEach { pair ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        pair.forEach { preset ->
          FilterChip(
            selected = theme.motionAndSound.animationPreset == preset,
            onClick = {
              onUpdateDraft {
                it.copy(
                  motionAndSound =
                    it.motionAndSound.copy(
                      animationPreset = preset,
                      animationsEnabled = preset != AnimationPreset.REDUCED_STATIC,
                    ),
                )
              }
            },
            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.weight(1f).testTag("anim_${preset.name.lowercase()}"),
          )
        }
      }
    }
  }
}
