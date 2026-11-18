package com.example.launcher.system

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.speech.RecognizerIntent

class LauncherDeviceAdminReceiver : DeviceAdminReceiver()

object LauncherSystemBridge {

  const val APP_WIDGET_HOST_ID = 4097

  fun isDefaultHomeLauncher(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolveInfo =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.resolveActivity(
          intent,
          PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
      } else {
        @Suppress("DEPRECATION")
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
      }
    return resolveInfo?.activityInfo?.packageName == context.packageName
  }

  fun buildDefaultHomeIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val rm = context.getSystemService(RoleManager::class.java)
      if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
        return rm.createRequestRoleIntent(RoleManager.ROLE_HOME)
      }
    }
    return Intent(Settings.ACTION_HOME_SETTINGS)
  }

  fun isDeviceAdminActive(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
    val admin = ComponentName(context, LauncherDeviceAdminReceiver::class.java)
    return dpm.isAdminActive(admin)
  }

  fun buildEnableDeviceAdminIntent(context: Context): Intent {
    val admin = ComponentName(context, LauncherDeviceAdminReceiver::class.java)
    return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
      putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
      putExtra(
        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
        "Optional: enables double-tap on an empty Home Screen spot to lock the screen offline.",
      )
    }
  }

  fun tryLockScreen(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
    val admin = ComponentName(context, LauncherDeviceAdminReceiver::class.java)
    return if (dpm.isAdminActive(admin)) {
      dpm.lockNow()
      true
    } else {
      false
    }
  }

  fun tryExpandNotificationShade(context: Context): Boolean {
    return runCatching {
      @Suppress("WrongConstant")
      val sbService = context.getSystemService("statusbar") ?: return false
      val clazz = Class.forName("android.app.StatusBarManager")
      val expand = clazz.getMethod("expandNotificationsPanel")
      expand.invoke(sbService)
      true
    }.getOrDefault(false)
  }

  fun isSpeechRecognitionAvailable(context: Context): Boolean {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    return intent.resolveActivity(context.packageManager) != null
  }

  fun getInstalledAppWidgetProviders(context: Context): List<AppWidgetProviderInfo> {
    val awm = AppWidgetManager.getInstance(context.applicationContext)
    return runCatching { awm.installedProviders }.getOrDefault(emptyList())
  }
}

class LauncherWidgetHostHolder(context: Context) {
  val appWidgetHost = AppWidgetHost(context.applicationContext, LauncherSystemBridge.APP_WIDGET_HOST_ID)
  val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context.applicationContext)

  fun startListening() {
    runCatching { appWidgetHost.startListening() }
  }

  fun stopListening() {
    runCatching { appWidgetHost.stopListening() }
  }
}
