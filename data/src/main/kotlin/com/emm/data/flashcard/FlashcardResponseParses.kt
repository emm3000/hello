package com.emm.data.flashcard

import com.emm.data.flashcard.iadto.GeminiResponse
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.FlashcardGenerated
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object FlashcardResponseParses {

    fun parse(raw: String, json: Json): FlashcardGenerated {
        return try {
            val response: GeminiResponse = json.decodeFromString<GeminiResponse>(raw)

            if (response.success && response.data != null) {
                val data = response.data
                FlashcardGenerated(
                    word = data.word,
                    translation = data.translation,
                    phonetics = data.phonetic,
                    meaning = data.meaning,
                    language = data.language,
                    examples = data.examples.map { dto ->
                        Example(
                            text = dto.text,
                            translation = dto.translation,
                            type = dto.type,
                            exampleId = ""
                        )
                    }
                )
            } else {
                val errorMsg = response.error?.message ?: "Unknown AI error"
                throw IllegalArgumentException(errorMsg)
            }
        } catch (e: SerializationException) {
            throw IllegalArgumentException("La respuesta de la IA no coincide con el formato esperado", e)
        } catch (e: Exception) {
            throw e
        }
    }
}
