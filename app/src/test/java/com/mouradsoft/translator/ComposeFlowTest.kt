package com.mouradsoft.translator

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.mouradsoft.translator.data.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h851dp-xxhdpi")
class ComposeFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun text(id: Int) = compose.activity.getString(id)
    private fun click(id: Int) { compose.onNodeWithText(text(id)).performClick(); compose.waitForIdle() }
    private fun enter(direction: Int = R.string.child_adult) {
        click(R.string.start)
        compose.onNodeWithText(text(R.string.continue_action)).assertIsNotEnabled()
        click(direction); click(R.string.continue_action)
    }
    @Test fun fiveScreensFinishAndRedo() {
        compose.onNodeWithText(text(R.string.welcome_title)).assertIsDisplayed()
        click(R.string.start); click(R.string.child_adult); click(R.string.continue_action)
        compose.onNodeWithText(text(R.string.translate)).assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.demo_label)).assertExists()
        compose.onNodeWithText(text(R.string.sample_dance)).performScrollTo().performClick()
        click(R.string.translate)
        compose.waitUntil(5000) { compose.onAllNodesWithText(text(R.string.done_title)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text(R.string.demo_dance_translation)).assertDoesNotExist()
        click(R.string.finish)
        compose.onNodeWithText(text(R.string.demo_dance_translation)).assertExists()
        click(R.string.redo); compose.onNodeWithText(text(R.string.welcome_title)).assertIsDisplayed()
        // No old NavHost entry remains: pressing Android Back now finishes the activity.
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        assertTrue(compose.activity.isFinishing)
    }

    @Test fun typingRotationAndAdultDirection() {
        enter(R.string.adult_child)
        compose.onNodeWithText(text(R.string.sample_compromise)).performScrollTo().performClick()
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assertTextContains(text(R.string.sample_compromise))
        click(R.string.translate)
        compose.waitUntil(5000) { compose.onAllNodesWithText(text(R.string.done_title)).fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNodeWithText(text(R.string.demo_compromise_translation)).assertDoesNotExist()
        click(R.string.finish); compose.onNodeWithText(text(R.string.demo_compromise_translation)).assertExists()
    }

    @Test fun keyboardTextInputIsKeptInTheMessageField() {
        enter()
        val field = compose.onNode(hasSetTextAction())
        field.performScrollTo().performClick().performTextInput("I am cooked")
        field.assertTextContains("I am cooked")
        compose.onNodeWithText("11 / 1,000").assertExists()
        compose.onNodeWithText(text(R.string.translate)).assertIsEnabled()
    }

    @Test fun demoUnknownAndClarificationStayOnInput() {
        enter()
        compose.onNode(hasSetTextAction()).performTextReplacement("An unsupported sample")
        click(R.string.translate)
        compose.waitUntil(5000) { compose.onAllNodesWithText(text(R.string.demo_unsupported)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction()).assertTextContains("An unsupported sample")
        compose.onNodeWithText(text(R.string.sample_cooked)).performScrollTo().performClick()
        click(R.string.translate)
        compose.waitUntil(5000) { compose.onAllNodesWithText(text(R.string.demo_cooked_question)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text(R.string.translate)).assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.done_title)).assertDoesNotExist()
    }

    @Test fun bundledExamplesPreserveLiteralMeaningAndRequireExactMatch() = runTest {
        val service = DemoTranslationService(compose.activity::getString)
        for (direction in Direction.entries) for (example in DemoCatalog.examples(direction)) {
            val answer = service.translate(TranslationRequest(text(example.input), Language.English, direction))
            if (example.question != null) assertTrue(answer is TranslationOutcome.Clarify)
            else assertEquals(text(example.translation!!), (answer as TranslationOutcome.Ready).translation.text)
        }
        val literal = service.translate(TranslationRequest(text(R.string.sample_pasta), Language.English, Direction.ChildToAdult)) as TranslationOutcome.Ready
        assertEquals("I made pasta for dinner.", literal.translation.text)
        assertEquals(TranslationOutcome.Failed(Problem.UnsupportedDemo), service.translate(TranslationRequest("You did NOT slay that dance!", Language.English, Direction.ChildToAdult)))
    }
}
