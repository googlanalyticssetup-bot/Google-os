package com.example.engine

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.LruCache
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class WallpaperTarget(val label: String) {
  HOME("Home Screen"),
  LOCK("Lock Screen"),
  BOTH("Home & Lock Screen"),
}

object ProceduralWallpaperRenderer {

  private const val MAX_CACHE_BYTES = 12 * 1024 * 1024 // Bounded 12MB thumbnail cache

  private val thumbnailCache =
    object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
      override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

  fun trimMemory() {
    thumbnailCache.evictAll()
  }

  suspend fun getOrRenderPreview(
    theme: ThemeSpec,
    rules: EffectiveRenderRules,
    widthPx: Int = 540,
    heightPx: Int = 960,
  ): Bitmap =
    withContext(Dispatchers.Default) {
      val targetW =
        if (theme.wallpaper.aspectMode == WallpaperAspectMode.TABLET_WIDE) 720 else widthPx
      val targetH =
        if (theme.wallpaper.aspectMode == WallpaperAspectMode.TABLET_WIDE) 480 else heightPx
      val scaledW = if (rules.tier == ResolvedTier.LITE) (targetW * 0.7f).toInt() else targetW
      val scaledH = if (rules.tier == ResolvedTier.LITE) (targetH * 0.7f).toInt() else targetH
      val key =
        "${theme.id}_${theme.wallpaper.style.name}_${theme.wallpaper.aspectMode.name}_${theme.palette.primaryHex}_${theme.palette.backgroundHex}_${rules.tier.name}_${theme.wallpaper.seed}_${theme.wallpaper.particleCount}"

      thumbnailCache.get(key)?.let { cached ->
        if (!cached.isRecycled) return@withContext cached
      }

      val bmp = renderWallpaperBitmap(theme, rules, scaledW, scaledH)
      thumbnailCache.put(key, bmp)
      bmp
    }

  suspend fun applyToDeviceWallpaper(
    context: Context,
    theme: ThemeSpec,
    target: WallpaperTarget,
    rules: EffectiveRenderRules,
  ): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        val longSide = rules.maxWallpaperLongSidePx
        val shortSide = (longSide * 9f / 16f).toInt()
        val (w, h) =
          if (theme.wallpaper.aspectMode == WallpaperAspectMode.TABLET_WIDE) {
            longSide to (longSide * 10f / 16f).toInt()
          } else {
            shortSide to longSide
          }

        val bitmap = renderWallpaperBitmap(theme, rules, w, h)
        try {
          val wm = WallpaperManager.getInstance(context.applicationContext)
          val flag =
            when (target) {
              WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
              WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
              WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
          wm.setBitmap(bitmap, null, true, flag)
          "Applied ${theme.name} wallpaper to ${target.label}"
        } finally {
          if (!bitmap.isRecycled) bitmap.recycle()
        }
      }
    }

  fun renderWallpaperBitmap(
    theme: ThemeSpec,
    rules: EffectiveRenderRules,
    width: Int,
    height: Int,
  ): Bitmap {
    val safeW = width.coerceIn(120, 2400)
    val safeH = height.coerceIn(120, 2400)
    val config =
      if (rules.useRgb565Thumbnails) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
    val bitmap = Bitmap.createBitmap(safeW, safeH, config)
    val canvas = Canvas(bitmap)

    val bg = theme.palette.backgroundHex.toInt()
    val surf = theme.palette.surfaceHex.toInt()
    val primary = theme.palette.primaryHex.toInt()
    val secondary = theme.palette.secondaryHex.toInt()
    val tertiary = theme.palette.tertiaryHex.toInt()
    val glow = theme.palette.accentGlowHex.toInt()

    val rng = Random(theme.wallpaper.seed)
    val w = safeW.toFloat()
    val h = safeH.toFloat()

    // Base fill
    val basePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            0f,
            0f,
            w,
            h,
            intArrayOf(bg, surf, bg),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRect(0f, 0f, w, h, basePaint)

    when (theme.wallpaper.style) {
      WallpaperStyle.AMOLED_GLOW -> {
        canvas.drawColor(bg)
        drawRadialOrb(canvas, w * 0.28f, h * 0.25f, w * 0.68f, withAlpha(primary, 95))
        drawRadialOrb(canvas, w * 0.78f, h * 0.72f, w * 0.62f, withAlpha(secondary, 80))
        drawRadialOrb(canvas, w * 0.5f, h * 0.5f, w * 0.42f, withAlpha(glow, 55))
      }
      WallpaperStyle.GLASS_RIBBON -> {
        drawRadialOrb(canvas, w * 0.2f, h * 0.2f, w * 0.75f, withAlpha(primary, 110))
        drawRadialOrb(canvas, w * 0.85f, h * 0.75f, w * 0.8f, withAlpha(secondary, 100))
        drawSilkRibbons(canvas, w, h, primary, secondary, tertiary, theme.wallpaper.patternDensity)
      }
      WallpaperStyle.GALAXY_NEBULA -> {
        canvas.drawColor(bg)
        drawRadialOrb(canvas, w * 0.52f, h * 0.36f, w * 0.85f, withAlpha(primary, 115))
        drawRadialOrb(canvas, w * 0.25f, h * 0.65f, w * 0.65f, withAlpha(tertiary, 90))
        drawRadialOrb(canvas, w * 0.8f, h * 0.2f, w * 0.55f, withAlpha(secondary, 75))
      }
      WallpaperStyle.ABSTRACT_PRISM -> {
        drawRadialOrb(canvas, w * 0.3f, h * 0.35f, w * 0.7f, withAlpha(primary, 90))
        drawCrystalShards(canvas, w, h, primary, secondary, tertiary, rng, rules.tier)
      }
      WallpaperStyle.MINIMAL_GRADIENT -> {
        val minPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader =
              LinearGradient(
                0f,
                0f,
                w * 0.85f,
                h,
                intArrayOf(bg, mixColor(bg, primary, 0.28f), mixColor(bg, secondary, 0.22f)),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP,
              )
          }
        canvas.drawRect(0f, 0f, w, h, minPaint)
        drawRadialOrb(canvas, w * 0.75f, h * 0.22f, w * 0.55f, withAlpha(primary, 65))
      }
      WallpaperStyle.NATURE_AURORA -> {
        drawAuroraCurtains(canvas, w, h, primary, secondary, tertiary)
      }
      WallpaperStyle.CYBER_GRID -> {
        canvas.drawColor(bg)
        drawRadialOrb(canvas, w * 0.5f, h * 0.28f, w * 0.75f, withAlpha(primary, 105))
        drawCyberGrid(canvas, w, h, primary, secondary)
      }
      WallpaperStyle.FRACTAL_WAVE -> {
        drawRadialOrb(canvas, w * 0.5f, h * 0.5f, w * 0.9f, withAlpha(secondary, 85))
        drawFractalWaves(canvas, w, h, primary, secondary, tertiary)
      }
    }

    // Particle stardust (scaled to hardware tier)
    val effectiveParticles =
      theme.wallpaper.particleCount.coerceAtMost(rules.maxParticleCount)
    if (effectiveParticles > 0) {
      val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
      for (i in 0 until effectiveParticles) {
        val px = rng.nextFloat() * w
        val py = rng.nextFloat() * h
        val r = (1.2f + rng.nextFloat() * 3.4f) * (w / 540f)
        dotPaint.color =
          withAlpha(if (i % 2 == 0) primary else Color.WHITE, 60 + rng.nextInt(150))
        canvas.drawCircle(px, py, r, dotPaint)
      }
    }

    // Lightweight deterministic grain (skipped in Lite mode)
    if (rules.tier != ResolvedTier.LITE && theme.wallpaper.noiseIntensity > 0.05f) {
      val grainPaint = Paint()
      val specks = (w * h / 420f).toInt().coerceIn(80, 1200)
      for (i in 0 until specks) {
        grainPaint.color = withAlpha(Color.WHITE, (theme.wallpaper.noiseIntensity * 28).toInt())
        canvas.drawPoint(rng.nextFloat() * w, rng.nextFloat() * h, grainPaint)
      }
    }

    return bitmap
  }

  private fun drawRadialOrb(canvas: Canvas, cx: Float, cy: Float, radius: Float, centerColor: Int) {
    val paint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          RadialGradient(
            cx,
            cy,
            radius.coerceAtLeast(10f),
            intArrayOf(centerColor, withAlpha(centerColor, 0)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawCircle(cx, cy, radius, paint)
  }

  private fun drawSilkRibbons(
    canvas: Canvas,
    w: Float,
    h: Float,
    c1: Int,
    c2: Int,
    c3: Int,
    density: Float,
  ) {
    val ribbons = (4 + (density * 4).toInt()).coerceIn(3, 8)
    for (i in 0 until ribbons) {
      val path = Path()
      val yBase = h * (0.18f + i * 0.11f)
      path.moveTo(-w * 0.1f, yBase)
      path.cubicTo(
        w * 0.25f,
        yBase - h * 0.16f,
        w * 0.65f,
        yBase + h * 0.18f,
        w * 1.1f,
        yBase - h * 0.06f,
      )
      path.lineTo(w * 1.1f, yBase + h * 0.14f)
      path.cubicTo(
        w * 0.6f,
        yBase + h * 0.28f,
        w * 0.3f,
        yBase - h * 0.02f,
        -w * 0.1f,
        yBase + h * 0.1f,
      )
      path.close()
      val ribbonColor = when (i % 3) {
        0 -> c1
        1 -> c2
        else -> c3
      }
      val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            LinearGradient(
              0f,
              yBase - h * 0.15f,
              w,
              yBase + h * 0.2f,
              withAlpha(ribbonColor, 65),
              withAlpha(Color.WHITE, 25),
              Shader.TileMode.CLAMP,
            )
        }
      canvas.drawPath(path, paint)
    }
  }

  private fun drawCrystalShards(
    canvas: Canvas,
    w: Float,
    h: Float,
    c1: Int,
    c2: Int,
    c3: Int,
    rng: Random,
    tier: ResolvedTier,
  ) {
    val count = if (tier == ResolvedTier.LITE) 6 else 12
    val strokePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (1.5f * (w / 540f)).coerceAtLeast(1f)
        color = withAlpha(Color.WHITE, 55)
      }
    for (i in 0 until count) {
      val cx = rng.nextFloat() * w
      val cy = rng.nextFloat() * h
      val size = w * (0.18f + rng.nextFloat() * 0.26f)
      val path =
        Path().apply {
          moveTo(cx, cy - size)
          lineTo(cx + size * 0.85f, cy + size * 0.15f)
          lineTo(cx - size * 0.15f, cy + size * 0.9f)
          lineTo(cx - size * 0.8f, cy - size * 0.1f)
          close()
        }
      val col = when (i % 3) {
        0 -> c1
        1 -> c2
        else -> c3
      }
      val fillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            LinearGradient(
              cx - size,
              cy - size,
              cx + size,
              cy + size,
              withAlpha(col, 85),
              withAlpha(Color.WHITE, 18),
              Shader.TileMode.CLAMP,
            )
        }
      canvas.drawPath(path, fillPaint)
      canvas.drawPath(path, strokePaint)
    }
  }

  private fun drawAuroraCurtains(
    canvas: Canvas,
    w: Float,
    h: Float,
    c1: Int,
    c2: Int,
    c3: Int,
  ) {
    for (band in 0..3) {
      val path = Path()
      val baseY = h * (0.22f + band * 0.14f)
      path.moveTo(0f, baseY)
      val steps = 16
      for (s in 0..steps) {
        val x = w * (s / steps.toFloat())
        val wave = sin(s * 0.65 + band * 1.1).toFloat() * h * 0.08f
        path.lineTo(x, baseY + wave)
      }
      path.lineTo(w, h)
      path.lineTo(0f, h)
      path.close()
      val col = when (band % 3) {
        0 -> c1
        1 -> c2
        else -> c3
      }
      val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            LinearGradient(
              0f,
              baseY - h * 0.1f,
              0f,
              baseY + h * 0.35f,
              withAlpha(col, 90),
              withAlpha(col, 0),
              Shader.TileMode.CLAMP,
            )
        }
      canvas.drawPath(path, paint)
    }
  }

  private fun drawCyberGrid(canvas: Canvas, w: Float, h: Float, c1: Int, c2: Int) {
    val horizonY = h * 0.56f
    val linePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = (1.4f * (w / 540f)).coerceAtLeast(1f)
        color = withAlpha(c1, 110)
      }
    // Perspective rays
    for (i in -8..8) {
      val topX = w * 0.5f + i * (w * 0.045f)
      val botX = w * 0.5f + i * (w * 0.18f)
      canvas.drawLine(topX, horizonY, botX, h, linePaint)
    }
    // Horizontal receding lines
    linePaint.color = withAlpha(c2, 95)
    for (row in 0..10) {
      val t = (row / 10f)
      val y = horizonY + (t * t) * (h - horizonY)
      canvas.drawLine(0f, y, w, y, linePaint)
    }
  }

  private fun drawFractalWaves(
    canvas: Canvas,
    w: Float,
    h: Float,
    c1: Int,
    c2: Int,
    c3: Int,
  ) {
    val stroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (2.2f * (w / 540f)).coerceAtLeast(1.2f)
      }
    for (layer in 0 until 7) {
      val path = Path()
      val baseY = h * (0.25f + layer * 0.09f)
      for (xStep in 0..36) {
        val t = xStep / 36f
        val x = t * w
        val angle = t * 2.0 * PI + layer * 0.55
        val y =
          baseY +
            (sin(angle) * h * 0.055 + cos(angle * 2.1) * h * 0.025).toFloat()
        if (xStep == 0) path.moveTo(x, y) else path.lineTo(x, y)
      }
      val col = when (layer % 3) {
        0 -> c1
        1 -> c2
        else -> c3
      }
      stroke.color = withAlpha(col, 140)
      canvas.drawPath(path, stroke)
    }
  }

  fun renderWidgetGlassBackdrop(
    theme: ThemeSpec,
    widthPx: Int = 640,
    heightPx: Int = 300,
  ): Bitmap {
    val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val rect = RectF(4f, 4f, widthPx - 4f, heightPx - 4f)
    val corner = 44f
    val primary = theme.palette.primaryHex.toInt()
    val secondary = theme.palette.secondaryHex.toInt()
    val surface = theme.palette.surfaceHex.toInt()

    val fillPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            0f,
            0f,
            widthPx.toFloat(),
            heightPx.toFloat(),
            intArrayOf(
              withAlpha(mixColor(surface, primary, 0.35f), 220),
              withAlpha(mixColor(surface, secondary, 0.25f), 195),
            ),
            null,
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRoundRect(rect, corner, corner, fillPaint)

    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        shader =
          LinearGradient(
            0f,
            0f,
            widthPx.toFloat(),
            heightPx.toFloat(),
            withAlpha(Color.WHITE, 140),
            withAlpha(primary, 90),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRoundRect(rect, corner, corner, borderPaint)
    return bmp
  }

  private fun withAlpha(color: Int, alpha: Int): Int =
    Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

  private fun mixColor(a: Int, b: Int, ratio: Float): Int {
    val r = (Color.red(a) * (1f - ratio) + Color.red(b) * ratio).toInt().coerceIn(0, 255)
    val g = (Color.green(a) * (1f - ratio) + Color.green(b) * ratio).toInt().coerceIn(0, 255)
    val bl = (Color.blue(a) * (1f - ratio) + Color.blue(b) * ratio).toInt().coerceIn(0, 255)
    return Color.rgb(r, g, bl)
  }
}
