package com.example.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher.model.CustomWidgetKind
import com.example.launcher.system.ControlCenterSystemSnapshot
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
internal fun RenderExtendedAtomicWidget(
  kind: CustomWidgetKind,
  snap: ControlCenterSystemSnapshot,
  totalLaunches: Int,
  lockedCount: Int,
  webAppCount: Int,
  primary: Color,
  secondary: Color,
  onSurface: Color,
  fontFamily: FontFamily,
  onOpenThemeGenerator: () -> Unit,
  onOpenClonerVault: () -> Unit,
  onOpenUsageSettings: () -> Unit,
) {
  when (kind) {
    CustomWidgetKind.ANALOG_CLOCK -> {
      var nowCal by remember { mutableStateOf(Calendar.getInstance()) }
      LaunchedEffect(Unit) {
        while (true) {
          delay(15_000L)
          nowCal = Calendar.getInstance()
        }
      }
      val hour = nowCal.get(Calendar.HOUR)
      val minute = nowCal.get(Calendar.MINUTE)
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(92.dp)) {
          val c = Offset(size.width / 2f, size.height / 2f)
          val r = size.minDimension * 0.44f
          drawCircle(color = Color.White.copy(alpha = 0.12f), radius = r)
          drawCircle(color = primary.copy(alpha = 0.72f), radius = r, style = Stroke(2.dp.toPx()))
          for (tick in 0 until 12) {
            val ang = Math.toRadians(tick * 30.0 - 90.0)
            val p1 = Offset(c.x + (cos(ang) * r * 0.80f).toFloat(), c.y + (sin(ang) * r * 0.80f).toFloat())
            val p2 = Offset(c.x + (cos(ang) * r * 0.94f).toFloat(), c.y + (sin(ang) * r * 0.94f).toFloat())
            drawLine(onSurface.copy(alpha = 0.55f), p1, p2, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
          }
          val minAng = Math.toRadians(minute * 6.0 - 90.0)
          val hrAng = Math.toRadians((hour + minute / 60.0) * 30.0 - 90.0)
          drawLine(
            color = onSurface,
            start = c,
            end = Offset(c.x + (cos(hrAng) * r * 0.52f).toFloat(), c.y + (sin(hrAng) * r * 0.52f).toFloat()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
          )
          drawLine(
            color = secondary,
            start = c,
            end = Offset(c.x + (cos(minAng) * r * 0.74f).toFloat(), c.y + (sin(minAng) * r * 0.74f).toFloat()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }
      }
    }

    CustomWidgetKind.STORAGE -> {
      val usedStr = String.format(Locale.US, "%.1f / %.0f GB", snap.storageUsedGb, snap.storageTotalGb)
      val ratio = (snap.storageUsedGb / snap.storageTotalGb.coerceAtLeast(1f)).coerceIn(0f, 1f)
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Storage, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Storage", fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onSurface)
          }
          Text("${(ratio * 100).toInt()}%", fontFamily = fontFamily, fontSize = 11.sp, color = secondary)
        }
        LinearProgressIndicator(
          progress = { ratio },
          color = primary,
          trackColor = onSurface.copy(alpha = 0.16f),
          modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
        )
        Text(usedStr, fontFamily = fontFamily, fontSize = 11.sp, color = onSurface.copy(alpha = 0.78f))
      }
    }

    CustomWidgetKind.WELLNESS -> {
      val mins = snap.todayScreenMinutes
      Column(
        modifier = Modifier.fillMaxWidth().clickable { if (mins == null) onOpenUsageSettings() },
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.HourglassTop, contentDescription = null, tint = secondary, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Wellness", fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onSurface)
        }
        Text(
          text = if (mins != null) "${mins / 60}h ${mins % 60}m active" else "$totalLaunches app launches",
          fontFamily = fontFamily,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = onSurface,
        )
        Text(
          text = if (mins != null) "UsageStats live" else "Tap to grant Usage Access",
          fontFamily = fontFamily,
          fontSize = 10.sp,
          color = primary,
        )
      }
    }

    CustomWidgetKind.AI_WORKSPACE -> {
      Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpenThemeGenerator() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Aether AI Architect", fontFamily = fontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = onSurface)
          }
          Spacer(Modifier.height(2.dp))
          Text(
            text = "Ask Gemini 3.8 Flash or synthesize a Liquid Glass palette",
            fontFamily = fontFamily,
            fontSize = 11.sp,
            color = onSurface.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Surface(
          shape = RoundedCornerShape(50),
          color = primary.copy(alpha = 0.22f),
          border = BorderStroke(1.dp, primary.copy(alpha = 0.5f)),
        ) {
          Text("Launch", fontFamily = fontFamily, fontSize = 11.sp, color = onSurface, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
        }
      }
    }

    CustomWidgetKind.DUAL_SPACE -> {
      Column(
        modifier = Modifier.fillMaxWidth().clickable { onOpenClonerVault() },
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Rounded.Security, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Cloner Vault", fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onSurface)
        }
        Text(
          text = "$lockedCount locked • $webAppCount web",
          fontFamily = fontFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = onSurface,
        )
        Text("Tap to manage Vault", fontFamily = fontFamily, fontSize = 10.sp, color = secondary)
      }
    }

    else -> {}
  }
}
