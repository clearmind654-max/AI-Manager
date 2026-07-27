package com.aimanager.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String,
    val title: String = "New Chat",
    val mode: String = "MANAGER",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isSensitive: Boolean = false
)

@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val workerId: String? = null,
    val modelUsed: String? = null,
    val tokensUsed: Int = 0,
    val latencyMs: Long = 0,
    val parentMessageId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false,
    val errorInfo: String? = null
)

@Serializable
data class WorkerTask(
    val id: String,
    val conversationId: String,
    val parentMessageId: String,
    val workerId: String,
    val status: TaskStatus = TaskStatus.CREATED,
    val inputPrompt: String = "",
    val output: String = "",
    val tokensIn: Int = 0,
    val tokensOut: Int = 0,
    val startedAt: Long = 0,
    val completedAt: Long = 0,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val fallbackFrom: String? = null
)

@Serializable
data class ApiKey(
    val id: String,
    val provider: ProviderType,
    val key: String,
    val tier: String = "free",
    val dailyLimit: Int = 1000,
    val weeklyLimit: Int = 5000,
    val usedToday: Int = 0,
    val usedThisWeek: Int = 0,
    val status: KeyStatus = KeyStatus.ACTIVE,
    val lastChecked: Long = 0,
    val lastError: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ProviderConfig(
    val providerId: String,
    val name: String,
    val baseUrl: String,
    val authHeader: String = "Authorization",
    val authPrefix: String = "Bearer ",
    val requestFormat: String = "openai_compatible",
    val streamingFormat: String = "sse",
    val models: List<String> = emptyList(),
    val maxContext: Int = 32000,
    val supportsVision: Boolean = false,
    val supportsStreaming: Boolean = true,
    val supportsFunctionCalling: Boolean = false,
    val isFree: Boolean = false,
    val freeModelIds: List<String> = emptyList()
)

@Serializable
data class Skill(
    val id: String,
    val name: String,
    val category: String,
    val description: String = "",
    val systemPrompt: String,
    val defaultModel: String? = null,
    val fallbackModels: List<String> = emptyList(),
    val parameters: List<SkillParameter> = emptyList(),
    val inputTemplate: String = "",
    val exampleInputs: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SkillParameter(
    val name: String,
    val type: String = "string",
    val options: List<String> = emptyList(),
    val defaultValue: String = "",
    val required: Boolean = false
)

@Serializable
data class Gem(
    val id: String,
    val name: String,
    val description: String = "",
    val steps: List<GemStep> = emptyList(),
    val parameters: List<SkillParameter> = emptyList(),
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null,
    val runCount: Int = 0
)

@Serializable
data class GemStep(
    val stepId: Int,
    val name: String,
    val type: StepType,
    val model: String? = null,
    val skillRef: String? = null,
    val promptTemplate: String,
    val inputs: List<String> = emptyList(),
    val outputs: List<String> = emptyList(),
    val timeoutMs: Long = 30000,
    val onFailure: FailureAction = FailureAction.RETRY,
    val fallbackModel: String? = null,
    val maxRetries: Int = 2,
    val dependsOn: List<Int> = emptyList()
)

@Serializable
data class CompressedContext(
    val version: String = "1.0",
    val userSummary: String = "",
    val currentPhase: String = "",
    val recentInteractions: List<ContextEntry> = emptyList(),
    val keyFacts: List<String> = emptyList(),
    val preferences: Map<String, String> = emptyList(),
    val activeSkills: List<String> = emptyList(),
    val activeGems: List<String> = emptyList()
)

@Serializable
data class ContextEntry(
    val role: String,
    val summary: String,
    val timestamp: Long = 0
)

@Serializable
data class ModelScore(
    val taskType: TaskType,
    val model: String,
    val totalAttempts: Int = 0,
    val acceptedCount: Int = 0,
    val rerunCount: Int = 0,
    val averageLatencyMs: Long = 0,
    val totalTokensUsed: Long = 0,
    val lastUpdated: Long = 0
) {
    val acceptanceRate: Float
        get() = if (totalAttempts > 0) acceptedCount.toFloat() / totalAttempts else 0f
}

@Serializable
data class UsageRecord(
    val id: String,
    val timestamp: Long,
    val provider: String,
    val model: String,
    val taskType: TaskType,
    val tokensIn: Int,
    val tokensOut: Int,
    val latencyMs: Long,
    val costEstimate: Double = 0.0,
    val usedFallback: Boolean = false,
    val conversationId: String = ""
)

@Serializable
data class WorkflowRun(
    val id: String,
    val gemId: String,
    val conversationId: String,
    val status: TaskStatus = TaskStatus.CREATED,
    val currentStep: Int = 0,
    val stepResults: Map<Int, StepResult> = emptyMap(),
    val startedAt: Long = 0,
    val completedAt: Long = 0,
    val errorMessage: String? = null
)

@Serializable
data class StepResult(
    val stepId: Int,
    val status: TaskStatus,
    val output: String = "",
    val model: String = "",
    val tokensUsed: Int = 0,
    val latencyMs: Long = 0,
    val errorMessage: String? = null,
    val timestamp: Long = 0
)

@Serializable
data class CanvasProject(
    val id: String,
    val name: String,
    val nodes: List<CanvasNode> = emptyList(),
    val edges: List<CanvasEdge> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class CanvasNode(
    val id: String,
    val type: NodeType,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 200f,
    val height: Float = 150f,
    val content: String = "",
    val messageId: String? = null,
    val mediaUrl: String? = null,
    val color: Long = 0xFFFFFFFF
)

@Serializable
data class CanvasEdge(
    val id: String,
    val sourceNodeId: String,
    val targetNodeId: String,
    val type: EdgeType = EdgeType.DATA
)

@Serializable
data class TaskPlan(
    val tasks: List<PlannedTask> = emptyList(),
    val synthesisStrategy: SynthesisStrategy = SynthesisStrategy.COMPLEMENTARY
)

@Serializable
data class PlannedTask(
    val id: String,
    val model: String,
    val priority: String = "medium",
    val prompt: String,
    val context: String = "",
    val timeoutMs: Long = 30000,
    val dependsOn: List<String> = emptyList()
)

@Serializable
data class NormalizedRequest(
    val model: String,
    val messages: List<NormalizedMessage>,
    val systemPrompt: String? = null,
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f,
    val stream: Boolean = true,
    val images: List<String>? = null
)

@Serializable
data class NormalizedMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null
)

data class NormalizedChunk(
    val content: String,
    val finishReason: FinishReason? = null,
    val tokensUsed: Int = 0
)

data class KeyValidationResult(
    val isValid: Boolean,
    val error: String? = null,
    val models: List<String> = emptyList()
)

data class ProviderHealth(
    val provider: ProviderType,
    val healthyKeys: Int,
    val totalKeys: Int,
    val averageLatencyMs: Long,
    val lastChecked: Long,
    val status: String = "ok"
)
