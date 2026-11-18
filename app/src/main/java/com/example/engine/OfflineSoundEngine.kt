package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.domain.model.SoundProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object OfflineSoundEngine {

  suspend fun playPreviewTone(profile: SoundProfile) {
    if (profile == SoundProfile.SILENT) return
    withContext(Dispatchers.Default) {
      val sampleRate = 22050
      val durationSec =
        when (profile) {
          SoundProfile.CRYSTAL_CHIME -> 0.28f
          SoundProfile.MINIMAL_TAP -> 0.08f
          SoundProfile.CYBER_SYNTH -> 0.22f
          SoundProfile.SILENT -> 0f
        }
      val numSamples = (sampleRate * durationSec).toInt().coerceAtLeast(1)
      val samples = ShortArray(numSamples)

      for (i in 0 until numSamples) {
        val t = i / sampleRate.toFloat()
        val env = exp(-t * 14f)
        val wave =
          when (profile) {
            SoundProfile.CRYSTAL_CHIME ->
              sin(2.0 * PI * 880.0 * t) * 0.6 + sin(2.0 * PI * 1320.0 * t) * 0.4
            SoundProfile.MINIMAL_TAP -> sin(2.0 * PI * 520.0 * t) * exp(-t * 45.0)
            SoundProfile.CYBER_SYNTH -> {
              val sweep = 440.0 + (t / durationSec) * 660.0
              sin(2.0 * PI * sweep * t) * 0.7
            }
            SoundProfile.SILENT -> 0.0
          }
        samples[i] = (wave * env * Short.MAX_VALUE * 0.35).toInt().coerceIn(-32767, 32767).toShort()
      }

      var track: AudioTrack? = null
      try {
        val minBuf =
          AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
          )
        val bufferBytes = maxOf(minBuf, samples.size * 2)
        track =
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        track.play()
        kotlinx.coroutines.delay((durationSec * 1000).toLong() + 40L)
      } catch (_: Throwable) {
        // Gracefully ignore on headless / muted test environments
      } finally {
        try {
          track?.stop()
        } catch (_: Throwable) {}
        try {
          track?.release()
        } catch (_: Throwable) {}
      }
    }
  }
}
