package com.emm.hello.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {

    @Serializable
    private data object RouteA : NavKey

    @Serializable
    private data class RouteB(val id: String) : NavKey

    @Test
    fun `navigateTo adds destination to back stack`() {
        val backStack: SnapshotStateList<NavKey> = mutableListOf<NavKey>(RouteA).toMutableStateList()
        val navigator = Navigator(backStack)

        navigator.navigateTo(RouteB("123"))

        assertEquals(2, backStack.size)
        assertEquals(RouteB("123"), backStack.last())
    }

    @Test
    fun `goBack removes last destination from back stack`() {
        val backStack: SnapshotStateList<NavKey> = mutableListOf<NavKey>(RouteA, RouteB("123")).toMutableStateList()
        val navigator = Navigator(backStack)

        navigator.goBack()

        assertEquals(1, backStack.size)
        assertEquals(RouteA, backStack.last())
    }

    @Test
    fun `goBack on empty back stack does nothing`() {
        val backStack: SnapshotStateList<NavKey> = mutableListOf<NavKey>().toMutableStateList()
        val navigator = Navigator(backStack)

        navigator.goBack()

        assertTrue(backStack.isEmpty())
    }

    @Test
    fun `multiple navigateTo calls append in order`() {
        val backStack: SnapshotStateList<NavKey> = mutableListOf<NavKey>(RouteA).toMutableStateList()
        val navigator = Navigator(backStack)

        navigator.navigateTo(RouteB("1"))
        navigator.navigateTo(RouteB("2"))

        assertEquals(3, backStack.size)
        assertEquals(listOf(RouteA, RouteB("1"), RouteB("2")), backStack)
    }
}
