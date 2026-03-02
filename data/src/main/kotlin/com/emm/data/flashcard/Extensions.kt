package com.emm.data.flashcard

fun String.removePrefixAndSuffix() = this
    .removePrefix("```json")
    .removePrefix("```")
    .removeSuffix("```")
    .trim()
