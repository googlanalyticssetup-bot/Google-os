package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.*
import com.example.engine.*
import com.example.launcher.data.LauncherBackupCodec
import com.example.launcher.data.LauncherPreferencesStore
import com.example.launcher.engine.ThemeGeneratorEngine
import com.example.launcher.model.*
import com.example.launcher.system.VaultSecurityHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aether Glass", appName)
  }

  @Test
  fun `built in theme library contains all 15 presets`() {
    assertEquals(15, BuiltInThemes.all.size)
  }

  @Test
  fun `atomic widgets library provides 13 widget kinds`() {
    assertEquals(13, CustomWidgetKind.entries.size)
    assertTrue(CustomWidgetKind.ANALOG_CLOCK in CustomWidgetKind.entries)
    assertTrue(CustomWidgetKind.STORAGE in CustomWidgetKind.entries)
    assertTrue(CustomWidgetKind.WELLNESS in CustomWidgetKind.entries)
    assertTrue(CustomWidgetKind.AI_WORKSPACE in CustomWidgetKind.entries)
    assertTrue(CustomWidgetKind.DUAL_SPACE in CustomWidgetKind.entries)
  }

  @Test
  fun `vault security helper hashes PIN and validates https URLs`() {
    val hash = VaultSecurityHelper.hashPin("2468")
    assertTrue(VaultSecurityHelper.verifyPin("2468", hash))
    assertFalse(VaultSecurityHelper.verifyPin("0000", hash))
    assertEquals("https://example.com", VaultSecurityHelper.sanitizeHttpsUrl("example.com"))
    assertNull(VaultSecurityHelper.sanitizeHttpsUrl("   "))
  }

  @Test
  fun `theme generator engine creates algorithmic color palette from text description`() {
    val result =
      ThemeGeneratorEngine.generatePaletteFromDescription(
        description = "Sunset amber gold luxury with deep AMOLED obsidian background",
        tier = ResolvedTier.BALANCED,
        formFactor = DeviceFormFactor.PHONE,
      )
    assertEquals(0xFF000000L, result.backgroundHex)
    assertTrue(result.contrastRatio >= 4.5f)
    assertTrue(result.keywordsMatched.isNotEmpty())
  }

  @Test
  fun `launcher settings and layout backup round trip preserves state`() {
    val settings =
      LauncherSettings(
        onboardingCompleted = true,
        gridColumns = 5,
        gridRows = 6,
        iconStyle = LauncherIconStyle.LIQUID_GLASS,
        dockStyle = DockStyle.TRANSPARENT,
        hiddenPackages = setOf("com.example.secretview"),
        lockedPackages = setOf("com.example.bank"),
      )
    val items =
      listOf(
        HomeItem(
          id = "folder_1",
          type = HomeItemType.FOLDER,
          page = 0,
          row = 2,
          col = 1,
          folderTitle = "Tools",
          folderStyle = FolderStyle.GLASS,
          folderAppPackages = listOf("com.a/Main", "com.b/Main"),
        ),
      )
    val encoded = LauncherBackupCodec.encodeBackup(settings, items, BuiltInThemes.all.first())
    val decoded = LauncherBackupCodec.decodeBackup(encoded).getOrThrow()
    assertEquals(5, decoded.settings.gridColumns)
    assertEquals(LauncherIconStyle.LIQUID_GLASS, decoded.settings.iconStyle)
    assertTrue("com.example.secretview" in decoded.settings.hiddenPackages)
    assertEquals(1, decoded.homeItems.size)
    assertEquals("Tools", decoded.homeItems.first().folderTitle)
  }

  @Test
  fun `preferences json codec preserves extended Aether Glass OS settings`() {
    val webApp = IsolatedWebApp(id = "w1", title = "Mail", url = "https://mail.example.com", isolateCookies = true)
    val tiles = listOf(
      ControlCenterTileConfig(ControlCenterTileId.WIFI, true, ControlCenterTileSize.WIDE_2X1),
      ControlCenterTileConfig(ControlCenterTileId.TORCH, false, ControlCenterTileSize.COMPACT_1X1),
    )
    val original =
      LauncherSettings(
        onboardingCompleted = true,
        zeroScreenEnabled = false,
        dynamicCapsuleEnabled = true,
        glassBlurStrength = 0.85f,
        glassTransparency = 0.18f,
        customAccentHex = 0xFF38BDF8L,
        lockedPackages = setOf("com.locked.app"),
        vaultPinHash = VaultSecurityHelper.hashPin("9876"),
        isolatedWebApps = listOf(webApp),
        controlCenterTiles = tiles,
        controlCenterTileShape = TileShape.PILL,
        controlCenterAccentHex = 0xFFF59E0BL,
        customAppLabels = mapOf("com.foo/Bar" to "Camera Pro"),
        customAppIconStyles = mapOf("com.foo/Bar" to LauncherIconStyle.NEON),
        autoAdaptLargeScreen = false,
        tabletColumnBoost = 3,
        tabletDrawerColumnBoost = 1,
      )
    val json = LauncherPreferencesStore.encodeSettings(original)
    val restored = LauncherPreferencesStore.decodeSettings(json)
    assertTrue(restored.onboardingCompleted)
    assertFalse(restored.zeroScreenEnabled)
    assertEquals(0.85f, restored.glassBlurStrength, 0.01f)
    assertEquals(0xFF38BDF8L, restored.customAccentHex)
    assertTrue("com.locked.app" in restored.lockedPackages)
    assertEquals(1, restored.isolatedWebApps.size)
    assertEquals("Mail", restored.isolatedWebApps.first().title)
    assertEquals(TileShape.PILL, restored.controlCenterTileShape)
    assertEquals(0xFFF59E0BL, restored.controlCenterAccentHex)
    assertFalse(restored.controlCenterTiles.first { it.id == ControlCenterTileId.TORCH }.visible)
    assertEquals("Camera Pro", restored.customAppLabels["com.foo/Bar"])
    assertEquals(LauncherIconStyle.NEON, restored.customAppIconStyles["com.foo/Bar"])
    assertFalse(restored.autoAdaptLargeScreen)
    assertEquals(3, restored.tabletColumnBoost)
    assertEquals(1, restored.tabletDrawerColumnBoost)
  }

  @Test
  fun `settings hierarchy covers all 17 sections across 4 groups`() {
    assertEquals(17, SettingsSection.entries.size)
    assertEquals(4, SettingsGroup.entries.size)
    assertEquals((1..17).toList(), SettingsSection.entries.map { it.number })
    SettingsGroup.entries.forEach { group ->
      assertTrue(SettingsSection.entries.any { it.group == group })
    }
  }

  @Test
  fun `procedural wallpaper renderer creates valid bitmap across styles`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val caps = HardwareCapabilityEngine.inspectDevice(context)
    val rules = HardwareCapabilityEngine.resolveRenderRules(PerformanceMode.LITE, caps, false)
    val theme = BuiltInThemes.all.first { it.name == "Purple Galaxy" }
    val bmp = ProceduralWallpaperRenderer.renderWallpaperBitmap(theme, rules, 320, 480)
    assertFalse(bmp.isRecycled)
    assertEquals(320, bmp.width)
    assertEquals(480, bmp.height)
    bmp.recycle()
  }
}
