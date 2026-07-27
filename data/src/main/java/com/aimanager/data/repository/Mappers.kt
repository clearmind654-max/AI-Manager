package com.aimanager.data.repository

import com.aimanager.core.model.*
import com.aimanager.data.dao.*
import com.aimanager.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

// --- Mappers ---

fun ConversationEntity.toModel() = Conversation(
    id = id, title = title, mode = mode, createdAt = createdAt,
    updatedAt = updatedAt, tags = json.decodeFromString(tags),
    isPinned = isPinned, isArchived = isArchived, isSensitive = isSensitive
)

fun Conversation.toEntity() = ConversationEntity(
    id = id, title = title, mode = mode, createdAt = createdAt,
    updatedAt = updatedAt, tags = json.encodeToString(tags),
    isPinned = isPinned, isArchived = isArchived, isSensitive = isSensitive
)

fun MessageEntity.toModel() = Message(
    id = id, conversationId = conversationId, role = MessageRole.valueOf(role),
    content = content, workerId = workerId, modelUsed = modelUsed,
    tokensUsed = tokensUsed, latencyMs = latencyMs, parentMessageId = parentMessageId,
    timestamp = timestamp, isBookmarked = isBookmarked, errorInfo = errorInfo
)

fun Message.toEntity() = MessageEntity(
    id = id, conversationId = conversationId, role = role.name,
    content = content, workerId = workerId, modelUsed = modelUsed,
    tokensUsed = tokensUsed, latencyMs = latencyMs, parentMessageId = parentMessageId,
    timestamp = timestamp, isBookmarked = isBookmarked, errorInfo = errorInfo
)

fun WorkerTaskEntity.toModel() = WorkerTask(
    id = id, conversationId = conversationId, parentMessageId = parentMessageId,
    workerId = workerId, status = TaskStatus.valueOf(status), inputPrompt = inputPrompt,
    output = output, tokensIn = tokensIn, tokensOut = tokensOut,
    startedAt = startedAt, completedAt = completedAt, retryCount = retryCount,
    errorMessage = errorMessage, fallbackFrom = fallbackFrom
)

fun WorkerTask.toEntity() = WorkerTaskEntity(
    id = id, conversationId = conversationId, parentMessageId = parentMessageId,
    workerId = workerId, status = status.name, inputPrompt = inputPrompt,
    output = output, tokensIn = tokensIn, tokensOut = tokensOut,
    startedAt = startedAt, completedAt = completedAt, retryCount = retryCount,
    errorMessage = errorMessage, fallbackFrom = fallbackFrom
)

fun ApiKeyEntity.toModel(decryptedKey: String) = ApiKey(
    id = id, provider = ProviderType.valueOf(provider), key = decryptedKey,
    tier = tier, dailyLimit = dailyLimit, weeklyLimit = weeklyLimit,
    usedToday = usedToday, usedThisWeek = usedThisWeek,
    status = KeyStatus.valueOf(status), lastChecked = lastChecked,
    lastError = lastError, notes = notes, createdAt = createdAt
)

fun ApiKey.toEntity(encryptedKey: String) = ApiKeyEntity(
    id = id, provider = provider.name, keyEncrypted = encryptedKey,
    tier = tier, dailyLimit = dailyLimit, weeklyLimit = weeklyLimit,
    usedToday = usedToday, usedThisWeek = usedThisWeek,
    status = status.name, lastChecked = lastChecked,
    lastError = lastError, notes = notes, createdAt = createdAt
)

fun SkillEntity.toModel() = Skill(
    id = id, name = name, category = category, description = description,
    systemPrompt = systemPrompt, defaultModel = defaultModel,
    fallbackModels = json.decodeFromString(fallbackModels),
    parameters = json.decodeFromString(parameters),
    inputTemplate = inputTemplate, exampleInputs = json.decodeFromString(exampleInputs),
    createdAt = createdAt, updatedAt = updatedAt
)

fun Skill.toEntity() = SkillEntity(
    id = id, name = name, category = category, description = description,
    systemPrompt = systemPrompt, defaultModel = defaultModel,
    fallbackModels = json.encodeToString(fallbackModels),
    parameters = json.encodeToString(parameters),
    inputTemplate = inputTemplate, exampleInputs = json.encodeToString(exampleInputs),
    createdAt = createdAt, updatedAt = updatedAt
)

fun GemEntity.toModel() = Gem(
    id = id, name = name, description = description,
    steps = json.decodeFromString(steps),
    parameters = json.decodeFromString(parameters),
    version = version, createdAt = createdAt, updatedAt = updatedAt,
    lastRunAt = lastRunAt, runCount = runCount
)

fun Gem.toEntity() = GemEntity(
    id = id, name = name, description = description,
    steps = json.encodeToString(steps),
    parameters = json.encodeToString(parameters),
    version = version, createdAt = createdAt, updatedAt = updatedAt,
    lastRunAt = lastRunAt, runCount = runCount
)

fun ModelScoreEntity.toModel() = ModelScore(
    taskType = TaskType.valueOf(taskType), model = model,
    totalAttempts = totalAttempts, acceptedCount = acceptedCount,
    rerunCount = rerunCount, averageLatencyMs = averageLatencyMs,
    totalTokensUsed = totalTokensUsed, lastUpdated = lastUpdated
)

fun ModelScore.toEntity() = ModelScoreEntity(
    taskType = taskType.name, model = model,
    totalAttempts = totalAttempts, acceptedCount = acceptedCount,
    rerunCount = rerunCount, averageLatencyMs = averageLatencyMs,
    totalTokensUsed = totalTokensUsed, lastUpdated = lastUpdated
)

fun UsageRecordEntity.toModel() = UsageRecord(
    id = id, timestamp = timestamp, provider = provider, model = model,
    taskType = TaskType.valueOf(taskType), tokensIn = tokensIn, tokensOut = tokensOut,
    latencyMs = latencyMs, costEstimate = costEstimate,
    usedFallback = usedFallback, conversationId = conversationId
)

fun UsageRecord.toEntity() = UsageRecordEntity(
    id = id, timestamp = timestamp, provider = provider, model = model,
    taskType = taskType.name, tokensIn = tokensIn, tokensOut = tokensOut,
    latencyMs = latencyMs, costEstimate = costEstimate,
    usedFallback = usedFallback, conversationId = conversationId
)

fun WorkflowRunEntity.toModel() = WorkflowRun(
    id = id, gemId = gemId, conversationId = conversationId,
    status = TaskStatus.valueOf(status), currentStep = currentStep,
    stepResults = json.decodeFromString(stepResults),
    startedAt = startedAt, completedAt = completedAt, errorMessage = errorMessage
)

fun WorkflowRun.toEntity() = WorkflowRunEntity(
    id = id, gemId = gemId, conversationId = conversationId,
    status = status.name, currentStep = currentStep,
    stepResults = json.encodeToString(stepResults),
    startedAt = startedAt, completedAt = completedAt, errorMessage = errorMessage
)

fun CanvasProject.toEntity() = CanvasProjectEntity(
    id = id, name = name,
    nodes = json.encodeToString(nodes),
    edges = json.encodeToString(edges),
    createdAt = createdAt, updatedAt = updatedAt
)

fun CanvasProjectEntity.toModel() = CanvasProject(
    id = id, name = name,
    nodes = json.decodeFromString(nodes),
    edges = json.decodeFromString(edges),
    createdAt = createdAt, updatedAt = updatedAt
)
