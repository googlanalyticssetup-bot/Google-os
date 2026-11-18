package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesDataStore
import com.example.data.ThemeDatabase
import com.example.domain.model.ThemeSpec
import com.example.engine.BuiltInThemes
import com.example.engine.ProceduralWallpaperRenderer
import com.example.engine.ThemeJsonCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ThemeStudioWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val activeId = PreferencesDataStore(context).activeThemeId.first()
        val entity = ThemeDatabase.getInstance(context).themeDao().getThemeById(activeId)
        val theme =
          entity?.let { ThemeJsonCodec.decode(it.payloadJson).getOrNull() }
            ?: BuiltInThemes.all.first { it.id == "builtin_premium_glass" }
        updateWidgets(context, appWidgetManager, appWidgetIds, theme)
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    fun pushThemeUpdate(context: Context, theme: ThemeSpec) {
      val manager = AppWidgetManager.getInstance(context.applicationContext)
      val component = ComponentName(context.applicationContext, ThemeStudioWidgetProvider::class.java)
      val ids = manager.getAppWidgetIds(component)
      if (ids.isNotEmpty()) {
        updateWidgets(context.applicationContext, manager, ids, theme)
      }
    }

    private fun updateWidgets(
      context: Context,
      manager: AppWidgetManager,
      ids: IntArray,
      theme: ThemeSpec,
    ) {
      val backdrop = ProceduralWallpaperRenderer.renderWidgetGlassBackdrop(theme)
      try {
        val primaryColor = theme.palette.primaryHex.toInt()
        val textColor = theme.palette.onSurfaceHex.toInt()
        val subTextColor =
          Color.argb(200, Color.red(textColor), Color.green(textColor), Color.blue(textColor))

        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent =
          PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
          )

        ids.forEach { appWidgetId ->
          val views = RemoteViews(context.packageName, R.layout.widget_theme_studio)
          views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
          views.setImageViewBitmap(R.id.widget_glass_bg, backdrop)
          views.setTextViewText(R.id.widget_theme_pill, theme.name)
          views.setTextColor(R.id.widget_theme_pill, primaryColor)
          views.setTextColor(R.id.widget_clock, textColor)
          views.setTextColor(R.id.widget_date, subTextColor)
          views.setTextColor(R.id.widget_status_line, subTextColor)
          views.setTextViewText(
            R.id.widget_status_line,
            "${theme.glass.style.label} • ${theme.icons.style.label} Icons",
          )
          manager.updateAppWidget(appWidgetId, views)
        }
      } finally {
        if (!backdrop.isRecycled) backdrop.recycle()
      }
    }
  }
}
