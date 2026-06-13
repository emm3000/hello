package com.emm.domain.flashcard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsParametersTest {

    // --- DEFAULT companion ---

    @Test
    fun `DEFAULT has 21 weights`() {
        assertEquals(21, FsrsParameters.DEFAULT.weights.size)
    }

    @Test
    fun `DEFAULT requestedRetention is 0 90`() {
        assertEquals(0.90, FsrsParameters.DEFAULT.requestedRetention, 0.0001)
    }

    @Test
    fun `DEFAULT maximumIntervalDays is 36500`() {
        assertEquals(36500L, FsrsParameters.DEFAULT.maximumIntervalDays)
    }

    @Test
    fun `DEFAULT weights first four entries are S0 values for AGAIN HARD GOOD EASY`() {
        // w[0]=S0(AGAIN), w[1]=S0(HARD), w[2]=S0(GOOD), w[3]=S0(EASY)
        // All must be positive (they become initial stability values)
        val w = FsrsParameters.DEFAULT.weights
        assertTrue("w[0] must be positive", w[0] > 0.0)
        assertTrue("w[1] must be positive", w[1] > 0.0)
        assertTrue("w[2] must be positive", w[2] > 0.0)
        assertTrue("w[3] must be positive", w[3] > 0.0)
    }

    @Test
    fun `DEFAULT weight ordering satisfies S0(EASY) greater than S0(GOOD) greater than S0(HARD)`() {
        val w = FsrsParameters.DEFAULT.weights
        // w[3]=EASY, w[2]=GOOD, w[1]=HARD, w[0]=AGAIN
        assertTrue("S0(EASY)>S0(GOOD)", w[3] > w[2])
        assertTrue("S0(GOOD)>S0(HARD)", w[2] > w[1])
        assertTrue("S0(HARD)>S0(AGAIN)", w[1] > w[0])
    }

    // --- equals/hashCode (DoubleArray) ---

    @Test
    fun `two DEFAULT instances are equal`() {
        val p1 = FsrsParameters.DEFAULT
        val p2 = FsrsParameters.DEFAULT
        assertEquals(p1, p2)
    }

    @Test
    fun `two params with same weights are equal`() {
        val weights = DoubleArray(21) { it.toDouble() }
        val p1 = FsrsParameters(weights = weights, requestedRetention = 0.9)
        val p2 = FsrsParameters(weights = weights.copyOf(), requestedRetention = 0.9)
        assertEquals(p1, p2)
    }

    @Test
    fun `params with different weights are not equal`() {
        val w1 = DoubleArray(21) { it.toDouble() }
        val w2 = DoubleArray(21) { it.toDouble() + 1.0 }
        val p1 = FsrsParameters(weights = w1, requestedRetention = 0.9)
        val p2 = FsrsParameters(weights = w2, requestedRetention = 0.9)
        assertFalse(p1 == p2)
    }

    @Test
    fun `equal params have equal hash codes`() {
        val weights = DoubleArray(21) { it.toDouble() }
        val p1 = FsrsParameters(weights = weights.copyOf(), requestedRetention = 0.9)
        val p2 = FsrsParameters(weights = weights.copyOf(), requestedRetention = 0.9)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    // --- constructor validation ---

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects weights array with wrong size`() {
        FsrsParameters(weights = DoubleArray(17), requestedRetention = 0.9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects retention outside 0 to 1`() {
        FsrsParameters(weights = DoubleArray(21), requestedRetention = 1.1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor rejects negative retention`() {
        FsrsParameters(weights = DoubleArray(21), requestedRetention = -0.1)
    }
}
