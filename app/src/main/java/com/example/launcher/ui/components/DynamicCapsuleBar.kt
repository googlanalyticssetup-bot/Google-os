package com.example.launcher.ui.components

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher.ui.LauncherUiState
import com.example.ui.components.FluidMotionSpecs
import java.util.Locale

@Composable
fun DynamicCapsuleBar(
  state: LauncherUiState,
  onToggleExpand: () -> Unit,
  onToggleStopwatch: () -> Unit,
  onResetStopwatch: () -> Unit,
  onOpenControlCenter: () -> Unit,
  onOpenZeroScreen: () -> Unit,
  onOpenAiArchitect: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val primary = Color(state.activeTheme.palette.primaryHex)
  val secondary = Color(state.activeTheme.palette.secondaryHex)
  val capsule = state.capsuleState
  val snap = state.controlCenterSnapshot
  val media = state.activeMedia

  val elapsedSec = capsule.stopwatchElapsedMs / 1000L
  val stopwatchText = String.format(Locale.US, "%02d:%02d", elapsedSec / 60L, elapsedSec % 60L)

  val headline =
    when {
      media != null -> "${media.title} • ${media.artist}"
      capsule.stopwatchRunning || elapsedSec > 0L -> "Focus Timer • $stopwatchText"
      snap.isCharging -> "Charging • ${snap.batteryPercent}%"
      else -> "Aether OS • ${snap.batteryPercent}%"
    }

  val leadingIcon =
    when {
      media != null -> Icons.Rounded.MusicNote
      capsule.stopwatchRunning -> Icons.Rounded.Timer
      snap.isCharging -> Icons.Rounded.BatteryChargingFull
      else -> Icons.Rounded.AutoAwesome
    }

  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      onClick = onToggleExpand,
      shape = RoundedCornerShape(if (capsule.expanded) 24.dp else 50.dp),
      color = Color(0xFF0A0D16).copy(alpha = 0.88f),
      border = BorderStroke(1.dp, primary.copy(alpha = 0.45f)),
      modifier = Modifier.fillMaxWidth().testTag("dynamic_capsule_pill"),
    ) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 32.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(shape = CircleShape, color = primary.copy(alpha = 0.22f), modifier = Modifier.size(26.dp)) {
              Box(contentAlignment = Alignment.Center) {
                Icon(leadingIcon, contentDescription = null, tint = primary, modifier = Modifier.size(15.dp))
              }
            }
            Text(
              text = headline,
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.settings.zeroScreenEnabled) {
              Surface(
                onClick = onOpenZeroScreen,
                shape = RoundedCornerShape(50),
                color = secondary.copy(alpha = 0.22f),
                modifier = Modifier.testTag("capsule_zero_btn"),
              ) {
                Text(
                  text = "Zero",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
            Icon(
              imageVector = if (capsule.expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
              contentDescription = if (capsule.expanded) "Collapse Dynamic Capsule" else "Expand Dynamic Capsule",
              tint = Color.White.copy(alpha = 0.78f),
              modifier = Modifier.size(18.dp),
            )
          }
        }

        AnimatedVisibility(
          visible = capsule.expanded,
          enter =
            expandVertically(
              animationSpec = FluidMotionSpecs.offsetSpring(state.settings.animationSpeed)
                .let { androidx.compose.animation.core.spring(dampingRatio = 0.78f, stiffness = 360f) },
            ) + fadeIn(),
          exit = shrinkVertically() + fadeOut(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

            // Media Transport Controls
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = media?.title ?: "Media Controls",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row {
                IconButton(
                  onClick = { dispatchCapsuleMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
                  modifier = Modifier.size(40.dp),
                ) {
                  Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous track", tint = Color.White)
                }
                IconButton(
                  onClick = { dispatchCapsuleMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
                  modifier = Modifier.size(40.dp),
                ) {
                  Icon(
                    imageVector = if (media?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play or pause",
                    tint = primary,
                  )
                }
                IconButton(
                  onClick = { dispatchCapsuleMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT) },
                  modifier = Modifier.size(40.dp),
                ) {
                  Icon(Icons.Rounded.SkipNext, contentDescription = "Next track", tint = Color.White)
                }
              }
            }

            // Stopwatch & Quick Workspace Pills
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                  onClick = onToggleStopwatch,
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.heightIn(min = 36.dp),
                ) {
                  Icon(
                    imageVector = if (capsule.stopwatchRunning) Icons.Rounded.Pause else Icons.Rounded.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(if (capsule.stopwatchRunning) "Pause $stopwatchText" else "Timer $stopwatchText", fontSize = 11.sp)
                }
                if (elapsedSec > 0L) {
                  IconButton(onClick = onResetStopwatch, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset stopwatch", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                  }
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                  onClick = onOpenControlCenter,
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.heightIn(min = 36.dp),
                ) {
                  Text("Control", fontSize = 11.sp, color = Color.White)
                }
                OutlinedButton(
                  onClick = onOpenAiArchitect,
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.heightIn(min = 36.dp),
                ) {
                  Text("AI", fontSize = 11.sp, color = primary)
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun dispatchCapsuleMediaKey(context: Context, keyCode: Int) {
  val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
  am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
  am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}
