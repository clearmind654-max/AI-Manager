package com.aimanager.domain.manager

import com.aimanager.core.model.*
import com.aimanager.core.network.provider.ApiClient
import com.aimanager.core.network.provider.KeyPoolManager
import com.aimanager.core.network.provider.ProviderRegistry
import com.aimanager.data.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagerEngine @Inject constructor(
    private val apiClient: ApiClient,
    private val providerRegistry: ProviderRegistry,
    private val keyPoolManager: KeyPoolManager,
    private val contextEngine: ContextEngine,
    private val modelScoreRepository: ModelScoreRepository,
    private val userPreferences: UserPreferencesRepository
) {
    suspend fun classifyTask(prompt: String): TaskType {
        val lower = prompt.lowercase()
        return when {
            lower.contains("code") || lower.contains("function") || lower.contains("debug") ||
            lower.contains("script") || lower.contains("program") || lower.contains("api") -> TaskType.CODE_GENERATION
            lower.contains("write") || lower.contains("story") || lower.contains("poem") ||
            lower.contains("creative") || lower.contains("blog") -> TaskType.CREATIVE_WRITING
            lower.contains("analyze") || lower.contains("data") || lower.contains("statistics") -> TaskType.DATA_ANALYSIS
            lower.contains("search") || lower.contains("find") || lower.contains("news") ||
            lower.contains("trending") || lower.contains("latest") -> TaskType.RESEARCH
            lower.contains("translate") || lower.contains("translation") -> TaskType.TRANSLATION
            lower.contains("image") || lower.contains("photo") || lower.contains("picture") ||
            lower.contains("draw") || lower.contains("generate image") -> TaskType.IMAGE_GENERATION
            lower.contains("video") || lower.contains("animate") -> TaskType.VIDEO_GENERATION
            lower.contains("summarize") || lower.contains("summary") -> TaskType.SUMMARIZATION
            else -> TaskType.GENERAL
        }
    }

    suspend fun shouldDelegate(taskType: TaskType, prompt: String): Boolean {
        val tokenEstimate = prompt.length / 4
        // Simple tasks: direct answer
        if (tokenEstimate < 200 && taskType == TaskType.GENERAL) return false
        if (prompt.split(" ").size < 15 && taskType == TaskType.GENERAL) return false
        // Complex tasks: delegate
        return taskType != TaskType.GENERAL || tokenEstimate > 500
    }

    suspend fun selectModel(taskType: TaskType): String {
        // Check learned preferences first
        val bestModel = modelScoreRepository.getBestModel(taskType)
        if (bestModel != null) return bestModel

        // Default routing
        return when (taskType) {
            TaskType.CODE_GENERATION -> "deepseek-chat"
            TaskType.CREATIVE_WRITING -> "claude-sonnet-4-20250514"
            TaskType.DATA_ANALYSIS -> "gemini-2.0-flash"
            TaskType.RESEARCH -> "grok-2"
            TaskType.TRANSLATION -> "qwen-turbo"
            TaskType.IMAGE_GENERATION -> "flux"
            TaskType.VIDEO_GENERATION -> "kling"
            TaskType.SUMMARIZATION -> "gemini-2.0-flash"
            TaskType.GENERAL -> "gemini-2.0-flash"
        }
    }

    fun buildSystemPrompt(
        compressedContext: CompressedContext?,
        availableWorkers: List<String>,
        keyPoolStatuses: List<String>
    ): String {
        return """
You are the orchestration manager for a multi-AI assistant app.

IDENTITY:
- You are NOT a general assistant. You are a router, decomposer, and synthesizer.
- For simple questions (greetings, basic facts, short answers under 200 words), respond directly.
- For complex tasks, decompose and delegate to specialist workers.

AVAILABLE WORKERS: ${availableWorkers.joinToString(", ")}

KEY POOL STATUS: ${keyPoolStatuses.joinToString("; ")}

CONTEXT: ${compressedContext?.keyFacts?.joinToString(". ") ?: "No prior context"}

RULES:
1. If the task is simple enough (under 200 words, no code, no research), answer directly.
2. For complex tasks, output a TASK_PLAN in XML format.
3. NEVER fabricate worker capabilities.
4. Always consider the user's budget.
5. Keep responses concise unless user asks for detail.
        """.trimIndent()
    }

    suspend fun directAnswer(
        conversationId: String,
        prompt: String,
        history: List<Message>
    ): Flow<String> = flow {
        val context = contextEngine.getCompressedContext(conversationId)
        val systemPrompt = buildSystemPrompt(context, emptyList(), emptyList())

        val messages = buildMessageHistory(history, prompt)
        val request = NormalizedRequest(
            model = "gemini-2.0-flash",
            messages = messages,
            systemPrompt = systemPrompt,
            maxTokens = 2048,
            stream = true
        )

        apiClient.complete(ProviderType.GEMINI, request).collect { chunk ->
            if (chunk.content.isNotEmpty()) emit(chunk.content)
        }
    }

    private fun buildMessageHistory(history: List<Message>, currentPrompt: String): List<NormalizedMessage> {
        val messages = mutableListOf<NormalizedMessage>()
        // Take last 10 messages for context
        val recentHistory = history.takeLast(10)
        for (msg in recentHistory) {
            messages.add(NormalizedMessage(
                role = if (msg.role == MessageRole.USER) "user" else "assistant",
                content = msg.content
            ))
        }
        messages.add(NormalizedMessage(role = "user", content = currentPrompt))
        return messages
    }
}
