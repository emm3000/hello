package com.emm.data.suggestion

import com.emm.domain.suggestion.SuggestedWord
import com.emm.domain.suggestion.WordSuggestionRepository
import com.emm.domain.suggestion.WordSuggestions
import kotlinx.coroutines.delay

class CannedWordSuggestionRepository(
    private val delayMs: Long = 600L,
) : WordSuggestionRepository {

    override suspend fun suggest(recentWords: List<String>): WordSuggestions {
        delay(delayMs)
        return SCENARIOS[recentWords.size % SCENARIOS.size]
    }

    private companion object {
        val SCENARIOS: List<WordSuggestions> = listOf(
            WordSuggestions(
                situation = "Ordering food at a busy restaurant",
                words = listOf(
                    SuggestedWord("Could I get...", "¿Podría pedir...?"),
                    SuggestedWord("the check", "la cuenta"),
                    SuggestedWord("to go", "para llevar"),
                    SuggestedWord("medium rare", "término medio"),
                    SuggestedWord("a reservation", "una reservación"),
                    SuggestedWord("to split the bill", "dividir la cuenta"),
                ),
            ),
            WordSuggestions(
                situation = "Asking for directions in a new city",
                words = listOf(
                    SuggestedWord("Excuse me", "Disculpe"),
                    SuggestedWord("straight ahead", "todo derecho"),
                    SuggestedWord("turn left", "doble a la izquierda"),
                    SuggestedWord("turn right", "doble a la derecha"),
                    SuggestedWord("the nearest", "el más cercano"),
                    SuggestedWord("how far is it", "qué tan lejos está"),
                ),
            ),
            WordSuggestions(
                situation = "A job interview for a junior position",
                words = listOf(
                    SuggestedWord("strengths and weaknesses", "fortalezas y debilidades"),
                    SuggestedWord("previous experience", "experiencia previa"),
                    SuggestedWord("team player", "trabajo en equipo"),
                    SuggestedWord("available to start", "disponible para empezar"),
                    SuggestedWord("salary expectations", "expectativas salariales"),
                    SuggestedWord("follow up", "dar seguimiento"),
                ),
            ),
        )
    }
}
