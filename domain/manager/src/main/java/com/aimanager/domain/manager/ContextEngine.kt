package com.aimanager.domain.manager

import com.aimanager.core.model.*
import com.aimanager.core.common.TokenEstimator
import com.aimanager.data.repository.ContextRepository
import com.aimanager.data.repository.MessageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextEngine @Inject constructor(
    private val contextRepository: ContextRepository,
    private val messageRepository: MessageRepository
) {
    suspend fun getCompressedContext(conversationId: String): CompressedContext? {
        return contextRepository.get(conversationId)
    }

    suspend fun compressContext(conversationId: String, messages: List<Message>): CompressedContext {
        val existing = contextRepository.get(conversationId)
        val recentMessages = messages.takeLast(10)

        val recentEntries = recentMessages.map { msg ->
            ContextEntry(
                role = msg.role.name.lowercase(),
                summary = summarizeMessage(msg.content),
                timestamp = msg.timestamp
            )
        }

        val keyFacts = existing?.keyFacts?.toMutableList() ?: mutableListOf()
        // Extract new key facts from recent messages
        for (msg in recentMessages) {
            if (msg.role == MessageRole.USER && msg.content.length > 20) {
                val fact = msg.content.take(100)
                if (keyFacts.none { it.contains(fact.take(30)) }) {
                    keyFacts.add(fact)
                }
            }
        }
        // Keep only last 20 key facts
        val trimmedFacts = if (keyFacts.size > 20) keyFacts.takeLast(20) else keyFacts

        val context = CompressedContext(
            version = "1.0",
            userSummary = existing?.userSummary ?: "",
            currentPhase = "active",
            recentInteractions = recentEntries,
            keyFacts = trimmedFacts,
            preferences = existing?.preferences ?: emptyMap(),
            activeSkills = existing?.activeSkills ?: emptyList(),
            activeGems = existing?.activeGems ?: emptyList()
        )

        contextRepository.save(conversationId, context)
        return context
    }

    suspend fun buildContextForWorker(
        conversationId: String,
        taskType: TaskType,
        workerModel: String,
        maxTokens: Int
    ): String {
        val context = getCompressedContext(conversationId) ?: return ""
        val budget = calculateTokenBudget(maxTokens)

        val sb = StringBuilder()
        sb.appendLine("=== CONTEXT ===")

        // Key facts (highest priority)
        var usedTokens = 0
        for (fact in context.keyFacts) {
            val factTokens = TokenEstimator.estimateTokens(fact)
            if (usedTokens + factTokens > budget.keyFactTokens) break
            sb.appendLine("- $fact")
            usedTokens += factTokens
        }

        // Recent interactions
        for (entry in context.recentInteractions.takeLast(5)) {
            val entryText = "${entry.role}: ${entry.summary}"
            val entryTokens = TokenEstimator.estimateTokens(entryText)
            if (usedTokens + entryTokens > budget.keyFactTokens + budget.historyTokens) break
            sb.appendLine(entryText)
            usedTokens += entryTokens
        }

        // Preferences
        if (context.preferences.isNotEmpty()) {
            val prefsText = "Preferences: ${context.preferences.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
            val prefsTokens = TokenEstimator.estimateTokens(prefsText)
            if (usedTokens + prefsTokens <= budget.totalBudget) {
                sb.appendLine(prefsText)
            }
        }

        sb.appendLine("=== END CONTEXT ===")
        return sb.toString()
    }

    private fun summarizeMessage(content: String): String {
        return if (content.length <= 150) content
        else content.take(147) + "..."
    }

    private fun calculateTokenBudget(maxTokens: Int): TokenBudget {
        val contextBudget = (maxTokens * 0.3).toInt().coerceAtMost(2000)
        return TokenBudget(
            totalBudget = contextBudget,
            keyFactTokens = (contextBudget * 0.4).toInt(),
            historyTokens = (contextBudget * 0.4).toInt(),
            preferenceTokens = (contextBudget * 0.2).toInt()
        )
    }
}

data class TokenBudget(
    val totalBudget: Int,
    val keyFactTokens: Int,
    val historyTokens: Int,
    val preferenceTokens: Int
)
