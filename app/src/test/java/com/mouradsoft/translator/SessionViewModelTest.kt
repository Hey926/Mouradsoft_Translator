package com.mouradsoft.translator

import com.mouradsoft.translator.data.*
import com.mouradsoft.translator.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var vm: SessionViewModel
    private var calls = 0
    private var answer: suspend (TranslationRequest) -> TranslationOutcome = { TranslationOutcome.Ready(Translation("A clear meaning.")) }
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = SessionViewModel(TranslationRepository(object : TranslationService {
            override val isDemo = true
            override suspend fun translate(request: TranslationRequest): TranslationOutcome { calls++; return answer(request) }
        }))
    }
    @After fun cleanup() { vm.redo(); Dispatchers.resetMain() }
    private fun enter(direction: Direction = Direction.ChildToAdult) {
        vm.start(); vm.choose(direction); vm.continueToInput()
    }

    @Test fun `exact five screen order and finish is required`() = runTest(dispatcher) {
        assertEquals(Screen.Welcome, vm.state.value.screen)
        vm.finish(); assertEquals(Screen.Welcome, vm.state.value.screen)
        vm.start(); assertEquals(Screen.Choose, vm.state.value.screen)
        vm.continueToInput(); assertEquals(Screen.Choose, vm.state.value.screen)
        vm.choose(Direction.ChildToAdult); vm.continueToInput(); assertEquals(Screen.Input, vm.state.value.screen)
        vm.edit("You slayed that dance!"); vm.translate()
        assertTrue(vm.state.value.work is Work.Translating)
        advanceUntilIdle(); assertEquals(Screen.Completion, vm.state.value.screen)
        advanceTimeBy(60000); assertEquals(Screen.Completion, vm.state.value.screen)
        vm.finish(); assertEquals(Screen.Result, vm.state.value.screen)
        assertEquals(1, calls)
    }
    @Test fun `empty input is disabled and both directions reach service`() = runTest(dispatcher) {
        enter(); vm.translate(); vm.edit(" \n "); vm.translate(); advanceUntilIdle(); assertEquals(0, calls)
        for (direction in Direction.entries) {
            vm.redo(); enter(direction); vm.edit("Some words")
            answer = { assertEquals(direction, it.direction); TranslationOutcome.Ready(Translation("A clear meaning.")) }
            vm.translate(); advanceUntilIdle(); assertEquals(Screen.Completion, vm.state.value.screen)
        }
        assertEquals(2, calls)
    }
    @Test fun `duplicate taps issue only one request`() = runTest(dispatcher) {
        answer = { delay(1000); TranslationOutcome.Ready(Translation("A clear meaning.")) }
        enter(); vm.edit("Some words"); repeat(8) { vm.translate() }; runCurrent()
        assertEquals(1, calls); advanceUntilIdle(); assertEquals(Screen.Completion, vm.state.value.screen)
    }
    @Test fun `failure preserves text and retry works`() = runTest(dispatcher) {
        answer = { TranslationOutcome.Failed(Problem.Connection) }
        enter(); vm.edit("Keep my words"); vm.translate(); advanceUntilIdle()
        assertEquals(Screen.Input, vm.state.value.screen); assertTrue(vm.state.value.work is Work.Failure)
        assertEquals("Keep my words", vm.state.value.input)
        answer = { TranslationOutcome.Ready(Translation("A clear meaning.")) }
        vm.translate(); advanceUntilIdle(); assertEquals(Screen.Completion, vm.state.value.screen)
    }
    @Test fun `clarification cannot succeed until input is changed`() = runTest(dispatcher) {
        answer = { TranslationOutcome.Clarify("What happened just before that?") }
        enter(); vm.edit("I'm cooked"); vm.translate(); advanceUntilIdle()
        assertEquals(Screen.Input, vm.state.value.screen); assertTrue(vm.state.value.work is Work.Clarification)
        assertFalse(vm.state.value.canTranslate); vm.translate(); assertEquals(1, calls)
        vm.edit("I forgot to study. I'm cooked"); assertTrue(vm.state.value.canTranslate)
    }
    @Test fun `empty success and invalid clarification are failures`() = runTest(dispatcher) {
        enter(); vm.edit("test")
        for (bad in listOf(TranslationOutcome.Ready(Translation(" ")), TranslationOutcome.Clarify(""))) {
            answer = { bad }; vm.translate(); advanceUntilIdle()
            assertTrue(vm.state.value.work is Work.Failure); assertEquals(Screen.Input, vm.state.value.screen)
        }
    }
    @Test fun `redo resets all memory and back cannot reveal old result`() = runTest(dispatcher) {
        enter(); vm.edit("Some words"); vm.consentSpeech(); vm.consentText(); vm.translate(); advanceUntilIdle(); vm.finish()
        vm.redo(); vm.back()
        assertEquals(SessionState(), vm.state.value)
    }
    @Test fun `redo rejects delayed noncooperative old response during new request`() = runTest(dispatcher) {
        answer = { withContext(NonCancellable) { delay(5000) }; TranslationOutcome.Ready(Translation("Old secret")) }
        enter(); vm.edit("Old words"); vm.translate(); runCurrent()
        vm.redo(); enter(Direction.AdultToChild); vm.edit("New words")
        answer = { delay(10000); TranslationOutcome.Ready(Translation("New meaning")) }
        vm.translate(); runCurrent(); advanceTimeBy(5001); runCurrent()
        assertEquals("New words", vm.state.value.input); assertTrue(vm.state.value.work is Work.Translating)
        advanceUntilIdle(); assertEquals("New meaning", (vm.state.value.work as Work.Success).translation.text)
    }
    @Test fun `editing and leaving input invalidate a pending response`() = runTest(dispatcher) {
        answer = { withContext(NonCancellable) { delay(1000) }; TranslationOutcome.Ready(Translation("Old meaning")) }
        enter(); vm.edit("first"); vm.translate(); runCurrent(); vm.edit("changed"); advanceUntilIdle()
        assertEquals(Work.Idle, vm.state.value.work); assertEquals("changed", vm.state.value.input)
        vm.translate(); runCurrent(); vm.back(); vm.choose(Direction.AdultToChild); advanceUntilIdle()
        assertEquals(Screen.Choose, vm.state.value.screen); assertEquals(Work.Idle, vm.state.value.work)
    }
    @Test fun `speech partials replace each other and preserve typed text`() {
        enter(); vm.edit("Typed first."); vm.beginListening()
        vm.transcript("Hello", false); vm.transcript("Hello there", false); vm.transcript("Hello there!", true)
        assertEquals("Typed first. Hello there!", vm.state.value.input)
        assertEquals(Work.Idle, vm.state.value.work)
        vm.transcript("late callback", true); assertEquals("Typed first. Hello there!", vm.state.value.input)
        vm.beginListening(); vm.transcript("Next", false); vm.endListening()
        assertEquals("Typed first. Hello there! Next", vm.state.value.input)
    }
    @Test fun `input limit handles pasted and spoken text without split surrogate`() {
        enter(); vm.edit("x".repeat(999) + "😀"); assertEquals(999, vm.state.value.input.length)
        vm.edit("x".repeat(2000)); assertEquals(1000, vm.state.value.input.length)
        vm.beginListening(); vm.transcript("more", true); assertEquals(1000, vm.state.value.input.length)
    }
    @Test fun `request timeout and thrown errors stay on input`() = runTest(dispatcher) {
        enter(); vm.edit("Some words"); answer = { delay(40000); TranslationOutcome.Ready(Translation("late")) }
        vm.translate(); advanceUntilIdle(); assertEquals(Work.Failure(Problem.Timeout), vm.state.value.work)
        answer = { error("Private details must not reach the UI") }; vm.translate(); advanceUntilIdle()
        assertEquals(Work.Failure(Problem.Unknown), vm.state.value.work)
    }
}
