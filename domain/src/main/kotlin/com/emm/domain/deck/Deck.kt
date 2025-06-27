package com.emm.domain.deck

import java.time.LocalDateTime

data class Deck(
    val id: String,
    val name: String,
    val description: String,
    val createdAt: LocalDateTime,
)