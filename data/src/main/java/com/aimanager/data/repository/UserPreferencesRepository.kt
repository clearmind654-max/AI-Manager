package com.aimanager.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aimanager.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds get() = context.dataStore

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = longPreferencesKey("accent_color")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val BACKGROUND_PROCESSING = booleanPreferencesKey("background_processing")
        val DEFAULT_MODEL = stringPreferencesKey("default_model")
        val MANAGER_MODEL = stringPreferencesKey("manager_model")
        val MAX_PARALLEL_WORKERS = intPreferencesKey("max_parallel_workers")
        val DEFAULT_TEMPERATURE = floatPreferencesKey("default_temperature")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val VOICE_MODE_SPEED = floatPreferencesKey("voice_speed")
        val COST_PREFERENCE = stringPreferencesKey("cost_preference")
        val DAILY_BUDGET = doublePreferencesKey("daily_budget")
        val WEEKLY_BUDGET = doublePreferencesKey("weekly_budget")
        val BUDGET_ALERT_THRESHOLD = floatPreferencesKey("budget_alert_threshold")
        val AUTO_ARCHIVE_DAYS = intPreferencesKey("auto_archive_days")
        val CONTEXT_COMPRESSION_THRESHOLD = intPreferencesKey("context_compression_threshold")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val COMPLEXITY_LEVEL = intPreferencesKey("complexity_level")
        val LANGUAGE = stringPreferencesKey("language")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    // Theme
    val themeMode: Flow<ThemeMode> = ds.data.map {
        try { ThemeMode.valueOf(it[THEME_MODE] ?: "SYSTEM") } catch (_: Exception) { ThemeMode.SYSTEM }
    }
    suspend fun setThemeMode(mode: ThemeMode) = ds.edit { it[THEME_MODE] = mode.name }

    val accentColor: Flow<Long> = ds.data.map { it[ACCENT_COLOR] ?: 0xFF6750A4 }
    suspend fun setAccentColor(color: Long) = ds.edit { it[ACCENT_COLOR] = color }

    val fontScale: Flow<Float> = ds.data.map { it[FONT_SCALE] ?: 1.0f }
    suspend fun setFontScale(scale: Float) = ds.edit { it[FONT_SCALE] = scale }

    // Processing
    val backgroundProcessing: Flow<Boolean> = ds.data.map { it[BACKGROUND_PROCESSING] ?: false }
    suspend fun setBackgroundProcessing(enabled: Boolean) = ds.edit { it[BACKGROUND_PROCESSING] = enabled }

    // Models
    val defaultManager: Flow<String> = ds.data.map { it[MANAGER_MODEL] ?: "gemini" }
    suspend fun setDefaultManager(model: String) = ds.edit { it[MANAGER_MODEL] = model }

    val maxParallelWorkers: Flow<Int> = ds.data.map { it[MAX_PARALLEL_WORKERS] ?: 3 }
    suspend fun setMaxParallelWorkers(max: Int) = ds.edit { it[MAX_PARALLEL_WORKERS] = max }

    val defaultTemperature: Flow<Float> = ds.data.map { it[DEFAULT_TEMPERATURE] ?: 0.7f }
    suspend fun setDefaultTemperature(temp: Float) = ds.edit { it[DEFAULT_TEMPERATURE] = temp }

    // Security
    val biometricLock: Flow<Boolean> = ds.data.map { it[BIOMETRIC_LOCK] ?: false }
    suspend fun setBiometricLock(enabled: Boolean) = ds.edit { it[BIOMETRIC_LOCK] = enabled }

    // Voice
    val voiceSpeed: Flow<Float> = ds.data.map { it[VOICE_MODE_SPEED] ?: 1.0f }
    suspend fun setVoiceSpeed(speed: Float) = ds.edit { it[VOICE_MODE_SPEED] = speed }

    // Cost
    val costPreference: Flow<String> = ds.data.map { it[COST_PREFERENCE] ?: "cheapest" }
    suspend fun setCostPreference(pref: String) = ds.edit { it[COST_PREFERENCE] = pref }

    val dailyBudget: Flow<Double> = ds.data.map { it[DAILY_BUDGET] ?: 5.0 }
    suspend fun setDailyBudget(amount: Double) = ds.edit { it[DAILY_BUDGET] = amount }

    val weeklyBudget: Flow<Double> = ds.data.map { it[WEEKLY_BUDGET] ?: 20.0 }
    suspend fun setWeeklyBudget(amount: Double) = ds.edit { it[WEEKLY_BUDGET] = amount }

    val budgetAlertThreshold: Flow<Float> = ds.data.map { it[BUDGET_ALERT_THRESHOLD] ?: 0.8f }
    suspend fun setBudgetAlertThreshold(threshold: Float) = ds.edit { it[BUDGET_ALERT_THRESHOLD] = threshold }

    // Misc
    val autoArchiveDays: Flow<Int> = ds.data.map { it[AUTO_ARCHIVE_DAYS] ?: 30 }
    val compressionThreshold: Flow<Int> = ds.data.map { it[CONTEXT_COMPRESSION_THRESHOLD] ?: 10 }
    val notificationsEnabled: Flow<Boolean> = ds.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val soundEffects: Flow<Boolean> = ds.data.map { it[SOUND_EFFECTS] ?: false }
    val hapticFeedback: Flow<Boolean> = ds.data.map { it[HAPTIC_FEEDBACK] ?: true }
    val onboardingComplete: Flow<Boolean> = ds.data.map { it[ONBOARDING_COMPLETE] ?: false }
    val complexityLevel: Flow<Int> = ds.data.map { it[COMPLEXITY_LEVEL] ?: 1 }
    val language: Flow<String> = ds.data.map { it[LANGUAGE] ?: "en" }
    val isFirstLaunch: Flow<Boolean> = ds.data.map { it[FIRST_LAUNCH] ?: true }

    suspend fun setOnboardingComplete() = ds.edit { it[ONBOARDING_COMPLETE] = true }
    suspend fun setComplexityLevel(level: Int) = ds.edit { it[COMPLEXITY_LEVEL] = level }
    suspend fun setLanguage(lang: String) = ds.edit { it[LANGUAGE] = lang }
    suspend fun setFirstLaunchDone() = ds.edit { it[FIRST_LAUNCH] = false }
}
