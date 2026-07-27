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

class OpenAICompatibleProvider(
    override val providerType: ProviderType,
    private val defaultBaseUrl: String,
    override val supportsVision: Boolean = false,
    override val supportsFunctionCalling: Boolean = false,
    private val defaultModels: List<String> = emptyList(),
    private val maxContextMap: Map<String, Int> = emptyMap()
) : LLMProvider {

    override val supportsStreaming: Boolean = true

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 2, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .build()
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun complete(request: NormalizedRequest, apiKey: String): Flow<NormalizedChunk> = callbackFlow {
        val body = buildRequestBody(request)
        val httpRequest = buildRequest(apiKey, request.model, body, "/v1/chat/completions")

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    close()
                    return
                }
                try {
                    val jsonObj = json.parseToJsonElement(data).jsonObject
                    val choices = jsonObj["choices"]?.jsonArray
                    if (choices != null && choices.isNotEmpty()) {
                        val delta = choices[0].jsonObject["delta"]?.jsonObject
                        val content = delta?.get("content")?.jsonPrimitive?.contentOrNull ?: ""
                        val finishStr = choices[0].jsonObject["finish_reason"]?.jsonPrimitive?.contentOrNull
                        val finish = finishStr?.let {
                            when (it) {
                                "stop" -> FinishReason.STOP
                                "length" -> FinishReason.LENGTH
                                else -> null
                            }
                        }
                        val usage = jsonObj["usage"]?.jsonObject
                        val tokens = usage?.get("total_tokens")?.jsonPrimitive?.intOrNull ?: 0
                        if (content.isNotEmpty() || finish != null) {
                            trySend(NormalizedChunk(content, finish, tokens))
                        }
                    }
                } catch (_: Exception) {
                    // Skip malformed chunks
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val errorMsg = when {
                    response?.code == 401 -> "Authentication failed. Check your API key."
                    response?.code == 429 -> "Rate limited. Please wait and try again."
                    response?.code == 500 -> "Server error. Please try again later."
                    response?.code == 503 -> "Service unavailable. Please try again later."
                    t is java.net.SocketTimeoutException -> "Request timed out."
                    t is java.net.UnknownHostException -> "No internet connection."
                    t is IOException -> "Network error: ${t.message ?: "Unknown"}"
                    else -> "Request failed: ${t?.message ?: "HTTP ${response?.code}"}"
                }
                trySend(NormalizedChunk("", FinishReason.ERROR, 0))
                close(t ?: Exception(errorMsg))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = EventSources.createFactory(client).newEventSource(httpRequest, listener)

        awaitClose {
            eventSource.cancel()
        }
    }

    override suspend fun validateKey(apiKey: String, baseUrl: String?): KeyValidationResult {
        return try {
            val url = "${baseUrl ?: defaultBaseUrl}/v1/models"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val models = try {
                    json.parseToJsonElement(body).jsonObject["data"]?.jsonArray?.map {
                        it.jsonObject["id"]?.jsonPrimitive?.content ?: ""
                    }?.filter { it.isNotEmpty() } ?: emptyList()
                } catch (_: Exception) { emptyList() }
                KeyValidationResult(true, models = models)
            } else {
                KeyValidationResult(false, error = "HTTP ${response.code}: Invalid key")
            }
        } catch (e: Exception) {
            KeyValidationResult(false, error = e.message ?: "Validation failed")
        }
    }

    override fun getDefaultModels(): List<String> = defaultModels

    override fun getMaxContextTokens(model: String): Int =
        maxContextMap[model] ?: 32000

    private fun buildRequestBody(request: NormalizedRequest): String {
        val messagesArray = buildJsonArray {
            request.systemPrompt?.let { sys ->
                addJsonObject {
                    put("role", "system")
                    put("content", sys)
                }
            }
            request.messages.forEach { msg ->
                addJsonObject {
                    put("role", if (msg.role == "user") "user" else "assistant")
                    if (msg.images != null && msg.images.isNotEmpty() && supportsVision) {
                        putJsonArray("content") {
                            addJsonObject {
                                put("type", "text")
                                put("text", msg.content)
                            }
                            msg.images.forEach { imgUrl ->
                                addJsonObject {
                                    put("type", "image_url")
                                    putJsonObject("image_url") {
                                        put("url", imgUrl)
                                    }
                                }
                            }
                        }
                    } else {
                        put("content", msg.content)
                    }
                }
            }
        }

        val body = buildJsonObject {
            put("model", request.model)
            put("messages", messagesArray)
            put("max_tokens", request.maxTokens)
            put("temperature", request.temperature)
            put("stream", request.stream)
        }
        return json.encodeToString(JsonObject.serializer(), body)
    }

    private fun buildRequest(apiKey: String, model: String, body: String, path: String): Request {
        return Request.Builder()
            .url("$defaultBaseUrl$path")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
    }
}
