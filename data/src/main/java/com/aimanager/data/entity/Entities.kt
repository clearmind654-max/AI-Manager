package com.aimanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["isArchived", "updatedAt"])
    ]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mode: String,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: String, // JSON array
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isSensitive: Boolean
)

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId", "timestamp"]),
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val workerId: String?,
    val modelUsed: String?,
    val tokensUsed: Int,
    val latencyMs: Long,
    val parentMessageId: String?,
    val timestamp: Long,
    val isBookmarked: Boolean,
    val errorInfo: String?
)

@Entity(
    tableName = "worker_tasks",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["status"]),
        Index(value = ["workerId"])
    ]
)
data class WorkerTaskEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val parentMessageId: String,
    val workerId: String,
    val status: String,
    val inputPrompt: String,
    val output: String,
    val tokensIn: Int,
    val tokensOut: Int,
    val startedAt: Long,
    val completedAt: Long,
    val retryCount: Int,
    val errorMessage: String?,
    val fallbackFrom: String?
)

@Entity(
    tableName = "api_keys",
    indices = [
        Index(value = ["provider", "status"]),
        Index(value = ["status"])
    ]
)
data class ApiKeyEntity(
    @PrimaryKey val id: String,
    val provider: String,
    val keyEncrypted: String,
    val tier: String,
    val dailyLimit: Int,
    val weeklyLimit: Int,
    val usedToday: Int,
    val usedThisWeek: Int,
    val status: String,
    val lastChecked: Long,
    val lastError: String?,
    val notes: String,
    val createdAt: Long
)

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val description: String,
    val systemPrompt: String,
    val defaultModel: String?,
    val fallbackModels: String, // JSON array
    val parameters: String, // JSON array
    val inputTemplate: String,
    val exampleInputs: String, // JSON array
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "gems")
data class GemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val steps: String, // JSON array
    val parameters: String, // JSON array
    val version: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val lastRunAt: Long?,
    val runCount: Int
)

@Entity(tableName = "model_scores")
data class ModelScoreEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val taskType: String,
    val model: String,
    val totalAttempts: Int,
    val acceptedCount: Int,
    val rerunCount: Int,
    val averageLatencyMs: Long,
    val totalTokensUsed: Long,
    val lastUpdated: Long
)

@Entity(
    tableName = "usage_records",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["provider"]),
        Index(value = ["conversationId"])
    ]
)
data class UsageRecordEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val provider: String,
    val model: String,
    val taskType: String,
    val tokensIn: Int,
    val tokensOut: Int,
    val latencyMs: Long,
    val costEstimate: Double,
    val usedFallback: Boolean,
    val conversationId: String
)

@Entity(tableName = "workflow_runs")
data class WorkflowRunEntity(
    @PrimaryKey val id: String,
    val gemId: String,
    val conversationId: String,
    val status: String,
    val currentStep: Int,
    val stepResults: String, // JSON
    val startedAt: Long,
    val completedAt: Long,
    val errorMessage: String?
)

@Entity(tableName = "canvas_projects")
data class CanvasProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nodes: String, // JSON
    val edges: String, // JSON
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "compressed_contexts")
data class CompressedContextEntity(
    @PrimaryKey val conversationId: String,
    val contextJson: String,
    val updatedAt: Long
)
