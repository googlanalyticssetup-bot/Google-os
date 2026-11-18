package com.example.launcher.system

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.os.StatFs
import android.os.UserManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

data class ControlCenterSystemSnapshot(
  val wifiConnected: Boolean = false,
  val cellularConnected: Boolean = false,
  val bluetoothEnabled: Boolean? = null, // null if BLUETOOTH_CONNECT missing on API 31+
  val airplaneModeOn: Boolean = false,
  val autoRotateOn: Boolean = false,
  val darkModeOn: Boolean = false,
  val powerSaverOn: Boolean = false,
  val torchAvailable: Boolean = false,
  val ringerMode: Int = AudioManager.RINGER_MODE_NORMAL,
  val brightnessPercent: Int = 50,
  val canWriteSystemSettings: Boolean = false,
  val mediaVolume: Int = 5,
  val mediaMaxVolume: Int = 15,
  val ringVolume: Int = 5,
  val ringMaxVolume: Int = 7,
  val batteryPercent: Int = 85,
  val isCharging: Boolean = false,
  val batteryTempCelsius: Float = 0f,
  val storageUsedGb: Float = 0f,
  val storageTotalGb: Float = 0f,
  val hasWorkProfile: Boolean = false,
  val notificationAccessGranted: Boolean = false,
  val usageStatsGranted: Boolean = false,
  val todayScreenMinutes: Long? = null,
)

object ControlCenterSystemController {

  fun inspectSnapshot(context: Context): ControlCenterSystemSnapshot {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
    val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    val cellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

    val btEnabled: Boolean? =
      runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
          ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
          PackageManager.PERMISSION_GRANTED
        ) {
          null
        } else {
          val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
          bm?.adapter?.isEnabled
        }
      }.getOrNull()

    val airplane =
      Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
    val autoRotate =
      Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
    val darkMode =
      (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val powerSaver = pm?.isPowerSaveMode == true

    val hasFlash =
      context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) &&
        findTorchCameraId(context) != null

    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val ringerMode = am?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
    val mediaVol = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
    val mediaMax = (am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
    val ringVol = am?.getStreamVolume(AudioManager.STREAM_RING) ?: 0
    val ringMax = (am?.getStreamMaxVolume(AudioManager.STREAM_RING) ?: 7).coerceAtLeast(1)

    val rawBrightness =
      Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
    val brightnessPct = ((rawBrightness.coerceIn(0, 255) / 255f) * 100f).roundToInt()
    val canWrite = Settings.System.canWrite(context)

    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val batteryPct = if (level >= 0 && scale > 0) ((level * 100f) / scale).roundToInt() else 85
    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging =
      status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0

    val (usedGb, totalGb) = readStorageGb()
    val workProfile = hasManagedWorkProfile(context)
    val notifAccess = NotificationStreamHub.isNotificationAccessGranted(context)
    val usageGranted = isUsageStatsPermissionGranted(context)
    val screenMins = if (usageGranted) readTodayUsageMinutes(context) else null

    return ControlCenterSystemSnapshot(
      wifiConnected = wifi,
      cellularConnected = cellular,
      bluetoothEnabled = btEnabled,
      airplaneModeOn = airplane,
      autoRotateOn = autoRotate,
      darkModeOn = darkMode,
      powerSaverOn = powerSaver,
      torchAvailable = hasFlash,
      ringerMode = ringerMode,
      brightnessPercent = brightnessPct,
      canWriteSystemSettings = canWrite,
      mediaVolume = mediaVol,
      mediaMaxVolume = mediaMax,
      ringVolume = ringVol,
      ringMaxVolume = ringMax,
      batteryPercent = batteryPct,
      isCharging = charging,
      batteryTempCelsius = tempTenths / 10f,
      storageUsedGb = usedGb,
      storageTotalGb = totalGb,
      hasWorkProfile = workProfile,
      notificationAccessGranted = notifAccess,
      usageStatsGranted = usageGranted,
      todayScreenMinutes = screenMins,
    )
  }

  fun setTorchEnabled(context: Context, enabled: Boolean): Boolean {
    val camMgr = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
    val camId = findTorchCameraId(context) ?: return false
    return runCatching {
      camMgr.setTorchMode(camId, enabled)
      true
    }.getOrDefault(false)
  }

  private fun findTorchCameraId(context: Context): String? {
    val camMgr = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return null
    return runCatching {
      camMgr.cameraIdList.firstOrNull { id ->
        val chars = camMgr.getCameraCharacteristics(id)
        val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
        hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK
      } ?: camMgr.cameraIdList.firstOrNull { id ->
        camMgr.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
      }
    }.getOrNull()
  }

  fun setSystemBrightnessPercent(context: Context, percent: Int): Boolean {
    if (!Settings.System.canWrite(context)) return false
    val value = ((percent.coerceIn(5, 100) / 100f) * 255f).roundToInt().coerceIn(10, 255)
    return runCatching {
      Settings.System.putInt(
        context.contentResolver,
        Settings.System.SCREEN_BRIGHTNESS_MODE,
        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
      )
      Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
      true
    }.getOrDefault(false)
  }

  fun setAutoRotate(context: Context, enabled: Boolean): Boolean {
    if (!Settings.System.canWrite(context)) return false
    return runCatching {
      Settings.System.putInt(
        context.contentResolver,
        Settings.System.ACCELEROMETER_ROTATION,
        if (enabled) 1 else 0,
      )
      true
    }.getOrDefault(false)
  }

  fun setMediaVolume(context: Context, index: Int) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    am.setStreamVolume(AudioManager.STREAM_MUSIC, index.coerceIn(0, max), 0)
  }

  fun cycleRingerMode(context: Context): Boolean {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    val canSilence = nm?.isNotificationPolicyAccessGranted == true
    val next =
      when (am.ringerMode) {
        AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
        AudioManager.RINGER_MODE_VIBRATE ->
          if (canSilence) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
        else -> AudioManager.RINGER_MODE_NORMAL
      }
    return runCatching {
      am.ringerMode = next
      true
    }.getOrDefault(false)
  }

  fun openSystemPanel(context: Context, action: String) {
    runCatching {
      val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    }.onFailure {
      runCatching {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      }
    }
  }

  fun openWifiPanel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      openSystemPanel(context, Settings.Panel.ACTION_WIFI)
    } else {
      openSystemPanel(context, Settings.ACTION_WIFI_SETTINGS)
    }
  }

  fun openMobileDataSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      openSystemPanel(context, Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
    } else {
      openSystemPanel(context, Settings.ACTION_DATA_ROAMING_SETTINGS)
    }
  }

  fun openBluetoothSettings(context: Context) =
    openSystemPanel(context, Settings.ACTION_BLUETOOTH_SETTINGS)

  fun openAirplaneSettings(context: Context) =
    openSystemPanel(context, Settings.ACTION_AIRPLANE_MODE_SETTINGS)

  fun openEyeComfortSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      openSystemPanel(context, Settings.ACTION_NIGHT_DISPLAY_SETTINGS)
    } else {
      openSystemPanel(context, Settings.ACTION_DISPLAY_SETTINGS)
    }
  }

  fun openDisplaySettings(context: Context) =
    openSystemPanel(context, Settings.ACTION_DISPLAY_SETTINGS)

  fun openBatterySaverSettings(context: Context) =
    openSystemPanel(context, Settings.ACTION_BATTERY_SAVER_SETTINGS)

  fun openWriteSettingsPermission(context: Context) {
    runCatching {
      val intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
          data = android.net.Uri.parse("package:${context.packageName}")
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
    }
  }

  fun openUsageAccessSettings(context: Context) =
    openSystemPanel(context, Settings.ACTION_USAGE_ACCESS_SETTINGS)

  fun readStorageGb(): Pair<Float, Float> =
    runCatching {
      val stat = StatFs(Environment.getDataDirectory().path)
      val total = stat.totalBytes.toFloat() / (1024f * 1024f * 1024f)
      val avail = stat.availableBytes.toFloat() / (1024f * 1024f * 1024f)
      val used = (total - avail).coerceAtLeast(0f)
      used to total.coerceAtLeast(1f)
    }.getOrDefault(0f to 1f)

  fun hasManagedWorkProfile(context: Context): Boolean =
    runCatching {
      val um = context.getSystemService(Context.USER_SERVICE) as? UserManager
      val self = Process.myUserHandle()
      um?.userProfiles.orEmpty().any { it != self }
    }.getOrDefault(false)

  fun isUsageStatsPermissionGranted(context: Context): Boolean =
    runCatching {
      val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
      @Suppress("DEPRECATION")
      val mode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
          )
        } else {
          appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
          )
        }
      mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

  private fun readTodayUsageMinutes(context: Context): Long? =
    runCatching {
      val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
      val end = System.currentTimeMillis()
      val start = end - 24L * 60L * 60L * 1000L
      val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
      val totalMs = stats?.sumOf { it.totalTimeInForeground } ?: 0L
      totalMs / 60_000L
    }.getOrNull()
}
