package com.emm.domain.flashcard

private val WHITESPACE_REGEX = "\\s+".toRegex()

fun String.wordCountNormalized(): Int {
    if (isBlank()) return 0
    return trim().split(WHITESPACE_REGEX).size
}
