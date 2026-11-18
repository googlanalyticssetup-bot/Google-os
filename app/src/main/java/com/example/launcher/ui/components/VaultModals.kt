package com.example.launcher.ui.components

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.launcher.model.InstalledAppInfo
import com.example.launcher.model.IsolatedWebApp
import com.example.launcher.system.VaultSecurityHelper

@Composable
fun AppUnlockDialog(
  app: InstalledAppInfo,
  vaultPinHash: String?,
  onDismiss: () -> Unit,
  onUnlocked: () -> Unit,
) {
  val context = LocalContext.current
  val biometricAvailable = remember { VaultSecurityHelper.canUseFrameworkBiometric(context) }
  var pinDraft by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(app.componentKey) {
    if (biometricAvailable) {
      VaultSecurityHelper.authenticateDeviceBiometric(
        context = context,
        title = "Unlock ${app.label}",
        subtitle = "Protected by Aether Cloner Vault",
        onSuccess = onUnlocked,
        onFallbackPin = { errorText = "Enter Vault PIN below" },
        onError = { msg -> errorText = msg },
      )
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
    title = { Text("Unlock ${app.label}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Launcher-level app lock prevents casual launches from Aether Glass Home & Drawer.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!vaultPinHash.isNullOrBlank()) {
          OutlinedTextField(
            value = pinDraft,
            onValueChange = {
              if (it.length <= 8) pinDraft = it
              errorText = null
            },
            label = { Text("Vault PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("vault_unlock_pin_input"),
          )
        } else if (!biometricAvailable) {
          Text(
            text = "No Vault PIN or device biometric set. Configure a PIN inside Cloner Vault Settings.",
            style = MaterialTheme.typography.bodySmall,
          )
        }
        errorText?.let { err ->
          Text(err, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
        if (biometricAvailable) {
          OutlinedButton(
            onClick = {
              VaultSecurityHelper.authenticateDeviceBiometric(
                context = context,
                title = "Unlock ${app.label}",
                subtitle = "Authenticate to launch",
                onSuccess = onUnlocked,
                onFallbackPin = {},
                onError = { errorText = it },
              )
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Use Biometric / Device Lock")
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when {
            vaultPinHash.isNullOrBlank() && !biometricAvailable -> onUnlocked()
            VaultSecurityHelper.verifyPin(pinDraft, vaultPinHash) -> onUnlocked()
            else -> errorText = "Incorrect Vault PIN"
          }
        },
        modifier = Modifier.testTag("vault_unlock_confirm_btn"),
      ) {
        Text(if (vaultPinHash.isNullOrBlank() && !biometricAvailable) "Continue" else "Unlock")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun IsolatedWebAppDialog(
  webApp: IsolatedWebApp,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val safeUrl = remember(webApp.url) { VaultSecurityHelper.sanitizeHttpsUrl(webApp.url) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxSize().padding(12.dp).testTag("isolated_webapp_dialog"),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(webApp.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              text = if (webApp.isolateCookies) "Isolated Sandbox Container" else "Standard Web Container",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Row {
            IconButton(
              onClick = { VaultSecurityHelper.openExternalBrowserFallback(context, webApp.url) },
            ) {
              Icon(Icons.Rounded.OpenInBrowser, contentDescription = "Open in Browser")
            }
            TextButton(onClick = onDismiss) { Text("Close") }
          }
        }
        HorizontalDivider()

        if (safeUrl == null) {
          Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Invalid URL. Only https:// and http:// URLs are permitted.")
          }
        } else {
          AndroidView(
            factory = { ctx ->
              WebView(ctx).apply {
                val cm = CookieManager.getInstance()
                if (webApp.isolateCookies) {
                  cm.removeAllCookies(null)
                  cm.setAcceptCookie(false)
                } else {
                  cm.setAcceptCookie(true)
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = !webApp.isolateCookies
                settings.cacheMode =
                  if (webApp.isolateCookies) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                settings.allowFileAccess = false
                webViewClient =
                  object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                      val scheme = request?.url?.scheme?.lowercase()
                      return scheme != "https" && scheme != "http"
                    }
                  }
                loadUrl(safeUrl)
              }
            },
            onRelease = { webView ->
              if (webApp.isolateCookies) {
                runCatching {
                  CookieManager.getInstance().removeAllCookies(null)
                  webView.clearHistory()
                  webView.clearCache(true)
                }
              }
              webView.destroy()
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
          )
        }
      }
    }
  }
}
