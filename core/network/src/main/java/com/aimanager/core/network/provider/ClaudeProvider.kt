package com.aimanager.core.network.provider

import com.aimanager.core.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit

class ClaudeProvider : LLMProvider {

    override val providerType = ProviderType.CLAUDE
    override val supportsStreaming = true
    override val supportsVision = true
    override val supportsFunctionCalling = false

    private val baseUrl = "https://api.anthropic.com/v1"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 2, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .build()
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun complete(request: NormalizedRequest, apiKey: String): Flow<NormalizedChunk> = callbackFlow {
        val body = buildClaudeBody(request)
        val httpRequest = Request.Builder()
            .url("$baseUrl/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val jsonObj = json.parseToJsonElement(data).jsonObject
                    val eventType = jsonObj["type"]?.jsonPrimitive?.contentOrNull
                    when (eventType) {
                        "content_block_delta" -> {
                            val delta = jsonObj["delta"]?.jsonObject
                            val text = delta?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
                            if (text.isNotEmpty()) {
                                trySend(NormalizedChunk(text, null, 0))
                            }
                        }
                        "message_stop" -> {
                            trySend(NormalizedChunk("", FinishReason.STOP, 0))
                            close()
                        }
                        "message_delta" -> {
                            val usage = jsonObj["usage"]?.jsonObject
                            val tokens = usage?.get("output_tokens")?.jsonPrimitive?.intOrNull ?: 0
                            trySend(NormalizedChunk("", null, tokens))
                        }
                    }
                } catch (_: Exception) {}
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val errorMsg = when {
                    response?.code == 401 -> "Claude API key invalid."
                    response?.code == 429 -> "Claude rate limited."
                    response?.code == 529 -> "Claude overloaded."
                    t is IOException -> "Network error: ${t.message}"
                    else -> "Claude request failed: ${t?.message ?: "HTTP ${response?.code}"}"
                }
                trySend(NormalizedChunk("", FinishReason.ERROR, 0))
                close(t ?: Exception(errorMsg))
            }

            override fun onClosed(eventSource: EventSource) { close() }
        }

        val eventSource = EventSources.createFactory(client).newEventSource(httpRequest, listener)
        awaitClose { eventSource.cancel() }
    }

    override suspend fun validateKey(apiKey: String, baseUrl: String?): KeyValidationResult {
        return try {
            val url = "${baseUrl ?: this.baseUrl}/messages"
            val body = """{"model":"claude-3-5-haiku-20241022","max_tokens":1,"messages":[{"role":"user","content":"hi"}]}"""
            val request = Request.Builder()
                .url(url)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful || response.code == 400) {
                KeyValidationResult(true, models = getDefaultModels())
            } else {
                KeyValidationResult(false, error = "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            KeyValidationResult(false, error = e.message ?: "Validation failed")
        }
    }

    override fun getDefaultModels() = listOf(
        "claude-sonnet-4-20250514",
        "claude-3-5-haiku-20241022",
        "claude-3-opus-20240229"
    )

    override fun getMaxContextTokens(model: String) = 200000

    private fun buildClaudeBody(request: NormalizedRequest): String {
        val messagesArray = buildJsonArray {
            request.messages.forEach { msg ->
                addJsonObject {
                    put("role", if (msg.role == "user") "user" else "assistant")
                    put("content", msg.content)
                }
            }
        }

        val body = buildJsonObject {
            put("model", request.model)
            put("max_tokens", request.maxTokens)
            put("stream", request.stream)
            request.systemPrompt?.let { put("system", it) }
            put("messages", messagesArray)
        }
        return json.encodeToString(JsonObject.serializer(), body)
    }
}
