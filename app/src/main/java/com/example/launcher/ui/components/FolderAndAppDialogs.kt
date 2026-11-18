package com.example.launcher.ui.components

import android.content.pm.ShortcutInfo
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.EffectiveRenderRules
import com.example.domain.model.ThemeSpec
import com.example.launcher.model.*
import com.example.launcher.system.AppCatalogLoader

private val FolderColors =
  listOf(0xFF38BDF8L, 0xFF8B5CF6L, 0xFF10B981L, 0xFFF59E0BL, 0xFFF472B6L, 0xFF1E293BL)

@Composable
fun FolderModalDialog(
  folder: HomeItem,
  allApps: List<InstalledAppInfo>,
  settings: LauncherSettings,
  theme: ThemeSpec,
  rules: EffectiveRenderRules,
  onDismiss: () -> Unit,
  onLaunchApp: (InstalledAppInfo) -> Unit,
  onUpdateFolder: (HomeItem) -> Unit,
  onDeleteFolder: () -> Unit,
) {
  var editing by remember { mutableStateOf(false) }
  var titleDraft by remember(folder.folderTitle) { mutableStateOf(folder.folderTitle) }
  var showAddApps by remember { mutableStateOf(false) }

  val folderApps =
    remember(folder.folderAppPackages, allApps) {
      folder.folderAppPackages.mapNotNull { key -> allApps.firstOrNull { it.componentKey == key } }
    }

  val containerAlpha =
    when (folder.folderStyle) {
      FolderStyle.ROUNDED -> 0.88f
      FolderStyle.GLASS -> 0.58f
      FolderStyle.TRANSPARENT -> 0.28f
    }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = Color(folder.folderColorHex).copy(alpha = containerAlpha * 0.45f)
        .let { Color(theme.palette.surfaceHex).copy(alpha = containerAlpha) },
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
      modifier = Modifier.fillMaxWidth().padding(4.dp).testTag("folder_modal_${folder.id}"),
    ) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (editing) {
            OutlinedTextField(
              value = titleDraft,
              onValueChange = { titleDraft = it },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            IconButton(
              onClick = {
                onUpdateFolder(folder.copy(folderTitle = titleDraft.ifBlank { "Folder" }))
                editing = false
              },
            ) {
              Icon(Icons.Rounded.Check, contentDescription = "Save folder settings")
            }
          } else {
            Text(
              text = folder.folderTitle,
              style = MaterialTheme.typography.titleLarge,
              color = Color(theme.palette.onSurfaceHex),
              modifier = Modifier.weight(1f),
            )
            Row {
              IconButton(onClick = { showAddApps = !showAddApps }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add apps to folder")
              }
              IconButton(onClick = { editing = true }) {
                Icon(Icons.Rounded.Edit, contentDescription = "Customize folder")
              }
            }
          }
        }

        if (editing) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FolderStyle.entries.forEach { fs ->
              FilterChip(
                selected = folder.folderStyle == fs,
                onClick = { onUpdateFolder(folder.copy(folderStyle = fs)) },
                label = { Text(fs.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FolderLayout.entries.forEach { fl ->
              FilterChip(
                selected = folder.folderLayout == fl,
                onClick = { onUpdateFolder(folder.copy(folderLayout = fl)) },
                label = { Text(fl.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f),
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FolderColors.forEach { hex ->
              Box(
                modifier =
                  Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(
                      width = if (folder.folderColorHex == hex) 2.5.dp else 1.dp,
                      color = Color.White,
                      shape = CircleShape,
                    )
                    .clickable { onUpdateFolder(folder.copy(folderColorHex = hex)) },
              )
            }
          }
          TextButton(onClick = onDeleteFolder) {
            Text("Delete Folder", color = MaterialTheme.colorScheme.error)
          }
        }

        if (showAddApps) {
          Text("Tap apps to add or remove:", style = MaterialTheme.typography.labelMedium)
          LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(allApps.take(32), key = { it.componentKey }) { app ->
              val inFolder = app.componentKey in folder.folderAppPackages
              Box(
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                      if (inFolder) Color(theme.palette.primaryHex).copy(alpha = 0.25f)
                      else Color.Transparent,
                    ),
              ) {
                LauncherAppIconTile(
                  app = app,
                  settings = settings,
                  theme = theme,
                  rules = rules,
                  iconSizeDp = 42,
                  onClick = {
                    val next = folder.folderAppPackages.toMutableList()
                    if (inFolder) next.remove(app.componentKey) else next.add(app.componentKey)
                    onUpdateFolder(folder.copy(folderAppPackages = next))
                  },
                  onLongClick = {},
                )
              }
            }
          }
        } else if (folderApps.isEmpty()) {
          Text(
            text = "Folder is empty. Tap + to add installed apps.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(theme.palette.onSurfaceHex).copy(alpha = 0.75f),
          )
        } else {
          LazyVerticalGrid(
            columns = GridCells.Fixed(if (folder.folderLayout == FolderLayout.STACK) 2 else 3),
            modifier = Modifier.heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(folderApps, key = { it.componentKey }) { app ->
              LauncherAppIconTile(
                app = app,
                settings = settings,
                theme = theme,
                rules = rules,
                onClick = { onLaunchApp(app) },
                onLongClick = {
                  val next = folder.folderAppPackages.filterNot { it == app.componentKey }
                  onUpdateFolder(folder.copy(folderAppPackages = next))
                },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun AppQuickActionDialog(
  app: InstalledAppInfo,
  homeItem: HomeItem?,
  settings: LauncherSettings,
  onDismiss: () -> Unit,
  onLaunch: () -> Unit,
  onAddToHome: () -> Unit,
  onRemoveFromHome: (() -> Unit)?,
  onMoveHomeItem: (() -> Unit)?,
  onToggleDock: () -> Unit,
  onToggleHidden: () -> Unit,
  onSetCustomLabel: (String?) -> Unit,
  onSetCustomIconStyle: (LauncherIconStyle?) -> Unit,
  onOpenAppInfo: () -> Unit,
) {
  val context = LocalContext.current
  val shortcuts = remember(app.packageName) { AppCatalogLoader.queryAppShortcuts(context, app.packageName) }
  var labelDraft by remember(app.componentKey) {
    mutableStateOf(settings.customAppLabels[app.componentKey] ?: app.label)
  }
  val inDock = app.componentKey in settings.dockPackages
  val isHidden = app.packageName in settings.hiddenPackages

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(app.label) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (shortcuts.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
          Text("App Shortcuts", style = MaterialTheme.typography.labelMedium)
          shortcuts.forEach { sc: ShortcutInfo ->
            OutlinedButton(
              onClick = {
                AppCatalogLoader.launchShortcut(context, sc)
                onDismiss()
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(sc.shortLabel?.toString() ?: sc.id)
            }
          }
        }

        OutlinedTextField(
          value = labelDraft,
          onValueChange = { labelDraft = it },
          label = { Text("Custom Label") },
          singleLine = true,
          trailingIcon = {
            IconButton(
              onClick = {
                onSetCustomLabel(labelDraft)
                onDismiss()
              },
            ) {
              Icon(Icons.Rounded.Check, contentDescription = "Apply custom label")
            }
          },
          modifier = Modifier.fillMaxWidth(),
        )

        Text("Individual Icon Style", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FilterChip(
            selected = settings.customAppIconStyles[app.componentKey] == null,
            onClick = { onSetCustomIconStyle(null) },
            label = { Text("Theme Default") },
          )
          LauncherIconStyle.entries.forEach { style ->
            FilterChip(
              selected = settings.customAppIconStyles[app.componentKey] == style,
              onClick = { onSetCustomIconStyle(style) },
              label = { Text(style.label) },
            )
          }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = { onToggleDock(); onDismiss() }, modifier = Modifier.weight(1f)) {
            Text(if (inDock) "Undock" else "Pin Dock")
          }
          if (homeItem == null) {
            OutlinedButton(onClick = { onAddToHome(); onDismiss() }, modifier = Modifier.weight(1f)) {
              Text("Add Home")
            }
          } else if (onRemoveFromHome != null) {
            OutlinedButton(onClick = { onRemoveFromHome(); onDismiss() }, modifier = Modifier.weight(1f)) {
              Text("Remove")
            }
          }
        }

        if (homeItem != null && onMoveHomeItem != null) {
          OutlinedButton(
            onClick = { onMoveHomeItem(); onDismiss() },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Move to Next Grid Slot")
          }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = { onToggleHidden(); onDismiss() },
            modifier = Modifier.weight(1f),
          ) {
            Text(if (isHidden) "Unhide" else "Hide App")
          }
          OutlinedButton(
            onClick = { onOpenAppInfo(); onDismiss() },
            modifier = Modifier.weight(1f),
          ) {
            Text("App Info")
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = { onLaunch(); onDismiss() }) {
        Text("Open App")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}
