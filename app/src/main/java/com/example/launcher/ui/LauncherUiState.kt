package com.example.launcher.ui

import android.graphics.Bitmap
import com.example.ai.AiArchitectChatTurn
import com.example.ai.AiRecommendedTheme
import com.example.domain.model.DeviceCapabilities
import com.example.domain.model.EffectiveRenderRules
import com.example.domain.model.ThemeSpec
import com.example.engine.BuiltInThemes
import com.example.launcher.engine.GeneratedPaletteResult
import com.example.launcher.model.*
import com.example.launcher.system.ActiveMediaTrack
import com.example.launcher.system.ControlCenterSystemSnapshot
import com.example.launcher.system.ContactSearchMatch
import com.example.launcher.system.LiveNotificationItem

enum class LauncherDestination {
  ONBOARDING,
  HOME,
  THEME_GENERATOR,
  SETTINGS,
  CLONER_VAULT,
  LOCK_DESIGNER,
}

data class AiArchitectUiState(
  val isGeneratingThemes: Boolean = false,
  val isStreamingAnswer: Boolean = false,
  val chatTurns: List<AiArchitectChatTurn> = emptyList(),
  val recommendations: List<AiRecommendedTheme> = emptyList(),
  val overviewMessage: String? = null,
  val errorMessage: String? = null,
)

data class DynamicCapsuleState(
  val expanded: Boolean = false,
  val stopwatchRunning: Boolean = false,
  val stopwatchElapsedMs: Long = 0L,
)

data class LauncherUiState(
  val isInitialized: Boolean = false,
  val destination: LauncherDestination = LauncherDestination.HOME,
  val settingsSection: SettingsSection? = null,
  val settings: LauncherSettings = LauncherSettings(),
  val activeTheme: ThemeSpec = BuiltInThemes.all.first(),
  val allThemes: List<ThemeSpec> = BuiltInThemes.all,
  val installedApps: List<InstalledAppInfo> = emptyList(),
  val homeItems: List<HomeItem> = emptyList(),
  val searchHistory: List<String> = emptyList(),
  val searchQuery: String = "",
  val contactMatches: List<ContactSearchMatch> = emptyList(),
  val isDrawerOpen: Boolean = false,
  val isControlCenterOpen: Boolean = false,
  val isZeroScreenActive: Boolean = false,
  val isEditMode: Boolean = false,
  val activeFolderId: String? = null,
  val pendingUnlockApp: InstalledAppInfo? = null,
  val activeWebApp: IsolatedWebApp? = null,
  val currentPage: Int = 0,
  val wallpaperBitmap: Bitmap? = null,
  val deviceCapabilities: DeviceCapabilities,
  val renderRules: EffectiveRenderRules,
  val controlCenterSnapshot: ControlCenterSystemSnapshot = ControlCenterSystemSnapshot(),
  val torchEnabled: Boolean = false,
  val liveNotifications: List<LiveNotificationItem> = emptyList(),
  val activeMedia: ActiveMediaTrack? = null,
  val capsuleState: DynamicCapsuleState = DynamicCapsuleState(),
  val isDefaultHome: Boolean = false,
  val isDeviceAdminActive: Boolean = false,
  val isSpeechAvailable: Boolean = false,
  val isForeground: Boolean = true,
  val themeGenPrompt: String =
    "Aether liquid glass with warm obsidian highlights and electric cyan accents",
  val themeGenResult: GeneratedPaletteResult? = null,
  val aiArchitect: AiArchitectUiState = AiArchitectUiState(),
  val statusMessage: String? = null,
)
