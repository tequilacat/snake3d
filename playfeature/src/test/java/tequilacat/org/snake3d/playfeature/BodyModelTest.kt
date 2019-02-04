package tequilacat.org.snake3d.playfeature

import org.junit.Test

import org.junit.Assert.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BodyModelTest {
    private val tStartX = 5.0
    private val tStartY = 2.0
    private val tStartZ = 1.0
    private val tStartAngle = PI/4
    private val tStartBeta = 0.0
    private val tRadius = 1.0
    private val sin45 = sin(PI/4).toFloat()

    private fun newModel(tailLen: Double, initialLen: Double) =
        BodyModel(tailLen, tRadius)
            .apply { init(tStartX, tStartY, tStartZ, tStartAngle, tStartBeta, initialLen) }

    // TODO test full 3d: Z coords too
    private fun assertSegment(
        segment: IBodySegment,
        alpha: Double, beta: Double,
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        endR: Double, length: Double) {

        assertEquals(alpha.toFloat(), segment.alpha, testFloatTolerance)
        assertEquals(beta.toFloat(), segment.beta, testFloatTolerance)

        assertEquals(x0.toFloat(), segment.startX, testFloatTolerance)
        assertEquals(y0.toFloat(), segment.startY, testFloatTolerance)
        // TODO implement Z check
//        assertEquals(z0.toFloat(), segment.startZ, testFloatTolerance)

        assertEquals(x1.toFloat(), segment.endX, testFloatTolerance)
        assertEquals(y1.toFloat(), segment.endY, testFloatTolerance)
        // TODO implement lastZ check
//        assertEquals(z1.toFloat(), segment.endZ, testFloatTolerance)

        // TODO implement end radius
//        assertEquals(endR.toFloat(), segment.endRadius, testFloatTolerance)
        assertEquals(length.toFloat(), segment.length, testFloatTolerance)
    }

    /**
     * creates a 1-segment body of exact tail length, with specified end radius
     */
    @Test
    fun `create 1segment exact tail`() {
        val totalLen = 2.0
        val tailLen = 2.0
        val segments = newModel(tailLen, totalLen).bodySegments

        assertEquals(1, segments.size)

        assertSegment(segments.first(), tStartAngle, tStartBeta,
            tStartX, tStartY, tStartZ,
            tStartX + totalLen * sin45, tStartY + totalLen * sin45, tStartZ.toFloat() + tRadius,
            tRadius, totalLen)
    }

    /**
     * creates a 1-segment body of tail longer than total length, with specified end radius -
     * truncated at length, radious is less that full radius
     */
    @Test
    fun `create 1segment shorter than tail`() {
        val totalLen = 1.0
        val tailLen = 2.0
        val segments = newModel(tailLen, totalLen).bodySegments

        assertEquals(1, segments.size)

        assertSegment(segments.first(), tStartAngle, tStartBeta, 
            tStartX, tStartY, tStartZ,
            tStartX + totalLen * sin45, tStartY + totalLen * sin45, tStartZ.toFloat() + tRadius / 2,
            tRadius/2, totalLen)
    }

    /**
     * full length greater than tail so 2 segments are created, the angle is same
     */
    @Test
    fun `create 2segments`() {
        val totalLen = 3.0
        val tailLen = 2.0
        val segments = newModel(tailLen, totalLen).bodySegments

        assertEquals(2, segments.size)

        // first ends at len = tailLen
        val end1 = doubleArrayOf(tStartX + tailLen * sin45, tStartY + tailLen * sin45, tStartZ.toFloat() + tRadius)

        val iter = segments.iterator()
        assertSegment(
            iter.next(), tStartAngle, tStartBeta, tStartX, tStartY, tStartZ,
            end1[0], end1[1], end1[2], tRadius, tailLen)

        val len2 = totalLen - tailLen
        assertSegment(
            iter.next(), tStartAngle, tStartBeta, end1[0], end1[1], end1[2],
            end1[0] + len2 * sin45, end1[1] + len2 * sin45, end1[2],
            tRadius, len2)
    }

    /**
     * check that next init will reset model to new content
     */
    @Test
    fun reinit() {
        // 2 segm
        val model = newModel(2.0, 3.0)
        assertEquals(2, model.bodySegments.size)

        // check we've reinited content to shorter sequence with new value
        model.init(0.0,0.0, 0.0, 0.0, 0.0,0.1)
        assertEquals(1, model.bodySegments.size)
        assertEquals(0.1f, model.bodySegments.first().length, testFloatTolerance)
    }

    /*
    advance 1
    segmentsequences: short, equal, large
    rotated/samedir defines whether last seqment is rotated or not
    */

    @Test
    fun `grow short lessThanLen samedir`() {
        // short (len less than body tail), grow still less than full length (by 0.5)
        val segments = newModel(tailLen = 2.0, initialLen = 1.0).apply {
            advance(0.5, 0.0, 0.0)
        }.bodySegments

        assertEquals(1, segments.size)

        val newLen = 1.5
        assertSegment(segments.first(), tStartAngle, tStartBeta,
            tStartX, tStartY, tStartZ,
            tStartX + newLen * sin45, tStartY + newLen * sin45, tStartZ.toFloat() + tRadius * 0.75,
            tRadius * 0.75, newLen)
    }

    @Test
    fun `grow short lessThanLen rotated`() {
        val segments = newModel(tailLen = 2.0, initialLen = 1.0).apply {
            advance(0.5, 0.5, 0.0)
        }.bodySegments.toList()

        assertEquals(2, segments.size)
        // assert length
        assertEquals(tStartAngle.toFloat(), segments[0].alpha, testFloatTolerance)
        assertEquals(1.0f, segments[0].length, testFloatTolerance)

        assertEquals(tStartAngle.toFloat() + 0.5f, segments[1].alpha, testFloatTolerance)
        assertEquals(0.5f, segments[1].length, testFloatTolerance)
    }



    @Test
    fun `grow short eqLen samedir`() {
        // short (len less than body tail), grow exactly to full length (by 1)
        val segments = newModel(tailLen = 2.0, initialLen = 1.0).apply {
            advance(1.0, 0.0, 0.0)
        }.bodySegments

        assertEquals(1, segments.size)

        val newLen = 2.0
        assertSegment(segments.first(), tStartAngle, tStartBeta,
            tStartX, tStartY, tStartZ,
            tStartX + newLen * sin45, tStartY + newLen * sin45, tStartZ.toFloat() + tRadius / 2,
            tRadius / 2, newLen)
    }

    @Test
    fun `grow short eqLen rotated`() {
        // short (len less than body tail), grow exactly to full length (by 1)
        val segments = newModel(tailLen = 2.0, initialLen = 1.0).apply {
            advance(1.0, 0.5, 0.0)
        }.bodySegments.toList()

        assertEquals(2, segments.size)
        // assert length
        assertEquals(tStartAngle.toFloat(), segments[0].alpha, testFloatTolerance)
        assertEquals(1.0f, segments[0].length, testFloatTolerance)

        assertEquals(tStartAngle.toFloat() + 0.5f, segments[1].alpha, testFloatTolerance)
        assertEquals(1f, segments[1].length, testFloatTolerance)
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

        // grow ower length, have 2 segments - one 2.0 (tail) and another 1.0 in same dir
        val segments = newModel(tailLen = tailLen, initialLen = 1.0).apply {
            advance(2.0, 0.0, 0.0)
        }.bodySegments

        assertEquals(2, segments.size)

        val iter = segments.iterator()
        val first = iter.next()

        assertSegment(
            first, tStartAngle, tStartBeta,
            tStartX, tStartY, tStartZ,
            tStartX + tailLen * sin45, tStartY + tailLen * sin45, tStartZ.toFloat() + tRadius / 2,
            tRadius / 2, tailLen)

        val x1 = first.endX.toDouble()
        val y1 = first.endY.toDouble()
        val z1 = first.endZ.toDouble()
        val secondLen = 1.0 // full - first len
        assertSegment(
            iter.next(), tStartAngle, tStartBeta,
            x1, y1, z1,
            x1 + secondLen * sin45, y1 + secondLen * sin45, z1 + tRadius / 2,
            tRadius / 2, secondLen)
    }

    /**
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
        val alpha2 = tStartAngle.toFloat() + 0.5

        // had 2 segments [1, 2] , becomes 3 segments
        val segments = newModel(tailLen = tailLen, initialLen = initLen).apply {
            advance(2.0, 0.5, 0.0)
        }.bodySegments.toList()

        assertEquals(3, segments.size)
        val bs = arrayOf(segments[0] as BodySegment, segments[1] as BodySegment, segments[2] as BodySegment)

        // 3: 1st same short as beginning, 2nd rotated till full len, 3rd remaining
        val r0 = tRadius * initLen / tailLen
        assertSegment(
            segments[0], tStartAngle, tStartBeta,
            tStartX, tStartY, tStartZ,
            tStartX + initLen * sin45, tStartY + initLen * sin45, tStartZ + r0,
            r0, initLen)

        // second completes tail - 1.5

        val sin2 = sin(alpha2)
        val cos2 = cos(alpha2)

        val secondLen = 1.5 // full - first len
        // up to length
        assertSegment(
            segments[1], alpha2, tStartBeta, // new angle
            bs[0].dblEndX, bs[0].dblEndY, bs[0].dblEndZ,
            bs[0].dblEndX + secondLen * cos2, bs[0].dblEndY + secondLen * sin2, bs[0].dblEndZ + tRadius,
            tRadius, secondLen)

        // remaining
        val thirdLen = 0.5
        assertSegment(
            segments[2], alpha2, tStartBeta, // new angle
            bs[1].dblEndX, bs[1].dblEndY, bs[1].dblEndZ,
            bs[1].dblEndX + thirdLen * cos2, bs[1].dblEndY + thirdLen * sin2, bs[1].dblEndZ + tRadius,
            tRadius, thirdLen)
    }

    @Test
    fun `grow short lessThanLen multiple rotate`() {
        val tailLen = 1.0
        val initLen = 0.1
        val dA1 = 0.1
        val alpha1 = tStartAngle + dA1
        val sin0 = sin(tStartAngle)
        val cos0 = sin(tStartAngle)

        // had 2 segments [1, 2] , becomes 3 segments
        val segments = newModel(tailLen = tailLen, initialLen = initLen).apply {
            advance(0.1, dA1, 0.0)
            advance(0.1, -dA1, 0.0)
        }.bodySegments.toList()

        assertEquals(3, segments.size)
        val bs = arrayOf(segments[0] as BodySegment, segments[1] as BodySegment, segments[2] as BodySegment)

        // 3: 1st same short as beginning, 2nd rotated , 3rd rotated back
        val r0 = tRadius * 0.1
        val r1 = tRadius * 0.2
        val r2 = tRadius * 0.3

        assertSegment(
            segments[0], tStartAngle, tStartBeta,
            tStartX, tStartY, tStartZ,
            tStartX + initLen * cos0, tStartY + initLen * sin0, tStartZ + r0,
            r0, initLen)

        // second completes tail - 1.5

        val sin2 = sin(alpha1)
        val cos2 = cos(alpha1)

        val secondLen = 0.1 // full - first len
        // up to length
        assertSegment(
            segments[1], alpha1, tStartBeta, // new angle
            bs[0].dblEndX, bs[0].dblEndY, bs[0].dblEndZ,
            bs[0].dblEndX + secondLen * cos2, bs[0].dblEndY + secondLen * sin2, bs[0].dblEndZ + r1,
            r1, secondLen)

        // remaining
        val thirdLen = 0.1
        assertSegment(
            segments[2], tStartAngle, tStartBeta, // new angle
            bs[1].dblEndX, bs[1].dblEndY, bs[1].dblEndZ,
            bs[1].dblEndX + thirdLen * cos0, bs[1].dblEndY + thirdLen * sin0, bs[1].dblEndZ + r2,
            r2, thirdLen)
    }


    /////////////// Grow and shrink
    @Test
    fun `grow and shrink direct`() {
        
    }
}