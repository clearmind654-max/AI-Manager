package com.aimanager.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aimanager.core.model.ModelScore
import com.aimanager.core.model.UsageRecord
import com.aimanager.data.repository.ModelScoreRepository
import com.aimanager.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val totalTokensToday: Long = 0,
    val totalCostToday: Double = 0.0,
    val totalTokensWeek: Long = 0,
    val totalCostWeek: Double = 0.0,
    val recentUsage: List<UsageRecord> = emptyList(),
    val modelScores: List<ModelScore> = emptyList(),
    val usageByProvider: Map<String, Long> = emptyMap()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val modelScoreRepository: ModelScoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayStart = now - 86_400_000
            val weekStart = now - 604_800_000

            val tokensToday = usageRepository.totalTokensSince(dayStart)
            val costToday = usageRepository.totalCostSince(dayStart)
            val tokensWeek = usageRepository.totalTokensSince(weekStart)
            val costWeek = usageRepository.totalCostSince(weekStart)
            val byProvider = usageRepository.usageByProviderSince(weekStart)

            _uiState.update {
                it.copy(
                    totalTokensToday = tokensToday,
                    totalCostToday = costToday,
                    totalTokensWeek = tokensWeek,
                    totalCostWeek = costWeek,
                    usageByProvider = byProvider.associate { p -> p.provider to p.total }
                )
            }

            usageRepository.getRecent(50).collect { records ->
                _uiState.update { it.copy(recentUsage = records) }
            }
        }

        viewModelScope.launch {
            modelScoreRepository.getAll().collect { scores ->
                _uiState.update { it.copy(modelScores = scores) }
            }
        }
    }
}
