package com.aimanager.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageRole { USER, MANAGER, WORKER, SYSTEM }

@Serializable
enum class TaskStatus {
    CREATED, DISPATCHING, WORKER_RUNNING, COLLECTING,
    SYNTHESIZING, DELIVERED, QUEUED, TIMEOUT,
    RETRYING, PARTIAL_FAIL, MANAGER_ERROR, DEGRADED_DELIVERY,
    CANCELLED
}

@Serializable
enum class FinishReason { STOP, LENGTH, ERROR, TIMEOUT, CANCELLED }

@Serializable
enum class ProviderType {
    GEMINI, DEEPSEEK, CLAUDE, GROK, QWEN, OPENROUTER,
    AGENTROUTER, CUSTOM
}

@Serializable
enum class MediaType { IMAGE, VIDEO }

@Serializable
enum class KeyStatus { ACTIVE, RATE_LIMITED, EXPIRED, INVALID, CHECKING }

@Serializable
enum class TaskType {
    CODE_GENERATION, CREATIVE_WRITING, DATA_ANALYSIS,
    RESEARCH, TRANSLATION, IMAGE_GENERATION, VIDEO_GENERATION,
    SUMMARIZATION, GENERAL
}

@Serializable
enum class SynthesisStrategy {
    CONSENSUS, PIPELINE, COMPETITIVE, COMPLEMENTARY
}

@Serializable
enum class NodeType {
    CHAT, MEDIA, NOTE, GROUP, WORKFLOW_TRIGGER, WEB_CONTENT
}

@Serializable
enum class EdgeType { DATA, REFERENCE, TRIGGER }

@Serializable
enum class StepType {
    AI_CALL, SKILL_CALL, IMAGE_GEN, VIDEO_GEN, USER_INPUT
}

@Serializable
enum class InputType {
    TEXT, IMAGE, AUDIO, URL, FILE, CAMERA
}

@Serializable
enum class FailureAction { SKIP, RETRY, ABORT, FALLBACK_MODEL }

@Serializable
enum class ThemeMode { LIGHT, DARK, HIGH_CONTRAST, SYSTEM }
