package com.emm.data.flashcard

import com.emm.data.flashcard.iadto.ExampleDto
import com.emm.data.flashcard.iadto.FlashcardDto
import com.emm.data.flashcard.iadto.ResponseError
import com.emm.domain.flashcard.Example
import com.emm.domain.flashcard.FlashcardGenerated
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object FlashcardResponseParses {

    fun parse(raw: String, json: Json): FlashcardGenerated {
        val cleaned: String = raw.removePrefixAndSuffix()

        return try {
            val root: JsonObject = json.parseToJsonElement(cleaned).jsonObject
            val isSuccess: Boolean = root["success"]?.jsonPrimitive?.booleanOrNull == true
            if (isSuccess) {
                val flashcardGenerated: FlashcardGenerated = parseToDomain(root, json)
                flashcardGenerated
            } else {
                val errorJson: String = root["error"]?.jsonObject.toString()
                val data: ResponseError = json.decodeFromString<ResponseError>(errorJson)
                throw IllegalArgumentException(data.message)
            }
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(e)
        } catch (_: Exception) {
            throw IllegalArgumentException("Invalid response")
        }
    }

    private fun parseToDomain(
        root: JsonObject,
        json: Json,
    ): FlashcardGenerated {
        val dataJson: String = root["data"]?.jsonObject.toString()
        val data: FlashcardDto = json.decodeFromString<FlashcardDto>(dataJson)
        val examples: List<Example> = exampleDtoToDomain(data)
        return FlashcardGenerated(
            word = data.word,
            translation = data.translation,
            phonetics = data.phonetic,
            meaning = data.meaning,
            language = data.language,
            examples = examples,
        )
    }

    private fun exampleDtoToDomain(data: FlashcardDto): List<Example> {
        return data.examples.map(::toDomain)
    }

    private fun toDomain(dto: ExampleDto): Example = Example(
        text = dto.text,
        translation = dto.translation,
        type = dto.type,
        exampleId = String()
    )
}