package com.emm.domain.flashcard

private val whitespaceRegex = "\\s+".toRegex()

fun String.wordCountNormalized(): Int {
    if (isBlank()) return 0
    return trim().split(whitespaceRegex).size
}
