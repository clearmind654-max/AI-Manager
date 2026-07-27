package com.aimanager.core.network.provider

import com.aimanager.core.model.*
import kotlinx.coroutines.flow.Flow

interface LLMProvider {
    val providerType: ProviderType
    val supportsStreaming: Boolean
    val supportsVision: Boolean
    val supportsFunctionCalling: Boolean

    suspend fun complete(request: NormalizedRequest, apiKey: String): Flow<NormalizedChunk>
    suspend fun validateKey(apiKey: String, baseUrl: String? = null): KeyValidationResult
    fun getDefaultModels(): List<String>
    fun getMaxContextTokens(model: String): Int
}
