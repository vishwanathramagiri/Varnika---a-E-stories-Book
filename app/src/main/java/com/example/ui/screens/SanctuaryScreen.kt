package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.preferences.AccessibilityPreset
import com.example.data.preferences.UserPreferences
import com.example.ui.theme.ThemeMode

@Composable
fun SanctuaryScreen(
    preferences: UserPreferences,
    onUpdateDisplayName: (String) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onApplyPreset: (AccessibilityPreset) -> Unit,
    onSetFontSize: (Float) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetTravelBatterySaver: (Boolean) -> Unit,
    onSetOfflineOnly: (Boolean) -> Unit,
    onSetDailyGoal: (Int) -> Unit,
    onSetReminderSchedule: (Boolean, Int, Int) -> Unit,
    onSendTestReminder: () -> Unit,
    onEnableVaultLock: (String) -> String,
    onDisableVaultLock: () -> Unit,
    onSyncLocalVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nameDraft by remember(preferences.readerDisplayName) { mutableStateOf(preferences.readerDisplayName) }
    var reminderHour by remember(preferences.reminderHour) { mutableIntStateOf(preferences.reminderHour) }
    var reminderMinute by remember(preferences.reminderMinute) { mutableIntStateOf(preferences.reminderMinute) }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var generatedRecoveryCode by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSetReminderSchedule(true, reminderHour, reminderMinute)
        }
    }

    fun requestOrScheduleReminder(enable: Boolean) {
        if (!enable) {
            onSetReminderSchedule(false, reminderHour, reminderMinute)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        onSetReminderSchedule(true, reminderHour, reminderMinute)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sanctuary & Accessibility Settings",
                style = MaterialTheme.typography.headlineMedium
            )

            // Author & Reader Identity
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Reader & Author Identity", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { onUpdateDisplayName(nameDraft) }) {
                            Text("Save")
                        }
                    }
                }
            }

            // Theme & Inclusive Accessibility (Ages 5–85)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Reading Comfort & Dark Mode", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Text("Accessibility Profile (Ages 5–85)", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccessibilityPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = preferences.accessibilityPreset == preset,
                                onClick = { onApplyPreset(preset) },
                                label = { Text("${preset.label} (${preset.ageBadge})") }
                            )
                        }
                    }

                    Text("Default Text Size: ${preferences.fontSizeSp.toInt()} sp", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = preferences.fontSizeSp,
                        onValueChange = onSetFontSize,
                        valueRange = 14f..32f
                    )

                    SettingToggleRow(
                        title = "Haptic Feedback on Page Turns",
                        subtitle = "Subtle tactile confirmation when turning chapters or picking choices",
                        checked = preferences.hapticPageTurnEnabled,
                        onCheckedChange = onSetHaptics
                    )
                }
            }

            // Travel Offline Mode & Battery Efficiency
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Offline Travel & Cloud Sync Status", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Cloud Sync is currently in Local-First mode because Firebase cloud provisioning was declined. All stories, versions, and encrypted notes remain safely stored offline on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SettingToggleRow(
                        title = "Offline-Only Travel Mode",
                        subtitle = "Filter library exclusively to locally cached stories",
                        checked = preferences.offlineOnlyMode,
                        onCheckedChange = onSetOfflineOnly
                    )
                    SettingToggleRow(
                        title = "Battery-Efficient Caching",
                        subtitle = "Throttles chapter prefetching during long travel reading sessions",
                        checked = preferences.travelBatterySaverEnabled,
                        onCheckedChange = onSetTravelBatterySaver
                    )
                    OutlinedButton(
                        onClick = onSyncLocalVault,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Flush & Verify Local Sync Queue")
                    }
                }
            }

            // Customizable Reading Goal Reminder Notifications
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Daily Reading Goal Reminders", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Daily Target: ${preferences.dailyGoalMinutes} minutes", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = preferences.dailyGoalMinutes.toFloat(),
                        onValueChange = { onSetDailyGoal(it.toInt()) },
                        valueRange = 5f..90f
                    )

                    Text("Preferred Reminder Time", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(8 to 0, 13 to 0, 18 to 30, 20 to 0, 21 to 30).forEach { (h, m) ->
                            val label = String.format("%02d:%02d", h, m)
                            FilterChip(
                                selected = reminderHour == h && reminderMinute == m,
                                onClick = {
                                    reminderHour = h
                                    reminderMinute = m
                                    if (preferences.reminderEnabled) {
                                        requestOrScheduleReminder(true)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    SettingToggleRow(
                        title = "Enable Scheduled Daily Notification",
                        subtitle = String.format("Scheduled for %02d:%02d each day", reminderHour, reminderMinute),
                        checked = preferences.reminderEnabled,
                        onCheckedChange = { enabled -> requestOrScheduleReminder(enabled) }
                    )

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onSendTestReminder()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_reminder_notification_btn")
                    ) {
                        Text("Send Test Reading Reminder Now")
                    }
                }
            }

            // 2-Factor Local Vault Security
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("2-Factor Local Vault Protection", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = if (preferences.vaultLockEnabled) {
                            "Active: Salted SHA-256 PIN + Recovery Code (${preferences.twoFactorRecoveryCode})"
                        } else {
                            "Protect your local stories, drafts, and encrypted notes with a 4+ digit PIN and a backup 2FA recovery key."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (preferences.vaultLockEnabled) {
                        OutlinedButton(
                            onClick = onDisableVaultLock,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disable Vault Lock")
                        }
                    } else {
                        Button(
                            onClick = { showPinSetupDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("enable_vault_lock_btn")
                        ) {
                            Text("Set Up Vault PIN & Recovery Key")
                        }
                    }
                }
            }
        }
    }

    if (showPinSetupDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinSetupDialog = false
                pinInput = ""
            },
            title = { Text("Enable 2-Factor Vault Lock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create a 4–8 digit PIN. A secondary recovery key will be generated automatically.")
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 8) pinInput = it },
                        label = { Text("Vault PIN (min 4 characters)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length >= 4) {
                            generatedRecoveryCode = onEnableVaultLock(pinInput)
                            pinInput = ""
                            showPinSetupDialog = false
                        }
                    },
                    enabled = pinInput.length >= 4
                ) {
                    Text("Activate Lock")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinSetupDialog = false
                    pinInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    generatedRecoveryCode?.let { code ->
        AlertDialog(
            onDismissRequest = { generatedRecoveryCode = null },
            title = { Text("Save Your 2FA Recovery Code") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Keep this recovery key safe in case you forget your Vault PIN:")
                    Text(
                        text = code,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { generatedRecoveryCode = null }) {
                    Text("Saved")
                }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
