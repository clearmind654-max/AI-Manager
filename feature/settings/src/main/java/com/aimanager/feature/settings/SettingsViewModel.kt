package com.aimanager.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aimanager.core.model.*
import com.aimanager.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiKeys: List<ApiKey> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: Long = 0xFF6750A4,
    val fontScale: Float = 1.0f,
    val backgroundProcessing: Boolean = false,
    val biometricLock: Boolean = false,
    val defaultManager: String = "gemini",
    val maxParallelWorkers: Int = 3,
    val dailyBudget: Double = 5.0,
    val weeklyBudget: Double = 20.0,
    val notificationsEnabled: Boolean = true,
    val soundEffects: Boolean = false,
    val hapticFeedback: Boolean = true,
    val showAddKeyDialog: Boolean = false,
    val selectedProvider: ProviderType? = null,
    val keyInputValue: String = "",
    val keyValidationResult: String? = null,
    val isValidating: Boolean = false
)

sealed class SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent()
    data class Error(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val userPreferences: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        loadSettings()
        loadApiKeys()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                userPreferences.themeMode,
                userPreferences.accentColor,
                userPreferences.fontScale,
                userPreferences.backgroundProcessing,
                userPreferences.biometricLock,
                userPreferences.defaultManager,
                userPreferences.maxParallelWorkers,
                userPreferences.dailyBudget,
                userPreferences.weeklyBudget,
                userPreferences.notificationsEnabled,
                userPreferences.soundEffects,
                userPreferences.hapticFeedback
            ) { values ->
                SettingsUiState(
                    themeMode = values[0] as ThemeMode,
                    accentColor = values[1] as Long,
                    fontScale = values[2] as Float,
                    backgroundProcessing = values[3] as Boolean,
                    biometricLock = values[4] as Boolean,
                    defaultManager = values[5] as String,
                    maxParallelWorkers = values[6] as Int,
                    dailyBudget = values[7] as Double,
                    weeklyBudget = values[8] as Double,
                    notificationsEnabled = values[9] as Boolean,
                    soundEffects = values[10] as Boolean,
                    hapticFeedback = values[11] as Boolean
                )
            }.collect { state ->
                _uiState.update {
                    it.copy(
                        themeMode = state.themeMode,
                        accentColor = state.accentColor,
                        fontScale = state.fontScale,
                        backgroundProcessing = state.backgroundProcessing,
                        biometricLock = state.biometricLock,
                        defaultManager = state.defaultManager,
                        maxParallelWorkers = state.maxParallelWorkers,
                        dailyBudget = state.dailyBudget,
                        weeklyBudget = state.weeklyBudget,
                        notificationsEnabled = state.notificationsEnabled,
                        soundEffects = state.soundEffects,
                        hapticFeedback = state.hapticFeedback
                    )
                }
            }
        }
    }

    private fun loadApiKeys() {
        viewModelScope.launch {
            apiKeyRepository.getAll().collect { keys ->
                _uiState.update { it.copy(apiKeys = keys) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }

    fun setBackgroundProcessing(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setBackgroundProcessing(enabled) }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setBiometricLock(enabled) }
    }

    fun setMaxParallelWorkers(max: Int) {
        viewModelScope.launch { userPreferences.setMaxParallelWorkers(max) }
    }

    fun setDailyBudget(amount: Double) {
        viewModelScope.launch { userPreferences.setDailyBudget(amount) }
    }

    fun setWeeklyBudget(amount: Double) {
        viewModelScope.launch { userPreferences.setWeeklyBudget(amount) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setNotificationsEnabled(enabled) }
    }

    fun showAddKeyDialog(provider: ProviderType) {
        _uiState.update { it.copy(showAddKeyDialog = true, selectedProvider = provider, keyInputValue = "", keyValidationResult = null) }
    }

    fun hideAddKeyDialog() {
        _uiState.update { it.copy(showAddKeyDialog = false, selectedProvider = null, keyInputValue = "") }
    }

    fun updateKeyInput(value: String) {
        _uiState.update { it.copy(keyInputValue = value) }
    }

    fun addApiKey() {
        val provider = _uiState.value.selectedProvider ?: return
        val key = _uiState.value.keyInputValue.trim()
        if (key.isEmpty()) {
            viewModelScope.launch { _events.emit(SettingsEvent.Error("API key cannot be empty")) }
            return
        }

        _uiState.update { it.copy(isValidating = true, keyValidationResult = null) }

        viewModelScope.launch {
            try {
                val apiKey = ApiKey(
                    id = com.aimanager.core.common.IdGenerator.newId(),
                    provider = provider,
                    key = key,
                    tier = "free",
                    status = KeyStatus.ACTIVE
                )
                apiKeyRepository.insert(apiKey)
                _uiState.update { it.copy(isValidating = false, showAddKeyDialog = false) }
                _events.emit(SettingsEvent.ShowMessage("✅ ${provider.name} key added successfully"))
            } catch (e: Exception) {
                _uiState.update { it.copy(isValidating = false, keyValidationResult = "Error: ${e.message}") }
            }
        }
    }

    fun deleteApiKey(id: String) {
        viewModelScope.launch {
            apiKeyRepository.delete(id)
            _events.emit(SettingsEvent.ShowMessage("Key removed"))
        }
    }

    fun autoDetectProvider(key: String): ProviderType? {
        return when {
            key.startsWith("AIza") -> ProviderType.GEMINI
            key.startsWith("sk-ant-") -> ProviderType.CLAUDE
            key.startsWith("sk-") -> ProviderType.DEEPSEEK
            key.contains(":free") -> ProviderType.OPENROUTER
            else -> null
        }
    }
}
