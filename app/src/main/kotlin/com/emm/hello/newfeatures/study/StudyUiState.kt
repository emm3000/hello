package com.emm.hello.newfeatures.study

import com.emm.hello.core.mvi.MviState

data class StudyUiState(
    val isLoading: Boolean = true,
    val loadError: StudyLoadError? = null,
    val currentItem: StudySessionItem? = null,
    val reviewedCount: Int = 0,
    val totalCount: Int = 0,
    val sessionFinished: Boolean = false,
) : MviState

/**
 * Distinguishes a failed session load from a genuinely empty due queue.
 * Without this, a DB read failure would silently render the "nothing due" empty state.
 */
sealed interface StudyLoadError {
    data object SessionLoadFailed : StudyLoadError
}
