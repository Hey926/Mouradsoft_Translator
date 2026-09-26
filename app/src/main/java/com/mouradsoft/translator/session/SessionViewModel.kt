package com.mouradsoft.translator.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mouradsoft.translator.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

enum class Screen { Welcome, Choose, Input, Completion, Result }

sealed interface Work {
    data object Idle : Work
    data class Listening(val base: String, val finishing: Boolean = false) : Work
    data object Translating : Work
    data class Clarification(val question: String) : Work
    data class Success(val translation: Translation) : Work
    data class Failure(val problem: Problem) : Work
}

data class SessionState(
    val screen: Screen = Screen.Welcome,
    val language: Language = Language.English,
    val direction: Direction? = null,
    val input: String = "",
    val work: Work = Work.Idle,
    val speechNotice: Int? = null,
    val needsSettings: Boolean = false,
    val cloudSpeechConsented: Boolean = false,
    val liveTextConsented: Boolean = false,
    val limitReached: Boolean = false
) {
    val canTranslate get() = direction != null && input.isNotBlank() && work !is Work.Translating && work !is Work.Clarification
}

/** In-memory session only. No SavedStateHandle, bundle, database, or disk history. */
class SessionViewModel(private val repository: TranslationRepository) : ViewModel() {
    private val mutable = MutableStateFlow(SessionState())
    val state = mutable.asStateFlow()
    val isDemo = repository.isDemo
    private var requestJob: Job? = null
    private var generation = 0L

    fun start() { if (mutable.value.screen == Screen.Welcome) update { copy(screen = Screen.Choose) } }
    fun choose(direction: Direction) {
        if (mutable.value.screen != Screen.Choose) return
        invalidate()
        update { copy(direction = direction, work = Work.Idle, speechNotice = null) }
    }
    fun continueToInput() {
        if (mutable.value.screen == Screen.Choose && mutable.value.direction != null) update { copy(screen = Screen.Input) }
    }
    fun edit(text: String) {
        if (mutable.value.screen != Screen.Input) return
        invalidate()
        update { copy(input = bounded(text), work = Work.Idle, speechNotice = null, needsSettings = false, limitReached = text.length >= 1000) }
    }
    fun beginListening() {
        if (mutable.value.screen != Screen.Input || mutable.value.work is Work.Translating) return
        invalidate()
        update { copy(work = Work.Listening(input), speechNotice = null, needsSettings = false) }
    }
    fun transcript(words: String, final: Boolean) {
        val listening = mutable.value.work as? Work.Listening ?: return
        val combined = if (words.isBlank()) listening.base else
            listening.base + (if (listening.base.isEmpty() || listening.base.last().isWhitespace()) "" else " ") + words.trim()
        update { copy(input = bounded(combined), work = if (final) Work.Idle else listening, limitReached = combined.length >= 1000) }
    }
    fun finishingSpeech() { (mutable.value.work as? Work.Listening)?.let { current -> update { copy(work = current.copy(finishing = true)) } } }
    fun endListening() { if (mutable.value.work is Work.Listening) update { copy(work = Work.Idle) } }
    fun speechProblem(message: Int, settings: Boolean = false) {
        endListening()
        update { copy(speechNotice = message, needsSettings = settings) }
    }
    fun consentSpeech() { update { copy(cloudSpeechConsented = true) } }
    fun consentText() { update { copy(liveTextConsented = true) } }

    fun translate() {
        val current = mutable.value
        if (current.screen != Screen.Input || !current.canTranslate || (!isDemo && !current.liveTextConsented)) return
        invalidate()
        val ticket = generation
        val request = TranslationRequest(current.input.trim(), current.language, requireNotNull(current.direction))
        update { copy(work = Work.Translating, speechNotice = null, needsSettings = false) }
        requestJob = viewModelScope.launch {
            val answer = try {
                withTimeout(36_000) { repository.translate(request) }
            } catch (_: TimeoutCancellationException) {
                TranslationOutcome.Failed(Problem.Timeout)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { TranslationOutcome.Failed(Problem.Unknown) }
            // Also protects against a non-cooperative service that returns after cancellation.
            if (generation != ticket || mutable.value.screen != Screen.Input) return@launch
            when (answer) {
                is TranslationOutcome.Ready -> update { copy(screen = Screen.Completion, work = Work.Success(answer.translation)) }
                is TranslationOutcome.Clarify -> update { copy(work = Work.Clarification(answer.question)) }
                is TranslationOutcome.Failed -> update { copy(work = Work.Failure(answer.problem)) }
            }
        }
    }
    fun finish() {
        if (mutable.value.screen == Screen.Completion && mutable.value.work is Work.Success) update { copy(screen = Screen.Result) }
    }
    fun back() {
        invalidate()
        when (mutable.value.screen) {
            Screen.Welcome -> Unit
            Screen.Choose -> update { copy(screen = Screen.Welcome, work = Work.Idle) }
            Screen.Input -> update { copy(screen = Screen.Choose, work = Work.Idle, speechNotice = null) }
            Screen.Completion -> update { copy(screen = Screen.Input, work = Work.Idle) }
            Screen.Result -> update { copy(screen = Screen.Completion) }
        }
    }
    fun redo() { invalidate(); mutable.value = SessionState() }
    private fun invalidate() { generation++; requestJob?.cancel(); requestJob = null }
    private fun update(change: SessionState.() -> SessionState) { mutable.value = mutable.value.change() }
    override fun onCleared() { invalidate() }
}

private fun bounded(text: String): String {
    val end = if (text.length > 1000 && text[999].isHighSurrogate()) 999 else minOf(text.length, 1000)
    return text.substring(0, end)
}
