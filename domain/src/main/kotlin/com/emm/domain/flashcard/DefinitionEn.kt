package com.emm.domain.flashcard

import com.emm.domain.text.lowercaseRoot

@JvmInline
value class DefinitionEn private constructor(val value: String) {

    val canonical: String
        get() = value.lowercaseRoot()

    companion object {
        fun from(rawValue: String): DefinitionEn = DefinitionEn(rawValue.normalizedDefinition())

        fun fromOrNull(rawValue: String): DefinitionEn? = runCatching { from(rawValue) }.getOrNull()
    }
}

fun String.toDefinitionEn(): DefinitionEn = DefinitionEn.from(this)

private fun String.normalizedDefinition(): String {
    val normalized = trim().replace("\\s+".toRegex(), " ")
    require(normalized.isNotEmpty()) { "definitionEn cannot be blank." }
    return normalized
}
