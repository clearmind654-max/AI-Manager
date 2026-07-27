package com.aimanager.feature.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aimanager.core.common.IdGenerator
import com.aimanager.core.model.Skill
import com.aimanager.core.model.SkillParameter
import com.aimanager.data.repository.SkillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SkillsUiState(
    val skills: List<Skill> = emptyList(),
    val showEditor: Boolean = false,
    val editingSkill: Skill? = null,
    val name: String = "",
    val category: String = "general",
    val systemPrompt: String = "",
    val defaultModel: String = "",
    val inputTemplate: String = ""
)

@HiltViewModel
class SkillsViewModel @Inject constructor(
    private val skillRepository: SkillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SkillsUiState())
    val uiState: StateFlow<SkillsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            skillRepository.getAll().collect { skills ->
                _uiState.update { it.copy(skills = skills) }
            }
        }
    }

    fun showEditor(skill: Skill? = null) {
        _uiState.update {
            it.copy(
                showEditor = true,
                editingSkill = skill,
                name = skill?.name ?: "",
                category = skill?.category ?: "general",
                systemPrompt = skill?.systemPrompt ?: "",
                defaultModel = skill?.defaultModel ?: "",
                inputTemplate = skill?.inputTemplate ?: ""
            )
        }
    }

    fun hideEditor() {
        _uiState.update { it.copy(showEditor = false, editingSkill = null) }
    }

    fun updateName(v: String) { _uiState.update { it.copy(name = v) } }
    fun updateCategory(v: String) { _uiState.update { it.copy(category = v) } }
    fun updateSystemPrompt(v: String) { _uiState.update { it.copy(systemPrompt = v) } }
    fun updateDefaultModel(v: String) { _uiState.update { it.copy(defaultModel = v) } }
    fun updateInputTemplate(v: String) { _uiState.update { it.copy(inputTemplate = v) } }

    fun saveSkill() {
        val state = _uiState.value
        if (state.name.isBlank() || state.systemPrompt.isBlank()) return

        viewModelScope.launch {
            val skill = Skill(
                id = state.editingSkill?.id ?: IdGenerator.newId(),
                name = state.name,
                category = state.category,
                systemPrompt = state.systemPrompt,
                defaultModel = state.defaultModel.ifBlank { null },
                inputTemplate = state.inputTemplate,
                createdAt = state.editingSkill?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            if (state.editingSkill != null) skillRepository.update(skill)
            else skillRepository.insert(skill)
            hideEditor()
        }
    }

    fun deleteSkill(id: String) {
        viewModelScope.launch { skillRepository.delete(id) }
    }
}
