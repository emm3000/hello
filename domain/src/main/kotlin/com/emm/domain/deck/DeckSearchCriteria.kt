package com.emm.domain.deck

import java.util.Locale

data class DeckSearchCriteria(
    val query: String = "",
    val selectedTags: Set<String> = emptySet(),
) {
    val normalizedQuery: String = query.trim().lowercase(Locale.ROOT)
    val normalizedSelectedTags: Set<String> = selectedTags
        .map { it.trim().lowercase(Locale.ROOT) }
        .filter { it.isNotBlank() }
        .toSet()
}
