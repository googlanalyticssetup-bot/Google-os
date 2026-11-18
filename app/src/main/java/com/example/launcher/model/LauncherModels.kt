package com.example.launcher.model

import com.example.domain.model.FontStyleOption
import com.example.domain.model.PerformanceMode
import com.example.domain.model.TileShape

enum class AppCategory(val label: String) {
  ALL("All"),
  COMMUNICATION("Communication"),
  MEDIA("Media"),
  TOOLS("Tools"),
  PRODUCTIVITY("Productivity"),
  SOCIAL("Social"),
  GAMES("Games"),
  SYSTEM("System"),
}

data class InstalledAppInfo(
  val packageName: String,
  val activityName: String,
  val label: String,
  val category: AppCategory,
  val isSystemApp: Boolean = false,
  val isWorkProfile: Boolean = false,
  val launchCount: Int = 0,
  val lastLaunchedAt: Long = 0L,
) {
  val componentKey: String get() = "$packageName/$activityName"
}

enum class LauncherIconStyle(val label: String) {
  COLOROS("ColorOS 7"),
  GLASS("Glass"),
  LIQUID_GLASS("Liquid Glass"),
  ROUND("Round"),
  SQUIRCLE("Squircle"),
  NEON("Neon"),
  AMOLED("AMOLED"),
  MINIMAL("Minimal"),
  GRADIENT("Gradient"),
  THREE_D("3D-Look"),
  TRANSPARENT("Transparent"),
}

enum class FolderStyle(val label: String) {
  ROUNDED("Rounded"),
  GLASS("Glass"),
  TRANSPARENT("Transparent"),
}

enum class FolderLayout(val label: String) {
  GRID("Grid"),
  STACK("Stack"),
}

enum class DockStyle(val label: String) {
  GLASS("Glass Dock"),
  TRANSPARENT("Transparent Dock"),
  SOLID("Solid Card"),
  HIDDEN("Hidden"),
}

enum class PageTransitionEffect(val label: String) {
  SILK_SLIDE("Silk Slide"),
  DEPTH_CARD("Depth Card"),
  CUBE_TILT("3D Perspective"),
  PARALLAX_FADE("Parallax Fade"),
  LITE_INSTANT("Instant (Lite)"),
}

enum class DrawerScrollMode(val label: String) {
  VERTICAL("Vertical Scroll"),
  HORIZONTAL("Horizontal Pages"),
}

enum class DrawerSortMode(val label: String) {
  ALPHABETICAL("A–Z Alphabetical"),
  FREQUENT("Most Frequent"),
  RECENT("Recently Used"),
  CUSTOM("Custom Order"),
}

enum class GestureAction(val label: String) {
  OPEN_DRAWER("Open App Drawer"),
  OPEN_CONTROL_CENTER("Open Control Center"),
  OPEN_ZERO_SCREEN("Open Zero Screen"),
  OPEN_NOTIFICATIONS("Notification Shade (if permitted)"),
  LOCK_SCREEN("Lock Screen (Admin Opt-In)"),
  OPEN_EDIT_MODE("Home Edit Mode"),
  OPEN_SEARCH("Instant Search"),
  OPEN_THEME_GENERATOR("Aether AI Architect"),
  OPEN_CLONER_VAULT("Open Cloner Vault"),
  OPEN_LOCK_DESIGNER("Lock Screen Designer"),
  OPEN_SETTINGS("Launcher Settings"),
  NEXT_PAGE("Next Home Page"),
  NONE("Disabled"),
}

enum class CustomWidgetKind(val label: String, val defaultSpanX: Int, val defaultSpanY: Int) {
  CLOCK("Digital Clock", 4, 1),
  ANALOG_CLOCK("Analog Crystal Clock", 2, 2),
  WEATHER("Weather Card", 2, 1),
  CALENDAR("Calendar Agenda", 2, 2),
  BATTERY("Battery & Device", 2, 1),
  MUSIC("Media Controller", 4, 1),
  STORAGE("Storage Telemetry", 2, 1),
  NOTES("Quick Notes", 2, 2),
  SEARCH("Search Pill", 4, 1),
  WELLNESS("Digital Wellness", 2, 1),
  AI_WORKSPACE("AI Workspace", 4, 1),
  DUAL_SPACE("Dual Space & Vault", 2, 1),
  PHOTO("Photo Frame", 2, 2),
}

enum class HomeItemType {
  APP,
  FOLDER,
  CUSTOM_WIDGET,
  SYSTEM_WIDGET,
}

data class HomeItem(
  val id: String,
  val type: HomeItemType,
  val page: Int,
  val row: Int,
  val col: Int,
  val spanX: Int = 1,
  val spanY: Int = 1,
  val packageName: String = "",
  val activityName: String = "",
  val customLabel: String? = null,
  val customIconStyle: LauncherIconStyle? = null,
  val folderTitle: String = "Folder",
  val folderColorHex: Long = 0xFF6366F1,
  val folderStyle: FolderStyle = FolderStyle.GLASS,
  val folderLayout: FolderLayout = FolderLayout.GRID,
  val folderAppPackages: List<String> = emptyList(),
  val customWidgetKind: CustomWidgetKind? = null,
  val appWidgetId: Int = -1,
  val widgetTransparent: Boolean = false,
  val widgetGlass: Boolean = true,
  val photoUri: String? = null,
  val noteText: String = "Tap to jot an offline note",
)

enum class SearchBarStyle(val label: String) {
  GLASS_PILL("Glass Pill"),
  TRANSPARENT_LINE("Transparent Outline"),
  MINIMAL_SOLID("Minimal Card"),
  HIDDEN("Hidden"),
}

enum class ControlCenterTileId(val label: String) {
  WIFI("Wi-Fi"),
  MOBILE_DATA("Cellular Data"),
  BLUETOOTH("Bluetooth"),
  AIRPLANE("Airplane Mode"),
  TORCH("Torch"),
  AUTO_ROTATE("Auto-Rotate"),
  EYE_COMFORT("Eye Comfort"),
  DARK_MODE("Dark Mode"),
  POWER_SAVER("Battery Saver"),
  SOUND_MODE("Sound Mode"),
}

enum class ControlCenterTileSize(val label: String, val weightCols: Int) {
  COMPACT_1X1("1×1", 1),
  WIDE_2X1("2×1", 2),
}

data class ControlCenterTileConfig(
  val id: ControlCenterTileId,
  val visible: Boolean = true,
  val size: ControlCenterTileSize = ControlCenterTileSize.COMPACT_1X1,
)

fun defaultControlCenterTiles(): List<ControlCenterTileConfig> =
  listOf(
    ControlCenterTileConfig(ControlCenterTileId.WIFI, true, ControlCenterTileSize.WIDE_2X1),
    ControlCenterTileConfig(ControlCenterTileId.BLUETOOTH, true, ControlCenterTileSize.WIDE_2X1),
    ControlCenterTileConfig(ControlCenterTileId.MOBILE_DATA, true, ControlCenterTileSize.COMPACT_1X1),
    ControlCenterTileConfig(ControlCenterTileId.TORCH, true, ControlCenterTileSize.COMPACT_1X1),
    ControlCenterTileConfig(ControlCenterTileId.AUTO_ROTATE, true, ControlCenterTileSize.COMPACT_1X1),
    ControlCenterTileConfig(ControlCenterTileId.DARK_MODE, true, ControlCenterTileSize.COMPACT_1X1),
    ControlCenterTileConfig(ControlCenterTileId.EYE_COMFORT, true, ControlCenterTileSize.COMPACT_1X1),
    ControlCenterTileConfig(ControlCenterTileId.AIRPLANE, true, ControlCenterTileSize.COMPACT_1X1),
    ControlCenterTileConfig(ControlCenterTileId.POWER_SAVER, true, ControlCenterTileSize.COMPACT_1X1),
    ControlCenterTileConfig(ControlCenterTileId.SOUND_MODE, true, ControlCenterTileSize.COMPACT_1X1),
  )

data class IsolatedWebApp(
  val id: String,
  val title: String,
  val url: String,
  val isolateCookies: Boolean = true,
  val accentHex: Long = 0xFF38BDF8L,
)

data class LauncherSettings(
  // 0. Onboarding & Workspace Surfaces
  val onboardingCompleted: Boolean = false,
  val zeroScreenEnabled: Boolean = true,
  val dynamicCapsuleEnabled: Boolean = true,
  // 1. Home Screen
  val gridColumns: Int = 4,
  val gridRows: Int = 5,
  val pageCount: Int = 2,
  val iconSizeDp: Int = 54,
  val iconSpacingDp: Int = 10,
  val showHomeLabels: Boolean = true,
  val fontStyle: FontStyleOption = FontStyleOption.PLUS_JAKARTA,
  val wallpaperParallax: Boolean = true,
  // 2. App Drawer
  val drawerScrollMode: DrawerScrollMode = DrawerScrollMode.VERTICAL,
  val drawerSortMode: DrawerSortMode = DrawerSortMode.ALPHABETICAL,
  val drawerColumns: Int = 4,
  val drawerBackgroundAlpha: Float = 0.72f,
  val drawerGlassEffect: Boolean = true,
  val drawerShowRecentFrequent: Boolean = true,
  val drawerShowCategories: Boolean = true,
  // 3. Icons
  val iconStyle: LauncherIconStyle = LauncherIconStyle.LIQUID_GLASS,
  val iconCornerPercent: Int = 34,
  val iconShadowEnabled: Boolean = true,
  val showDrawerLabels: Boolean = true,
  // 4. Widgets
  val defaultWidgetsGlass: Boolean = true,
  val defaultWidgetsTransparent: Boolean = false,
  val weatherCityNote: String = "Local Offline Mode",
  // 5. Folders
  val defaultFolderStyle: FolderStyle = FolderStyle.GLASS,
  val defaultFolderLayout: FolderLayout = FolderLayout.GRID,
  val defaultFolderColorHex: Long = 0xFF8B5CF6,
  // 6. Gestures
  val swipeUpAction: GestureAction = GestureAction.OPEN_DRAWER,
  val swipeDownAction: GestureAction = GestureAction.OPEN_CONTROL_CENTER,
  val doubleTapAction: GestureAction = GestureAction.LOCK_SCREEN,
  val twoFingerSwipeAction: GestureAction = GestureAction.OPEN_THEME_GENERATOR,
  val pinchAction: GestureAction = GestureAction.OPEN_EDIT_MODE,
  val longPressAction: GestureAction = GestureAction.OPEN_EDIT_MODE,
  // 7. Animations & Glass FX
  val animationsEnabled: Boolean = true,
  val pageTransition: PageTransitionEffect = PageTransitionEffect.SILK_SLIDE,
  val animationSpeed: Float = 1.0f,
  val glassBlurStrength: Float = 0.68f,
  val glassTransparency: Float = 0.24f,
  val glassBorderReflection: Float = 0.44f,
  val glassShadowDepth: Float = 0.52f,
  val customAccentHex: Long? = null,
  // 8. Wallpaper
  val customWallpaperUri: String? = null,
  // 9. Search
  val instantSearch: Boolean = true,
  val showSearchSuggestions: Boolean = true,
  val showSearchHistory: Boolean = true,
  val searchContactsEnabled: Boolean = false,
  val voiceSearchEnabled: Boolean = true,
  val searchBarStyle: SearchBarStyle = SearchBarStyle.GLASS_PILL,
  // 10. Dock
  val dockStyle: DockStyle = DockStyle.GLASS,
  val dockMaxIcons: Int = 5,
  val showDockLabels: Boolean = false,
  val dockPackages: List<String> = emptyList(),
  // 11. Themes
  val activeThemeId: String = "builtin_coloros7",
  val wallpaperBasedPalette: Boolean = false,
  // 12. Performance
  val performanceMode: PerformanceMode = PerformanceMode.AUTO,
  // 13. Privacy & Cloner Vault
  val hiddenPackages: Set<String> = emptySet(),
  val lockedPackages: Set<String> = emptySet(),
  val vaultPinHash: String? = null,
  val isolatedWebApps: List<IsolatedWebApp> = emptyList(),
  val customAppLabels: Map<String, String> = emptyMap(),
  val customAppIconStyles: Map<String, LauncherIconStyle> = emptyMap(),
  val customSortOrder: List<String> = emptyList(),
  // 14. Control Center Customization
  val controlCenterTiles: List<ControlCenterTileConfig> = defaultControlCenterTiles(),
  val controlCenterTileShape: TileShape = TileShape.SQUIRCLE,
  val controlCenterAccentHex: Long? = null,
  // 15. Tablet & Foldable
  val autoAdaptLargeScreen: Boolean = true,
  val tabletColumnBoost: Int = 2,
  val tabletDrawerColumnBoost: Int = 2,
)

enum class SettingsGroup(
  val title: String,
  val subtitle: String,
) {
  WORKSPACE(
    title = "Workspace & Layout",
    subtitle = "Home grid, app drawer, icons, 13 atomic widgets, folders & dock",
  ),
  VISUALS(
    title = "Themes, Glass FX & Lock Screen",
    subtitle = "Fluid motion, wallpapers, glass material sliders & AI Architect",
  ),
  INTERACTION(
    title = "Interaction, Control Center & Data",
    subtitle = "Gestures, Control Center tiles, search & JSON backup",
  ),
  SYSTEM(
    title = "System, Cloner Vault & Privacy",
    subtitle = "Performance, app lock, work profile, tablets & default HOME",
  ),
}

enum class SettingsSection(
  val number: Int,
  val title: String,
  val subtitle: String,
  val group: SettingsGroup,
) {
  HOME_SCREEN(1, "Home Screen", "Grid size, Zero Screen, Dynamic Capsule & fonts", SettingsGroup.WORKSPACE),
  APP_DRAWER(2, "App Drawer", "Scroll direction, sorting, categories & glass", SettingsGroup.WORKSPACE),
  ICONS(3, "Icon Studio", "11 icon styles including Liquid Glass, shapes & shadows", SettingsGroup.WORKSPACE),
  WIDGETS(4, "Widget Studio", "13 Atomic Widgets, transparency & Android App Widgets", SettingsGroup.WORKSPACE),
  FOLDERS(5, "Folders", "Rounded, glass & stack folder styles", SettingsGroup.WORKSPACE),
  DOCK(6, "Floating Dock", "Glass, transparent, solid & icon slots", SettingsGroup.WORKSPACE),
  ANIMATIONS(7, "Fluid Motion & Glass FX", "Blur, transparency, reflections, shadows & spring speed", SettingsGroup.VISUALS),
  WALLPAPERS(8, "Wallpapers & Lock Screen", "Procedural art, photo picker & Lock Screen Designer", SettingsGroup.VISUALS),
  THEMES(9, "Themes & AI Architect", "Gemini recommendations, palette studio & presets", SettingsGroup.VISUALS),
  GESTURES(10, "Gestures & Control Center", "Swipes, quick tiles shape/order & screen lock", SettingsGroup.INTERACTION),
  SEARCH(11, "Search", "Instant search, history, contacts & voice", SettingsGroup.INTERACTION),
  BACKUP_RESTORE(12, "Backup & Restore", "Export/import layout & settings JSON", SettingsGroup.INTERACTION),
  PERFORMANCE(13, "Performance", "Auto, Lite, Balanced & Premium engine", SettingsGroup.SYSTEM),
  PRIVACY(14, "Cloner Vault & Privacy", "App lock, hidden apps, work profile & isolated web apps", SettingsGroup.SYSTEM),
  TABLET_FOLDABLE(15, "Tablet & Foldable", "Adaptive column boost, split panes & form factor", SettingsGroup.SYSTEM),
  DEFAULT_HOME_COMPAT(16, "Default Home & OEM Matrix", "Default HOME role & 13-brand compatibility", SettingsGroup.SYSTEM),
  ABOUT(17, "About Aether Glass OS", "Hardware telemetry, permission audit & Welcome Setup", SettingsGroup.SYSTEM),
}
