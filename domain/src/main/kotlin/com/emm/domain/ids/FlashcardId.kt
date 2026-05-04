package com.emm.domain.ids

@JvmInline
value class FlashcardId private constructor(val value: String) {

    companion object {
        fun from(rawValue: String): FlashcardId = FlashcardId(rawValue.normalizedFlashcardId())
    }
}

fun String.toFlashcardId(): FlashcardId = FlashcardId.from(this)

private fun String.normalizedFlashcardId(): String {
    val normalized = trim()
    require(normalized.isNotEmpty()) { "FlashcardId cannot be blank." }
    return normalized
}
