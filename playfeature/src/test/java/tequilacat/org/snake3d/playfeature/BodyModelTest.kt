package tequilacat.org.snake3d.playfeature

import org.junit.Test

import org.junit.Assert.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BodyModelTest {

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
        val body = BodyModel(tailLen, tRadius, tHeadOffset, tHeadR)
            .apply { init(tStartX, tStartY, tStartZ, tStartAngle, initLen) }

        for (i in 0 until advanceLengths.size) {
            body.advance(advanceLengths[i], advanceAngles[i], 0.0)
        }
        body.assertBodySegments(tailLen, expectedLengths, expectedAngles)
    }

    private fun BodyModel.assertBodySegments(tailLen: Double, expectedLengths: DoubleArray, expectedAngles: DoubleArray,
                                             startX: Double = tStartX, startY: Double = tStartY) {
        val body = this
        val segments = body.bodySegments.toList()
        assertEquals(body.segmentCount, segments.size)
        assertEquals(expectedLengths.size, body.segmentCount)

        var totalLen = 0.0
        val floorZ = segments[0].startZ // always at floor

        for (i in 0 until expectedLengths.size) {
            val x = if (i == 0) startX else segments[i - 1].endX.toDouble()
            val y = if (i == 0) startY else segments[i - 1].endY.toDouble()

            val startR = when {
                (i == 0) -> 0.0
                totalLen >= tailLen -> tRadius
                else -> (totalLen / tailLen) * tRadius
            }
            val expLengh = expectedLengths[i]
            val endR =
                (if (totalLen + expLengh >= tailLen) 1.0 else (totalLen + expLengh) / tailLen) * tRadius
            val expAngle = expectedAngles[i]

            assertSegment("[$i]",
                segments[i], expAngle, x, y, floorZ + startR,
                x + cos(expAngle) * expLengh, y + sin(expAngle) * expLengh, floorZ + endR,
                startR, endR, expLengh)

            totalLen += expLengh
        }
    }

    private fun assertSegment(
        msg: String,
        segment: IBodySegmentModel,
        alpha: Double,
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        startR: Double, endR: Double, length: Double) {

        assertEquals(msg, alpha.toFloat(), segment.alpha, testFloatTolerance)

        assertEquals(msg, x0.toFloat(), segment.startX, testFloatTolerance)
        assertEquals(msg, y0.toFloat(), segment.startY, testFloatTolerance)
        assertEquals(msg, z0.toFloat(), segment.startZ, testFloatTolerance)

        assertEquals(msg, x1.toFloat(), segment.endX, testFloatTolerance)
        assertEquals(msg, y1.toFloat(), segment.endY, testFloatTolerance)
        assertEquals(msg, z1.toFloat(), segment.endZ, testFloatTolerance)

        assertEquals(msg, startR.toFloat(), segment.startRadius, testFloatTolerance)
        assertEquals(msg, endR.toFloat(), segment.endRadius, testFloatTolerance)
        assertEquals(msg, length.toFloat(), segment.length, testFloatTolerance)
    }

    @Test
    fun `init segments short`() {
        // shorter than tail
        val tailLen = 2.0
        val initLen = 1.0
        BodyModel(tailLen, tRadius, tHeadOffset, tHeadR)
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, initLen)
            }.assertBodySegments(
                tailLen = 2.0, expectedLengths = doubleArrayOf(1.0), expectedAngles = doubleArrayOf(tStartAngle)
            )
    }

    @Test
    fun `init segments long`() {
        // original length is longer than tail = 2 segments
        val initLen = 1.5
        val len2 = 0.5
        val tailLen = 1.0

        BodyModel(tailLen, tRadius, tHeadOffset, tHeadR)
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, initLen)
            }.assertBodySegments(tailLen, doubleArrayOf(tailLen, len2),
                doubleArrayOf(tStartAngle, tStartAngle))
    }

    /**
     * check that next init will reset model to new content
     */
    @Test
    fun reinit() {
        // 2 segments
        val model = BodyModel(tailLength = 2.0, radius = tRadius, headOffset = tHeadOffset, headRadius = tHeadR)
            .apply { init(tStartX, tStartY, tStartZ, tStartAngle, 3.0) }

        assertEquals(2, model.segmentCount)

        // check we've reinited content to shorter sequence with new value
        model.init(0.0,0.0, 0.0, 0.0, 0.1)
        assertEquals(1, model.segmentCount)
        assertEquals(0.1f, model.bodySegments.first().length, testFloatTolerance)
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

        BodyModel(tailLen, tRadius, tHeadOffset, tHeadR)
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, initLen)
                advance(len2, NO_ROTATE, 0.0)
            }.assertBodySegments(
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

        BodyModel(tailLen, tRadius, tHeadOffset, tHeadR)
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, initLen)
                advance(delta, NO_ROTATE, delta)
            }.assertBodySegments(tailLen, doubleArrayOf(initLen),
                doubleArrayOf(tStartAngle),
                tStartX + delta*cos(tStartAngle), tStartY + delta*cos(tStartAngle)
                )
    }

    @Test
    fun `advance short across taillength`() {
        val tailLen = 1.0
        val initLen = 0.3
        val delta = 0.8

        // 2 fragments
        BodyModel(tailLen, tRadius, tHeadOffset, tHeadR)
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, initLen)
                advance(delta, NO_ROTATE, delta)
            }.assertBodySegments(tailLen, doubleArrayOf(0.3),
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

        val body = BodyModel(1.0, 1.0, offset, 2.0).apply {
            init(startX, startY, 0.0, angle, bodyLength)
        }

        assertEquals((startX + (bodyLength+ offset)*cos(angle)).toFloat(), body.headX, testFloatTolerance)
        assertEquals((startY + (bodyLength+ offset)*sin(angle)).toFloat(), body.headY, testFloatTolerance)
        assertEquals(angle.toFloat(), body.viewDirection, testFloatTolerance)
    }

}