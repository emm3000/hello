package com.emm.data.deck

import com.emm.domain.deck.Deck
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Long.toLocalDateTime(): LocalDateTime = Instant
    .ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()

fun DeckEntity.toDomain() = Deck(
    id = id,
    name = name,
    description = description.orEmpty(),
    createdAt = createdAt.toLocalDateTime(),
)

fun List<DeckEntity>.toDomain() = map(DeckEntity::toDomain)