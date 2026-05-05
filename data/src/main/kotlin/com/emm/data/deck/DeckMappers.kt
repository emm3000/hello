package com.emm.data.deck

import com.emm.domain.deck.Deck
import com.emm.domain.ids.toDeckId
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Long.toLocalDateTime(): LocalDateTime = Instant
    .ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()

fun DeckEntity.toDomain() = Deck(
    id = id.toDeckId(),
    name = name,
    description = description.orEmpty(),
    createdAt = createdAt.toLocalDateTime(),
    cards = emptyList(),
    cardsCount = 0L,
)

fun List<DeckEntity>.toDomain() = map(DeckEntity::toDomain)
