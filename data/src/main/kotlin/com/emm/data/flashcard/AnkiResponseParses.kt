package com.emm.data.flashcard

import com.emm.data.flashcard.iadto.AnkiDto
import com.emm.domain.flashcard.FlashcardGenerated
import kotlinx.serialization.json.Json

object AnkiResponseParses {

    fun parse(raw: String, json: Json): FlashcardGenerated {
        val cleaned: String = raw

        return try {
            val data: AnkiDto = json.decodeFromString<AnkiDto>(cleaned)
            FlashcardGenerated(
                word = data.front,
                translation = data.back,
                phonetics = "",
                meaning = "",
                language = "",
                examples = listOf()
            )
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(e)
        } catch (_: Exception) {
            throw IllegalArgumentException("Invalid response")
        }
    }
}
