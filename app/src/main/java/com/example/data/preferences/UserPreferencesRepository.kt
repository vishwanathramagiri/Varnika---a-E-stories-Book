package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.varnikaDataStore: DataStore<Preferences> by preferencesDataStore(name = "varnika_prefs")

enum class ReaderFontOption(val label: String) {
    LORA_SERIF("Lora Serif"),
    JAKARTA_SANS("Plus Jakarta Sans"),
    PLAYFAIR_EDITORIAL("Playfair Display")
}

enum class AccessibilityPreset(val label: String, val ageBadge: String) {
    YOUNG_READER("Young Reader", "5–10 yrs"),
    BALANCED("Classic Balanced", "All Ages"),
    LARGE_PRINT("High-Legibility", "65–85+ yrs")
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val fontSizeSp: Float = 19f,
    val lineHeightMultiplier: Float = 1.6f,
    val readerFont: ReaderFontOption = ReaderFontOption.LORA_SERIF,
    val accessibilityPreset: AccessibilityPreset = AccessibilityPreset.BALANCED,
    val hapticPageTurnEnabled: Boolean = true,
    val travelBatterySaverEnabled: Boolean = false,
    val offlineOnlyMode: Boolean = false,
    val dailyGoalMinutes: Int = 20,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val readerDisplayName: String = "Reader",
    val vaultLockEnabled: Boolean = false,
    val vaultPinHash: String = "",
    val vaultSalt: String = "",
    val twoFactorRecoveryCode: String = "",
    val lastSyncTimestamp: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SIZE_SP = floatPreferencesKey("font_size_sp")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val READER_FONT = stringPreferencesKey("reader_font")
        val ACCESSIBILITY_PRESET = stringPreferencesKey("accessibility_preset")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val BATTERY_SAVER = booleanPreferencesKey("travel_battery_saver")
        val OFFLINE_ONLY = booleanPreferencesKey("offline_only_mode")
        val DAILY_GOAL_MINS = intPreferencesKey("daily_goal_minutes")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val READER_NAME = stringPreferencesKey("reader_display_name")
        val VAULT_LOCK_ENABLED = booleanPreferencesKey("vault_lock_enabled")
        val VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        val VAULT_SALT = stringPreferencesKey("vault_salt")
        val RECOVERY_CODE = stringPreferencesKey("two_factor_recovery_code")
        val LAST_SYNC_TS = longPreferencesKey("last_sync_timestamp")
    }

    val preferencesFlow: Flow<UserPreferences> = context.varnikaDataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let { raw ->
            ThemeMode.entries.firstOrNull { it.name == raw }
        } ?: ThemeMode.LIGHT

        val font = prefs[Keys.READER_FONT]?.let { raw ->
            ReaderFontOption.entries.firstOrNull { it.name == raw }
        } ?: ReaderFontOption.LORA_SERIF

        val preset = prefs[Keys.ACCESSIBILITY_PRESET]?.let { raw ->
            AccessibilityPreset.entries.firstOrNull { it.name == raw }
        } ?: AccessibilityPreset.BALANCED

        UserPreferences(
            themeMode = theme,
            fontSizeSp = prefs[Keys.FONT_SIZE_SP] ?: 19f,
            lineHeightMultiplier = prefs[Keys.LINE_HEIGHT] ?: 1.6f,
            readerFont = font,
            accessibilityPreset = preset,
            hapticPageTurnEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            travelBatterySaverEnabled = prefs[Keys.BATTERY_SAVER] ?: false,
            offlineOnlyMode = prefs[Keys.OFFLINE_ONLY] ?: false,
            dailyGoalMinutes = prefs[Keys.DAILY_GOAL_MINS] ?: 20,
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: false,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 20,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            readerDisplayName = prefs[Keys.READER_NAME] ?: "Reader",
            vaultLockEnabled = prefs[Keys.VAULT_LOCK_ENABLED] ?: false,
            vaultPinHash = prefs[Keys.VAULT_PIN_HASH] ?: "",
            vaultSalt = prefs[Keys.VAULT_SALT] ?: "",
            twoFactorRecoveryCode = prefs[Keys.RECOVERY_CODE] ?: "",
            lastSyncTimestamp = prefs[Keys.LAST_SYNC_TS] ?: 0L
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.varnikaDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setFontSize(sizeSp: Float) {
        context.varnikaDataStore.edit { it[Keys.FONT_SIZE_SP] = sizeSp.coerceIn(14f, 32f) }
    }

    suspend fun setLineHeight(multiplier: Float) {
        context.varnikaDataStore.edit { it[Keys.LINE_HEIGHT] = multiplier.coerceIn(1.3f, 2.0f) }
    }

    suspend fun setReaderFont(font: ReaderFontOption) {
        context.varnikaDataStore.edit { it[Keys.READER_FONT] = font.name }
    }

    suspend fun applyAccessibilityPreset(preset: AccessibilityPreset) {
        context.varnikaDataStore.edit { prefs ->
            prefs[Keys.ACCESSIBILITY_PRESET] = preset.name
            when (preset) {
                AccessibilityPreset.YOUNG_READER -> {
                    prefs[Keys.FONT_SIZE_SP] = 22f
                    prefs[Keys.LINE_HEIGHT] = 1.75f
                    prefs[Keys.READER_FONT] = ReaderFontOption.JAKARTA_SANS.name
                }
                AccessibilityPreset.BALANCED -> {
                    prefs[Keys.FONT_SIZE_SP] = 19f
                    prefs[Keys.LINE_HEIGHT] = 1.6f
                    prefs[Keys.READER_FONT] = ReaderFontOption.LORA_SERIF.name
                }
                AccessibilityPreset.LARGE_PRINT -> {
                    prefs[Keys.FONT_SIZE_SP] = 27f
                    prefs[Keys.LINE_HEIGHT] = 1.85f
                    prefs[Keys.READER_FONT] = ReaderFontOption.JAKARTA_SANS.name
                }
            }
        }
    }

    suspend fun setHapticPageTurn(enabled: Boolean) {
        context.varnikaDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setTravelBatterySaver(enabled: Boolean) {
        context.varnikaDataStore.edit { it[Keys.BATTERY_SAVER] = enabled }
    }

    suspend fun setOfflineOnlyMode(enabled: Boolean) {
        context.varnikaDataStore.edit { it[Keys.OFFLINE_ONLY] = enabled }
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.varnikaDataStore.edit { it[Keys.DAILY_GOAL_MINS] = minutes.coerceIn(5, 180) }
    }

    suspend fun setReminderSchedule(enabled: Boolean, hour: Int, minute: Int) {
        context.varnikaDataStore.edit {
            it[Keys.REMINDER_ENABLED] = enabled
            it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setReaderDisplayName(name: String) {
        val clean = name.trim().ifBlank { "Reader" }
        context.varnikaDataStore.edit { it[Keys.READER_NAME] = clean }
    }

    suspend fun enableVaultLock(pinHash: String, salt: String, recoveryCode: String) {
        context.varnikaDataStore.edit {
            it[Keys.VAULT_LOCK_ENABLED] = true
            it[Keys.VAULT_PIN_HASH] = pinHash
            it[Keys.VAULT_SALT] = salt
            it[Keys.RECOVERY_CODE] = recoveryCode
        }
    }

    suspend fun disableVaultLock() {
        context.varnikaDataStore.edit {
            it[Keys.VAULT_LOCK_ENABLED] = false
            it[Keys.VAULT_PIN_HASH] = ""
            it[Keys.VAULT_SALT] = ""
            it[Keys.RECOVERY_CODE] = ""
        }
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        context.varnikaDataStore.edit { it[Keys.LAST_SYNC_TS] = timestamp }
    }
}
