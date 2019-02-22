package tequilacat.org.snake3d.playfeature

import org.junit.Test

import org.junit.Assert.*

class BodyProportionsTest {

    /** old style: specified with taillen */
    @Test
    fun `RadiusLength no neck`() {
        val noneck = TailLenBodyProportions(1.0, 2.0, 0.0, 1.0) as IBodyProportions
        // for undersized it's linearly radiused 1 segment
        noneck.resize(1.0)
        assertEquals(1, noneck.segmentCount)
        assertEquals(1.0, noneck.segmentEndFromTail(0), testDoubleTolerance)
        assertEquals(0.5, noneck.segmentRadius(0), testDoubleTolerance)

        // exact size - still one segment having one radius
        noneck.resize(2.0)
        assertEquals(1, noneck.segmentCount)
        assertEquals(2.0, noneck.segmentEndFromTail(0), testDoubleTolerance)
        assertEquals(1.0, noneck.segmentRadius(0), testDoubleTolerance)

        // for oversized, when tail becomes body the max R and then maxR too
        noneck.resize(4.0)
        assertEquals(2, noneck.segmentCount)
        assertEquals(2.0, noneck.segmentEndFromTail(0), testDoubleTolerance)
        assertEquals(1.0, noneck.segmentRadius(0), testDoubleTolerance)

        assertEquals(4.0, noneck.segmentEndFromTail(1), testDoubleTolerance)
        assertEquals(1.0, noneck.segmentRadius(1), testDoubleTolerance)
    }
}