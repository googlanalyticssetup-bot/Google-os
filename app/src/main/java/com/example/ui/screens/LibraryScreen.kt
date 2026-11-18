package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemeCategory
import com.example.domain.model.ThemeSpec
import com.example.ui.ThemeStudioUiState
import com.example.ui.components.GlassSurfaceCard
import com.example.ui.components.PaletteSwatchRow
import com.example.ui.components.StudioTopHeader

@Composable
fun LibraryScreen(
  state: ThemeStudioUiState,
  onSelectForStudio: (ThemeSpec) -> Unit,
  onActivateTheme: (ThemeSpec) -> Unit,
  onDeleteTheme: (ThemeSpec) -> Unit,
  onExportJson: () -> String,
  onImportJson: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf<ThemeCategory?>(null) }

  val exportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri
      ->
      if (uri != null) {
        val json = onExportJson()
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(json.toByteArray(Charsets.UTF_8))
        }
      }
    }

  val importLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        try {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        } catch (_: SecurityException) {
          // Non-persistable URI is still readable immediately below
        }
        val text =
          context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (!text.isNullOrBlank()) {
          onImportJson(text)
        }
      }
    }

  val filteredThemes =
    remember(state.themes, searchQuery, selectedCategory) {
      state.themes.filter { theme ->
        val matchesCategory = selectedCategory == null || theme.category == selectedCategory
        val matchesQuery =
          searchQuery.isBlank() ||
            theme.name.contains(searchQuery, ignoreCase = true) ||
            theme.description.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
      }
    }

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    StudioTopHeader(
      title = "Offline Theme Library",
      subtitle = "${state.themes.size} themes stored locally • Export/Import JSON",
      rules = state.renderRules,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search 15+ offline themes...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.weight(1f).testTag("library_search_input"),
      )
      FilledTonalIconButton(
        onClick = {
          val safeName =
            state.draftTheme.name.lowercase().replace(Regex("[^a-z0-9]+"), "_") + ".json"
          exportLauncher.launch(safeName)
        },
        modifier = Modifier.size(48.dp).testTag("export_theme_btn"),
      ) {
        Icon(Icons.Rounded.FileUpload, contentDescription = "Export active theme JSON")
      }
      FilledTonalIconButton(
        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
        modifier = Modifier.size(48.dp).testTag("import_theme_btn"),
      ) {
        Icon(Icons.Rounded.FileDownload, contentDescription = "Import theme JSON")
      }
    }

    Row(
      modifier = Modifier.horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = selectedCategory == null,
        onClick = { selectedCategory = null },
        label = { Text("All (${state.themes.size})") },
        modifier = Modifier.testTag("filter_all"),
      )
      ThemeCategory.entries.forEach { cat ->
        FilterChip(
          selected = selectedCategory == cat,
          onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
          label = { Text(cat.label) },
          modifier = Modifier.testTag("filter_${cat.name.lowercase()}"),
        )
      }
    }

    if (filteredThemes.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Rounded.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
          )
          Spacer(Modifier.height(8.dp))
          Text("No themes match \"$searchQuery\"", style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(4.dp))
          Text(
            "Clear the search filter or generate a new theme in AI Studio.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 270.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(filteredThemes, key = { it.id }) { theme ->
          val isActive = theme.id == state.activeTheme.id
          ThemeLibraryCard(
            theme = theme,
            isActive = isActive,
            state = state,
            onActivate = { onActivateTheme(theme) },
            onCustomize = { onSelectForStudio(theme) },
            onDelete = { onDeleteTheme(theme) },
          )
        }
      }
    }
  }
}

@Composable
private fun ThemeLibraryCard(
  theme: ThemeSpec,
  isActive: Boolean,
  state: ThemeStudioUiState,
  onActivate: () -> Unit,
  onCustomize: () -> Unit,
  onDelete: () -> Unit,
) {
  val primary = Color(theme.palette.primaryHex)
  val secondary = Color(theme.palette.secondaryHex)
  val bg = Color(theme.palette.backgroundHex)

  GlassSurfaceCard(
    glass = state.draftTheme.glass.copy(transparency = 0.3f),
    palette = theme.palette,
    rules = state.renderRules,
    modifier = Modifier.fillMaxWidth().testTag("theme_card_${theme.id}"),
  ) {
    // Visual gradient banner preview
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(74.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Brush.linearGradient(listOf(bg, primary.copy(alpha = 0.72f), secondary.copy(alpha = 0.55f))))
          .padding(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = Color.Black.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        ) {
          Text(
            text = if (theme.isBuiltIn) theme.category.label else "Custom",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
        if (isActive) {
          Surface(
            shape = RoundedCornerShape(50),
            color = primary,
          ) {
            Text(
              text = "ACTIVE",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = Color.Black,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = theme.name,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(Modifier.width(8.dp))
      PaletteSwatchRow(theme.palette)
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = theme.description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "${theme.glass.style.label} • ${theme.icons.style.label} • ${theme.lockScreen.clockStyle.label}",
      style = MaterialTheme.typography.labelSmall,
      color = primary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Button(
        onClick = onActivate,
        modifier = Modifier.weight(1f).heightIn(min = 44.dp).testTag("activate_${theme.id}"),
      ) {
        Text(if (isActive) "Active" else "Apply")
      }
      OutlinedButton(
        onClick = onCustomize,
        modifier = Modifier.weight(1f).heightIn(min = 44.dp).testTag("edit_${theme.id}"),
      ) {
        Text("Studio")
      }
      if (!theme.isBuiltIn) {
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(48.dp).testTag("delete_${theme.id}"),
        ) {
          Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete custom theme")
        }
      }
    }
  }
}
