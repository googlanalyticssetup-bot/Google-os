package com.example.launcher.system

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import java.security.MessageDigest

object VaultSecurityHelper {

  fun hashPin(pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest("aether_vault:$pin".toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun verifyPin(pin: String, storedHash: String?): Boolean {
    if (storedHash.isNullOrBlank()) return false
    return hashPin(pin) == storedHash
  }

  fun isDeviceCredentialAvailable(context: Context): Boolean {
    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    return km?.isDeviceSecure == true
  }

  fun canUseFrameworkBiometric(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val bm = context.getSystemService(BiometricManager::class.java) ?: return false
    val res =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        bm.canAuthenticate(
          BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
      } else {
        @Suppress("DEPRECATION")
        bm.canAuthenticate()
      }
    return res == BiometricManager.BIOMETRIC_SUCCESS
  }

  fun authenticateDeviceBiometric(
    context: Context,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onFallbackPin: () -> Unit,
    onError: (String) -> Unit,
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && canUseFrameworkBiometric(context)) {
      val executor = ContextCompat.getMainExecutor(context)
      val builder =
        BiometricPrompt.Builder(context)
          .setTitle(title)
          .setSubtitle(subtitle)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setAllowedAuthenticators(
          BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
      } else {
        @Suppress("DEPRECATION")
        builder.setDeviceCredentialAllowed(true)
      }
      val prompt = builder.build()
      prompt.authenticate(
        CancellationSignal(),
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
            onSuccess()
          }

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS ||
              errorCode == BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE
            ) {
              onFallbackPin()
            } else {
              onError(errString?.toString() ?: "Authentication cancelled")
            }
          }
        },
      )
    } else {
      onFallbackPin()
    }
  }

  fun sanitizeHttpsUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    val normalized =
      if (trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("http://", ignoreCase = true)
      ) {
        trimmed
      } else {
        "https://$trimmed"
      }
    val uri = Uri.parse(normalized)
    val scheme = uri.scheme?.lowercase()
    return if ((scheme == "https" || scheme == "http") && !uri.host.isNullOrBlank()) {
      normalized
    } else {
      null
    }
  }

  fun openExternalBrowserFallback(context: Context, url: String): Boolean {
    val safeUrl = sanitizeHttpsUrl(url) ?: return false
    return runCatching {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      true
    }.getOrDefault(false)
  }
}
