package com.aimanager.core.network.provider

import com.aimanager.core.model.KeyStatus
import com.aimanager.core.model.ProviderType
import com.aimanager.data.repository.ApiKeyRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyPoolManager @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository
) {
    private val rateLimitedKeys = mutableMapOf<String, Long>() // keyId -> retryAfter timestamp

    suspend fun getKey(provider: ProviderType): String? {
        val keys = apiKeyRepository.getActiveByProvider(provider)
        // Filter out rate-limited keys
        val available = keys.filter { key ->
            val rateLimitedUntil = rateLimitedKeys[key.id]
            rateLimitedUntil == null || System.currentTimeMillis() > rateLimitedUntil
        }
        if (available.isEmpty()) return null
        // Pick least-used key today
        val best = available.minByOrNull { it.usedToday } ?: return null
        apiKeyRepository.incrementUsage(best.id)
        return best.key
    }

    suspend fun getKeyWithFallback(provider: ProviderType): Pair<String, String?>? {
        val key = getKey(provider) ?: return null
        return Pair(key, null)
    }

    suspend fun markRateLimited(keyId: String, retryAfterMs: Long = 60_000) {
        rateLimitedKeys[keyId] = System.currentTimeMillis() + retryAfterMs
        apiKeyRepository.updateStatus(com.aimanager.core.model.KeyStatus.RATE_LIMITED)
    }

    suspend fun markKeyInvalid(keyId: String) {
        apiKeyRepository.updateStatus(keyId, KeyStatus.INVALID)
    }

    suspend fun getKeyStatus(provider: ProviderType): KeyPoolStatus {
        val keys = apiKeyRepository.getActiveByProvider(provider)
        val total = keys.size
        val available = keys.count { key ->
            val rateLimitedUntil = rateLimitedKeys[key.id]
            rateLimitedUntil == null || System.currentTimeMillis() > rateLimitedUntil
        }
        val totalRemaining = keys.sumOf { it.dailyLimit - it.usedToday }
        return KeyPoolStatus(
            provider = provider,
            totalKeys = total,
            availableKeys = available,
            remainingDailyCalls = totalRemaining
        )
    }

    fun clearRateLimits() {
        rateLimitedKeys.clear()
    }
}

data class KeyPoolStatus(
    val provider: ProviderType,
    val totalKeys: Int,
    val availableKeys: Int,
    val remainingDailyCalls: Int
)
