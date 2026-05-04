package com.emm.domain.ids

@JvmInline
value class DeckId private constructor(val value: String) {

    companion object {
        fun from(rawValue: String): DeckId = DeckId(rawValue.normalizedDeckId())
    }
}

fun String.toDeckId(): DeckId = DeckId.from(this)

private fun String.normalizedDeckId(): String {
    val normalized = trim()
    require(normalized.isNotEmpty()) { "DeckId cannot be blank." }
    return normalized
}
