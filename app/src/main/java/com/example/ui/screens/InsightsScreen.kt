package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.cache.CacheTelemetry
import com.example.data.local.HighlightNoteEntity
import com.example.data.local.ReadingSessionEntity
import com.example.data.preferences.UserPreferences
import com.example.ui.components.DailyPatternChart
import com.example.ui.components.WeeklyProgressChart
import com.example.ui.components.buildDailyPatternBuckets
import com.example.ui.components.buildWeeklyChartPoints

@Composable
fun InsightsScreen(
    sessions: List<ReadingSessionEntity>,
    highlights: List<HighlightNoteEntity>,
    preferences: UserPreferences,
    cacheTelemetry: CacheTelemetry,
    onDeleteHighlight: (String) -> Unit,
    onExportEncrypted: (Uri, String) -> Unit,
    onImportEncrypted: (Uri, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val weeklyPoints = remember(sessions) { buildWeeklyChartPoints(sessions) }
    val dailyBuckets = remember(sessions) { buildDailyPatternBuckets(sessions) }
    val totalMinutes = remember(sessions) { sessions.sumOf { it.minutesRead } }
    val totalWords = remember(sessions) { sessions.sumOf { it.wordsRead } }
    val todayMinutes = weeklyPoints.lastOrNull()?.minutes ?: 0
    val activeDaysStreak = remember(weeklyPoints) {
        var streak = 0
        for (pt in weeklyPoints.reversed()) {
            if (pt.minutes > 0) streak++ else if (streak > 0) break
        }
        streak
    }

    var pendingPassphraseMode by remember { mutableStateOf<String?>(null) } // "EXPORT" or "IMPORT"
    var passphraseInput by remember { mutableStateOf("") }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            pendingExportUri = uri
            pendingPassphraseMode = "EXPORT"
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingExportUri = uri
            pendingPassphraseMode = "IMPORT"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Reading Insights & Encrypted Vault",
            style = MaterialTheme.typography.headlineMedium
        )

        // Summary KPI Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                title = "Today",
                value = "${todayMinutes}m",
                subtitle = "Goal: ${preferences.dailyGoalMinutes}m",
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                title = "Streak",
                value = "${activeDaysStreak}d",
                subtitle = "Total: ${totalMinutes}m",
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                title = "Words",
                value = if (totalWords >= 1000) "${totalWords / 1000}k" else "$totalWords",
                subtitle = "${highlights.size} notes",
                modifier = Modifier.weight(1f)
            )
        }

        // Responsive Two-Column Charts on Medium/Expanded
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth >= 700.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WeeklyChartCard(weeklyPoints, preferences.dailyGoalMinutes, Modifier.weight(1f))
                    DailyPatternCard(dailyBuckets, Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    WeeklyChartCard(weeklyPoints, preferences.dailyGoalMinutes, Modifier.fillMaxWidth())
                    DailyPatternCard(dailyBuckets, Modifier.fillMaxWidth())
                }
            }
        }

        // Battery-Efficient Cache Telemetry Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Battery-Efficient LRU Cache",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${cacheTelemetry.cachedChapters} chapters cached • ${(cacheTelemetry.estimatedBytes / 1024).coerceAtLeast(1)} KB RAM • ${cacheTelemetry.hitRatePercent}% hit rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Encrypted Highlights & Notes Vault Section
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "AES-256-GCM Encrypted Highlights & Notes",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "Back up your margin annotations and highlights into an encrypted .varnika archive to restore across devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { createBackupLauncher.launch("varnika_vault_backup.varnika") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_encrypted_btn")
                    ) {
                        Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export")
                    }
                    OutlinedButton(
                        onClick = { openBackupLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_encrypted_btn")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore")
                    }
                }

                if (highlights.isEmpty()) {
                    Text(
                        text = "No highlights yet. Tap any paragraph while reading a story to highlight & annotate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    highlights.forEach { hl ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${hl.storyTitle} • Ch. ${hl.chapterIndex + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "\"${hl.quotedText}\"",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (hl.noteText.isNotBlank()) {
                                        Text(
                                            text = "Note: ${hl.noteText}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "Integrity SHA-256: ${hl.encryptedChecksum}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteHighlight(hl.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete highlight")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingPassphraseMode?.let { mode ->
        AlertDialog(
            onDismissRequest = {
                pendingPassphraseMode = null
                pendingExportUri = null
                passphraseInput = ""
            },
            title = {
                Text(if (mode == "EXPORT") "Encrypt Vault Backup" else "Decrypt Vault Backup")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a passphrase (minimum 4 characters) for AES-256-GCM encryption.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = passphraseInput,
                        onValueChange = { passphraseInput = it },
                        label = { Text("Encryption Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingExportUri
                        if (uri != null && passphraseInput.length >= 4) {
                            if (mode == "EXPORT") onExportEncrypted(uri, passphraseInput)
                            else onImportEncrypted(uri, passphraseInput)
                        }
                        pendingPassphraseMode = null
                        pendingExportUri = null
                        passphraseInput = ""
                    },
                    enabled = passphraseInput.length >= 4
                ) {
                    Text(if (mode == "EXPORT") "Encrypt & Save" else "Decrypt & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingPassphraseMode = null
                    pendingExportUri = null
                    passphraseInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyChartCard(
    points: List<com.example.ui.components.DailyBarPoint>,
    goalMinutes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Weekly Reading Progress", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (points.all { it.minutes == 0 }) {
                    "Open any story to begin tracking your daily reading minutes against your ${goalMinutes}m target."
                } else {
                    "Dashed line indicates your ${goalMinutes}-minute daily reading goal."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            WeeklyProgressChart(points = points, goalMinutes = goalMinutes)
        }
    }
}

@Composable
private fun DailyPatternCard(
    buckets: List<com.example.ui.components.TimeBucketPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Daily Usage Patterns", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Time-of-day reading habits across morning, afternoon, evening, and late-night sessions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DailyPatternChart(buckets = buckets)
        }
    }
}
