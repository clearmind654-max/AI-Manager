package com.aimanager.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aimanager.core.common.IdGenerator
import com.aimanager.core.model.*
import com.aimanager.data.repository.*
import com.aimanager.domain.manager.ContextEngine
import com.aimanager.domain.manager.ManagerEngine
import com.aimanager.domain.orchestration.OrchestrationEngine
import com.aimanager.domain.orchestration.OrchestrationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val currentConversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isManagerTyping: Boolean = false,
    val isWorkerRunning: Boolean = false,
    val workerStatuses: Map<String, TaskStatus> = emptyMap(),
    val streamingContent: String = "",
    val error: String? = null,
    val showSidebar: Boolean = false,
    val activeWorkers: List<WorkerTask> = emptyList()
)

sealed class ChatEvent {
    data class ShowError(val message: String) : ChatEvent()
    data class TaskCompleted(val taskId: String, val output: String) : ChatEvent()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val workerTaskRepository: WorkerTaskRepository,
    private val managerEngine: ManagerEngine,
    private val orchestrationEngine: OrchestrationEngine,
    private val contextEngine: ContextEngine,
    private val usageRepository: UsageRepository,
    private val modelScoreRepository: ModelScoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>()
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private var currentStreamJob: Job? = null

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            conversationRepository.getAllActive().collect { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
            }
        }
    }

    fun selectConversation(id: String) {
        viewModelScope.launch {
            val conversation = conversationRepository.getById(id)
            _uiState.update { it.copy(currentConversation = conversation) }
            messageRepository.getByConversation(id).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun newConversation() {
        viewModelScope.launch {
            val conv = Conversation(
                id = IdGenerator.newId(),
                title = "New Chat"
            )
            conversationRepository.create(conv)
            selectConversation(conv.id)
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val convId = _uiState.value.currentConversation?.id
        if (convId == null) {
            newConversation()
            // Will be called again after conversation is created
            _uiState.update { it.copy(inputText = text) }
            viewModelScope.launch {
                kotlinx.coroutines.delay(100)
                sendMessage()
            }
            return
        }

        _uiState.update { it.copy(inputText = "", isManagerTyping = true, error = null) }

        viewModelScope.launch {
            // Save user message
            val userMessage = Message(
                id = IdGenerator.newId(),
                conversationId = convId,
                role = MessageRole.USER,
                content = text
            )
            messageRepository.insert(userMessage)

            // Auto-title on first message
            val msgCount = messageRepository.count(convId)
            if (msgCount <= 1) {
                val title = text.take(50).let { t ->
                    if (t.length < text.length) "$t..." else t
                }
                conversationRepository.update(
                    _uiState.value.currentConversation!!.copy(title = title, updatedAt = System.currentTimeMillis())
                )
            }

            // Classify task
            val taskType = managerEngine.classifyTask(text)
            val shouldDelegate = managerEngine.shouldDelegate(taskType, text)

            if (!shouldDelegate) {
                // Direct answer from Manager
                directAnswer(convId, text)
            } else {
                // Delegate to workers
                delegateTask(convId, text, taskType)
            }
        }
    }

    private suspend fun directAnswer(convId: String, prompt: String) {
        val history = messageRepository.getRecent(convId, 10)
        val startTime = System.currentTimeMillis()
        val responseBuilder = StringBuilder()

        try {
            managerEngine.directAnswer(convId, prompt, history).collect { chunk ->
                responseBuilder.append(chunk)
                _uiState.update { it.copy(streamingContent = responseBuilder.toString()) }
            }

            val latency = System.currentTimeMillis() - startTime
            val response = responseBuilder.toString()

            val managerMessage = Message(
                id = IdGenerator.newId(),
                conversationId = convId,
                role = MessageRole.MANAGER,
                content = response,
                modelUsed = "gemini-2.0-flash",
                tokensUsed = response.length / 4,
                latencyMs = latency
            )
            messageRepository.insert(managerMessage)

            // Record usage
            usageRepository.insert(UsageRecord(
                id = IdGenerator.newId(),
                timestamp = System.currentTimeMillis(),
                provider = "GEMINI",
                model = "gemini-2.0-flash",
                taskType = TaskType.GENERAL,
                tokensIn = prompt.length / 4,
                tokensOut = response.length / 4,
                latencyMs = latency,
                conversationId = convId
            ))

            contextEngine.compressContext(convId, messageRepository.getRecent(convId, 20))

        } catch (e: Exception) {
            val errorMsg = friendlyErrorMessage(e)
            _uiState.update { it.copy(error = errorMsg) }
            _events.emit(ChatEvent.ShowError(errorMsg))
        } finally {
            _uiState.update { it.copy(isManagerTyping = false, streamingContent = "") }
        }
    }

    private suspend fun delegateTask(convId: String, prompt: String, taskType: TaskType) {
        val model = managerEngine.selectModel(taskType)

        val plan = TaskPlan(
            tasks = listOf(
                PlannedTask(
                    id = IdGenerator.newId(),
                    model = model,
                    prompt = prompt,
                    timeoutMs = 30000
                )
            ),
            synthesisStrategy = SynthesisStrategy.COMPETITIVE
        )

        _uiState.update { it.copy(isWorkerRunning = true) }

        try {
            orchestrationEngine.executePlan(plan, convId).collect { event ->
                when (event) {
                    is OrchestrationEvent.PlanStarted -> {
                        _uiState.update { it.copy(isManagerTyping = false) }
                    }
                    is OrchestrationEvent.TaskCompleted -> {
                        val result = event.result
                        _uiState.update { state ->
                            state.copy(
                                workerStatuses = state.workerStatuses + (result.taskId to result.status)
                            )
                        }

                        if (result.status == TaskStatus.DELIVERED && result.output.isNotEmpty()) {
                            val workerMessage = Message(
                                id = IdGenerator.newId(),
                                conversationId = convId,
                                role = MessageRole.WORKER,
                                content = result.output,
                                modelUsed = result.model,
                                tokensUsed = result.output.length / 4,
                                latencyMs = result.latencyMs
                            )
                            messageRepository.insert(workerMessage)

                            modelScoreRepository.recordOutcome(
                                taskType, result.model, true, result.latencyMs, result.output.length / 4
                            )

                            _events.emit(ChatEvent.TaskCompleted(result.taskId, result.output))
                        } else if (result.error != null) {
                            _events.emit(ChatEvent.ShowError("${result.model}: ${result.error}"))
                        }
                    }
                    is OrchestrationEvent.PlanCompleted -> {}
                    is OrchestrationEvent.Error -> {
                        _events.emit(ChatEvent.ShowError(event.message))
                    }
                }
            }
        } catch (e: Exception) {
            _events.emit(ChatEvent.ShowError(friendlyErrorMessage(e)))
        } finally {
            _uiState.update { it.copy(isWorkerRunning = false, workerStatuses = emptyMap()) }
        }
    }

    fun cancelAllTasks() {
        currentStreamJob?.cancel()
        viewModelScope.launch {
            orchestrationEngine.cancelAllTasks()
            _uiState.update { it.copy(isManagerTyping = false, isWorkerRunning = false, streamingContent = "") }
        }
    }

    fun toggleBookmark(messageId: String) {
        viewModelScope.launch {
            val msg = messageRepository.getById(messageId) ?: return@launch
            messageRepository.update(msg.copy(isBookmarked = !msg.isBookmarked))
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationRepository.delete(id)
            if (_uiState.value.currentConversation?.id == id) {
                _uiState.update { it.copy(currentConversation = null, messages = emptyList()) }
            }
        }
    }

    fun toggleSidebar() {
        _uiState.update { it.copy(showSidebar = !it.showSidebar) }
    }

    private fun friendlyErrorMessage(e: Throwable): String {
        return when {
            e is java.net.SocketTimeoutException -> "Request timed out. Please try again."
            e is java.net.UnknownHostException -> "No internet connection. Check your network."
            e.message?.contains("401") == true -> "API key invalid. Please check your settings."
            e.message?.contains("429") == true -> "Rate limited. Please wait a moment and try again."
            e.message?.contains("500") == true -> "Server error. Please try again later."
            e.message?.contains("503") == true -> "Service temporarily unavailable."
            else -> "Something went wrong. Please try again."
        }
    }
}
