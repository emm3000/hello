package com.emm.domain.flashcard

import com.emm.domain.text.lowercaseRoot

@JvmInline
value class IntendedMeaningEs private constructor(val value: String) {

    val canonical: String
        get() = value.lowercaseRoot()

    companion object {
        fun from(rawValue: String): IntendedMeaningEs = IntendedMeaningEs(rawValue.normalizedIntendedMeaning())

        fun fromOrNull(rawValue: String): IntendedMeaningEs? = runCatching { from(rawValue) }.getOrNull()
    }
}

fun String.toIntendedMeaningEs(): IntendedMeaningEs = IntendedMeaningEs.from(this)

private fun String.normalizedIntendedMeaning(): String {
    val normalized = trim().replace("\\s+".toRegex(), " ")
    require(normalized.isNotEmpty()) { "intendedMeaningEs cannot be blank." }
    return normalized
}
