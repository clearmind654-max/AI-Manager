package com.aimanager.core.network.provider

import com.aimanager.core.model.ProviderType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRegistry @Inject constructor() {

    private val providers = mutableMapOf<ProviderType, LLMProvider>()

    init {
        // Register built-in providers
        register(GeminiProvider())
        register(ClaudeProvider())
        register(OpenAICompatibleProvider(
            providerType = ProviderType.DEEPSEEK,
            defaultBaseUrl = "https://api.deepseek.com",
            defaultModels = listOf("deepseek-chat", "deepseek-coder", "deepseek-reasoner"),
            maxContextMap = mapOf("deepseek-chat" to 64000, "deepseek-coder" to 64000, "deepseek-reasoner" to 64000)
        ))
        register(OpenAICompatibleProvider(
            providerType = ProviderType.GROK,
            defaultBaseUrl = "https://api.x.ai",
            defaultModels = listOf("grok-2", "grok-2-mini"),
            maxContextMap = mapOf("grok-2" to 131072, "grok-2-mini" to 131072)
        ))
        register(OpenAICompatibleProvider(
            providerType = ProviderType.QWEN,
            defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode",
            defaultModels = listOf("qwen-turbo", "qwen-plus", "qwen-max"),
            maxContextMap = mapOf("qwen-turbo" to 131072, "qwen-plus" to 131072, "qwen-max" to 32000)
        ))
        register(OpenAICompatibleProvider(
            providerType = ProviderType.OPENROUTER,
            defaultBaseUrl = "https://openrouter.ai/api",
            defaultModels = listOf(
                "deepseek/deepseek-chat-v3-0324:free",
                "google/gemini-2.0-flash-exp:free",
                "meta-llama/llama-4-maverick:free",
                "microsoft/phi-4-reasoning:free"
            ),
            maxContextMap = mapOf(
                "deepseek/deepseek-chat-v3-0324:free" to 64000,
                "google/gemini-2.0-flash-exp:free" to 1048576
            )
        ))
    }

    fun register(provider: LLMProvider) {
        providers[provider.providerType] = provider
    }

    fun get(type: ProviderType): LLMProvider? = providers[type]

    fun getAll(): Map<ProviderType, LLMProvider> = providers.toMap()

    fun getProviderForModel(model: String): ProviderType? {
        return when {
            model.startsWith("gemini") -> ProviderType.GEMINI
            model.startsWith("claude") -> ProviderType.CLAUDE
            model.startsWith("deepseek") -> ProviderType.DEEPSEEK
            model.startsWith("grok") -> ProviderType.GROK
            model.startsWith("qwen") -> ProviderType.QWEN
            model.contains("/") -> ProviderType.OPENROUTER // e.g., "deepseek/deepseek-chat:free"
            else -> null
        }
    }
}
