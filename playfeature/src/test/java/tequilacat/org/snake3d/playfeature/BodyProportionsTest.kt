package tequilacat.org.snake3d.playfeature

import org.junit.Test

import org.junit.Assert.*

class BodyProportionsTest {
    private val props = BodyProportions(1.0, 10.0,
        3.0, 2.0, 0.8)

    @Test
    fun effectiveMaxRadius() {
        assertEquals(0.0, props.effectiveMaxRadius(0.0), testDoubleTolerance)
        assertEquals(0.5, props.effectiveMaxRadius(5.0), testDoubleTolerance)
        assertEquals(1.0, props.effectiveMaxRadius(10.0), testDoubleTolerance)
        assertEquals(1.0, props.effectiveMaxRadius(12.0), testDoubleTolerance)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `effectiveMaxRadius bad length`() {
        props.effectiveMaxRadius(-1.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `findRadius bad args 1`() {
        props.findRadius(-1.0, 10.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `findRadius bad args 2`() {
        props.findRadius(11.0, 10.0)
    }

    @Test
    fun `findRadius still growing`() {
        val curLen = 5.0
        val R = 0.5
        // tailpoint
        assertEquals(0.0, props.findRadius(0.0, curLen), testDoubleTolerance)

        // tail center - r = R / 2
        assertEquals(R / 2, props.findRadius(0.15 * curLen, curLen), testDoubleTolerance)

        // tail to body: r = R
        assertEquals(R, props.findRadius(0.3 * curLen, curLen), testDoubleTolerance)

        // body center: r = R
        assertEquals(R, props.findRadius(0.5 * curLen, curLen), testDoubleTolerance)

        // body to neck: r = R
        assertEquals(R, props.findRadius(0.8 * curLen, curLen), testDoubleTolerance)

        // neck center: mid between R and R*faceRRatio
        assertEquals((R + R * 0.8) / 2, props.findRadius(0.9 * curLen, curLen), testDoubleTolerance)

        // face ring
        assertEquals(R * 0.8, props.findRadius(curLen, curLen), testDoubleTolerance)
    }

    @Test
    fun `findRadius oversized`() {
        val curLen = 15.0
        val R = 1.0
        // tailpoint
        assertEquals(0.0, props.findRadius(0.0, curLen), testDoubleTolerance)

        // tail center - r = effR/2
        assertEquals(R / 2, props.findRadius(3*R/2, curLen), testDoubleTolerance)

        // tail to body: r = effR
        assertEquals(R, props.findRadius(3*R, curLen), testDoubleTolerance)

        // body center: r = R
        assertEquals(R, props.findRadius(0.5 * curLen, curLen), testDoubleTolerance)

        // body to neck: r = R
        assertEquals(R, props.findRadius(curLen - 2 * R, curLen), testDoubleTolerance)

        // neck center (Lneck = 2R, so distance = curLen - R
        assertEquals((R + R * 0.8) / 2, props.findRadius(curLen - R, curLen), testDoubleTolerance)

        // face ring
        assertEquals(R * 0.8, props.findRadius(curLen, curLen), testDoubleTolerance)
    }
}