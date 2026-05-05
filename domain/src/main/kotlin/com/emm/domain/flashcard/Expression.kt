package com.emm.domain.flashcard

import com.emm.domain.text.lowercaseRoot

@JvmInline
value class Expression private constructor(val value: String) {

    val canonical: String
        get() = value.lowercaseRoot()

    companion object {
        fun from(rawValue: String): Expression = Expression(rawValue.normalizedExpression())
    }
}

fun String.toExpression(): Expression = Expression.from(this)

private fun String.normalizedExpression(): String {
    val normalized = trim().replace("\\s+".toRegex(), " ")
    require(normalized.isNotEmpty()) { "expression cannot be blank." }
    return normalized
}
