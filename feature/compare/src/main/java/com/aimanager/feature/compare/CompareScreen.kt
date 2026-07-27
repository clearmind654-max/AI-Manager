package com.aimanager.feature.compare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.aimanager.core.model.*
import com.aimanager.core.network.provider.ApiClient
import com.aimanager.core.network.provider.ProviderRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompareResult(
    val model: String,
    val output: String = "",
    val latencyMs: Long = 0,
    val tokens: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class CompareUiState(
    val prompt: String = "",
    val selectedModels: Set<String> = setOf("gemini-2.0-flash", "deepseek-chat", "claude-sonnet-4-20250514"),
    val results: List<CompareResult> = emptyList(),
    val isRunning: Boolean = false
)

@HiltViewModel
class CompareViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val providerRegistry: ProviderRegistry
) : ViewModel() {
    private val _uiState = MutableStateFlow(CompareUiState())
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    fun updatePrompt(text: String) { _uiState.update { it.copy(prompt = text) } }

    fun toggleModel(model: String) {
        _uiState.update {
            val models = if (model in it.selectedModels) it.selectedModels - model else it.selectedModels + model
            it.copy(selectedModels = models)
        }
    }

    fun runComparison() {
        val state = _uiState.value
        if (state.prompt.isBlank() || state.selectedModels.isEmpty()) return

        _uiState.update { it.copy(isRunning = true, results = state.selectedModels.map { m -> CompareResult(model = m, isLoading = true) }) }

        viewModelScope.launch {
            val results = state.selectedModels.map { model ->
                launch {
                    val startTime = System.currentTimeMillis()
                    try {
                        val providerType = providerRegistry.getProviderForModel(model) ?: ProviderType.GEMINI
                        val request = NormalizedRequest(
                            model = model,
                            messages = listOf(NormalizedMessage(role = "user", content = state.prompt)),
                            maxTokens = 1024, stream = false
                        )
                        val output = StringBuilder()
                        apiClient.complete(providerType, request).collect { chunk -> output.append(chunk.content) }
                        val latency = System.currentTimeMillis() - startTime
                        _uiState.update { s -> s.copy(results = s.results.map { r ->
                            if (r.model == model) r.copy(output = output.toString(), latencyMs = latency, tokens = output.length / 4, isLoading = false) else r
                        }) }
                    } catch (e: Exception) {
                        _uiState.update { s -> s.copy(results = s.results.map { r ->
                            if (r.model == model) r.copy(error = e.message ?: "Failed", isLoading = false) else r
                        }) }
                    }
                }
            }.forEach { it.join() }
            _uiState.update { it.copy(isRunning = false) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    viewModel: CompareViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allModels = listOf("gemini-2.0-flash", "deepseek-chat", "claude-sonnet-4-20250514", "grok-2", "qwen-turbo")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare Models") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.prompt,
                    onValueChange = { viewModel.updatePrompt(it) },
                    label = { Text("Prompt") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    placeholder = { Text("Enter a prompt to compare across models...") }
                )
            }

            item {
                Text("Select Models", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    allModels.forEach { model ->
                        FilterChip(
                            selected = model in uiState.selectedModels,
                            onClick = { viewModel.toggleModel(model) },
                            label = { Text(model.take(10), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.runComparison() },
                    enabled = uiState.prompt.isNotBlank() && uiState.selectedModels.isNotEmpty() && !uiState.isRunning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isRunning) "Running..." else "Compare")
                }
            }

            items(uiState.results.size) { index ->
                val result = uiState.results[index]
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(result.model, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (result.latencyMs > 0) Text("${result.latencyMs}ms", style = MaterialTheme.typography.labelSmall)
                            if (result.tokens > 0) Text(" • ${result.tokens}t", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        when {
                            result.isLoading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            result.error != null -> Text("❌ ${result.error}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            else -> Text(result.output, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
