package com.example.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.EffectiveRenderRules
import com.example.domain.model.ResolvedTier
import com.example.domain.model.ThemeSpec
import com.example.launcher.model.*
import com.example.launcher.system.AppCatalogLoader
import com.example.ui.components.fontStyleToFamily

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherAppIconTile(
  app: InstalledAppInfo,
  settings: LauncherSettings,
  theme: ThemeSpec,
  rules: EffectiveRenderRules,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
  showLabel: Boolean = settings.showHomeLabels,
  overrideStyle: LauncherIconStyle? = null,
  iconSizeDp: Int = settings.iconSizeDp,
) {
  val context = LocalContext.current
  val effectiveStyle =
    overrideStyle ?: settings.customAppIconStyles[app.componentKey] ?: settings.iconStyle
  var loadedIcon by remember(app.componentKey) { mutableStateOf<ImageBitmap?>(null) }

  LaunchedEffect(app.componentKey) {
    loadedIcon = AppCatalogLoader.loadAppIcon(context, app.packageName, app.activityName)
  }

  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (pressed && rules.animationScale > 0f) 0.90f else 1f,
      animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
      label = "icon_press",
    )

  val shape: Shape =
    when (effectiveStyle) {
      LauncherIconStyle.ROUND -> CircleShape
      LauncherIconStyle.SQUIRCLE, LauncherIconStyle.COLOROS ->
        RoundedCornerShape((iconSizeDp * 0.34f).dp)
      else -> RoundedCornerShape((iconSizeDp * (settings.iconCornerPercent / 100f)).dp)
    }

  val primary = Color(theme.palette.primaryHex)
  val secondary = Color(theme.palette.secondaryHex)
  val onSurface = Color(theme.palette.onSurfaceHex)
  val fontFamily = fontStyleToFamily(settings.fontStyle)

  val bgModifier =
    when (effectiveStyle) {
      LauncherIconStyle.COLOROS ->
        Modifier.background(
          Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), primary.copy(alpha = 0.16f))),
        )
      LauncherIconStyle.GLASS ->
        Modifier.background(
          Brush.linearGradient(listOf(Color.White.copy(alpha = 0.24f), primary.copy(alpha = 0.28f))),
        ).border(1.dp, Color.White.copy(alpha = 0.45f), shape)
      LauncherIconStyle.LIQUID_GLASS ->
        Modifier.background(
          Brush.linearGradient(listOf(primary.copy(alpha = 0.36f), secondary.copy(alpha = 0.30f))),
        ).border(1.2.dp, Color.White.copy(alpha = 0.55f), shape)
      LauncherIconStyle.NEON ->
        Modifier.background(Color(0xFF070812)).border(1.8.dp, primary, shape)
      LauncherIconStyle.AMOLED ->
        Modifier.background(Color.Black).border(1.dp, Color.White.copy(alpha = 0.22f), shape)
      LauncherIconStyle.MINIMAL ->
        Modifier.background(Color(theme.palette.surfaceHex).copy(alpha = 0.9f))
      LauncherIconStyle.GRADIENT ->
        Modifier.background(Brush.linearGradient(listOf(primary, secondary)))
      LauncherIconStyle.THREE_D ->
        Modifier.background(
          Brush.verticalGradient(listOf(primary.copy(alpha = 0.72f), Color(theme.palette.surfaceHex))),
        ).border(1.5.dp, Color.White.copy(alpha = 0.38f), shape)
      LauncherIconStyle.TRANSPARENT ->
        Modifier.background(Color.White.copy(alpha = 0.08f))
          .border(1.dp, Color.White.copy(alpha = 0.32f), shape)
      LauncherIconStyle.ROUND, LauncherIconStyle.SQUIRCLE ->
        Modifier.background(Color(theme.palette.surfaceHex).copy(alpha = 0.75f))
    }

  val shadowModifier =
    if (settings.iconShadowEnabled && rules.tier != ResolvedTier.LITE) {
      Modifier.shadow(elevation = 6.dp, shape = shape, ambientColor = primary.copy(alpha = 0.4f))
    } else {
      Modifier
    }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      modifier
        .scale(scale)
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .combinedClickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
          onLongClick = onLongClick,
        )
        .padding(2.dp)
        .testTag("app_icon_${app.packageName}"),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier
          .size(iconSizeDp.dp)
          .then(shadowModifier)
          .clip(shape)
          .then(bgModifier),
    ) {
      val bmp = loadedIcon
      if (bmp != null) {
        val innerPadding =
          when (effectiveStyle) {
            LauncherIconStyle.COLOROS, LauncherIconStyle.ROUND, LauncherIconStyle.SQUIRCLE -> 4.dp
            else -> 7.dp
          }
        Image(
          bitmap = bmp,
          contentDescription = "${app.label} app icon",
          modifier = Modifier.fillMaxSize().padding(innerPadding).clip(shape),
        )
      } else {
        Icon(
          imageVector = Icons.Rounded.Apps,
          contentDescription = "${app.label} app icon",
          tint = onSurface,
          modifier = Modifier.size((iconSizeDp * 0.55f).dp),
        )
      }
    }

    if (showLabel) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = app.label,
        fontFamily = fontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherFolderTile(
  folder: HomeItem,
  allApps: List<InstalledAppInfo>,
  settings: LauncherSettings,
  theme: ThemeSpec,
  rules: EffectiveRenderRules,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val iconSize = settings.iconSizeDp.dp
  val shape = RoundedCornerShape((settings.iconSizeDp * 0.34f).dp)
  val folderColor = Color(folder.folderColorHex)
  val onSurface = Color(theme.palette.onSurfaceHex)
  val fontFamily = fontStyleToFamily(settings.fontStyle)

  val previewApps =
    remember(folder.folderAppPackages, allApps) {
      folder.folderAppPackages.mapNotNull { key -> allApps.firstOrNull { it.componentKey == key } }.take(4)
    }

  val containerColor =
    when (folder.folderStyle) {
      FolderStyle.ROUNDED -> folderColor.copy(alpha = 0.38f)
      FolderStyle.GLASS -> folderColor.copy(alpha = 0.24f)
      FolderStyle.TRANSPARENT -> Color.White.copy(alpha = 0.08f)
    }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .padding(2.dp)
        .testTag("folder_tile_${folder.id}"),
  ) {
    Surface(
      shape = shape,
      color = containerColor,
      border =
        BorderStroke(
          1.dp,
          if (rules.enableReflections) Color.White.copy(alpha = 0.42f)
          else folderColor.copy(alpha = 0.5f),
        ),
      modifier = Modifier.size(iconSize),
    ) {
      if (folder.folderLayout == FolderLayout.STACK && previewApps.isNotEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(8.dp)) {
          previewApps.reversed().forEachIndexed { idx, app ->
            var bmp by remember(app.componentKey) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(app.componentKey) {
              bmp = AppCatalogLoader.loadAppIcon(context, app.packageName, app.activityName, 64)
            }
            bmp?.let {
              Image(
                bitmap = it,
                contentDescription = null,
                modifier =
                  Modifier
                    .size(20.dp)
                    .offset(x = (idx * 3 - 4).dp, y = (idx * -2 + 2).dp)
                    .clip(RoundedCornerShape(6.dp)),
              )
            }
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxSize().padding(6.dp),
          verticalArrangement = Arrangement.SpaceEvenly,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          for (rowIdx in 0..1) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              for (colIdx in 0..1) {
                val app = previewApps.getOrNull(rowIdx * 2 + colIdx)
                if (app != null) {
                  var bmp by remember(app.componentKey) { mutableStateOf<ImageBitmap?>(null) }
                  LaunchedEffect(app.componentKey) {
                    bmp = AppCatalogLoader.loadAppIcon(context, app.packageName, app.activityName, 64)
                  }
                  if (bmp != null) {
                    Image(
                      bitmap = bmp!!,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)),
                    )
                  } else {
                    Box(
                      modifier =
                        Modifier
                          .size(14.dp)
                          .clip(CircleShape)
                          .background(Color.White.copy(alpha = 0.35f)),
                    )
                  }
                } else {
                  Box(modifier = Modifier.size(14.dp))
                }
              }
            }
          }
        }
      }
    }

    if (settings.showHomeLabels) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = folder.folderTitle,
        fontFamily = fontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
    }
  }
}
