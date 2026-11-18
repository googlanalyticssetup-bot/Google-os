package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

enum class PreviewSurfaceMode(val label: String) {
  HOME("Home"),
  LOCK("Lock"),
  CONTROL("Control"),
  NOTIFS("Notifs"),
  AOD("AOD"),
}

fun fontStyleToFamily(option: FontStyleOption): FontFamily =
  when (option) {
    FontStyleOption.PLUS_JAKARTA -> PlusJakartaSansFamily
    FontStyleOption.SPACE_GROTESK -> SpaceGroteskFamily
    FontStyleOption.PLAYFAIR_DISPLAY -> PlayfairDisplayFamily
    FontStyleOption.JETBRAINS_MONO -> JetBrainsMonoFamily
  }

@Composable
fun ThemeDevicePreview(
  theme: ThemeSpec,
  previewBitmap: Bitmap?,
  rules: EffectiveRenderRules,
  isAppForeground: Boolean,
  modifier: Modifier = Modifier,
  initialMode: PreviewSurfaceMode = PreviewSurfaceMode.HOME,
) {
  var surfaceMode by remember { mutableStateOf(initialMode) }
  val animate =
    isAppForeground &&
      theme.motionAndSound.animationsEnabled &&
      rules.animationScale > 0f &&
      theme.motionAndSound.animationPreset != AnimationPreset.REDUCED_STATIC

  val infiniteTransition = rememberInfiniteTransition(label = "preview_pulse")
  val pulse by
    if (animate) {
      infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = (1800 / rules.animationScale.coerceAtLeast(0.5f)).toInt()),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "pulse_alpha",
      )
    } else {
      remember { mutableFloatStateOf(0.85f) }
    }

  val isTabletPreview = theme.wallpaper.aspectMode == WallpaperAspectMode.TABLET_WIDE
  val primary = Color(theme.palette.primaryHex)
  val secondary = Color(theme.palette.secondaryHex)
  val onSurface = Color(theme.palette.onSurfaceHex)
  val fontFamily = fontStyleToFamily(theme.fontStyle)

  Column(modifier = modifier.fillMaxWidth()) {
    // Surface mode switcher pills
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      PreviewSurfaceMode.entries.forEach { mode ->
        val selected = mode == surfaceMode
        FilterChip(
          selected = selected,
          onClick = { surfaceMode = mode },
          label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
          modifier = Modifier.weight(1f).testTag("preview_tab_${mode.name.lowercase()}"),
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Device mockup shell
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(if (isTabletPreview) 290.dp else 380.dp)
          .clip(RoundedCornerShape(28.dp))
          .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
          .background(Color(theme.palette.backgroundHex)),
    ) {
      // Backdrop
      if (surfaceMode != PreviewSurfaceMode.AOD) {
        if (previewBitmap != null && !previewBitmap.isRecycled) {
          Image(
            bitmap = previewBitmap.asImageBitmap(),
            contentDescription = "Generated theme wallpaper preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .background(
                  Brush.linearGradient(
                    listOf(
                      Color(theme.palette.backgroundHex),
                      primary.copy(alpha = 0.45f),
                      secondary.copy(alpha = 0.3f),
                    ),
                  ),
                ),
          )
        }
      } else {
        // Pure AMOLED #000000 canvas for Always-On Display
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
      }

      // Status bar inside mockup
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "11:09",
          fontFamily = fontFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = if (surfaceMode == PreviewSurfaceMode.AOD) Color.White.copy(alpha = 0.6f) else onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.Wifi,
            contentDescription = null,
            tint = if (surfaceMode == PreviewSurfaceMode.AOD) Color.White.copy(alpha = 0.6f) else onSurface,
            modifier = Modifier.size(13.dp),
          )
          Icon(
            imageVector = Icons.Rounded.BatteryChargingFull,
            contentDescription = null,
            tint = primary,
            modifier = Modifier.size(14.dp),
          )
        }
      }

      // Surface-specific content
      when (surfaceMode) {
        PreviewSurfaceMode.HOME -> {
          Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 34.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            // Glass Widget Preview
            GlassSurfaceCard(
              glass = theme.glass.copy(transparency = theme.widget.glassOpacity),
              palette = theme.palette,
              rules = rules,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = "11:09",
                    fontFamily = fontFamily,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                  )
                  Text(
                    text = "Mon, Nov 16 • ${theme.widget.layout.label}",
                    fontFamily = fontFamily,
                    fontSize = 11.sp,
                    color = onSurface.copy(alpha = 0.78f),
                  )
                }
                Surface(
                  shape = RoundedCornerShape(50),
                  color = primary.copy(alpha = 0.22f),
                  border = BorderStroke(1.dp, primary.copy(alpha = 0.5f)),
                ) {
                  Text(
                    text = theme.name,
                    fontFamily = fontFamily,
                    fontSize = 10.sp,
                    color = onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }
            }

            // Icon Grid Preview
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
              ) {
                SampleStudioIcons.take(4).forEach { app ->
                  ThemedVectorAppIcon(
                    appIcon = app,
                    config = theme.icons,
                    palette = theme.palette,
                    rules = rules,
                    size = 42.dp,
                  )
                }
              }
              // Glass Dock
              GlassSurfaceCard(
                glass = theme.glass,
                palette = theme.palette,
                rules = rules,
                cornerRadius = 22.dp,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                  SampleStudioIcons.drop(4).take(4).forEach { app ->
                    ThemedVectorAppIcon(
                      appIcon = app,
                      config = theme.icons,
                      palette = theme.palette,
                      rules = rules,
                      size = 38.dp,
                      showLabel = false,
                    )
                  }
                }
              }
            }
          }
        }

        PreviewSurfaceMode.LOCK -> {
          Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            LockScreenClockDisplay(
              clockStyle = theme.lockScreen.clockStyle,
              fontFamily = fontFamily,
              primary = primary,
              secondary = secondary,
              onSurface = onSurface,
              showSeconds = theme.lockScreen.showSeconds,
            )

            // Fingerprint + charging indicator
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(68.dp),
              ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  drawCircle(
                    color = primary.copy(alpha = 0.35f * pulse),
                    radius = size.minDimension * 0.46f,
                    style = Stroke(width = 2.dp.toPx()),
                  )
                  drawCircle(
                    color = secondary.copy(alpha = 0.25f * pulse),
                    radius = size.minDimension * 0.34f,
                  )
                }
                Icon(
                  imageVector = Icons.Rounded.Fingerprint,
                  contentDescription = "Lock screen fingerprint visual",
                  tint = primary,
                  modifier = Modifier.size(34.dp),
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${theme.lockScreen.fingerprintEffect.label} • ${theme.lockScreen.chargingAnimation.label}",
                fontFamily = fontFamily,
                fontSize = 10.sp,
                color = onSurface.copy(alpha = 0.78f),
              )
            }
          }
        }

        PreviewSurfaceMode.CONTROL -> {
          Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Control Centre (${theme.controlCenter.tileShape.label})",
              fontFamily = fontFamily,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = onSurface,
            )
            val tileCorner =
              when (theme.controlCenter.tileShape) {
                TileShape.PILL -> 26.dp
                TileShape.SQUIRCLE -> 16.dp
                TileShape.CIRCLE -> 50.dp
                TileShape.SHARP_GLASS -> 8.dp
              }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              listOf("Wi-Fi" to Icons.Rounded.Wifi, "Bluetooth" to Icons.Rounded.Bluetooth).forEach {
                (label, icon) ->
                GlassSurfaceCard(
                  glass = theme.glass,
                  palette = theme.palette,
                  rules = rules,
                  cornerRadius = tileCorner,
                  modifier = Modifier.weight(1f),
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontFamily = fontFamily, fontSize = 12.sp, color = onSurface)
                  }
                }
              }
            }
            GlassSurfaceCard(
              glass = theme.glass,
              palette = theme.palette,
              rules = rules,
              cornerRadius = tileCorner,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BrightnessHigh, contentDescription = null, tint = secondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                LinearProgressIndicator(
                  progress = { 0.72f },
                  color = primary,
                  trackColor = onSurface.copy(alpha = 0.16f),
                  modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                )
              }
            }
          }
        }

        PreviewSurfaceMode.NOTIFS -> {
          Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 38.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Notifications (${theme.controlCenter.notificationStyle.label})",
              fontFamily = fontFamily,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = onSurface,
            )
            listOf(
              "Theme Studio" to "Offline palette & procedural glass ready",
              "System Status" to "${rules.tier.label} rendering mode active",
            ).forEach { (title, body) ->
              GlassSurfaceCard(
                glass = theme.glass,
                palette = theme.palette,
                rules = rules,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier =
                      Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = primary, modifier = Modifier.size(15.dp))
                  }
                  Spacer(Modifier.width(10.dp))
                  Column {
                    Text(title, fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onSurface)
                    Text(body, fontFamily = fontFamily, fontSize = 11.sp, color = onSurface.copy(alpha = 0.78f))
                  }
                }
              }
            }
          }
        }

        PreviewSurfaceMode.AOD -> {
          Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              if (theme.lockScreen.aodStyle == AodStyle.ORBIT_HALO) {
                Box(
                  modifier =
                    Modifier
                      .size(130.dp)
                      .border(2.dp, primary.copy(alpha = 0.65f * pulse), CircleShape),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = "11:09",
                    fontFamily = fontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    color = primary,
                  )
                }
              } else {
                Text(
                  text = "11:09",
                  fontFamily = fontFamily,
                  fontSize = 46.sp,
                  fontWeight = FontWeight.Light,
                  color =
                    if (theme.lockScreen.aodStyle == AodStyle.MONOCHROME_MONO) Color.White
                    else primary,
                )
              }
              Spacer(Modifier.height(8.dp))
              Text(
                text = "Mon, Nov 16 • ${theme.lockScreen.aodStyle.label}",
                fontFamily = fontFamily,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LockScreenClockDisplay(
  clockStyle: ClockStyle,
  fontFamily: FontFamily,
  primary: Color,
  secondary: Color,
  onSurface: Color,
  showSeconds: Boolean,
) {
  val timeText = if (showSeconds) "11:09:42" else "11:09"
  when (clockStyle) {
    ClockStyle.ANALOG_CRYSTAL -> {
      Canvas(modifier = Modifier.size(116.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.44f
        drawCircle(color = Color.White.copy(alpha = 0.14f), radius = r)
        drawCircle(color = primary.copy(alpha = 0.7f), radius = r, style = Stroke(2.dp.toPx()))
        for (tick in 0 until 12) {
          val ang = Math.toRadians(tick * 30.0)
          val p1 = Offset(c.x + (cos(ang) * r * 0.84f).toFloat(), c.y + (sin(ang) * r * 0.84f).toFloat())
          val p2 = Offset(c.x + (cos(ang) * r * 0.96f).toFloat(), c.y + (sin(ang) * r * 0.96f).toFloat())
          drawLine(onSurface.copy(alpha = 0.6f), p1, p2, strokeWidth = 1.5.dp.toPx())
        }
        drawLine(onSurface, c, Offset(c.x - r * 0.35f, c.y - r * 0.25f), strokeWidth = 3.dp.toPx())
        drawLine(secondary, c, Offset(c.x + r * 0.52f, c.y - r * 0.12f), strokeWidth = 2.dp.toPx())
      }
    }
    ClockStyle.NEON_SPLIT -> {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("11", fontFamily = fontFamily, fontSize = 42.sp, fontWeight = FontWeight.Bold, color = primary)
        Text("09", fontFamily = fontFamily, fontSize = 42.sp, fontWeight = FontWeight.Bold, color = secondary)
      }
    }
    ClockStyle.GLASS_PILL -> {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, primary.copy(alpha = 0.5f)),
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
          Text(timeText, fontFamily = fontFamily, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = onSurface)
          Text("MON, NOV 16", fontFamily = fontFamily, fontSize = 11.sp, color = primary)
        }
      }
    }
    ClockStyle.EDITORIAL_SERIF -> {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(timeText, fontFamily = PlayfairDisplayFamily, fontSize = 46.sp, fontWeight = FontWeight.Bold, color = onSurface)
        Text("Monday • November 16", fontFamily = PlayfairDisplayFamily, fontSize = 13.sp, color = primary)
      }
    }
    ClockStyle.MINIMAL_DIGITAL -> {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(timeText, fontFamily = fontFamily, fontSize = 46.sp, fontWeight = FontWeight.SemiBold, color = onSurface)
        Text("Mon, Nov 16", fontFamily = fontFamily, fontSize = 12.sp, color = onSurface.copy(alpha = 0.78f))
      }
    }
  }
}
