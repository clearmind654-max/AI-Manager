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

class GeminiProvider : LLMProvider {

    override val providerType = ProviderType.GEMINI
    override val supportsStreaming = true
    override val supportsVision = true
    override val supportsFunctionCalling = true

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

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
        val body = buildGeminiBody(request)
        val url = "$baseUrl/models/${request.model}:streamGenerateContent?alt=sse&key=$apiKey"

        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val jsonObj = json.parseToJsonElement(data).jsonObject
                    val candidates = jsonObj["candidates"]?.jsonArray
                    if (candidates != null && candidates.isNotEmpty()) {
                        val content = candidates[0].jsonObject["content"]?.jsonObject
                        val parts = content?.get("parts")?.jsonArray
                        val text = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: ""
                        val finishReason = candidates[0].jsonObject["finishReason"]?.jsonPrimitive?.contentOrNull
                        val finish = when (finishReason) {
                            "STOP" -> FinishReason.STOP
                            "MAX_TOKENS" -> FinishReason.LENGTH
                            else -> null
                        }
                        val usage = jsonObj["usageMetadata"]?.jsonObject
                        val tokens = usage?.get("totalTokenCount")?.jsonPrimitive?.intOrNull ?: 0
                        if (text.isNotEmpty() || finish != null) {
                            trySend(NormalizedChunk(text, finish, tokens))
                        }
                    }
                } catch (_: Exception) {}
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val errorMsg = when {
                    response?.code == 400 -> "Invalid request to Gemini."
                    response?.code == 401 || response?.code == 403 -> "Gemini API key invalid or expired."
                    response?.code == 429 -> "Gemini rate limited. Please wait."
                    response?.code == 500 -> "Gemini server error."
                    t is IOException -> "Network error: ${t.message}"
                    else -> "Gemini request failed: ${t?.message ?: "HTTP ${response?.code}"}"
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
            val url = "${baseUrl ?: this.baseUrl}/models?key=$apiKey"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val models = try {
                    json.parseToJsonElement(body).jsonObject["models"]?.jsonArray?.map {
                        it.jsonObject["name"]?.jsonPrimitive?.content?.removePrefix("models/") ?: ""
                    }?.filter { it.isNotEmpty() } ?: emptyList()
                } catch (_: Exception) { emptyList() }
                KeyValidationResult(true, models = models)
            } else {
                KeyValidationResult(false, error = "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            KeyValidationResult(false, error = e.message ?: "Validation failed")
        }
    }

    override fun getDefaultModels() = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro")

    override fun getMaxContextTokens(model: String) = when {
        model.contains("1.5-pro") -> 2097152
        model.contains("1.5-flash") -> 1048576
        model.contains("2.0") -> 1048576
        else -> 32000
    }

    private fun buildGeminiBody(request: NormalizedRequest): String {
        val contents = buildJsonArray {
            request.messages.forEach { msg ->
                addJsonObject {
                    put("role", if (msg.role == "user") "user" else "model")
                    putJsonArray("parts") {
                        addJsonObject { put("text", msg.content) }
                        msg.images?.forEach { img ->
                            // Gemini expects inline_data with base64
                            if (img.startsWith("data:")) {
                                val base64 = img.substringAfter("base64,")
                                val mimeType = img.substringAfter("data:").substringBefore(";")
                                addJsonObject {
                                    putJsonObject("inline_data") {
                                        put("mime_type", mimeType)
                                        put("data", base64)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val body = buildJsonObject {
            put("contents", contents)
            request.systemPrompt?.let {
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        addJsonObject { put("text", it) }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("maxOutputTokens", request.maxTokens)
                put("temperature", request.temperature)
            }
        }
        return json.encodeToString(JsonObject.serializer(), body)
    }
}
