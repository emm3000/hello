package com.emm.hello.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppBuildSyncRuntimePolicyTest {

    @Test
    fun `local-only flag disables remote runtime`() {
        val subject = AppBuildSyncRuntimePolicy(localOnlyMode = true)

        assertThat(subject.remoteEnabled).isFalse()
        assertThat(subject.modeLabel).isEqualTo("local-only")
    }

    @Test
    fun `remote mode stays enabled when flag is false`() {
        val subject = AppBuildSyncRuntimePolicy(localOnlyMode = false)

        assertThat(subject.remoteEnabled).isTrue()
        assertThat(subject.modeLabel).isEqualTo("remote")
    }
}
