package com.emm.hello.sync

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectivitySyncTriggerTest {

    @Test
    fun `unregister resets failed registration so register can reactivate in process`() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0

        val context = mockk<Context>(relaxed = true)
        val connectivityManager = mockk<ConnectivityManager>(relaxed = true)

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager

        var registerCalls = 0
        every { connectivityManager.registerDefaultNetworkCallback(any()) } answers {
            registerCalls += 1
            if (registerCalls == 1) {
                throw IllegalStateException("boom")
            }
            Unit
        }

        ConnectivitySyncTrigger.unregister(context)
        ConnectivitySyncTrigger.register(context) { }
        ConnectivitySyncTrigger.unregister(context)
        ConnectivitySyncTrigger.register(context) { }

        assertEquals(2, registerCalls)

        ConnectivitySyncTrigger.unregister(context)
    }
}
