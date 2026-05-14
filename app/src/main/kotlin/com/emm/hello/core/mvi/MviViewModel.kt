package com.emm.hello.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface MviState

interface MviIntent

interface MviEffect

abstract class MviViewModel<S : MviState, I : MviIntent, E : MviEffect>(
    initialState: S,
) : ViewModel() {

    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<S> = mutableState.asStateFlow()

    private val effectChannel = Channel<E>(Channel.BUFFERED)
    val effect: Flow<E> = effectChannel.receiveAsFlow()

    protected val currentState: S get() = mutableState.value

    abstract fun onIntent(intent: I)

    protected fun setState(reduce: S.() -> S) {
        mutableState.update { it.reduce() }
    }

    protected fun sendEffect(effect: E) {
        viewModelScope.launch { effectChannel.send(effect) }
    }
}
