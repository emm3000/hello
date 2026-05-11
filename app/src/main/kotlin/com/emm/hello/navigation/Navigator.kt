package com.emm.hello.navigation

import androidx.navigation3.runtime.NavKey

class Navigator(
    private val backStack: MutableList<NavKey>
) {

    fun navigateTo(destination: NavKey) {
        backStack.add(destination)
    }

    fun goBack() {
        backStack.removeLastOrNull()
    }
}
