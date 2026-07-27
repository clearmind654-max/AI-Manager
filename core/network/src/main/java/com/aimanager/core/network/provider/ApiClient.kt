package com.aimanager.core.network.provider

import com.aimanager.core.model.*
import com.aimanager.core.network.provider.LLMProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    private val registry: ProviderRegistry,
    private val keyPoolManager: KeyPoolManager
) {
    suspend fun complete(
        providerType: ProviderType,
        request: NormalizedRequest,
        specificKey: String? = null
    ): Flow<NormalizedChunk> = flow {
        val provider = registry.get(providerType)
            ?: throw IllegalArgumentException("Provider $providerType not registered")

        val apiKey = specificKey ?: keyPoolManager.getKey(providerType)
            ?: throw IllegalStateException("No available API key for $providerType")

        provider.complete(request, apiKey).collect { emit(it) }
    }

    suspend fun validateKey(providerType: ProviderType, key: String): KeyValidationResult {
        val provider = registry.get(providerType)
            ?: return KeyValidationResult(false, error = "Provider not found")
        return provider.validateKey(key)
    }

    fun getProvider(providerType: ProviderType): LLMProvider? = registry.get(providerType)

    fun getProviderForModel(model: String): LLMProvider? {
        val type = registry.getProviderForModel(model) ?: return null
        return registry.get(type)
    }
}
