package com.emm.data.deck

import com.emm.domain.deck.Deck
import com.emm.domain.deck.Tag
import com.emm.domain.ids.toDeckId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.decodeFromString
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Long.toLocalDateTime(): LocalDateTime = Instant
    .ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()

private val tagJson = Json { ignoreUnknownKeys = true }

fun parseTags(tagsJson: String?): List<Tag> {
    if (tagsJson.isNullOrBlank()) return emptyList()

    val element = try {
        tagJson.decodeFromString<JsonElement>(tagsJson)
    } catch (_: Exception) {
        return emptyList()
    }

    return if (element is JsonArray) {
        element.mapNotNull { item -> item.toTagOrNull() }
    } else {
        emptyList()
    }
}

private fun JsonElement.toTagOrNull(): Tag? {
    if (this !is JsonPrimitive || !isString) return null
    val value = content
    if (value.isBlank()) return null
    return Tag(value = value.lowercase().trim())
}

fun DeckEntity.toDomain(): Deck = Deck(
    id = id.toDeckId(),
    name = name,
    description = description.orEmpty(),
    createdAt = createdAt.toLocalDateTime(),
    cards = emptyList(),
    cardsCount = 0L,
    tags = emptyList(), // Tags loaded via fetchTagsForDeck separately
)

fun List<DeckEntity>.toDomain(): List<Deck> = map(DeckEntity::toDomain)

fun String.toDomainTagsFromCsv(): List<Tag> {
    if (isBlank()) return emptyList()
    return split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .map(::Tag)
}
