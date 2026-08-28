package com.emm.hello.newfeatures.library

import com.emm.domain.deck.Deck
import com.emm.domain.ids.DeckId
import com.emm.domain.library.LibraryFlashcard
import com.emm.hello.core.mvi.MviState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class LibraryUiState(
    val cards: List<LibraryFlashcard> = emptyList(),
    val decks: List<Deck> = emptyList(),
    val query: String = "",
    val selectedDeckId: DeckId? = null,
    val isLoading: Boolean = true,
    val referenceNow: Instant = Instant.EPOCH,
) : MviState {

    val isFiltered: Boolean
        get() = query.isNotBlank() || selectedDeckId != null

    val isLibraryEmpty: Boolean
        get() = !isLoading && cards.isEmpty() && !isFiltered

    val hasNoResults: Boolean
        get() = !isLoading && cards.isEmpty() && isFiltered
}

data class LibrarySearchCriteria(
    val query: String = "",
    val deckId: DeckId? = null,
)

sealed interface ScheduleStatus {
    data object New : ScheduleStatus
    data object DueToday : ScheduleStatus
    data class InDays(val days: Long) : ScheduleStatus
}

fun LibraryFlashcard.scheduleStatus(now: Instant, zone: ZoneId): ScheduleStatus {
    val reviewAtMillis: Long = nextReviewAt ?: return ScheduleStatus.New
    val today: LocalDate = now.atZone(zone).toLocalDate()
    val reviewDay: LocalDate = Instant.ofEpochMilli(reviewAtMillis).atZone(zone).toLocalDate()
    val days: Long = ChronoUnit.DAYS.between(today, reviewDay)
    return if (days <= 0) ScheduleStatus.DueToday else ScheduleStatus.InDays(days)
}
