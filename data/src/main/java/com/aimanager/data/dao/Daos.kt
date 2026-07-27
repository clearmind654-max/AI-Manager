package com.aimanager.data.dao

import androidx.room.*
import com.aimanager.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActive(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeById(id: String): Flow<ConversationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConversationEntity)

    @Update
    suspend fun update(entity: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE conversations SET isArchived = 1 WHERE updatedAt < :timestamp AND isArchived = 0")
    suspend fun archiveOlderThan(timestamp: Long)

    @Query("""
        SELECT * FROM conversations 
        WHERE title LIKE '%' || :query || '%' 
        OR tags LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 50): List<ConversationEntity>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getByConversation(convId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(convId: String, limit: Int = 50): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarked(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MessageEntity>)

    @Update
    suspend fun update(entity: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteByConversation(convId: String)

    @Query("""
        SELECT * FROM messages 
        WHERE content LIKE '%' || :query || '%'
        ORDER BY timestamp DESC LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 100): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :convId")
    suspend fun countByConversation(convId: String): Int
}

@Dao
interface WorkerTaskDao {
    @Query("SELECT * FROM worker_tasks WHERE conversationId = :convId ORDER BY startedAt ASC")
    fun getByConversation(convId: String): Flow<List<WorkerTaskEntity>>

    @Query("SELECT * FROM worker_tasks WHERE id = :id")
    suspend fun getById(id: String): WorkerTaskEntity?

    @Query("SELECT * FROM worker_tasks WHERE status IN ('CREATED', 'DISPATCHING', 'WORKER_RUNNING')")
    suspend fun getActive(): List<WorkerTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkerTaskEntity)

    @Update
    suspend fun update(entity: WorkerTaskEntity)

    @Query("UPDATE worker_tasks SET status = 'CANCELLED' WHERE status IN ('CREATED', 'DISPATCHING', 'WORKER_RUNNING')")
    suspend fun cancelAllActive()
}

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys ORDER BY provider, usedToday ASC")
    fun getAll(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys WHERE provider = :provider AND status = 'ACTIVE' ORDER BY usedToday ASC")
    suspend fun getActiveByProvider(provider: String): List<ApiKeyEntity>

    @Query("SELECT * FROM api_keys WHERE id = :id")
    suspend fun getById(id: String): ApiKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ApiKeyEntity)

    @Update
    suspend fun update(entity: ApiKeyEntity)

    @Delete
    suspend fun delete(entity: ApiKeyEntity)

    @Query("DELETE FROM api_keys WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE api_keys SET usedToday = 0")
    suspend fun resetDailyUsage()

    @Query("UPDATE api_keys SET usedThisWeek = 0")
    suspend fun resetWeeklyUsage()

    @Query("UPDATE api_keys SET usedToday = usedToday + 1 WHERE id = :id")
    suspend fun incrementDailyUsage(id: String)

    @Query("UPDATE api_keys SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT * FROM api_keys WHERE provider = :provider AND status = 'ACTIVE' AND usedToday < dailyLimit ORDER BY usedToday ASC LIMIT 1")
    suspend fun getBestKey(provider: String): ApiKeyEntity?

    @Query("SELECT COUNT(*) FROM api_keys WHERE status = 'ACTIVE'")
    suspend fun countActive(): Int

    @Query("SELECT provider, COUNT(*) as count FROM api_keys WHERE status = 'ACTIVE' GROUP BY provider")
    suspend fun countByProvider(): List<ProviderCount>
}

data class ProviderCount(val provider: String, val count: Int)

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY category, name")
    fun getAll(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE id = :id")
    suspend fun getById(id: String): SkillEntity?

    @Query("SELECT * FROM skills WHERE category = :category")
    suspend fun getByCategory(category: String): List<SkillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SkillEntity)

    @Update
    suspend fun update(entity: SkillEntity)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface GemDao {
    @Query("SELECT * FROM gems ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<GemEntity>>

    @Query("SELECT * FROM gems WHERE id = :id")
    suspend fun getById(id: String): GemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GemEntity)

    @Update
    suspend fun update(entity: GemEntity)

    @Query("DELETE FROM gems WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE gems SET lastRunAt = :time, runCount = runCount + 1 WHERE id = :id")
    suspend fun recordRun(id: String, time: Long)
}

@Dao
interface ModelScoreDao {
    @Query("SELECT * FROM model_scores WHERE taskType = :taskType ORDER BY (acceptedCount * 1.0 / MAX(totalAttempts, 1)) DESC")
    suspend fun getByTaskType(taskType: String): List<ModelScoreEntity>

    @Query("SELECT * FROM model_scores WHERE taskType = :taskType AND model = :model LIMIT 1")
    suspend fun getScore(taskType: String, model: String): ModelScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ModelScoreEntity)

    @Update
    suspend fun update(entity: ModelScoreEntity)

    @Query("SELECT * FROM model_scores ORDER BY lastUpdated DESC LIMIT 50")
    fun getAll(): Flow<List<ModelScoreEntity>>
}

@Dao
interface UsageRecordDao {
    @Query("SELECT * FROM usage_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<UsageRecordEntity>>

    @Query("SELECT * FROM usage_records WHERE timestamp >= :since")
    suspend fun getSince(since: Long): List<UsageRecordEntity>

    @Query("SELECT * FROM usage_records WHERE provider = :provider AND timestamp >= :since")
    suspend fun getByProviderSince(provider: String, since: Long): List<UsageRecordEntity>

    @Query("SELECT SUM(tokensIn + tokensOut) FROM usage_records WHERE timestamp >= :since")
    suspend fun totalTokensSince(since: Long): Long?

    @Query("SELECT SUM(costEstimate) FROM usage_records WHERE timestamp >= :since")
    suspend fun totalCostSince(since: Long): Double?

    @Query("SELECT provider, SUM(tokensIn + tokensOut) as total FROM usage_records WHERE timestamp >= :since GROUP BY provider")
    suspend fun usageByProviderSince(since: Long): List<ProviderUsage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UsageRecordEntity)
}

data class ProviderUsage(val provider: String, val total: Long)

@Dao
interface WorkflowRunDao {
    @Query("SELECT * FROM workflow_runs ORDER BY startedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<WorkflowRunEntity>>

    @Query("SELECT * FROM workflow_runs WHERE id = :id")
    suspend fun getById(id: String): WorkflowRunEntity?

    @Query("SELECT * FROM workflow_runs WHERE status IN ('CREATED', 'DISPATCHING', 'WORKER_RUNNING')")
    suspend fun getActive(): List<WorkflowRunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkflowRunEntity)

    @Update
    suspend fun update(entity: WorkflowRunEntity)
}

@Dao
interface CanvasProjectDao {
    @Query("SELECT * FROM canvas_projects ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<CanvasProjectEntity>>

    @Query("SELECT * FROM canvas_projects WHERE id = :id")
    suspend fun getById(id: String): CanvasProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CanvasProjectEntity)

    @Delete
    suspend fun delete(entity: CanvasProjectEntity)
}

@Dao
interface CompressedContextDao {
    @Query("SELECT * FROM compressed_contexts WHERE conversationId = :convId")
    suspend fun getByConversation(convId: String): CompressedContextEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CompressedContextEntity)

    @Query("DELETE FROM compressed_contexts WHERE conversationId = :convId")
    suspend fun deleteByConversation(convId: String)
}
