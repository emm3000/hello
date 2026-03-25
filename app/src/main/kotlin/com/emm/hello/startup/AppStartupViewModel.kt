package com.emm.hello.startup

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class AppStartupViewModel(
    private val coordinator: AppStartupCoordinator,
) : ViewModel() {

    val state: StateFlow<AppStartupState> = coordinator.state

    init {
        coordinator.start()
    }

    fun retry() {
        coordinator.start()
    }
}
