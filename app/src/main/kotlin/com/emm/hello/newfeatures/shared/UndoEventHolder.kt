package com.emm.hello.newfeatures.shared

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-level coordination bus for undo events emitted after a soft-delete.
 *
 * Design: extraBufferCapacity = 1, replay = 0.
 * - Buffer guarantees the event is not dropped even if the landing ViewModel is
 *   alive in the backstack but its collector has not yet resumed after navigation.
 * - No replay (replay = 0) means a screen that re-enters later (e.g. back to
 *   dashboard a second time) does NOT see the stale event again — consume-once
 *   semantics come for free.
 * - DashboardViewModel and DeckDetailViewModel collect in init{} with viewModelScope,
 *   which survives on the Nav3 backstack, so the event is always caught.
 */
class UndoEventHolder {

    private val _events = MutableSharedFlow<UndoEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UndoEvent> = _events.asSharedFlow()

    fun tryEmit(event: UndoEvent) {
        _events.tryEmit(event)
    }
}

sealed interface UndoEvent {
    data class DeckDeleted(
        val deckId: String,
        val deletedAt: Long,
        val deckName: String,
    ) : UndoEvent

    data class CardDeleted(
        val flashcardId: String,
        val deletedAt: Long,
    ) : UndoEvent
}
