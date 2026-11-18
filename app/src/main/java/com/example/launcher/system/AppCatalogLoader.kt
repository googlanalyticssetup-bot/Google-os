package com.example.launcher.system

import android.annotation.SuppressLint
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.example.launcher.model.AppCategory
import com.example.launcher.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactSearchMatch(
  val id: String,
  val displayName: String,
  val lookupUri: Uri?,
)

object AppCatalogLoader {

  // Bounded icon cache (8 MB max) for low-RAM entry-level phone safety
  private val iconCache =
    object : LruCache<String, ImageBitmap>(8 * 1024 * 1024) {
      override fun sizeOf(key: String, value: ImageBitmap): Int =
        value.width * value.height * 4
    }

  fun trimIconCache() {
    iconCache.evictAll()
  }

  suspend fun loadInstalledApps(context: Context): List<InstalledAppInfo> =
    withContext(Dispatchers.IO) {
      val pm = context.packageManager
      val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
      val resolved =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
          @Suppress("DEPRECATION")
          pm.queryIntentActivities(intent, 0)
        }

      val selfPkg = context.packageName
      val workProfileApps =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          runCatching {
            val la = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val selfHandle = Process.myUserHandle()
            la?.profiles
              ?.filter { it != selfHandle }
              ?.flatMap { profile ->
                la.getActivityList(null, profile).orEmpty().mapNotNull { info ->
                  val comp = info.componentName
                  if (comp.packageName == selfPkg) return@mapNotNull null
                  val appInfo = info.applicationInfo
                  val label = info.label?.toString()?.trim().orEmpty().ifEmpty { comp.packageName }
                  InstalledAppInfo(
                    packageName = comp.packageName,
                    activityName = comp.className,
                    label = "$label (Work)",
                    category = classifyApp(appInfo, comp.packageName, label),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isWorkProfile = true,
                  )
                }
              }
              .orEmpty()
          }.getOrDefault(emptyList())
        } else {
          emptyList()
        }

      val primaryApps =
        resolved
          .asSequence()
          .filter { it.activityInfo != null && it.activityInfo.packageName != selfPkg }
          .map { ri ->
            val ai = ri.activityInfo
            val appInfo = ai.applicationInfo
            val label = ri.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { ai.packageName }
            val isSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            InstalledAppInfo(
              packageName = ai.packageName,
              activityName = ai.name,
              label = label,
              category = classifyApp(appInfo, ai.packageName, label),
              isSystemApp = isSys,
              isWorkProfile = false,
            )
          }
          .toList()

      (primaryApps + workProfileApps)
        .distinctBy { "${it.componentKey}:${it.isWorkProfile}" }
        .sortedBy { it.label.lowercase() }
    }

  suspend fun loadAppIcon(
    context: Context,
    packageName: String,
    activityName: String,
    iconPx: Int = 128,
  ): ImageBitmap? =
    withContext(Dispatchers.IO) {
      val key = "$packageName/$activityName@$iconPx"
      iconCache.get(key)?.let { return@withContext it }

      val pm = context.packageManager
      val drawable: Drawable? =
        runCatching {
          pm.getActivityIcon(ComponentName(packageName, activityName))
        }.getOrNull()
          ?: runCatching { pm.getApplicationIcon(packageName) }.getOrNull()

      drawable?.let { d ->
        val bmp = drawableToScaledBitmap(d, iconPx.coerceIn(64, 160))
        val img = bmp.asImageBitmap()
        iconCache.put(key, img)
        img
      }
    }

  fun launchApp(context: Context, packageName: String, activityName: String): Boolean {
    return runCatching {
      val intent =
        Intent(Intent.ACTION_MAIN).apply {
          addCategory(Intent.CATEGORY_LAUNCHER)
          component = ComponentName(packageName, activityName)
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
      context.startActivity(intent)
      true
    }.getOrElse {
      runCatching {
        val fallback = context.packageManager.getLaunchIntentForPackage(packageName)
        if (fallback != null) {
          fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(fallback)
          true
        } else {
          false
        }
      }.getOrDefault(false)
    }
  }

  fun launchInstalledApp(context: Context, app: InstalledAppInfo): Boolean {
    if (!app.isWorkProfile || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return launchApp(context, app.packageName, app.activityName)
    }
    return runCatching {
      val la = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return false
      val selfHandle = Process.myUserHandle()
      val workProfile = la.profiles.firstOrNull { it != selfHandle } ?: return false
      la.startMainActivity(ComponentName(app.packageName, app.activityName), workProfile, null, null)
      true
    }.getOrElse {
      launchApp(context, app.packageName, app.activityName)
    }
  }

  fun openAppInfo(context: Context, packageName: String) {
    runCatching {
      val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = Uri.fromParts("package", packageName, null)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
    }
  }

  fun queryAppShortcuts(context: Context, packageName: String): List<ShortcutInfo> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
    val la = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return emptyList()
    if (!la.hasShortcutHostPermission()) return emptyList()
    return runCatching {
      val query =
        LauncherApps.ShortcutQuery().apply {
          setPackage(packageName)
          setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
              LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
              LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
          )
        }
      la.getShortcuts(query, Process.myUserHandle())?.take(4) ?: emptyList()
    }.getOrDefault(emptyList())
  }

  fun launchShortcut(context: Context, shortcut: ShortcutInfo): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return false
    val la = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return false
    return runCatching {
      val user: UserHandle = Process.myUserHandle()
      la.startShortcut(shortcut.`package`, shortcut.id, null, null, user)
      true
    }.getOrDefault(false)
  }

  suspend fun searchContactsIfPermitted(
    context: Context,
    query: String,
  ): List<ContactSearchMatch> =
    withContext(Dispatchers.IO) {
      if (query.isBlank()) return@withContext emptyList()
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) return@withContext emptyList()

      val matches = mutableListOf<ContactSearchMatch>()
      runCatching {
        context.contentResolver
          .query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
              ContactsContract.Contacts._ID,
              ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
              ContactsContract.Contacts.LOOKUP_KEY,
            ),
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?",
            arrayOf("%$query%"),
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC",
          )
          ?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val keyIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
            if (idIdx < 0 || nameIdx < 0) return@use
            while (cursor.moveToNext() && matches.size < 6) {
              val id = cursor.getString(idIdx) ?: continue
              val name = cursor.getString(nameIdx) ?: continue
              val lookupKey = if (keyIdx >= 0) cursor.getString(keyIdx) else null
              val uri =
                if (lookupKey != null) {
                  ContactsContract.Contacts.getLookupUri(id.toLongOrNull() ?: 0L, lookupKey)
                } else null
              matches.add(ContactSearchMatch(id = id, displayName = name, lookupUri = uri))
            }
          }
      }
      matches
    }

  @SuppressLint("SwitchIntDef")
  private fun classifyApp(info: ApplicationInfo, pkg: String, label: String): AppCategory {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      when (info.category) {
        ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
        ApplicationInfo.CATEGORY_AUDIO,
        ApplicationInfo.CATEGORY_VIDEO,
        ApplicationInfo.CATEGORY_IMAGE,
        -> return AppCategory.MEDIA
        ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
        else -> {}
      }
    }
    val lower = "$pkg $label".lowercase()
    return when {
      listOf("dialer", "phone", "message", "sms", "mail", "gmail", "contacts", "call").any {
        it in lower
      } -> AppCategory.COMMUNICATION
      listOf("camera", "photo", "gallery", "music", "video", "spotify", "youtube", "player").any {
        it in lower
      } -> AppCategory.MEDIA
      listOf("chat", "whatsapp", "telegram", "insta", "discord", "social", "reddit").any {
        it in lower
      } -> AppCategory.SOCIAL
      listOf("note", "docs", "sheet", "calendar", "drive", "office", "clock", "calc").any {
        it in lower
      } -> AppCategory.PRODUCTIVITY
      listOf("game", "playgames").any { it in lower } -> AppCategory.GAMES
      (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
        listOf("settings", "android", "system", "deskclock", "files").any { it in lower } ->
        AppCategory.SYSTEM
      else -> AppCategory.TOOLS
    }
  }

  private fun drawableToScaledBitmap(drawable: Drawable, targetSize: Int): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
      val src = drawable.bitmap
      if (src.width <= targetSize && src.height <= targetSize) return src
      return src.scale(targetSize, targetSize, true)
    }
    val bitmap = createBitmap(targetSize, targetSize)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
  }
}
