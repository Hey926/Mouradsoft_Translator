package com.mouradsoft.translator.data

import com.mouradsoft.translator.R

enum class Language(val code: String, val speechTag: String, val label: Int) {
    English("en", "en-US", R.string.english)
}

enum class Direction(val wire: String, val label: Int, val description: Int) {
    ChildToAdult("child_to_adult", R.string.child_adult, R.string.child_adult_body),
    AdultToChild("adult_to_child", R.string.adult_child, R.string.adult_child_body)
}

data class TranslationRequest(val text: String, val language: Language, val direction: Direction)
data class Translation(val text: String, val explanation: String = "")

enum class Problem(val message: Int) {
    UnsupportedDemo(R.string.demo_unsupported), Connection(R.string.error_connection),
    Timeout(R.string.error_timeout), RateLimited(R.string.error_rate),
    Unavailable(R.string.error_unavailable), InvalidResponse(R.string.error_invalid),
    Refused(R.string.error_refused), Unknown(R.string.error_generic)
}

sealed interface TranslationOutcome {
    data class Ready(val translation: Translation) : TranslationOutcome
    data class Clarify(val question: String) : TranslationOutcome
    data class Failed(val problem: Problem) : TranslationOutcome
}

interface TranslationService {
    val isDemo: Boolean
    suspend fun translate(request: TranslationRequest): TranslationOutcome
}

/** A second boundary protects the UI even if a service implementation misbehaves. */
class TranslationRepository(private val service: TranslationService) {
    val isDemo get() = service.isDemo
    suspend fun translate(request: TranslationRequest): TranslationOutcome {
        if (request.text.isBlank() || request.text.length > 1000) return TranslationOutcome.Failed(Problem.InvalidResponse)
        return when (val answer = service.translate(request)) {
            is TranslationOutcome.Ready -> {
                if (!meaningful(answer.translation.text, 2000) || answer.translation.explanation.length > 600)
                    TranslationOutcome.Failed(Problem.InvalidResponse)
                else answer.copy(translation = answer.translation.copy(
                    text = answer.translation.text.trim(),
                    explanation = answer.translation.explanation.trim().takeUnless {
                        it.equals(answer.translation.text.trim(), ignoreCase = true)
                    }.orEmpty()
                ))
            }
            is TranslationOutcome.Clarify -> if (meaningful(answer.question, 300) && answer.question.trim().length >= 8)
                answer else TranslationOutcome.Failed(Problem.InvalidResponse)
            is TranslationOutcome.Failed -> answer
        }
    }
}

internal fun meaningful(text: String, limit: Int) = text.trim().length in 2..limit &&
    text.any { it.isLetterOrDigit() } && !text.trim().startsWith("{") && !text.trim().startsWith("[") &&
    !text.contains("```") && !text.contains('\u0000')
