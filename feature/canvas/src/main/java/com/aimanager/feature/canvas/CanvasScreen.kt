package com.aimanager.feature.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.aimanager.core.common.IdGenerator
import com.aimanager.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class CanvasNodeState(
    val node: CanvasNode,
    val offset: Offset = Offset(node.x, node.y)
)

data class CanvasUiState(
    val nodes: List<CanvasNodeState> = emptyList(),
    val selectedNodeId: String? = null,
    val showAddMenu: Boolean = false
)

@HiltViewModel
class CanvasViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(CanvasUiState())
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

    fun addNode(type: NodeType) {
        val node = CanvasNode(
            id = IdGenerator.newId(),
            type = type,
            x = 100f + (_uiState.value.nodes.size * 50f),
            y = 200f + (_uiState.value.nodes.size * 30f),
            content = when (type) {
                NodeType.NOTE -> "New note..."
                NodeType.CHAT -> "Chat node"
                else -> type.name
            }
        )
        _uiState.update { it.copy(nodes = it.nodes + CanvasNodeState(node), showAddMenu = false) }
    }

    fun selectNode(id: String?) { _uiState.update { it.copy(selectedNodeId = id) } }

    fun moveNode(id: String, delta: Offset) {
        _uiState.update { state ->
            state.copy(nodes = state.nodes.map { ns ->
                if (ns.node.id == id) ns.copy(offset = ns.offset + delta) else ns
            })
        }
    }

    fun deleteSelected() {
        val id = _uiState.value.selectedNodeId ?: return
        _uiState.update { it.copy(nodes = it.nodes.filter { ns -> ns.node.id != id }, selectedNodeId = null) }
    }

    fun toggleAddMenu() { _uiState.update { it.copy(showAddMenu = !it.showAddMenu) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    viewModel: CanvasViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Canvas") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (uiState.selectedNodeId != null) {
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, "Delete Node")
                        }
                    }
                    IconButton(onClick = { viewModel.toggleAddMenu() }) {
                        Icon(Icons.Default.Add, "Add Node")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.showAddMenu) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NodeType.entries.forEach { type ->
                        SmallFloatingActionButton(
                            onClick = { viewModel.addNode(type) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(type.name.take(3), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { viewModel.selectNode(null) })
                }
        ) {
            if (uiState.nodes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Dashboard, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text("Empty Canvas", style = MaterialTheme.typography.titleMedium)
                        Text("Tap + to add nodes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            uiState.nodes.forEach { nodeState ->
                CanvasNodeCard(
                    nodeState = nodeState,
                    isSelected = nodeState.node.id == uiState.selectedNodeId,
                    onSelect = { viewModel.selectNode(nodeState.node.id) },
                    onDrag = { delta -> viewModel.moveNode(nodeState.node.id, delta) }
                )
            }
        }
    }
}

@Composable
fun CanvasNodeCard(
    nodeState: CanvasNodeState,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDrag: (Offset) -> Unit
) {
    val node = nodeState.node
    val bgColor = when (node.type) {
        NodeType.CHAT -> MaterialTheme.colorScheme.primaryContainer
        NodeType.MEDIA -> MaterialTheme.colorScheme.secondaryContainer
        NodeType.NOTE -> MaterialTheme.colorScheme.tertiaryContainer
        NodeType.GROUP -> MaterialTheme.colorScheme.surfaceVariant
        NodeType.WORKFLOW_TRIGGER -> MaterialTheme.colorScheme.errorContainer
        NodeType.WEB_CONTENT -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val icon = when (node.type) {
        NodeType.CHAT -> "💬"
        NodeType.MEDIA -> "🖼️"
        NodeType.NOTE -> "📝"
        NodeType.GROUP -> "📁"
        NodeType.WORKFLOW_TRIGGER -> "⚡"
        NodeType.WEB_CONTENT -> "🌐"
    }

    Card(
        modifier = Modifier
            .offset(x = nodeState.offset.x.dp, y = nodeState.offset.y.dp)
            .width(200.dp)
            .pointerInput(node.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
            .pointerInput(node.id) {
                detectTapGestures(onTap = { onSelect() })
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(node.type.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(node.content, style = MaterialTheme.typography.bodySmall, maxLines = 3)
        }
    }
}
