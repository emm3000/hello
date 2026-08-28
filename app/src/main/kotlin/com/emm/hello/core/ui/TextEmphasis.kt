package com.emm.hello.core.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

fun underlineFirstMatch(text: String, target: String): AnnotatedString {
    val matchStart: Int = if (target.isBlank()) -1 else text.indexOf(target, ignoreCase = true)
    return buildAnnotatedString {
        if (matchStart < 0) {
            append(text)
            return@buildAnnotatedString
        }
        val matchEnd: Int = matchStart + target.length
        append(text.substring(0, matchStart))
        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
            append(text.substring(matchStart, matchEnd))
        }
        append(text.substring(matchEnd))
    }
}
