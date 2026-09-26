package com.mouradsoft.translator.data

import com.mouradsoft.translator.R
import kotlinx.coroutines.delay
import java.util.Locale

data class DemoExample(val input: Int, val translation: Int? = null, val explanation: Int? = null, val question: Int? = null)

object DemoCatalog {
    fun examples(direction: Direction) = when (direction) {
        Direction.ChildToAdult -> listOf(
            DemoExample(R.string.sample_test, R.string.demo_test_translation, R.string.demo_test_explanation),
            DemoExample(R.string.sample_dance, R.string.demo_dance_translation, R.string.demo_dance_explanation),
            DemoExample(R.string.sample_pasta, R.string.demo_pasta_translation),
            DemoExample(R.string.sample_cooked, question = R.string.demo_cooked_question)
        )
        Direction.AdultToChild -> listOf(
            DemoExample(R.string.sample_compromise, R.string.demo_compromise_translation, R.string.demo_compromise_explanation),
            DemoExample(R.string.sample_hold, R.string.demo_hold_translation),
            DemoExample(R.string.sample_prioritize, R.string.demo_prioritize_translation)
        )
    }
}

/** Exact normalized examples only: never substring-match an invented meaning. */
class DemoTranslationService(private val string: (Int) -> String) : TranslationService {
    override val isDemo = true
    override suspend fun translate(request: TranslationRequest): TranslationOutcome {
        delay(450) // A small, cancelable preview transition; never described as live AI.
        val example = DemoCatalog.examples(request.direction).firstOrNull {
            normalize(string(it.input)) == normalize(request.text)
        } ?: return TranslationOutcome.Failed(Problem.UnsupportedDemo)
        return example.question?.let { TranslationOutcome.Clarify(string(it)) }
            ?: TranslationOutcome.Ready(Translation(string(requireNotNull(example.translation)), example.explanation?.let(string).orEmpty()))
    }

    private fun normalize(text: String) = text.trim().lowercase(Locale.ROOT)
        .replace('’', '\'').replace(Regex("\\s+"), " ").trimEnd('.', '!', '?')
}
