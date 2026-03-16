package com.emm.hello.core.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

abstract class MviViewModel<UiState : Any, UiIntent : Any, UiEffect : Any>(
    initialState: UiState,
) : ViewModel() {

    protected val mutableState = MutableStateFlow(initialState)
    val uiState: StateFlow<UiState> = mutableState.asStateFlow()

    protected val mutableEffect = Channel<UiEffect>(Channel.BUFFERED)
    val effect = mutableEffect.receiveAsFlow()

    abstract fun onIntent(intent: UiIntent)
}
