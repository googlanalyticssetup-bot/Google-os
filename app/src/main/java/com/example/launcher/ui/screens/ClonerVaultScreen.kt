package com.example.launcher.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.launcher.model.InstalledAppInfo
import com.example.launcher.model.IsolatedWebApp
import com.example.launcher.system.VaultSecurityHelper
import com.example.launcher.ui.LauncherUiState
import com.example.ui.components.GlassSurfaceCard

private enum class VaultTab(val label: String) {
  APP_LOCK("App Lock"),
  WORK_PROFILE("Work Space"),
  WEB_APPS("Web Apps"),
  AUDIT("Hidden & Audit"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClonerVaultScreen(
  state: LauncherUiState,
  onToggleLockPackage: (String, Boolean) -> Unit,
  onSetVaultPin: (String?) -> Unit,
  onToggleHiddenPackage: (String, Boolean) -> Unit,
  onLaunchApp: (InstalledAppInfo) -> Unit,
  onAddIsolatedWebApp: (String, String, Boolean) -> Unit,
  onRemoveIsolatedWebApp: (String) -> Unit,
  onOpenIsolatedWebApp: (IsolatedWebApp) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onBack() }

  val context = LocalContext.current
  val theme = state.activeTheme
  val primary = Color(theme.palette.primaryHex)
  var activeTab by remember { mutableStateOf(VaultTab.APP_LOCK) }
  var pinDraft by remember { mutableStateOf("") }
  var webTitleDraft by remember { mutableStateOf("") }
  var webUrlDraft by remember { mutableStateOf("https://") }
  var isolateCookies by remember { mutableStateOf(true) }
  var appFilter by remember { mutableStateOf("") }

  Scaffold(
    containerColor = Color.Transparent,
    topBar = {
      TopAppBar(
        title = { Text("Cloner Vault & Work Space") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("vault_back_btn")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .testTag("cloner_vault_screen"),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Honest Security Banner + Biometric & PIN Card
      GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          val hasPin = !state.settings.vaultPinHash.isNullOrBlank()
          val bioReady = remember { VaultSecurityHelper.canUseFrameworkBiometric(context) }
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = primary)
            Text(
              text =
                "Protection: " +
                  (if (hasPin) "PIN Active" else "No PIN") +
                  " • " +
                  (if (bioReady) "Biometric Available" else "No Biometric"),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
            )
          }
          Text(
            text = "App Lock protects launches inside Aether Glass OS. It does not encrypt or block external Android system launchers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
              value = pinDraft,
              onValueChange = { if (it.length <= 8) pinDraft = it },
              label = { Text(if (hasPin) "New 4+ Digit PIN" else "Set 4+ Digit Vault PIN") },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            Button(
              enabled = pinDraft.length >= 4,
              onClick = {
                onSetVaultPin(pinDraft)
                pinDraft = ""
              },
              modifier = Modifier.heightIn(min = 48.dp),
            ) {
              Text("Save PIN")
            }
            if (hasPin) {
              TextButton(onClick = { onSetVaultPin(null) }) { Text("Clear") }
            }
          }
        }
      }

      // Tabs
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        VaultTab.entries.forEachIndexed { idx, tab ->
          SegmentedButton(
            selected = activeTab == tab,
            onClick = { activeTab = tab },
            shape = SegmentedButtonDefaults.itemShape(idx, VaultTab.entries.size),
            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
          )
        }
      }

      when (activeTab) {
        VaultTab.APP_LOCK -> {
          OutlinedTextField(
            value = appFilter,
            onValueChange = { appFilter = it },
            placeholder = { Text("Filter installed apps…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
          )
          val apps =
            remember(state.installedApps, appFilter) {
              state.installedApps
                .filter { appFilter.isBlank() || it.label.contains(appFilter, ignoreCase = true) }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
            }
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(apps, key = { it.packageName }) { app ->
              val locked = app.packageName in state.settings.lockedPackages
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.titleSmall)
                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Switch(
                    checked = locked,
                    onCheckedChange = { c -> onToggleLockPackage(app.packageName, c) },
                  )
                }
              }
            }
          }
        }

        VaultTab.WORK_PROFILE -> {
          val workApps = remember(state.installedApps) { state.installedApps.filter { it.isWorkProfile } }
          if (workApps.isEmpty()) {
            GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Android Work Profile / Dual Space", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                  text =
                    if (state.controlCenterSnapshot.hasWorkProfile)
                      "Work profile detected, but no launchable work apps are currently visible."
                    else
                      "No managed Android Work Profile is configured on this device. Aether Glass uses Android's official LauncherApps profile APIs rather than fake or unsafe app-cloning hacks. Add a Work Profile in Android Settings -> Accounts, or use the Isolated Web Apps tab.",
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              items(workApps, key = { it.componentKey }) { app ->
                Surface(
                  onClick = { onLaunchApp(app) },
                  shape = RoundedCornerShape(14.dp),
                  color = primary.copy(alpha = 0.14f),
                  border = BorderStroke(1.dp, primary.copy(alpha = 0.4f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("${app.label} (Work)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                      Text(app.packageName, style = MaterialTheme.typography.labelSmall)
                    }
                    Icon(Icons.Rounded.Work, contentDescription = "Launch Work App", tint = primary)
                  }
                }
              }
            }
          }
        }

        VaultTab.WEB_APPS -> {
          GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Create Isolated Web App", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              OutlinedTextField(
                value = webTitleDraft,
                onValueChange = { webTitleDraft = it },
                label = { Text("App Name (e.g. Second Account)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              OutlinedTextField(
                value = webUrlDraft,
                onValueChange = { webUrlDraft = it },
                label = { Text("HTTPS URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              SettingsSwitchRow("Isolate Cookies & Clear on Close", isolateCookies) { isolateCookies = it }
              Button(
                onClick = {
                  onAddIsolatedWebApp(webTitleDraft, webUrlDraft, isolateCookies)
                  webTitleDraft = ""
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("vault_add_webapp_btn"),
              ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add Isolated Web App")
              }
            }
          }

          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(state.settings.isolatedWebApps, key = { it.id }) { item ->
              Surface(
                onClick = { onOpenIsolatedWebApp(item) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(item.url, style = MaterialTheme.typography.labelSmall, color = primary)
                  }
                  IconButton(onClick = { onRemoveIsolatedWebApp(item.id) }) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove web app")
                  }
                }
              }
            }
          }
        }

        VaultTab.AUDIT -> {
          val snap = state.controlCenterSnapshot
          val contactsGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
              PackageManager.PERMISSION_GRANTED
          LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            item {
              GlassSurfaceCard(theme.glass, theme.palette, state.renderRules, Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text("System Permission Audit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  AuditStatusLine("Default HOME Role", state.isDefaultHome)
                  AuditStatusLine("Notification Listener Service", snap.notificationAccessGranted)
                  AuditStatusLine("Modify System Settings (Brightness/Rotate)", snap.canWriteSystemSettings)
                  AuditStatusLine("Digital Wellness Usage Access", snap.usageStatsGranted)
                  AuditStatusLine("Lock-Screen Device Admin", state.isDeviceAdminActive)
                  AuditStatusLine("Contacts Search Opt-In", contactsGranted)
                }
              }
            }
            item {
              Text("Hidden Launcher Apps (${state.settings.hiddenPackages.size})", style = MaterialTheme.typography.labelLarge)
            }
            items(state.settings.hiddenPackages.toList(), key = { it }) { pkg ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(pkg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                  TextButton(onClick = { onToggleHiddenPackage(pkg, false) }) { Text("Restore") }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AuditStatusLine(label: String, granted: Boolean) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall)
    Text(
      text = if (granted) "Granted ✓" else "Optional / Off",
      style = MaterialTheme.typography.labelSmall,
      color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
