package com.emm.data.quote

import com.emm.domain.quote.Quote
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun parseQuoteResponse(content: String, json: Json): QuoteResponse? {
    val root: JsonObject = json.parseToJsonElement(content).jsonObject
    val isSuccess: Boolean = root["success"]?.jsonPrimitive?.booleanOrNull == true
    if (isSuccess) {
        val dataJson: String = root["data"]?.jsonObject.toString()
        val data: QuoteResponse = json.decodeFromString<QuoteResponse>(dataJson)
        return data
    } else {
        return null
    }
}

fun QuoteEntity.toDomain() = Quote(
    id = id,
    title = title,
    phrase = phrase,
    description = description,
    translation = translation,
    example = example,
    context = context,
    pronunciation = pronunciation,
    formality = formality,
    tags = tags.split("|").map(String::trim),
    category = category,
)