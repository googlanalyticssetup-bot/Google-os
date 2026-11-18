package com.example.ui.screens

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ThemeSpec
import com.example.domain.model.WidgetLayout
import com.example.ui.ThemeStudioUiState
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.StudioTopHeader
import com.example.ui.components.fontStyleToFamily
import com.example.widget.ThemeStudioWidgetProvider

@Composable
fun WidgetsStudioScreen(
  state: ThemeStudioUiState,
  onUpdateDraft: ((ThemeSpec) -> ThemeSpec) -> Unit,
  onActivateAndSyncWidget: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val theme = state.draftTheme
  val fontFamily = fontStyleToFamily(theme.fontStyle)
  val primary = Color(theme.palette.primaryHex)
  val onSurface = Color(theme.palette.onSurfaceHex)
  var pinFeedback by remember { mutableStateOf<String?>(null) }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    StudioTopHeader(
      title = "Widget Studio",
      subtitle = "Offline glass Android App Widgets synced with Room",
      rules = state.renderRules,
    )

    // Live interactive widget preview
    GlassSurfaceCard(
      glass = theme.glass.copy(transparency = theme.widget.glassOpacity),
      palette = theme.palette,
      rules = state.renderRules,
      modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
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
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface,
          )
          Text(
            text = "Mon, Nov 16",
            fontFamily = fontFamily,
            fontSize = 13.sp,
            color = onSurface.copy(alpha = 0.8f),
          )
        }
        Surface(
          shape = RoundedCornerShape(50),
          color = primary.copy(alpha = 0.2f),
          border = BorderStroke(1.dp, primary.copy(alpha = 0.5f)),
        ) {
          Text(
            text = theme.name,
            fontFamily = fontFamily,
            style = MaterialTheme.typography.labelSmall,
            color = primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (theme.widget.showBattery) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
              text = "${state.deviceCapabilities.batteryPercent}%",
              fontFamily = fontFamily,
              style = MaterialTheme.typography.labelSmall,
              color = onSurface,
            )
          }
        }
        if (theme.widget.showWeatherPlaceholder) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Widgets, contentDescription = null, tint = Color(theme.palette.secondaryHex), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
              text = "${theme.widget.layout.label} • ${state.renderRules.tier.label}",
              fontFamily = fontFamily,
              style = MaterialTheme.typography.labelSmall,
              color = onSurface.copy(alpha = 0.8f),
            )
          }
        }
      }
    }

    // Widget configuration controls
    GlassSurfaceCard(
      glass = theme.glass,
      palette = theme.palette,
      rules = state.renderRules,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Widget Layout Preset", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(8.dp))
      WidgetLayout.entries.chunked(2).forEach { pair ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          pair.forEach { layout ->
            FilterChip(
              selected = theme.widget.layout == layout,
              onClick = {
                onUpdateDraft { it.copy(widget = it.widget.copy(layout = layout)) }
              },
              label = { Text(layout.label, style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.weight(1f).testTag("widget_layout_${layout.name.lowercase()}"),
            )
          }
        }
      }

      Spacer(Modifier.height(10.dp))
      Text(
        "Glass Opacity (${(theme.widget.glassOpacity * 100).toInt()}%)",
        style = MaterialTheme.typography.bodyMedium,
      )
      Slider(
        value = theme.widget.glassOpacity,
        onValueChange = { v ->
          onUpdateDraft { it.copy(widget = it.widget.copy(glassOpacity = v)) }
        },
        valueRange = 0.1f..0.75f,
        modifier = Modifier.testTag("widget_opacity_slider"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Show Device Battery Telemetry", style = MaterialTheme.typography.bodyMedium)
        Switch(
          checked = theme.widget.showBattery,
          onCheckedChange = { checked ->
            onUpdateDraft { it.copy(widget = it.widget.copy(showBattery = checked)) }
          },
          modifier = Modifier.testTag("widget_battery_switch"),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Show Theme Style Pill", style = MaterialTheme.typography.bodyMedium)
        Switch(
          checked = theme.widget.showWeatherPlaceholder,
          onCheckedChange = { checked ->
            onUpdateDraft { it.copy(widget = it.widget.copy(showWeatherPlaceholder = checked)) }
          },
          modifier = Modifier.testTag("widget_pill_switch"),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Button(
        onClick = {
          onActivateAndSyncWidget()
          pinFeedback = "Synced \"${theme.name}\" to all active Theme Studio home widgets."
        },
        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("sync_widget_btn"),
      ) {
        Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Sync Widget")
      }
      OutlinedButton(
        onClick = {
          onActivateAndSyncWidget()
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val awm = AppWidgetManager.getInstance(context)
            if (awm.isRequestPinAppWidgetSupported) {
              val provider = ComponentName(context, ThemeStudioWidgetProvider::class.java)
              val callbackIntent = Intent(context, ThemeStudioWidgetProvider::class.java)
              val pending =
                PendingIntent.getBroadcast(
                  context,
                  101,
                  callbackIntent,
                  PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
              awm.requestPinAppWidget(provider, null, pending)
              pinFeedback = "Launcher pin prompt opened. Confirm to add widget to your home screen."
            } else {
              pinFeedback = "Long-press your Home Screen → Widgets → Theme Studio Glass Widget."
            }
          } else {
            pinFeedback = "Long-press your Home Screen → Widgets → Theme Studio Glass Widget."
          }
        },
        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("pin_widget_btn"),
      ) {
        Icon(Icons.AutoMirrored.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Pin to Home")
      }
    }

    pinFeedback?.let { msg ->
      Text(
        text = msg,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
      )
    }
  }
}
