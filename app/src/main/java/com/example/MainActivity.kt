package com.example

import android.content.ComponentCallbacks2
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ai.configureAppCheck
import com.example.launcher.system.LauncherWidgetHostHolder
import com.example.launcher.ui.LauncherAppRoot
import com.example.launcher.ui.LauncherDestination
import com.example.launcher.ui.LauncherViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: LauncherViewModel by viewModels()
  private lateinit var widgetHostHolder: LauncherWidgetHostHolder

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()
    widgetHostHolder = LauncherWidgetHostHolder(applicationContext)
    setContent {
      LauncherAppRoot(viewModel = viewModel, widgetHostHolder = widgetHostHolder)
    }
  }

  override fun onStart() {
    super.onStart()
    widgetHostHolder.startListening()
  }

  override fun onStop() {
    super.onStop()
    widgetHostHolder.stopListening()
  }

  override fun onResume() {
    super.onResume()
    viewModel.setForegroundState(true)
  }

  override fun onPause() {
    super.onPause()
    viewModel.setForegroundState(false)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) {
      viewModel.openDrawer(false)
      viewModel.openControlCenter(false)
      viewModel.openZeroScreen(false)
      viewModel.setEditMode(false)
      viewModel.openFolder(null)
      if (viewModel.uiState.value.settings.onboardingCompleted) {
        viewModel.navigateTo(LauncherDestination.HOME)
      }
      viewModel.setCurrentPage(0)
    }
  }

  @Suppress("DEPRECATION")
  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
      viewModel.onTrimMemory()
    }
  }
}
