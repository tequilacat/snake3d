package tequilacat.org.snake3d.playfeature

import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.PI

/**
 * tests how generated body sections correspond to IBodyProportions used to build the body
 */
class BodyModelShapingTest {
    private val tStartX = 5.0
    private val tStartY = 2.0
    private val tStartZ = 1.0
    private val tStartAngle = PI /4

    @Before
    fun beforeTests() = mockAndroidStatics(true)

    @After
    fun afterTests() = unmockkAll()

    /**
     * Test both
     * append of single piece
     * and shaping by norestrictive shaper (not segmented proportions).
     * Single segment rotation body that fits within straight line, generates 1 segment
     * */
    @Test
    fun `no division 1 segment`() {
        val radius = 1.5

        // does not affect length whatever segment count of original body
        val sections = BodyModel(NotSegmentedProportions(radius)).apply {
            init(tStartX, tStartY, tStartZ, tStartAngle, 100.0)
        }.bodySections.toList()

        val lrPairs = sections.drop(1).toList().flatMap { s -> listOf(s.prevLength, s.radius) }.toFloatArray()
        assertArrayEquals(floatArrayOf(100f, radius.toFloat()), lrPairs, testFloatTolerance)
        assertArrayEquals(floatArrayOfNumbers(tStartAngle, tStartAngle),
            sections.map { it -> it.alpha }.toFloatArray(), testFloatTolerance
        )
    }

    /**
     * Test both append and shaping by norestrictive shaper (not segmented proportions).
     * Several segments for single shape segment body are not changed */
    @Test
    fun `no division multisegment`() {
        val radius = 1.5f

        // does not affect length whatever segment count of original body
        val sections = BodyModel(NotSegmentedProportions(radius.toDouble())).apply {
            init(tStartX, tStartY, tStartZ, tStartAngle, 10.0)
            feed(1000.0) // just add without shortening
            advance(5.0, 0.1)
            advance(5.0, -0.1)
        }.bodySections.toList()

        // totally 3 segments, radiuses and lengths distributed accordingly
        val lrPairs = sections.drop(1).toList().flatMap { s -> listOf(s.prevLength, s.radius) }.toFloatArray()
        assertArrayEquals(floatArrayOf(10f, radius * 0.5f, 5f, radius * 0.75f, 5f, radius),
            lrPairs, testFloatTolerance)
    }

    /** straight segment is split onto 4 segments of different end radiuses */
    @Test
    fun `split single segment onto multiple`() {
        val shaper = ArrayProportions(0.5, 1, 0.7, 1, 0.8, 0.5, 1, 0.8)
        val sections = BodyModel(shaper).apply {
            init(tStartX, tStartY, tStartZ, tStartAngle, 10.0)
        }.bodySections

        val lrPairs = sections.drop(1).toList().flatMap { s -> listOf(s.prevLength, s.radius) }.toFloatArray()
        assertArrayEquals(floatArrayOfNumbers(5, 1, 2, 1, 1, 0.5, 2, 0.8),
            lrPairs, testFloatTolerance)
    }

    /**
     * when after expanding the proportions of body are exactly as specified by shaper -
     *   no segments are split.
     * only radiuses must change but the number and size of segments should stay the same
     */
    @Test
    fun `reshape on existing break - no split`() {
        val radius = 1.5f
        val facade = FacadeProportions(NotSegmentedProportions(radius.toDouble()))
        val body = BodyModel(facade).apply {
            init(tStartX, tStartY, tStartZ, tStartAngle, 1.0)
            feed(1000.0) // just add without shortening
        }

        facade.backer = ArrayProportions(0.25, 1.1, 1, 0.5)
        body.advance(3.0, 0.01)

        // see its same sizes 1, 3 with radiuses 1.1, 0.5
        val lrPairs = body.bodySections.drop(1).toList().flatMap { s -> listOf(s.prevLength, s.radius) }.toFloatArray()
        assertArrayEquals(floatArrayOfNumbers(1, 1.1, 3, 0.5), lrPairs, testFloatTolerance)
    }

    @Test
    fun `split last segment`() {
        val radius = 1.5f

        val facade = FacadeProportions(NotSegmentedProportions(radius.toDouble()))
        val body = BodyModel(facade).apply {
            init(tStartX, tStartY, tStartZ, tStartAngle, 2.0)
            feed(1000.0) // just add without shortening
        }

        // [2, 8] split at 0.8 should make 3 segments: 2[r=0.25], 8[r=1], 1[r=0.5]
        facade.backer = ArrayProportions(0.8, 1, 1, 0.5)
        body.advance(8.0, 0.01) // add another segment

        val lrPairs = body.bodySections.drop(1).toList().flatMap { s -> listOf(s.prevLength, s.radius) }.toFloatArray()
        assertArrayEquals(
            floatArrayOfNumbers(2, 0.25, 6, 1, 2, 0.5),
            lrPairs, testFloatTolerance )
    }

    /**
     * Tests error case with small difference in lengths that led to zero length/radius extra segment
     */
    @Test
    fun `test against zero radius last segment`() {
        val hOffset = 10.0
        val sections = BodyModelTest.initByCoords(
            1.0, hOffset, 2.0,
            50.0, 10.0, 50.0, 56.0, 100.0, 55.0
        ).bodySections.toList()

        assertArrayEquals(
            doubleArrayOf(0.0, 1.0, 1.0),
            sections.map { s -> s.dRadius }.toDoubleArray(),
            testDoubleTolerance)
    }

}