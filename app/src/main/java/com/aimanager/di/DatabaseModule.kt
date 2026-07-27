package com.aimanager.di

import android.content.Context
import androidx.room.Room
import com.aimanager.data.dao.*
import com.aimanager.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ai_manager.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideConversationDao(db: AppDatabase): ConversationDao = db.conversationDao()
    @Provides fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()
    @Provides fun provideWorkerTaskDao(db: AppDatabase): WorkerTaskDao = db.workerTaskDao()
    @Provides fun provideApiKeyDao(db: AppDatabase): ApiKeyDao = db.apiKeyDao()
    @Provides fun provideSkillDao(db: AppDatabase): SkillDao = db.skillDao()
    @Provides fun provideGemDao(db: AppDatabase): GemDao = db.gemDao()
    @Provides fun provideModelScoreDao(db: AppDatabase): ModelScoreDao = db.modelScoreDao()
    @Provides fun provideUsageRecordDao(db: AppDatabase): UsageRecordDao = db.usageRecordDao()
    @Provides fun provideWorkflowRunDao(db: AppDatabase): WorkflowRunDao = db.workflowRunDao()
    @Provides fun provideCanvasProjectDao(db: AppDatabase): CanvasProjectDao = db.canvasProjectDao()
    @Provides fun provideCompressedContextDao(db: AppDatabase): CompressedContextDao = db.compressedContextDao()
}
