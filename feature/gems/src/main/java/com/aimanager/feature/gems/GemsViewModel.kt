package com.aimanager.feature.gems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aimanager.core.common.IdGenerator
import com.aimanager.core.model.*
import com.aimanager.data.repository.GemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GemsUiState(
    val gems: List<Gem> = emptyList(),
    val showEditor: Boolean = false,
    val editingGem: Gem? = null,
    val name: String = "",
    val description: String = "",
    val steps: List<GemStep> = emptyList()
)

@HiltViewModel
class GemsViewModel @Inject constructor(
    private val gemRepository: GemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GemsUiState())
    val uiState: StateFlow<GemsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            gemRepository.getAll().collect { gems ->
                _uiState.update { it.copy(gems = gems) }
            }
        }
    }

    fun showEditor(gem: Gem? = null) {
        _uiState.update {
            it.copy(
                showEditor = true,
                editingGem = gem,
                name = gem?.name ?: "",
                description = gem?.description ?: "",
                steps = gem?.steps ?: emptyList()
            )
        }
    }

    fun hideEditor() { _uiState.update { it.copy(showEditor = false, editingGem = null) } }
    fun updateName(v: String) { _uiState.update { it.copy(name = v) } }
    fun updateDescription(v: String) { _uiState.update { it.copy(description = v) } }

    fun addStep() {
        val steps = _uiState.value.steps.toMutableList()
        steps.add(GemStep(
            stepId = steps.size + 1,
            name = "Step ${steps.size + 1}",
            type = StepType.AI_CALL,
            promptTemplate = ""
        ))
        _uiState.update { it.copy(steps = steps) }
    }

    fun updateStep(index: Int, step: GemStep) {
        val steps = _uiState.value.steps.toMutableList()
        if (index in steps.indices) steps[index] = step
        _uiState.update { it.copy(steps = steps) }
    }

    fun removeStep(index: Int) {
        val steps = _uiState.value.steps.toMutableList()
        if (index in steps.indices) steps.removeAt(index)
        _uiState.update { it.copy(steps = steps) }
    }

    fun saveGem() {
        val state = _uiState.value
        if (state.name.isBlank()) return

        viewModelScope.launch {
            val gem = Gem(
                id = state.editingGem?.id ?: IdGenerator.newId(),
                name = state.name,
                description = state.description,
                steps = state.steps,
                version = (state.editingGem?.version ?: 0) + 1,
                createdAt = state.editingGem?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            if (state.editingGem != null) gemRepository.update(gem)
            else gemRepository.insert(gem)
            hideEditor()
        }
    }

    fun deleteGem(id: String) {
        viewModelScope.launch { gemRepository.delete(id) }
    }
}
