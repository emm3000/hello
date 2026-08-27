package com.emm.domain.text

import java.text.Normalizer

private val combiningMarks: Regex = Regex("\\p{Mn}+")

fun String.searchNormalized(): String {
    val decomposed: String = Normalizer.normalize(trim(), Normalizer.Form.NFD)
    return decomposed.replace(combiningMarks, "").lowercaseRoot()
}
