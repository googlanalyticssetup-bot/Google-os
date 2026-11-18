package com.example.launcher.data

import com.example.domain.model.ThemeSpec
import com.example.engine.ThemeJsonCodec
import com.example.launcher.model.*
import org.json.JSONArray
import org.json.JSONObject

data class LauncherBackupPayload(
  val settings: LauncherSettings,
  val homeItems: List<HomeItem>,
  val activeTheme: ThemeSpec?,
)

object LauncherBackupCodec {

  fun encodeBackup(
    settings: LauncherSettings,
    homeItems: List<HomeItem>,
    activeTheme: ThemeSpec?,
  ): String {
    val root = JSONObject()
    root.put("backupVersion", 1)
    root.put("exportedAt", System.currentTimeMillis())
    root.put("settings", JSONObject(LauncherPreferencesStore.encodeSettings(settings)))

    val itemsArr = JSONArray()
    homeItems.forEach { item ->
      val o = JSONObject()
      o.put("id", item.id)
      o.put("type", item.type.name)
      o.put("page", item.page)
      o.put("row", item.row)
      o.put("col", item.col)
      o.put("spanX", item.spanX)
      o.put("spanY", item.spanY)
      o.put("packageName", item.packageName)
      o.put("activityName", item.activityName)
      if (item.customLabel != null) o.put("customLabel", item.customLabel)
      if (item.customIconStyle != null) o.put("customIconStyle", item.customIconStyle.name)
      o.put("folderTitle", item.folderTitle)
      o.put("folderColorHex", item.folderColorHex)
      o.put("folderStyle", item.folderStyle.name)
      o.put("folderLayout", item.folderLayout.name)
      o.put("folderAppPackages", JSONArray(item.folderAppPackages))
      if (item.customWidgetKind != null) o.put("customWidgetKind", item.customWidgetKind.name)
      o.put("widgetTransparent", item.widgetTransparent)
      o.put("widgetGlass", item.widgetGlass)
      o.put("noteText", item.noteText)
      itemsArr.put(o)
    }
    root.put("homeItems", itemsArr)

    if (activeTheme != null) {
      root.put("activeTheme", JSONObject(ThemeJsonCodec.encode(activeTheme)))
    }
    return root.toString(2)
  }

  fun decodeBackup(rawJson: String): Result<LauncherBackupPayload> =
    runCatching {
      val root = JSONObject(rawJson)
      val settings =
        LauncherPreferencesStore.decodeSettings(root.getJSONObject("settings").toString())
      val itemsArr = root.optJSONArray("homeItems") ?: JSONArray()
      val items =
        (0 until itemsArr.length()).map { idx ->
          val o = itemsArr.getJSONObject(idx)
          val fArr = o.optJSONArray("folderAppPackages") ?: JSONArray()
          HomeItem(
            id = o.getString("id"),
            type =
              HomeItemType.entries.firstOrNull { it.name == o.optString("type") }
                ?: HomeItemType.APP,
            page = o.optInt("page", 0),
            row = o.optInt("row", 0),
            col = o.optInt("col", 0),
            spanX = o.optInt("spanX", 1),
            spanY = o.optInt("spanY", 1),
            packageName = o.optString("packageName", ""),
            activityName = o.optString("activityName", ""),
            customLabel = o.optString("customLabel", "").ifBlank { null },
            customIconStyle =
              o.optString("customIconStyle", "").let { s ->
                LauncherIconStyle.entries.firstOrNull { it.name == s }
              },
            folderTitle = o.optString("folderTitle", "Folder"),
            folderColorHex = o.optLong("folderColorHex", 0xFF8B5CF6),
            folderStyle =
              FolderStyle.entries.firstOrNull { it.name == o.optString("folderStyle") }
                ?: FolderStyle.GLASS,
            folderLayout =
              FolderLayout.entries.firstOrNull { it.name == o.optString("folderLayout") }
                ?: FolderLayout.GRID,
            folderAppPackages = (0 until fArr.length()).map { fArr.getString(it) },
            customWidgetKind =
              o.optString("customWidgetKind", "").let { k ->
                CustomWidgetKind.entries.firstOrNull { it.name == k }
              },
            appWidgetId = -1, // System widget IDs are device-session bound
            widgetTransparent = o.optBoolean("widgetTransparent", false),
            widgetGlass = o.optBoolean("widgetGlass", true),
            noteText = o.optString("noteText", ""),
          )
        }
      val theme =
        root.optJSONObject("activeTheme")?.let {
          ThemeJsonCodec.decode(it.toString(), overrideAsCustomImport = true).getOrNull()
        }
      LauncherBackupPayload(settings = settings, homeItems = items, activeTheme = theme)
    }
}
