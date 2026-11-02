package com.emm.data.suggestion

import com.emm.domain.suggestion.SuggestedWord
import com.emm.domain.suggestion.WordSuggestionRepository
import com.emm.domain.suggestion.WordSuggestionRequest
import com.emm.domain.suggestion.WordSuggestions
import java.util.UUID
import kotlinx.coroutines.delay

class CannedWordSuggestionRepository(
    private val delayMs: Long = 600L,
) : WordSuggestionRepository {

    override suspend fun suggest(request: WordSuggestionRequest): WordSuggestions {
        delay(delayMs)
        val excluded: List<String> = request.excludedSituations
        return SCENARIOS.firstOrNull { scenario -> scenario.situation !in excluded }
            ?: SCENARIOS.random().uniqueVariant()
    }

    private fun WordSuggestions.uniqueVariant(): WordSuggestions {
        val suffix: String = UUID.randomUUID().toString().take(VARIANT_SUFFIX_LENGTH)
        return copy(
            situation = "$situation ($suffix)",
            words = words.map { item -> item.copy(word = "${item.word}-$suffix") },
        )
    }

    private companion object {
        const val VARIANT_SUFFIX_LENGTH: Int = 8

        val SCENARIOS: List<WordSuggestions> = listOf(
            WordSuggestions(
                situation = "Ordering food at a busy restaurant",
                words = listOf(
                    SuggestedWord("menu", "la carta"),
                    SuggestedWord("tip", "la propina"),
                    SuggestedWord("spicy", "picante"),
                    SuggestedWord("Could we get the check, please?", "¿Nos trae la cuenta, por favor?"),
                    SuggestedWord("Can I get this to go?", "¿Me lo puede poner para llevar?"),
                    SuggestedWord("I'll have the same, please.", "Para mí lo mismo, por favor."),
                ),
            ),
            WordSuggestions(
                situation = "Asking for directions in a new city",
                words = listOf(
                    SuggestedWord("downtown", "el centro"),
                    SuggestedWord("crosswalk", "el paso peatonal"),
                    SuggestedWord("block", "la cuadra"),
                    SuggestedWord("Excuse me, how do I get to the station?", "Disculpe, ¿cómo llego a la estación?"),
                    SuggestedWord("Is it within walking distance?", "¿Se puede ir caminando?"),
                    SuggestedWord("Keep going straight for two blocks.", "Siga derecho dos cuadras."),
                ),
            ),
            WordSuggestions(
                situation = "A job interview for a junior position",
                words = listOf(
                    SuggestedWord("resume", "el currículum"),
                    SuggestedWord("salary", "el sueldo"),
                    SuggestedWord("deadline", "la fecha límite"),
                    SuggestedWord("Tell me a little about yourself.", "Cuénteme un poco sobre usted."),
                    SuggestedWord("I'm a quick learner.", "Aprendo rápido."),
                    SuggestedWord("When would you be able to start?", "¿Cuándo podría empezar?"),
                ),
            ),
        )
    }
}
