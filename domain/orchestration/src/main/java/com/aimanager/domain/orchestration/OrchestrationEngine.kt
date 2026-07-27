package com.aimanager.domain.orchestration

import com.aimanager.core.model.*
import com.aimanager.core.network.provider.ApiClient
import com.aimanager.core.network.provider.KeyPoolManager
import com.aimanager.core.network.provider.ProviderRegistry
import com.aimanager.data.repository.*
import com.aimanager.domain.manager.ContextEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrchestrationEngine @Inject constructor(
    private val apiClient: ApiClient,
    private val providerRegistry: ProviderRegistry,
    private val keyPoolManager: KeyPoolManager,
    private val contextEngine: ContextEngine,
    private val workerTaskRepository: WorkerTaskRepository,
    private val usageRepository: UsageRepository,
    private val modelScoreRepository: ModelScoreRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun executePlan(
        plan: TaskPlan,
        conversationId: String,
        maxParallel: Int = 3
    ): Flow<OrchestrationEvent> = channelFlow {
        send(OrchestrationEvent.PlanStarted(plan.tasks.size))

        // Group tasks by dependency - tasks with no deps run first in parallel
        val independentTasks = plan.tasks.filter { it.dependsOn.isEmpty() }
        val dependentTasks = plan.tasks.filter { it.dependsOn.isNotEmpty() }

        val results = mutableMapOf<String, String>()

        // Execute independent tasks in parallel
        val deferredResults = independentTasks.take(maxParallel).map { task ->
            async {
                executeSingleTask(task, conversationId, results)
            }
        }

        // Collect results as they complete
        for (deferred in deferredResults) {
            val result = deferred.await()
            results[result.taskId] = result.output
            send(OrchestrationEvent.TaskCompleted(result))
        }

        // Execute dependent tasks sequentially
        for (task in dependentTasks) {
            val result = executeSingleTask(task, conversationId, results)
            results[result.taskId] = result.output
            send(OrchestrationEvent.TaskCompleted(result))
        }

        send(OrchestrationEvent.PlanCompleted(results.toMap()))
    }

    private suspend fun executeSingleTask(
        task: PlannedTask,
        conversationId: String,
        previousResults: Map<String, String>
    ): TaskExecutionResult {
        val workerTask = WorkerTask(
            id = task.id,
            conversationId = conversationId,
            parentMessageId = "",
            workerId = task.model,
            status = TaskStatus.WORKER_RUNNING,
            inputPrompt = task.prompt,
            startedAt = System.currentTimeMillis()
        )
        workerTaskRepository.insert(workerTask)

        // Inject dependency outputs into prompt
        var finalPrompt = task.prompt
        for (depId in task.dependsOn) {
            previousResults[depId]?.let { depOutput ->
                finalPrompt = finalPrompt.replace("{{$depId}}", depOutput)
            }
        }

        // Get context for this worker
        val context = contextEngine.buildContextForWorker(
            conversationId, TaskType.GENERAL, task.model, 4096
        )

        val providerType = providerRegistry.getProviderForModel(task.model)
            ?: ProviderType.GEMINI

        val messages = listOf(NormalizedMessage(role = "user", content = finalPrompt))
        val request = NormalizedRequest(
            model = task.model,
            messages = messages,
            systemPrompt = context.ifEmpty { null },
            maxTokens = 4096,
            stream = false
        )

        var attempt = 0
        var lastError: String? = null
        val maxRetries = 2

        while (attempt <= maxRetries) {
            try {
                val outputBuilder = StringBuilder()
                val startTime = System.currentTimeMillis()

                withTimeout(task.timeoutMs) {
                    apiClient.complete(providerType, request).collect { chunk ->
                        outputBuilder.append(chunk.content)
                    }
                }

                val latency = System.currentTimeMillis() - startTime
                val output = outputBuilder.toString()

                workerTaskRepository.update(workerTask.copy(
                    status = TaskStatus.DELIVERED,
                    output = output,
                    completedAt = System.currentTimeMillis()
                ))

                // Record usage
                usageRepository.insert(UsageRecord(
                    id = "usage_${task.id}_$attempt",
                    timestamp = System.currentTimeMillis(),
                    provider = providerType.name,
                    model = task.model,
                    taskType = TaskType.GENERAL,
                    tokensIn = request.messages.sumOf { it.content.length / 4 },
                    tokensOut = output.length / 4,
                    latencyMs = latency,
                    conversationId = conversationId
                ))

                return TaskExecutionResult(
                    taskId = task.id,
                    model = task.model,
                    output = output,
                    status = TaskStatus.DELIVERED,
                    latencyMs = latency
                )
            } catch (e: TimeoutCancellationException) {
                lastError = "Timeout after ${task.timeoutMs}ms"
                attempt++
                if (attempt <= maxRetries) {
                    // Try fallback model
                    val fallback = task.fallbackModel
                    if (fallback != null) {
                        return executeSingleTask(
                            task.copy(model = fallback, fallbackModel = null),
                            conversationId, previousResults
                        )
                    }
                    delay(1000L * attempt) // Exponential backoff
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                attempt++
                if (attempt <= maxRetries) delay(1000L * attempt)
            }
        }

        workerTaskRepository.update(workerTask.copy(
            status = TaskStatus.MANAGER_ERROR,
            errorMessage = lastError,
            completedAt = System.currentTimeMillis()
        ))

        return TaskExecutionResult(
            taskId = task.id,
            model = task.model,
            output = "",
            status = TaskStatus.MANAGER_ERROR,
            latencyMs = 0,
            error = lastError
        )
    }

    suspend fun cancelAllTasks() {
        workerTaskRepository.cancelAllActive()
    }
}

data class TaskExecutionResult(
    val taskId: String,
    val model: String,
    val output: String,
    val status: TaskStatus,
    val latencyMs: Long,
    val error: String? = null
)

sealed class OrchestrationEvent {
    data class PlanStarted(val taskCount: Int) : OrchestrationEvent()
    data class TaskCompleted(val result: TaskExecutionResult) : OrchestrationEvent()
    data class PlanCompleted(val results: Map<String, String>) : OrchestrationEvent()
    data class Error(val message: String) : OrchestrationEvent()
}
