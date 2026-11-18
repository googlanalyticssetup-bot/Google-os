package com.example.launcher.ui.components

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.domain.model.DeviceCapabilities
import com.example.domain.model.EffectiveRenderRules
import com.example.domain.model.ThemeSpec
import com.example.launcher.model.*
import com.example.launcher.system.LauncherWidgetHostHolder
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.fontStyleToFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun LauncherHomeWidgetCard(
  item: HomeItem,
  theme: ThemeSpec,
  settings: LauncherSettings,
  capabilities: DeviceCapabilities,
  rules: EffectiveRenderRules,
  isEditMode: Boolean,
  widgetHostHolder: LauncherWidgetHostHolder,
  onOpenSearch: () -> Unit,
  onOpenThemeGenerator: () -> Unit,
  onUpdateItem: (HomeItem) -> Unit,
  onRemoveItem: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val primary = Color(theme.palette.primaryHex)
  val secondary = Color(theme.palette.secondaryHex)
  val onSurface = Color(theme.palette.onSurfaceHex)
  val fontFamily = fontStyleToFamily(settings.fontStyle)

  val glassConfig =
    theme.glass.copy(
      transparency =
        if (item.widgetTransparent) 0.12f
        else if (item.widgetGlass) theme.widget.glassOpacity
        else 0.72f,
    )

  var showNoteEditor by remember { mutableStateOf(false) }

  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        try {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        } catch (_: SecurityException) {
        }
        onUpdateItem(item.copy(photoUri = uri.toString()))
      }
    }

  Box(modifier = modifier.fillMaxWidth()) {
    if (item.type == HomeItemType.SYSTEM_WIDGET && item.appWidgetId != -1) {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(theme.palette.surfaceHex).copy(alpha = if (item.widgetTransparent) 0.25f else 0.6f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxSize(),
      ) {
        AndroidView(
          factory = { ctx ->
            val info = widgetHostHolder.appWidgetManager.getAppWidgetInfo(item.appWidgetId)
            val hostView =
              if (info != null) {
                runCatching {
                  widgetHostHolder.appWidgetHost.createView(ctx, item.appWidgetId, info) as AppWidgetHostView
                }.getOrNull()
              } else {
                null
              }
            hostView
              ?: android.widget.TextView(ctx).apply {
                setText(R.string.widget_unavailable)
                setTextColor(android.graphics.Color.WHITE)
                setPadding(24, 24, 24, 24)
              }
          },
          modifier = Modifier.fillMaxSize().padding(4.dp),
        )
      }
    } else {
      val kind = item.customWidgetKind ?: CustomWidgetKind.CLOCK
      GlassSurfaceCard(
        glass = glassConfig,
        palette = theme.palette,
        rules = rules,
        modifier = Modifier.fillMaxSize().testTag("widget_${kind.name.lowercase()}"),
      ) {
        when (kind) {
          CustomWidgetKind.CLOCK -> {
            var now by remember { mutableStateOf(Date()) }
            LaunchedEffect(Unit) {
              while (true) {
                delay(15_000L)
                now = Date()
              }
            }
            val timeStr = remember(now) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) }
            val dateStr = remember(now) { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now) }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = timeStr,
                  fontFamily = fontFamily,
                  fontSize = 34.sp,
                  fontWeight = FontWeight.Bold,
                  color = onSurface,
                )
                Text(
                  text = dateStr,
                  fontFamily = fontFamily,
                  fontSize = 13.sp,
                  color = onSurface.copy(alpha = 0.78f),
                )
              }
              Surface(
                onClick = onOpenThemeGenerator,
                shape = RoundedCornerShape(50),
                color = primary.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.5f)),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = primary, modifier = Modifier.size(15.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(
                    text = theme.name,
                    fontFamily = fontFamily,
                    fontSize = 11.sp,
                    color = onSurface,
                  )
                }
              }
            }
          }

          CustomWidgetKind.SEARCH -> {
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 42.dp)
                  .clickable { onOpenSearch() },
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, contentDescription = "Search launcher", tint = primary)
                Spacer(Modifier.width(10.dp))
                Text(
                  text = "Search apps, settings & shortcuts…",
                  fontFamily = fontFamily,
                  fontSize = 13.sp,
                  color = onSurface.copy(alpha = 0.78f),
                )
              }
              Icon(Icons.Rounded.Mic, contentDescription = "Voice search", tint = secondary, modifier = Modifier.size(18.dp))
            }
          }

          CustomWidgetKind.CALENDAR -> {
            val monthStr = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }
            val dayStr = remember { SimpleDateFormat("d", Locale.getDefault()).format(Date()) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(monthStr.uppercase(), fontFamily = fontFamily, fontSize = 11.sp, color = primary, fontWeight = FontWeight.Bold)
              Text(dayStr, fontFamily = fontFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = onSurface)
              Text("Offline Calendar Ready", fontFamily = fontFamily, fontSize = 12.sp, color = onSurface.copy(alpha = 0.78f))
            }
          }

          CustomWidgetKind.WEATHER -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Local Climate", fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = onSurface)
              }
              Text(
                "Offline Widget • Add system weather widget via Edit Mode",
                fontFamily = fontFamily,
                fontSize = 11.sp,
                color = onSurface.copy(alpha = 0.75f),
                maxLines = 2,
              )
            }
          }

          CustomWidgetKind.BATTERY -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("${capabilities.batteryPercent}%", fontFamily = fontFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface)
                }
                Text(rules.tier.label, fontFamily = fontFamily, fontSize = 10.sp, color = secondary)
              }
              LinearProgressIndicator(
                progress = { capabilities.batteryPercent / 100f },
                color = primary,
                trackColor = onSurface.copy(alpha = 0.16f),
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
              )
              Text(
                "${capabilities.availableRamMb} MB RAM free",
                fontFamily = fontFamily,
                fontSize = 11.sp,
                color = onSurface.copy(alpha = 0.76f),
              )
            }
          }

          CustomWidgetKind.MUSIC -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(primary.copy(alpha = 0.25f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = primary)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                  Text("Media Controller", fontFamily = fontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = onSurface)
                  Text("System Audio Controls", fontFamily = fontFamily, fontSize = 11.sp, color = onSurface.copy(alpha = 0.75f))
                }
              }
              Row {
                IconButton(onClick = { dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS) }) {
                  Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous track", tint = onSurface)
                }
                IconButton(onClick = { dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }) {
                  Icon(Icons.Rounded.PlayArrow, contentDescription = "Play or pause", tint = primary)
                }
                IconButton(onClick = { dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT) }) {
                  Icon(Icons.Rounded.SkipNext, contentDescription = "Next track", tint = onSurface)
                }
              }
            }
          }

          CustomWidgetKind.PHOTO -> {
            var photoBmp by remember(item.photoUri) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(item.photoUri) {
              val uri = item.photoUri ?: return@LaunchedEffect
              photoBmp =
                withContext(Dispatchers.IO) {
                  runCatching {
                    val parsed = Uri.parse(uri)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(parsed)?.use { s ->
                      BitmapFactory.decodeStream(s, null, bounds)
                    }
                    var sample = 1
                    while (bounds.outWidth / sample > 720 || bounds.outHeight / sample > 720) {
                      sample *= 2
                    }
                    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                    context.contentResolver.openInputStream(parsed)?.use { s ->
                      BitmapFactory.decodeStream(s, null, opts)?.asImageBitmap()
                    }
                  }.getOrNull()
                }
            }
            Box(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(110.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                  },
              contentAlignment = Alignment.Center,
            ) {
              if (photoBmp != null) {
                Image(
                  bitmap = photoBmp!!,
                  contentDescription = "Selected photo widget image",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = primary)
                  Spacer(Modifier.height(4.dp))
                  Text("Tap to Pick Photo", fontFamily = fontFamily, fontSize = 12.sp, color = onSurface)
                }
              }
            }
          }

          CustomWidgetKind.NOTES -> {
            Column(
              modifier = Modifier.fillMaxWidth().clickable { showNoteEditor = true },
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("Offline Note", fontFamily = fontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primary)
                Icon(Icons.Rounded.Edit, contentDescription = "Edit note", tint = onSurface.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
              }
              Text(
                text = item.noteText,
                fontFamily = fontFamily,
                fontSize = 13.sp,
                color = onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }

    // Edit Mode overlay controls (Resize, Transparency/Glass toggle, Remove)
    if (isEditMode) {
      Row(
        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (item.type == HomeItemType.CUSTOM_WIDGET) {
          SmallCircleBadgeButton(
            icon = Icons.Rounded.Opacity,
            contentDescription = "Toggle transparent widget",
            onClick = { onUpdateItem(item.copy(widgetTransparent = !item.widgetTransparent)) },
          )
        }
        SmallCircleBadgeButton(
          icon = Icons.Rounded.AspectRatio,
          contentDescription = "Resize widget",
          onClick = {
            val nextSpanX = if (item.spanX >= settings.gridColumns) 2 else settings.gridColumns
            val nextSpanY = if (item.spanY >= 2) 1 else 2
            onUpdateItem(item.copy(spanX = nextSpanX, spanY = nextSpanY))
          },
        )
        SmallCircleBadgeButton(
          icon = Icons.Rounded.Close,
          contentDescription = "Remove widget",
          onClick = onRemoveItem,
        )
      }
    }
  }

  if (showNoteEditor) {
    var draftNote by remember(item.noteText) { mutableStateOf(item.noteText) }
    AlertDialog(
      onDismissRequest = { showNoteEditor = false },
      title = { Text("Edit Offline Note Widget") },
      text = {
        OutlinedTextField(
          value = draftNote,
          onValueChange = { draftNote = it },
          minLines = 3,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateItem(item.copy(noteText = draftNote))
            showNoteEditor = false
          },
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showNoteEditor = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun SmallCircleBadgeButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = Color.Black.copy(alpha = 0.72f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
    modifier = Modifier.size(38.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(16.dp))
    }
  }
}

private fun dispatchMediaKey(context: Context, keyCode: Int) {
  val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
  am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
  am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}
