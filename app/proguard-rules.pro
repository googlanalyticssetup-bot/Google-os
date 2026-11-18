# Add project specific ProGuard rules here.
# Keep Room database, entities, and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Android AppWidgetProvider and DeviceAdminReceiver components declared in AndroidManifest.xml
-keep public class com.example.widget.ThemeStudioWidgetProvider { *; }
-keep public class com.example.launcher.system.LauncherDeviceAdminReceiver { *; }

# Keep domain and launcher data models
-keep class com.example.domain.model.** { *; }
-keep class com.example.launcher.model.** { *; }

# Keep line numbers for stack traces if minification is enabled
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
