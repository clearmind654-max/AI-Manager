package com.aimanager.data.repository

import com.aimanager.core.model.*
import com.aimanager.data.dao.*
import com.aimanager.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val dao: ConversationDao,
    private val messageDao: MessageDao
) {
    fun getAllActive(): Flow<List<Conversation>> =
        dao.getAllActive().map { list -> list.map { it.toModel() } }

    suspend fun getById(id: String): Conversation? = dao.getById(id)?.toModel()

    fun observeById(id: String): Flow<Conversation?> =
        dao.observeById(id).map { it?.toModel() }

    suspend fun create(conversation: Conversation) = dao.insert(conversation.toEntity())

    suspend fun update(conversation: Conversation) = dao.update(conversation.toEntity())

    suspend fun delete(id: String) {
        messageDao.deleteByConversation(id)
        dao.deleteById(id)
    }

    suspend fun archiveOlderThan(timestamp: Long) = dao.archiveOlderThan(timestamp)

    suspend fun search(query: String) = dao.search(query).map { it.toModel() }
}

@Singleton
class MessageRepository @Inject constructor(
    private val dao: MessageDao
) {
    fun getByConversation(convId: String): Flow<List<Message>> =
        dao.getByConversation(convId).map { list -> list.map { it.toModel() } }

    suspend fun getRecent(convId: String, limit: Int = 50) =
        dao.getRecent(convId, limit).map { it.toModel() }

    suspend fun getById(id: String): Message? = dao.getById(id)?.toModel()

    fun getBookmarked(): Flow<List<Message>> =
        dao.getBookmarked().map { list -> list.map { it.toModel() } }

    suspend fun insert(message: Message) = dao.insert(message.toEntity())

    suspend fun insertAll(messages: List<Message>) = dao.insertAll(messages.map { it.toEntity() })

    suspend fun update(message: Message) = dao.update(message.toEntity())

    suspend fun search(query: String) = dao.search(query).map { it.toModel() }

    suspend fun count(convId: String) = dao.countByConversation(convId)
}

@Singleton
class WorkerTaskRepository @Inject constructor(
    private val dao: WorkerTaskDao
) {
    fun getByConversation(convId: String): Flow<List<WorkerTask>> =
        dao.getByConversation(convId).map { list -> list.map { it.toModel() } }

    suspend fun getById(id: String): WorkerTask? = dao.getById(id)?.toModel()

    suspend fun getActive(): List<WorkerTask> = dao.getActive().map { it.toModel() }

    suspend fun insert(task: WorkerTask) = dao.insert(task.toEntity())

    suspend fun update(task: WorkerTask) = dao.update(task.toEntity())

    suspend fun cancelAllActive() = dao.cancelAllActive()
}

@Singleton
class ApiKeyRepository @Inject constructor(
    private val dao: ApiKeyDao,
    private val encryptionHelper: EncryptionHelper
) {
    fun getAll(): Flow<List<ApiKey>> =
        dao.getAll().map { list ->
            list.map { entity ->
                entity.toModel(encryptionHelper.decrypt(entity.keyEncrypted))
            }
        }

    suspend fun getBestKey(provider: ProviderType): ApiKey? {
        val entity = dao.getBestKey(provider.name) ?: return null
        return entity.toModel(encryptionHelper.decrypt(entity.keyEncrypted))
    }

    suspend fun getActiveByProvider(provider: ProviderType): List<ApiKey> {
        return dao.getActiveByProvider(provider.name).map { entity ->
            entity.toModel(encryptionHelper.decrypt(entity.keyEncrypted))
        }
    }

    suspend fun insert(apiKey: ApiKey) {
        val encrypted = encryptionHelper.encrypt(apiKey.key)
        dao.insert(apiKey.toEntity(encrypted))
    }

    suspend fun update(apiKey: ApiKey) {
        val existing = dao.getById(apiKey.id)
        val encrypted = existing?.keyEncrypted ?: encryptionHelper.encrypt(apiKey.key)
        dao.update(apiKey.toEntity(encrypted))
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    suspend fun resetDailyUsage() = dao.resetDailyUsage()

    suspend fun resetWeeklyUsage() = dao.resetWeeklyUsage()

    suspend fun incrementUsage(id: String) = dao.incrementDailyUsage(id)

    suspend fun updateStatus(id: String, status: KeyStatus) =
        dao.updateStatus(id, status.name)

    suspend fun countActive() = dao.countActive()
}

@Singleton
class SkillRepository @Inject constructor(private val dao: SkillDao) {
    fun getAll(): Flow<List<Skill>> = dao.getAll().map { list -> list.map { it.toModel() } }
    suspend fun getById(id: String): Skill? = dao.getById(id)?.toModel()
    suspend fun insert(skill: Skill) = dao.insert(skill.toEntity())
    suspend fun update(skill: Skill) = dao.update(skill.toEntity())
    suspend fun delete(id: String) = dao.deleteById(id)
}

@Singleton
class GemRepository @Inject constructor(private val dao: GemDao) {
    fun getAll(): Flow<List<Gem>> = dao.getAll().map { list -> list.map { it.toModel() } }
    suspend fun getById(id: String): Gem? = dao.getById(id)?.toModel()
    suspend fun insert(gem: Gem) = dao.insert(gem.toEntity())
    suspend fun update(gem: Gem) = dao.update(gem.toEntity())
    suspend fun delete(id: String) = dao.deleteById(id)
    suspend fun recordRun(id: String) = dao.recordRun(id, System.currentTimeMillis())
}

@Singleton
class ModelScoreRepository @Inject constructor(private val dao: ModelScoreDao) {
    fun getAll(): Flow<List<ModelScore>> = dao.getAll().map { list -> list.map { it.toModel() } }

    suspend fun getBestModel(taskType: TaskType): String? {
        val scores = dao.getByTaskType(taskType.name)
        return scores.maxByOrNull {
            if (it.totalAttempts > 0) it.acceptedCount.toFloat() / it.totalAttempts else 0f
        }?.model
    }

    suspend fun recordOutcome(taskType: TaskType, model: String, accepted: Boolean, latencyMs: Long, tokens: Int) {
        val existing = dao.getScore(taskType.name, model)
        if (existing != null) {
            val newTotal = existing.totalAttempts + 1
            val newAccepted = existing.acceptedCount + if (accepted) 1 else 0
            val newLatency = (existing.averageLatencyMs * existing.totalAttempts + latencyMs) / newTotal
            dao.update(existing.copy(
                totalAttempts = newTotal,
                acceptedCount = newAccepted,
                averageLatencyMs = newLatency,
                totalTokensUsed = existing.totalTokensUsed + tokens,
                lastUpdated = System.currentTimeMillis()
            ))
        } else {
            dao.insert(ModelScoreEntity(
                taskType = taskType.name, model = model,
                totalAttempts = 1, acceptedCount = if (accepted) 1 else 0,
                rerunCount = 0, averageLatencyMs = latencyMs,
                totalTokensUsed = tokens.toLong(), lastUpdated = System.currentTimeMillis()
            ))
        }
    }
}

@Singleton
class UsageRepository @Inject constructor(private val dao: UsageRecordDao) {
    fun getRecent(limit: Int = 100) = dao.getRecent(limit).map { list -> list.map { it.toModel() } }
    suspend fun getSince(since: Long) = dao.getSince(since).map { it.toModel() }
    suspend fun totalTokensSince(since: Long) = dao.totalTokensSince(since) ?: 0L
    suspend fun totalCostSince(since: Long) = dao.totalCostSince(since) ?: 0.0
    suspend fun usageByProviderSince(since: Long) = dao.usageByProviderSince(since)
    suspend fun insert(record: UsageRecord) = dao.insert(record.toEntity())
}

@Singleton
class WorkflowRunRepository @Inject constructor(private val dao: WorkflowRunDao) {
    fun getRecent(limit: Int = 20) = dao.getRecent(limit).map { list -> list.map { it.toModel() } }
    suspend fun getById(id: String): WorkflowRun? = dao.getById(id)?.toModel()
    suspend fun getActive(): List<WorkflowRun> = dao.getActive().map { it.toModel() }
    suspend fun insert(run: WorkflowRun) = dao.insert(run.toEntity())
    suspend fun update(run: WorkflowRun) = dao.update(run.toEntity())
}

@Singleton
class CanvasRepository @Inject constructor(private val dao: CanvasProjectDao) {
    fun getAll() = dao.getAll()
    suspend fun getById(id: String) = dao.getById(id)
    suspend fun insert(project: CanvasProject) = dao.insert(project.toEntity())
    suspend fun delete(project: CanvasProject) = dao.delete(project.toEntity())
}

@Singleton
class ContextRepository @Inject constructor(private val dao: CompressedContextDao) {
    suspend fun get(convId: String): CompressedContext? {
        val entity = dao.getByConversation(convId) ?: return null
        return kotlinx.serialization.json.Json.decodeFromString(entity.contextJson)
    }

    suspend fun save(convId: String, context: CompressedContext) {
        val json = kotlinx.serialization.json.Json.encodeToString(CompressedContext.serializer(), context)
        dao.insert(CompressedContextEntity(convId, json, System.currentTimeMillis()))
    }
}
