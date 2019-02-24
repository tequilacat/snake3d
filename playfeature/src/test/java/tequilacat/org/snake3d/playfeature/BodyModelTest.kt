package tequilacat.org.snake3d.playfeature

import io.mockk.unmockkAll
import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import tequilacat.org.snake3d.playfeature.glutils.IDirectedSection
import kotlin.math.*

class BodyModelTest {

    @Before
    fun beforeTests() = mockAndroidStatics()

    @After
    fun afterTests() = unmockkAll()

    // for tests that must fail until they don't test for new segments anymore (to be replaced by NotSegmentedProportions)

    companion object {
        fun initByCoords(radius: Double, headOffset: Double, headRadius: Double, vararg coords: Double): BodyModel {
            //val model = this
            val floorZ = 0.0

            var lastX = coords[0]
            var lastY = coords[1]

            val firstSegmentLen = hypot(coords[2]-coords[0], coords[3] - coords[1])
            val model = BodyModel(TailLenBodyProportions(radius, firstSegmentLen, headOffset, headRadius))

            for (i in 2 until coords.size step 2) {

                val x = coords[i]
                val y = coords[i+1]

                val angle = atan2(y - lastY, x - lastX)
                val length = hypot(y - lastY, x - lastX)

                if(i == 2) {
                    model.init(lastX, lastY, floorZ, angle, length)
                    // never shrink
                    model.feed(1000000.0)
                } else {
                    model.advance(length, angle - model.viewDirection)
                }

                lastX = x
                lastY = y
            }

            return model
        }
    }

    private val TAILLEN_LARGE_100 = 100.0
    /** indicates that no shortening should take place */
    private val FOODLENGTH_LARGE = 100.0
    private val NO_ROTATE = 0.0

    private val tStartX = 5.0
    private val tStartY = 2.0
    private val tStartZ = 1.0
    private val tStartAngle = PI/4
    private val tRadius = 1.0

    private val tHeadOffset = 0.0
    private val tHeadR = 0.0

    private fun assertBodySegments(
        tailLen: Double, initLen: Double,
        expectedLengths: DoubleArray, expectedAngles: DoubleArray,
        advanceLengths: DoubleArray,
        advanceAngles: DoubleArray
    ) {
        val body = BodyModel(
            TailLenBodyProportions(tRadius, tailLen, tHeadOffset, tHeadR)
        )
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, initLen)
                feed(FOODLENGTH_LARGE)
            }

        for (i in 0 until advanceLengths.size) {
            body.advance(advanceLengths[i], advanceAngles[i])
        }
        body.assertSegments(tailLen, expectedLengths, expectedAngles)
    }

    private fun BodyModel.assertSegments(tailLen: Double, expectedLengths: DoubleArray, expectedAngles: DoubleArray,
                                         startX: Double = tStartX, startY: Double = tStartY) {
        val body = this
        val sections = body.bodySections.toList()
        assertEquals("expected lengths differ", expectedLengths.size, sections.size - 1)

        var totalLen = 0.0
        val floorZ = sections[0].centerZ // always at floor

        // test starts exactly as specified
        assertEquals("bad startX", startX.toFloat(), sections[0].centerX, testFloatTolerance)
        assertEquals("bad startY", startY.toFloat(), sections[0].centerY, testFloatTolerance)

        for (i in 0 until expectedLengths.size) {
            val thisSegment = sections[i + 1]
            val prevSection = sections[i]
            val x = prevSection.centerX.toDouble()
            val y = prevSection.centerY.toDouble()

            val expLengh = expectedLengths[i]
            val endR =
                (if (totalLen + expLengh >= tailLen) 1.0 else (totalLen + expLengh) / tailLen) * tRadius
            val expAngle = expectedAngles[i]

            assertSegment("section #$i",
                thisSegment, expAngle,
                x + cos(expAngle) * expLengh, y + sin(expAngle) * expLengh, floorZ + endR,
                endR, expLengh)

            totalLen += expLengh
        }
    }

    private fun assertSegment(
        msg: String,
        section: IDirectedSection,
        alpha: Double,
        x1: Double, y1: Double, z1: Double,
        radius: Double, length: Double) {

        assertEquals("$msg alpha", alpha.toFloat(), section.alpha, testFloatTolerance)

        assertEquals("$msg x", x1.toFloat(), section.centerX, testFloatTolerance)
        assertEquals("$msg y", y1.toFloat(), section.centerY, testFloatTolerance)

        assertEquals("$msg length", length.toFloat(), section.prevLength, testFloatTolerance)
        assertEquals("$msg z", z1.toFloat(), section.centerZ, testFloatTolerance)
        assertEquals("$msg radius", radius.toFloat(), section.radius, testFloatTolerance)
    }

    /**
     * check that next init will reset model to new content
     */
    @Test
    fun reinit() {
        // 2 segments
        val model = BodyModel(NotSegmentedProportions(tRadius, tHeadOffset, tHeadR))// NotImplProportions(2.0, tRadius, tHeadOffset, tHeadR))
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, 3.0)
                advance(1.0, 0.1)
            }

        assertEquals(3, model.bodySections.toList().size)

        // check we've reinited content to shorter sequence with new value
        model.init(0.0,0.0, 0.0, 0.0, 0.1)
        assertEquals(2, model.bodySections.toList().size)
        assertEquals(0.1f, model.bodySections.toList()[1].prevLength, testFloatTolerance)
    }

    @Test
    fun `init straight`() {
        val sections = BodyModel(NotSegmentedProportions(tRadius, tHeadOffset, tHeadR))// NotImplProportions(2.0, tRadius, tHeadOffset, tHeadR))
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, 3.0)
            }.bodySections.toList()

        val lrPairs = sections.flatMap { s -> listOf(s.prevLength, s.radius) }.toFloatArray()
        assertArrayEquals(
            floatArrayOfNumbers(0, 0, 3, tRadius),
            lrPairs, testFloatTolerance
        )
    }

    @Test
    fun `grow short lessThanLen rotated`() {
        val initLen = 0.5
        val len2 = 0.25
        val tailLen = 2.0
        val rotateAngle = 0.5

        assertBodySegments(
            tailLen, initLen,
            doubleArrayOf(initLen, len2), doubleArrayOf(tStartAngle, tStartAngle + rotateAngle),
            doubleArrayOf(len2), doubleArrayOf(rotateAngle)
        )
    }

    /** grow up in same dir to tail length */
    @Test
    fun `grow short eqLen samedir`() {
        val initLen = 0.5
        val len2 = 1.5
        val tailLen = 2.0

        assertBodySegments(
            tailLen = tailLen, initLen = initLen, advanceLengths = doubleArrayOf(len2), advanceAngles = doubleArrayOf(NO_ROTATE),
            expectedLengths = doubleArrayOf(tailLen), expectedAngles = doubleArrayOf(tStartAngle)
        )
    }

    @Test
    fun `grow short eqLen rotated`() {
        val initLen = 0.5
        val len2 = 1.5
        val tailLen = 2.0
        val rotateAngle = 0.5

        assertBodySegments(
            tailLen = tailLen, initLen = initLen, advanceLengths = doubleArrayOf(len2), advanceAngles = doubleArrayOf(rotateAngle),
            expectedLengths = doubleArrayOf(initLen, len2),expectedAngles = doubleArrayOf(tStartAngle, tStartAngle + rotateAngle)
        )
    }


    /**
     * single segment shorter than tail,
     * appended length so summary length exceeding the tail len, under angle:
     * 2 segments: first extended to tail length
     * 2nd is continuation of 1st
     */
    @Test
    fun `grow short overLen samedir`() {
        val tailLen = 2.0
        val initialLen = 1.0
        val len2 = 2.0

        assertBodySegments(
            tailLen = tailLen,
            initLen = initialLen,
            advanceLengths = doubleArrayOf(len2),
            advanceAngles = doubleArrayOf(NO_ROTATE),
            expectedLengths = doubleArrayOf(tailLen, initialLen + len2 - tailLen),
            expectedAngles = doubleArrayOf(tStartAngle, tStartAngle)
        )
    }

    /**
     * Tests now the new "ring" appears
     *
     * single segment shorter than tail,
     * appended length so summary length exceeding the tail len, under angle:
     * 3 segments: first exactly as original part,
     * 2nd is rotated from original part, end radius is full,
     * 3rd is continuatio of 2nd
     */
    @Test
    fun `grow short overLen rotated`() {
        val tailLen = 2.0
        val initLen = 0.5
        val deltaAlpha = 0.5

        assertBodySegments(
            tailLen = tailLen,
            initLen = initLen,
            advanceLengths = doubleArrayOf(2.0),
            advanceAngles = doubleArrayOf(deltaAlpha),
            expectedLengths = doubleArrayOf(initLen, 1.5, 0.5),
            expectedAngles = doubleArrayOf(tStartAngle, tStartAngle + deltaAlpha, tStartAngle + deltaAlpha)
        )
    }



    // how the increase before length keeps direction
    @Test
    fun `grow short lessThanLen samedir`() {
        // short (len less than body tail), grow still less than full length (by 0.5)
        // same direction from short to still shorter: one part

        val initLen = 1.0
        val len2 = 0.5
        val tailLen = 2.0

        BodyModel(TailLenBodyProportions(tRadius, tailLen, tHeadOffset, tHeadR))
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, initLen)
                feed(FOODLENGTH_LARGE)
                advance(len2, NO_ROTATE)
            }.assertSegments(
                tailLen, doubleArrayOf(1.5), doubleArrayOf(tStartAngle)
            )
    }


    @Test
    fun `grow short lessThanLen +3 no rotate`() {
        val tailLen = 1.0
        val initLen = 0.1
        val deltaLen = 0.1

        assertBodySegments(
            tailLen = tailLen,
            initLen = initLen,
            advanceLengths = doubleArrayOf(deltaLen, deltaLen),
            advanceAngles = doubleArrayOf(NO_ROTATE, NO_ROTATE),
            expectedLengths = doubleArrayOf(initLen * 3),
            expectedAngles = doubleArrayOf(tStartAngle))
    }

    @Test
    fun `grow short lessThanLen multiple rotate`() {
        val tailLen = 1.0
        val initLen = 0.1
        val dA1 = 0.1
        val deltaLen = 0.1

        assertBodySegments(
            tailLen = tailLen,
            initLen = initLen,
            advanceLengths = doubleArrayOf(deltaLen, deltaLen),
            advanceAngles = doubleArrayOf(dA1, -dA1),
            expectedLengths = doubleArrayOf(initLen, deltaLen, deltaLen),
            expectedAngles = doubleArrayOf(tStartAngle, tStartAngle + dA1, tStartAngle)
        )
    }

//    @Test(expected = IllegalArgumentException::class)
//    fun `advance too far`() {
//        val tailLen = 1.0
//        val initLen = 0.2
//        val delta = 2.0
//        BodyModel(tailLen, tRadius)
//            .apply {
//                init(tStartX, tStartY, tStartZ, tStartAngle, initLen)
//                advance(delta, NO_ROTATE, delta + 0.1)
//            }
//    }

    @Test
    fun `advance short far forward`() {
        val tailLen = 1.0
        val initLen = 0.2
        val delta = 2.0

        BodyModel(TailLenBodyProportions(tRadius, tailLen, tHeadOffset, tHeadR))
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, initLen)
                advance(delta, NO_ROTATE)
            }.assertSegments(tailLen, doubleArrayOf(initLen),
                doubleArrayOf(tStartAngle),
                tStartX + delta*cos(tStartAngle), tStartY + delta*cos(tStartAngle)
                )
    }

    @Test
    fun `advance exact length`() {
        val tailLen = 1.0
        val initLen = 0.1
        val delta = 0.1

        BodyModel(TailLenBodyProportions(tRadius, tailLen, tHeadOffset, tHeadR))
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, initLen)
                // make 2 segments: advance without shorten, 2 segments of L=0.1
                feed(delta) // must leave start pos at the sstart
                advance(delta, PI/2)
                // now continue straight, subtracting exact delta from tail:
                //   food is over, move end forward
                advance(delta, 0.0)

            }.assertSegments(tailLen,
                doubleArrayOf(initLen * 2),
                doubleArrayOf(tStartAngle + PI/2),
                tStartX + delta*cos(tStartAngle),
                tStartY + delta*cos(tStartAngle)
            )
    }


    @Test
    fun `advance short across taillength`() {
        val tailLen = 1.0
        val initLen = 0.3
        val delta = 0.8

        // 2 fragments
        BodyModel(TailLenBodyProportions(tRadius, tailLen, tHeadOffset, tHeadR))
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, initLen)
                advance(delta, NO_ROTATE)
            }.assertSegments(tailLen, doubleArrayOf(0.3),
                doubleArrayOf(tStartAngle),
                tStartX + delta*cos(tStartAngle), tStartY + delta*cos(tStartAngle)
            )
    }

    @Test
    fun `head coordinates`() {
        val bodyLength = 10.0
        val angle = PI / 3
        val startX = 2.0
        val startY = 4.0
        val offset = 0.5

        val body = BodyModel(NotSegmentedProportions(tRadius, offset, 2.0))
            .apply {
            init(startX, startY, 0.0, angle, bodyLength)
        }

        assertEquals((startX + (bodyLength+ offset)*cos(angle)).toFloat(), body.headX, testFloatTolerance)
        assertEquals((startY + (bodyLength+ offset)*sin(angle)).toFloat(), body.headY, testFloatTolerance)
        assertEquals(angle.toFloat(), body.viewDirection, testFloatTolerance)
    }

    ////////////////////////////////////
    //  Test Feeding

    /**
     * advance bigger than feeding length
     * */
    @Test
    fun `feed small advance greater`() {
        val bodyLength = 10.0
        val feed = 1.0
        val advance = 3.0
        val angle = 0.0// tStartAngle

        // test on single segment
        val body = BodyModel(TailLenBodyProportions(tRadius, TAILLEN_LARGE_100, 1.0, 2.0))
            .apply { init(tStartX, tStartY, tStartZ, angle, bodyLength) }

        body.feed(feed)
        body.advance(advance, NO_ROTATE)
        body.assertSegments(TAILLEN_LARGE_100, doubleArrayOf(bodyLength + feed), doubleArrayOf(angle),
            tStartX + (advance - feed) * cos(angle),
            tStartY + (advance - feed) * sin(angle)
            )
    }

    @Test
    fun `feed small advance exact`() {
        val bodyLength = 10.0
        val feed = 1.0
        val advance = 1.0

        // test on single segment
        val body = BodyModel(TailLenBodyProportions(tRadius, TAILLEN_LARGE_100, 1.0, 2.0)).apply {
            init(tStartX, tStartY, tStartZ, tStartAngle, bodyLength)
        }

        body.feed(feed)
        body.advance(advance, NO_ROTATE)
        body.assertSegments(TAILLEN_LARGE_100, doubleArrayOf(bodyLength + feed), doubleArrayOf(tStartAngle),
            tStartX + (advance - feed) * cos(tStartAngle),
            tStartY + (advance - feed) * sin(tStartAngle)
        )
    }

    @Test
    fun `feed small advance less multiple`() {
        val bodyLength = 10.0
        val feed = 1.5
        val advance = 1.0

        // test on single segment
        val body = BodyModel(TailLenBodyProportions(tRadius, TAILLEN_LARGE_100, 1.0, 2.0))
            .apply {
            init(tStartX, tStartY, tStartZ, tStartAngle, bodyLength)
        }

        body.feed(feed)

        // stay, increase body length
        body.advance(advance, NO_ROTATE)
        body.assertSegments(TAILLEN_LARGE_100, doubleArrayOf(bodyLength + advance), doubleArrayOf(tStartAngle),
            tStartX, tStartY)

        // moved forward really 0.5 (exhausted internal food)
        body.advance(advance, NO_ROTATE)
        body.assertSegments(TAILLEN_LARGE_100, doubleArrayOf(bodyLength + feed), doubleArrayOf(tStartAngle),
            tStartX + (advance * 2 - feed) * cos(tStartAngle),
            tStartY + (advance * 2 - feed) * sin(tStartAngle)
        )

        // moved 3, feed 1.5 , remains 1.5
        body.advance(advance, NO_ROTATE)
        body.assertSegments(TAILLEN_LARGE_100, doubleArrayOf(bodyLength + feed), doubleArrayOf(tStartAngle),
            tStartX + (advance * 3 - feed) * cos(tStartAngle),
            tStartY + (advance * 3 - feed) * sin(tStartAngle)
        )
    }

    // init after food will reset the counter
    @Test
    fun `feed init resets`() {
        val bodyLength = 10.0

        // test on single segment
        val body = BodyModel(TailLenBodyProportions(tRadius, TAILLEN_LARGE_100, 1.0, 2.0))
            .apply { init(tStartX, tStartY, tStartZ, tStartAngle, bodyLength) }

        body.feed(1.5)
        body.init(tStartX, tStartY, tStartZ, tStartAngle, bodyLength)
        // as if feeding 0 - no increase in length
        body.advance(1.0, NO_ROTATE)
        assertEquals(bodyLength.toFloat(), body.bodySections.toList()[1].prevLength, testFloatTolerance)
    }

    @Test
    fun `feed append`() {
        val bodyLength = 10.0
        val advance = 10.0

        // test on single segment
        val body = BodyModel(TailLenBodyProportions(tRadius, TAILLEN_LARGE_100, 1.0, 2.0))
            .apply { init(tStartX, tStartY, tStartZ, tStartAngle, bodyLength) }

        body.feed(1.0)
        body.feed(2.0)
        body.advance(advance, NO_ROTATE)
        // far advance adds at once, will sum up
        assertEquals((bodyLength + 3.0).toFloat(), body.bodySections.toList()[1].prevLength, testFloatTolerance)
    }

}
