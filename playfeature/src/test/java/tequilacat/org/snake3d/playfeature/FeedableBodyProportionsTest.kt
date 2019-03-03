package tequilacat.org.snake3d.playfeature


import org.junit.Test

import org.junit.Assert.*

class FeedableBodyProportionsTest {

    /**
     * RL = (0.1, 0.5), (0.3, 0.8), (0.6, 0.8), (0.8, 0.5), (0.9, 1.5), (1, 0.5)
     * */
    private val stdRL = doubleArrayOfNumbers(0.1, 0.5, 0.3, 0.8, 0.6, 0.8, 0.8, 0.5, 0.9, 1.5, 1, 0.5)
    private val stdRadiusIndex1 = 1
    private val stdMaxR1dot6 = 1.6
    private val defaultFeedRatio_is_1 = 1.0 // radius

    @Test(expected = IllegalArgumentException::class)
    fun `bad fullRadius index too small`() {
        FeedableProportions(stdRL, 10.0, -1, defaultFeedRatio_is_1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `bad fullRadius index toobig`() {
        FeedableProportions(stdRL, 10.0, 5, defaultFeedRatio_is_1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `bad fullRadius index noncontinuous`() {
        FeedableProportions(stdRL, 10.0, 2, defaultFeedRatio_is_1)
    }

    @Test
    fun `notinit segmentCount`() {
        val props = FeedableProportions(stdRL, stdMaxR1dot6, stdRadiusIndex1, defaultFeedRatio_is_1)
        assertEquals(6, props.segmentCount)
        assertEquals(3, props.neckIndex)
    }

    /**
     * check diff aspect of feedable proportions]
     * */
    @Test
    fun `lengthsAndRadiuses within max radius`() {
        // val proportions = FeedableProportions(doubleArrayOfNumbers())
        val props =  FeedableProportions(stdRL, stdMaxR1dot6, stdRadiusIndex1, defaultFeedRatio_is_1)
        props.resize(0.1)
        // all lengths and radiuses are * 0.1
        assertArrayEquals(
            doubleArrayOfNumbers(0.01, 0.05, 0.03, 0.08, 0.06, 0.08, 0.08, 0.05, 0.09, 0.15, 0.1, 0.05),
            (0..5).flatMap { listOf(props.segmentEndFromTail(it), props.segmentRadius(it)) }.toDoubleArray(),
            testDoubleTolerance
        )

        assertEquals(0.1, props.fullLength, testDoubleTolerance)
        // test only once since it's trivial
        assertEquals(0.08, props.lengthToNeck, testDoubleTolerance)
    }

    @Test
    fun `oversized segmentEndFromTail`() {
        // val proportions = FeedableProportions(doubleArrayOfNumbers())
        val props =  FeedableProportions(stdRL, stdMaxR1dot6, stdRadiusIndex1, defaultFeedRatio_is_1)
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
        val props =  FeedableProportions(stdRL, stdMaxR1dot6, stdRadiusIndex1, defaultFeedRatio_is_1)
        props.resize(4.0)
        // maxR: 1.6, so all oversized radiuses are * 2
        // the stable part R = 0.8 (measured at 0.8) where maxR is measured

        assertArrayEquals(
            doubleArrayOfNumbers(1, 1.6, 1.6, 1, 3, 1),
            (0..5).map { props.segmentRadius(it) }.toDoubleArray(),
            testDoubleTolerance
        )
    }

    @Test
    fun `advance without feeding`() {
        val props =  FeedableProportions(stdRL, stdMaxR1dot6, stdRadiusIndex1, defaultFeedRatio_is_1)
        props.resize(0.1)
        props.advance(0.05)

        assertArrayEquals(
            doubleArrayOfNumbers(0.01, 0.05, 0.03, 0.08, 0.06, 0.08, 0.08, 0.05, 0.09, 0.15, 0.1, 0.05),
            (0..5).flatMap { listOf(props.segmentEndFromTail(it), props.segmentRadius(it)) }.toDoubleArray(),
            testDoubleTolerance
        )

        assertEquals(0.1, props.fullLength, testDoubleTolerance)
        // test only once since it's trivial
        assertEquals(0.08, props.lengthToNeck, testDoubleTolerance)
    }

    private fun IFeedableBodyProportions.toLR() = (0 until this.segmentCount)
            .flatMap { i -> listOf(this.segmentEndFromTail(i), this.segmentRadius(i)) }
            .toDoubleArray()
    private fun IFeedableBodyProportions.toL() = (0 until this.segmentCount)
        .map { i -> this.segmentEndFromTail(i) }
        .toDoubleArray()

    @Test
    fun `feed and advance`() {
        val feedBy = 1.0 // always relative to current fullRadius
        val bodyRL = doubleArrayOf(0.3, 0.1, 0.8, 0.1, 0.9, 0.2, 1.0, 0.1)
        val maxR = 1.0 // max size reach at

        val props =  FeedableProportions(bodyRL, maxR, 0, feedBy)
        props.resize(10.0) // R exactly up to max

        // advance less than feed: increase
        props.feed() // by 1* curR = 1
        props.advance(0.4) // len+= 0.4 , kept 0.6 feed for future

        assertArrayEquals(doubleArrayOfNumbers(3, 1, 8.4, 1, 9.4, 2, 10.4, 1),
            props.toLR(), testDoubleTolerance)
        assertEquals(10.4, props.fullLength, testDoubleTolerance)

        // next test: having kept advance 0.6, advance by 1.0 - len += 0.4 (advancement exhausted)
        props.advance(1.1) // len += 0.6
        assertArrayEquals(doubleArrayOfNumbers(3, 1, 9, 1, 10, 2, 11, 1),
            props.toLR(), testDoubleTolerance)
        assertEquals(11.0, props.fullLength, testDoubleTolerance)
    }

    @Test
    fun `feed and advance under maxlen recompute ratio`() {
        val feedBy = 1.0 // always relative to current fullRadius
        val bodyRL = doubleArrayOf(0.3, 0.1, 0.8, 0.1, 0.9, 0.2, 1.0, 0.1)
        val maxR = 1.0 // max size reach at
        val props =  FeedableProportions(bodyRL, maxR, 0, feedBy)
        props.resize(5.0) // r = 0.5, half to maxR

        props.feed() // feed len = 1 * effR = 0.5
        props.advance(1.0)
        // advance() must recompute length AND RATIO so the R and L are computed from new sizes
        // the size is increased by food added in .feed()
        val newLength = 5.5
        assertEquals(newLength, props.fullLength, testDoubleTolerance)

        // and L and R are computed from this new length now:
        // compare full set of L and only first R since further Radiuses will be modified during feed
        assertEquals(0.1 * newLength, props.segmentRadius(0), testDoubleTolerance)
        assertArrayEquals(
            doubleArrayOfNumbers(0.3 * newLength, 0.8 * newLength, 0.9 * newLength, 1 * newLength),
            props.toL(), testDoubleTolerance)
    }

    /** multiple feeds append the */
    @Test
    fun `feed multiple`() {
        val feedBy = 1.0 // always relative to current fullRadius
        val bodyRL = doubleArrayOf(0.3, 0.1, 0.8, 0.1, 0.9, 0.2, 1.0, 0.1)
        val maxR = 1.0 // max size reach at

        val props =  FeedableProportions(bodyRL, maxR, 0, feedBy)
        props.resize(10.0) // R exactly up to max

        props.feed()
        props.feed()
        props.advance(2.5) // must increase by 2
        assertEquals(12.0, props.fullLength, testDoubleTolerance)
    }


    @Test
    fun `resize resets fed`() {
        val props =  FeedableProportions(doubleArrayOf(0.3, 0.1, 0.8, 0.1, 0.9, 0.2, 1.0, 0.1),
            1.0, 0, 1.0)
        props.resize(10.0) // R exactly up to max
        props.feed()
        props.resize(10.0) // R exactly up to max
        props.advance(1.0)
        // in spite of feeding, resize resets fed counter - length stays same after advance
        assertEquals(10.0, props.fullLength, testDoubleTolerance)
    }

}