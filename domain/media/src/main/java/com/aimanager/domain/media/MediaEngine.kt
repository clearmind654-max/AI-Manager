package com.aimanager.domain.media

import com.aimanager.core.model.*
import com.aimanager.core.network.provider.ApiClient
import com.aimanager.core.network.provider.ProviderRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaEngine @Inject constructor(
    private val apiClient: ApiClient,
    private val providerRegistry: ProviderRegistry
) {
    suspend fun generateImage(
        prompt: String,
        preferredModel: String? = null
    ): Flow<MediaGenerationEvent> = flow {
        emit(MediaGenerationEvent.Started)
        val model = preferredModel ?: "flux"

        try {
            // For image generation, we use the text model to describe the image
            // then use a dedicated image API. For now, we send the prompt to the model.
            val request = NormalizedRequest(
                model = model,
                messages = listOf(NormalizedMessage(role = "user", content = "Generate an image: $prompt")),
                maxTokens = 1024,
                stream = false
            )

            val providerType = providerRegistry.getProviderForModel(model) ?: ProviderType.GEMINI
            val outputBuilder = StringBuilder()

            apiClient.complete(providerType, request).collect { chunk ->
                outputBuilder.append(chunk.content)
                emit(MediaGenerationEvent.Progress(chunk.content))
            }

            emit(MediaGenerationEvent.Completed(
                url = outputBuilder.toString().take(500),
                type = MediaType.IMAGE
            ))
        } catch (e: Exception) {
            emit(MediaGenerationEvent.Error(e.message ?: "Image generation failed"))
        }
    }

    suspend fun generateVideo(
        prompt: String,
        preferredModel: String? = null
    ): Flow<MediaGenerationEvent> = flow {
        emit(MediaGenerationEvent.Started)
        try {
            val model = preferredModel ?: "gemini-2.0-flash"
            val request = NormalizedRequest(
                model = model,
                messages = listOf(NormalizedMessage(role = "user", content = "Describe a video for: $prompt")),
                maxTokens = 1024,
                stream = false
            )
            val providerType = providerRegistry.getProviderForModel(model) ?: ProviderType.GEMINI
            val outputBuilder = StringBuilder()
            apiClient.complete(providerType, request).collect { chunk ->
                outputBuilder.append(chunk.content)
            }
            emit(MediaGenerationEvent.Completed(url = outputBuilder.toString().take(500), type = MediaType.VIDEO))
        } catch (e: Exception) {
            emit(MediaGenerationEvent.Error(e.message ?: "Video generation failed"))
        }
    }
}

sealed class MediaGenerationEvent {
    data object Started : MediaGenerationEvent()
    data class Progress(val partialOutput: String) : MediaGenerationEvent()
    data class Completed(val url: String, val type: MediaType) : MediaGenerationEvent()
    data class Error(val message: String) : MediaGenerationEvent()
}
