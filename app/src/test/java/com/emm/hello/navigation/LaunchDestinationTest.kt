package com.emm.hello.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LaunchDestinationTest {

    @Test
    fun `fromExtraValue with study_due returns StudyDue`() {
        val destination: LaunchDestination? = LaunchDestination.fromExtraValue("study_due")

        assertThat(destination).isEqualTo(LaunchDestination.StudyDue)
    }

    @Test
    fun `fromExtraValue with null returns null`() {
        val destination: LaunchDestination? = LaunchDestination.fromExtraValue(null)

        assertThat(destination).isNull()
    }

    @Test
    fun `fromExtraValue with unknown value returns null`() {
        val destination: LaunchDestination? = LaunchDestination.fromExtraValue("unknown")

        assertThat(destination).isNull()
    }
}
