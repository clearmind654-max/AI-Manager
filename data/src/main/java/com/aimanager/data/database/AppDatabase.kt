package com.aimanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aimanager.data.dao.*
import com.aimanager.data.entity.*

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        WorkerTaskEntity::class,
        ApiKeyEntity::class,
        SkillEntity::class,
        GemEntity::class,
        ModelScoreEntity::class,
        UsageRecordEntity::class,
        WorkflowRunEntity::class,
        CanvasProjectEntity::class,
        CompressedContextEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun workerTaskDao(): WorkerTaskDao
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun skillDao(): SkillDao
    abstract fun gemDao(): GemDao
    abstract fun modelScoreDao(): ModelScoreDao
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun workflowRunDao(): WorkflowRunDao
    abstract fun canvasProjectDao(): CanvasProjectDao
    abstract fun compressedContextDao(): CompressedContextDao
}
