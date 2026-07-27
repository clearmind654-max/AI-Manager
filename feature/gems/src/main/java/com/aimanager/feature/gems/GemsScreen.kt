package com.aimanager.feature.gems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aimanager.core.model.Gem
import com.aimanager.core.model.GemStep
import com.aimanager.core.model.StepType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GemsScreen(
    viewModel: GemsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showEditor) {
        GemEditorScreen(
            name = uiState.name,
            description = uiState.description,
            steps = uiState.steps,
            isEditing = uiState.editingGem != null,
            onNameChange = viewModel::updateName,
            onDescriptionChange = viewModel::updateDescription,
            onAddStep = viewModel::addStep,
            onUpdateStep = viewModel::updateStep,
            onRemoveStep = viewModel::removeStep,
            onSave = viewModel::saveGem,
            onBack = viewModel::hideEditor
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gems (Workflows)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showEditor() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Gem")
            }
        }
    ) { padding ->
        if (uiState.gems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No Gems yet", style = MaterialTheme.typography.titleMedium)
                    Text("Create multi-step AI workflows", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.gems, key = { it.id }) { gem ->
                    GemCard(
                        gem = gem,
                        onClick = { viewModel.showEditor(gem) },
                        onDelete = { viewModel.deleteGem(gem.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun GemCard(gem: Gem, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(gem.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                if (gem.description.isNotEmpty()) {
                    Text(gem.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${gem.steps.size} steps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (gem.runCount > 0) {
                        Text("Run ${gem.runCount}x", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GemEditorScreen(
    name: String, description: String, steps: List<GemStep>, isEditing: Boolean,
    onNameChange: (String) -> Unit, onDescriptionChange: (String) -> Unit,
    onAddStep: () -> Unit, onUpdateStep: (Int, GemStep) -> Unit, onRemoveStep: (Int) -> Unit,
    onSave: () -> Unit, onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Gem" else "New Gem") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { TextButton(onClick = onSave, enabled = name.isNotBlank()) { Text("Save") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddStep) { Icon(Icons.Default.Add, "Add Step") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(name, onNameChange, label = { Text("Gem Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            item {
                OutlinedTextField(description, onDescriptionChange, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            item { Text("Steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            itemsIndexed(steps, key = { _, s -> s.stepId }) { index, step ->
                StepCard(
                    step = step,
                    onUpdate = { onUpdateStep(index, it) },
                    onRemove = { onRemoveStep(index) }
                )
            }
        }
    }
}

@Composable
fun StepCard(step: GemStep, onUpdate: (GemStep) -> Unit, onRemove: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Step ${step.stepId}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                FilterChip(
                    selected = false,
                    onClick = { onUpdate(step.copy(type = when(step.type) {
                        StepType.AI_CALL -> StepType.SKILL_CALL
                        StepType.SKILL_CALL -> StepType.IMAGE_GEN
                        StepType.IMAGE_GEN -> StepType.USER_INPUT
                        StepType.USER_INPUT -> StepType.AI_CALL
                        else -> StepType.AI_CALL
                    })) },
                    label = { Text(step.type.name, style = MaterialTheme.typography.labelSmall) }
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = step.name,
                onValueChange = { onUpdate(step.copy(name = it)) },
                label = { Text("Step Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = step.model ?: "",
                onValueChange = { onUpdate(step.copy(model = it.ifBlank { null })) },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g., deepseek-chat") }
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = step.promptTemplate,
                onValueChange = { onUpdate(step.copy(promptTemplate = it)) },
                label = { Text("Prompt Template") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                placeholder = { Text("Use {{variable}} to reference previous step outputs") }
            )
        }
    }
}
