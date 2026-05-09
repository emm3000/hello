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
    return try {
        when (val element: JsonElement = tagJson.decodeFromString(tagsJson)) {
            is JsonArray -> element.mapNotNull { item ->
                if (item is JsonPrimitive && item.isString) {
                    val value = item.content
                    if (value.isNotBlank()) {
                        Tag(value = value.lowercase().trim())
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            else -> emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun DeckEntity.toDomain(): Deck = Deck(
    id = id.toDeckId(),
    name = name,
    description = description.orEmpty(),
    createdAt = createdAt.toLocalDateTime(),
    cards = emptyList(),
    cardsCount = 0L,
    tags = emptyList(), // Tags loaded via findTagsForDeck separately
)

fun List<DeckEntity>.toDomain(): List<Deck> = map(DeckEntity::toDomain)
