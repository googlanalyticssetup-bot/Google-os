package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ThemeRepository
import com.example.domain.model.*
import com.example.engine.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ThemeStudioUiState(
  val themes: List<ThemeSpec> = BuiltInThemes.all,
  val activeTheme: ThemeSpec = BuiltInThemes.all[1],
  val draftTheme: ThemeSpec = BuiltInThemes.all[1],
  val aiPrompt: String =
    "Create a premium purple glass theme with AMOLED black wallpaper, glowing icons, transparent widgets, elegant clock and smooth animations.",
  val lastAiReport: AiGenerationReport? = null,
  val isGenerating: Boolean = false,
  val isApplyingWallpaper: Boolean = false,
  val previewBitmap: Bitmap? = null,
  val performanceMode: PerformanceMode = PerformanceMode.AUTO,
  val animationsMasterEnabled: Boolean = true,
  val deviceCapabilities: DeviceCapabilities,
  val renderRules: EffectiveRenderRules,
  val lastWallpaperTarget: String = "Not applied yet",
  val statusMessage: String? = null,
  val isAppForeground: Boolean = true,
)

class ThemeStudioViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = ThemeRepository(application)
  private val initialCaps = HardwareCapabilityEngine.inspectDevice(application)
  private val initialRules =
    HardwareCapabilityEngine.resolveRenderRules(PerformanceMode.AUTO, initialCaps, true)

  private val _draftTheme = MutableStateFlow(BuiltInThemes.all[1])
  private val _aiPrompt =
    MutableStateFlow(
      "Create a premium purple glass theme with AMOLED black wallpaper, glowing icons, transparent widgets, elegant clock and smooth animations.",
    )
  private val _lastAiReport = MutableStateFlow<AiGenerationReport?>(null)
  private val _isGenerating = MutableStateFlow(false)
  private val _isApplyingWallpaper = MutableStateFlow(false)
  private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
  private val _capabilities = MutableStateFlow(initialCaps)
  private val _statusMessage = MutableStateFlow<String?>(null)
  private val _isAppForeground = MutableStateFlow(true)

  private var generationJob: Job? = null
  private var previewJob: Job? = null
  private var wallpaperJob: Job? = null

  private val basePrefsFlow =
    combine(
      repository.allThemes,
      repository.activeThemeId,
      repository.performanceMode,
      repository.animationsEnabled,
      repository.lastWallpaperTarget,
    ) { themes, activeId, perfMode, animEnabled, lastTarget ->
      QuadState(themes, activeId, perfMode, animEnabled, lastTarget)
    }

  private val engineFlow =
    combine(
      _draftTheme,
      _aiPrompt,
      _lastAiReport,
      _isGenerating,
      _isApplyingWallpaper,
    ) { draft, prompt, report, gen, applying ->
      EngineState(draft, prompt, report, gen, applying)
    }

  val uiState: StateFlow<ThemeStudioUiState> =
    combine(
      basePrefsFlow,
      engineFlow,
      _previewBitmap,
      _capabilities,
      _statusMessage,
      _isAppForeground,
    ) { values ->
      val prefs = values[0] as QuadState
      val eng = values[1] as EngineState
      val bmp = values[2] as Bitmap?
      val caps = values[3] as DeviceCapabilities
      val msg = values[4] as String?
      val fg = values[5] as Boolean

      val active =
        prefs.themes.firstOrNull { it.id == prefs.activeId } ?: prefs.themes.first()
      val rules =
        HardwareCapabilityEngine.resolveRenderRules(
          mode = prefs.perfMode,
          caps = caps,
          animationsMasterEnabled = prefs.animEnabled,
        )

      ThemeStudioUiState(
        themes = prefs.themes,
        activeTheme = active,
        draftTheme = eng.draft,
        aiPrompt = eng.prompt,
        lastAiReport = eng.report,
        isGenerating = eng.isGenerating,
        isApplyingWallpaper = eng.isApplying,
        previewBitmap = bmp,
        performanceMode = prefs.perfMode,
        animationsMasterEnabled = prefs.animEnabled,
        deviceCapabilities = caps,
        renderRules = rules,
        lastWallpaperTarget = prefs.lastTarget,
        statusMessage = msg,
        isAppForeground = fg,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue =
        ThemeStudioUiState(
          deviceCapabilities = initialCaps,
          renderRules = initialRules,
        ),
    )

  init {
    viewModelScope.launch {
      repository.ensureBuiltInThemesSeeded()
      val activeId = repository.activeThemeId.first()
      val all = repository.allThemes.first()
      val active = all.firstOrNull { it.id == activeId } ?: BuiltInThemes.all[1]
      _draftTheme.value = active
      refreshPreviewBitmap(active, initialRules)
    }
  }

  fun setForegroundState(inForeground: Boolean) {
    _isAppForeground.value = inForeground
    if (!inForeground) {
      generationJob?.cancel()
    } else {
      refreshDeviceCapabilities()
    }
  }

  fun onTrimMemory() {
    ProceduralWallpaperRenderer.trimMemory()
  }

  fun refreshDeviceCapabilities() {
    _capabilities.value = HardwareCapabilityEngine.inspectDevice(getApplication())
  }

  fun updateAiPrompt(prompt: String) {
    _aiPrompt.value = prompt
  }

  fun generateThemeFromPrompt(promptOverride: String? = null) {
    val promptText = promptOverride ?: _aiPrompt.value
    _aiPrompt.value = promptText
    generationJob?.cancel()
    generationJob =
      viewModelScope.launch {
        _isGenerating.value = true
        try {
          val state = uiState.value
          val report =
            LocalAiThemeEngine.generateFromPrompt(
              rawPrompt = promptText,
              tier = state.renderRules.tier,
              formFactor = state.deviceCapabilities.formFactor,
            )
          _lastAiReport.value = report
          _draftTheme.value = report.theme
          refreshPreviewBitmap(report.theme, state.renderRules)
          OfflineSoundEngine.playPreviewTone(report.theme.motionAndSound.soundProfile)
        } finally {
          _isGenerating.value = false
        }
      }
  }

  fun cancelGeneration() {
    generationJob?.cancel()
    _isGenerating.value = false
    _statusMessage.value = "Theme generation cancelled"
  }

  fun updateDraftTheme(transform: (ThemeSpec) -> ThemeSpec) {
    val updated = transform(_draftTheme.value)
    val customSafe =
      if (updated.isBuiltIn) {
        updated.copy(
          id = "custom_${System.currentTimeMillis()}",
          name = "${updated.name} Custom",
          isBuiltIn = false,
          category = ThemeCategory.CUSTOM,
        )
      } else {
        updated
      }
    _draftTheme.value = customSafe
    refreshPreviewBitmap(customSafe, uiState.value.renderRules)
  }

  fun selectThemeIntoStudio(theme: ThemeSpec) {
    _draftTheme.value = theme
    refreshPreviewBitmap(theme, uiState.value.renderRules)
  }

  fun saveDraftToLibrary() {
    viewModelScope.launch {
      val current = _draftTheme.value
      val toSave =
        if (current.isBuiltIn) {
          current.copy(
            id = "custom_${System.currentTimeMillis()}",
            name = "${current.name} Mix",
            isBuiltIn = false,
            category = ThemeCategory.CUSTOM,
          )
        } else {
          current
        }
      repository.saveTheme(toSave)
      _draftTheme.value = toSave
      _statusMessage.value = "Saved \"${toSave.name}\" offline in Room library"
    }
  }

  fun activateTheme(theme: ThemeSpec) {
    viewModelScope.launch {
      repository.setActiveTheme(theme)
      _draftTheme.value = theme
      refreshPreviewBitmap(theme, uiState.value.renderRules)
      _statusMessage.value = "Activated \"${theme.name}\" & synced Home Widget"
    }
  }

  fun deleteCustomTheme(theme: ThemeSpec) {
    if (theme.isBuiltIn) return
    viewModelScope.launch {
      repository.deleteCustomTheme(theme.id)
      if (_draftTheme.value.id == theme.id) {
        val fallback = BuiltInThemes.all[1]
        _draftTheme.value = fallback
        refreshPreviewBitmap(fallback, uiState.value.renderRules)
      }
      _statusMessage.value = "Removed \"${theme.name}\""
    }
  }

  fun applyDraftWallpaper(target: WallpaperTarget) {
    wallpaperJob?.cancel()
    wallpaperJob =
      viewModelScope.launch {
        _isApplyingWallpaper.value = true
        try {
          val state = uiState.value
          val res =
            ProceduralWallpaperRenderer.applyToDeviceWallpaper(
              context = getApplication(),
              theme = state.draftTheme,
              target = target,
              rules = state.renderRules,
            )
          res.onSuccess { msg ->
            repository.setActiveTheme(state.draftTheme)
            repository.setLastWallpaperTarget("${state.draftTheme.name} → ${target.label}")
            _statusMessage.value = msg
          }.onFailure { err ->
            _statusMessage.value = "Wallpaper apply failed: ${err.message ?: "Unknown error"}"
          }
        } finally {
          _isApplyingWallpaper.value = false
        }
      }
  }

  fun setPerformanceMode(mode: PerformanceMode) {
    viewModelScope.launch {
      repository.setPerformanceMode(mode)
      val caps = HardwareCapabilityEngine.inspectDevice(getApplication())
      _capabilities.value = caps
      val rules =
        HardwareCapabilityEngine.resolveRenderRules(
          mode,
          caps,
          uiState.value.animationsMasterEnabled,
        )
      refreshPreviewBitmap(_draftTheme.value, rules)
      _statusMessage.value = "Engine switched to ${mode.label} (${rules.tier.label})"
    }
  }

  fun optimizeDraftForCurrentDevice() {
    val tier = uiState.value.renderRules.tier
    val optimized = LocalAiThemeEngine.optimizeThemeForDevice(_draftTheme.value, tier)
    _draftTheme.value = optimized
    refreshPreviewBitmap(optimized, uiState.value.renderRules)
    _statusMessage.value = "Optimised theme for ${tier.label} hardware tier"
  }

  fun setAnimationsMasterEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setAnimationsEnabled(enabled)
    }
  }

  fun previewSound(profile: SoundProfile) {
    viewModelScope.launch {
      OfflineSoundEngine.playPreviewTone(profile)
    }
  }

  fun exportActiveThemeJson(): String = repository.exportThemeJson(_draftTheme.value)

  fun importThemeJson(json: String) {
    viewModelScope.launch {
      repository.importThemeJson(json).onSuccess { imported ->
        _draftTheme.value = imported
        refreshPreviewBitmap(imported, uiState.value.renderRules)
        _statusMessage.value = "Imported \"${imported.name}\" offline"
      }.onFailure {
        _statusMessage.value = "Invalid theme JSON file"
      }
    }
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  private fun refreshPreviewBitmap(theme: ThemeSpec, rules: EffectiveRenderRules) {
    previewJob?.cancel()
    previewJob =
      viewModelScope.launch {
        val bmp = ProceduralWallpaperRenderer.getOrRenderPreview(theme, rules)
        _previewBitmap.value = bmp
      }
  }

  override fun onCleared() {
    super.onCleared()
    generationJob?.cancel()
    previewJob?.cancel()
    wallpaperJob?.cancel()
  }

  private data class QuadState(
    val themes: List<ThemeSpec>,
    val activeId: String,
    val perfMode: PerformanceMode,
    val animEnabled: Boolean,
    val lastTarget: String,
  )

  private data class EngineState(
    val draft: ThemeSpec,
    val prompt: String,
    val report: AiGenerationReport?,
    val isGenerating: Boolean,
    val isApplying: Boolean,
  )
}
