package tequilacat.org.snake3d.playfeature


import org.junit.Test

import org.junit.Assert.*

class FeedableBodyProportionsTest {

    private val stdRL = doubleArrayOfNumbers(0.1, 0.5, 0.3, 0.8, 0.6, 0.8, 0.8, 0.5, 0.9, 1.5, 1, 0.5)
    private val stdRadiusIndex1 = 1
    private val stdMaxR1dot6 = 1.6

    @Test(expected = IllegalArgumentException::class)
    fun `bad fullRadius index too small`() {
        FeedableProportions(stdRL, 10.0, -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `bad fullRadius index toobig`() {
        FeedableProportions(stdRL, 10.0, 5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `bad fullRadius index noncontinuous`() {
        FeedableProportions(stdRL, 10.0, 2)
    }

    @Test(expected = IllegalStateException::class)
    fun `notinit segmentEndFromTail`() {
        FeedableProportions(stdRL, stdMaxR1dot6, stdRadiusIndex1).segmentEndFromTail(0)
    }

    @Test(expected = IllegalStateException::class)
    fun `notinit segmentRadius`() {
        FeedableProportions(stdRL, stdMaxR1dot6, stdRadiusIndex1).segmentRadius(0)
    }

    @Test
    fun `notinit segmentCount`() {
        val props = FeedableProportions(stdRL, stdMaxR1dot6, stdRadiusIndex1)
        assertEquals(6, props.segmentCount)
        assertEquals(3, props.neckIndex)
    }

    /**
     * check diff aspect of feedable proportions]
     * */
    @Test
    fun `lengthsAndRadiuses within max radius`() {
        // val proportions = FeedableProportions(doubleArrayOfNumbers())
        val props =  FeedableProportions(stdRL, stdMaxR1dot6, stdRadiusIndex1)
        props.resize(0.1)
        // all lengths and radiuses are * 0.1
        assertArrayEquals(
            doubleArrayOfNumbers(0.01, 0.05, 0.03, 0.08, 0.06, 0.08, 0.08, 0.05, 0.09, 0.15, 0.1, 0.05),
            (0..5).flatMap { listOf(props.segmentEndFromTail(it), props.segmentRadius(it)) }.toDoubleArray(),
            testDoubleTolerance
        )
    }

    @Test
    fun `oversized segmentEndFromTail`() {
        // val proportions = FeedableProportions(doubleArrayOfNumbers())
        val props =  FeedableProportions(stdRL, stdMaxR1dot6, stdRadiusIndex1)
        props.resize(4.0)
        // the stable part R = 0.8 (measured at 0.8) where maxR is measured

        val segmentEnds = (0..5).map { props.segmentEndFromTail(it) }.toDoubleArray()
        assertArrayEquals(
            doubleArrayOfNumbers(0.2, 0.6, 4 - 0.8, 4 - 0.4, 4 - 0.2, 4),
            segmentEnds,
            testDoubleTolerance
        )
    }

    @Test
    fun `oversized segmentRadius`() {
        val props =  FeedableProportions(stdRL, stdMaxR1dot6, stdRadiusIndex1)
        props.resize(4.0)
        // maxR: 1.6, so all oversized radiuses are * 2
        // the stable part R = 0.8 (measured at 0.8) where maxR is measured

        assertArrayEquals(
            doubleArrayOfNumbers(1, 1.6, 1.6, 1, 3, 1),
            (0..5).map { props.segmentRadius(it) }.toDoubleArray(),
            testDoubleTolerance
        )
    }


    // TODO test resize resets eaten lumps
}