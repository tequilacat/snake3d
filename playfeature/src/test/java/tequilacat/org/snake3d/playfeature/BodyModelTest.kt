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

    /** indicates that no shortening should take place */
    private val FOODLENGTH_LARGE = 100.0

    private val tStartX = 5.0
    private val tStartY = 2.0
    private val tFloorZ = 1.0
    private val tStartAngle = PI/4

    /**
     * check that init will reset model to new specified values
     */
    @Test
    fun reinit() {
        val len1 = 10.0
        initTestBodyNonExp811_L10(initLen = len1).apply {
            init(tStartX + 10, tStartY + 20, tFloorZ, tStartAngle * 2, len1 * 2)
            val tail = bodySections.first()
            // check that tail section and summ length are new
            assertEquals(tStartX + 10, tail.dCenterX, testDoubleTolerance)
            assertEquals(tStartY + 20, tail.dCenterY, testDoubleTolerance)
            assertEquals(tStartAngle*2, tail.dAlpha, testDoubleTolerance)
            assertEquals(len1 * 2, bodyAndHeadSections.sumByDouble { s-> s.dPrevLength }, testDoubleTolerance)
        }
    }



    private fun Sequence<IDirectedSection>.toL() = this.map { s -> s.dPrevLength }.toList().toDoubleArray()

    private fun Sequence<IDirectedSection>.toLR() = this.toList()
        .flatMap { s -> listOf(s.prevLength, s.radius) }.toFloatArray()

    /**
     * Asserts correct radiuses / Z, and that body with head is exactly 2 segments longer.
     * We don't check angle here!
     * */
    private fun assertSegmentsLR(bodySections: Sequence<IDirectedSection>, bodyAndHeadSections: Sequence<IDirectedSection>,
                                 vararg lrArray: Number) {
        val lrExpectedFloatArray = floatArrayOfNumbers(*lrArray)
        val bodyAndHeadSectionsLR = bodyAndHeadSections.toLR()

        val bodySectionsLR = bodySections.toLR()
        // expected bodyLR is expected bodyHEadLR without last 2 rings
        assertArrayEquals("body sections", lrExpectedFloatArray.take(lrExpectedFloatArray.size - 4).toFloatArray(),
            bodySectionsLR, testFloatTolerance)
        assertArrayEquals("body and head sections", lrExpectedFloatArray, bodyAndHeadSectionsLR, testFloatTolerance)

        // check Z of all elements is floor + R
        assertTrue(bodySections.all { s -> s.dCenterZ == tFloorZ + s.dRadius })
        assertTrue(bodyAndHeadSections.all { s -> s.dCenterZ == tFloorZ + s.dRadius })
    }

    /**
     * asserts that centers are placed correctly relative prev sections according to expected deltaAngles
     * and also checks the section angles,
     * assuming lengths are correct
     */
    private fun assertCenters(bodySectionSeq: Sequence<IDirectedSection>, vararg deltaAngles: Number) {
        val bodySections = bodySectionSeq.toList()
        assertEquals(bodySections.size, deltaAngles.size)

        //var i = 0

        var prev :IDirectedSection? = null

        for (sectionTuple in bodySections.withIndex()) {
            val expectedDA = deltaAngles[sectionTuple.index].toDouble()
            val section = sectionTuple.value

            if(prev == null) {
                assertEquals(expectedDA, section.dAlpha, testDoubleTolerance)
            } else {
                assertEquals("#${sectionTuple.index} da", expectedDA + prev.dAlpha, section.dAlpha, testDoubleTolerance)
                assertEquals("#${sectionTuple.index} x", prev.dCenterX + cos(section.dAlpha) * section.dPrevLength, section.dCenterX, testDoubleTolerance)
                assertEquals("#${sectionTuple.index} y", prev.dCenterY + sin(section.dAlpha) * section.dPrevLength, section.dCenterY, testDoubleTolerance)
            }

            prev = section
        }
    }


    /**
     * 1 body segment (L=8), 2 head segments (1,1)
     */
    private fun initTestBodyNonExp811_L10(radius: Double = 0.1, initLen: Double = 10.0, feedSize: Double = 0.5) = BodyModel(
        TestConicalProportions(doubleArrayOfNumbers(0.8, radius, 0.9, radius*2, 1, radius),
            0, feedSize)).apply {
        init(tStartX, tStartY, tFloorZ, tStartAngle, initLen)
    }

    /**
     * first segment bound to tail when expanding
     */
    private fun initTestBody1711_L10(radius: Double = 0.1, initLen: Double = 10.0, feedSize: Double = 0.5) = BodyModel(
        TestConicalProportions(
            doubleArrayOfNumbers(0.1, radius / 2, 0.8, radius, 0.9, radius * 2, 1, radius), 1, feedSize
        )).apply {
        init(tStartX, tStartY, tFloorZ, tStartAngle, initLen)
    }

    /**
     * one body segment fits 1 shaper segment
     */
    @Test
    fun `init 1 body segment 1 shape segment`() {
        val radius = 0.1
        val feedSize = 0.1
        val initLen = 10.0

        // 1 segment body, 2 segments head
        val shaper = TestConicalProportions(doubleArrayOfNumbers(0.8, radius, 0.9, radius*2, 1, radius),
            0, feedSize)
        BodyModel(shaper).apply {
            init(tStartX, tStartY, tFloorZ, tStartAngle, initLen)
            assertSegmentsLR(
                bodySections, bodyAndHeadSections,
                0, 0, 8, radius, 1, radius * 2, 1, radius
            )
            // summary length is as initialized
            assertEquals(initLen, bodyAndHeadSections.sumByDouble { it.dPrevLength }, testDoubleTolerance)
            // minus head sections
            assertEquals(initLen - 2, bodySections.sumByDouble { it.dPrevLength }, testDoubleTolerance)

            // 4 sections
            assertCenters(bodyAndHeadSections, tStartAngle, 0, 0, 0)
        }
    }

    /**
     * one body segment split by several shaper segments
     */
    @Test
    fun `init 1 body segment several shape segments`() {
        val radius = 0.1
        val feedSize = 0.1
        val initLen = 10.0

        // add intermediate segment @0.1L w/ half radius
        val shaper = TestConicalProportions(
            doubleArrayOfNumbers(0.1, radius/2, 0.8, radius, 0.9, radius*2, 1, radius), 1, feedSize)
        BodyModel(shaper).apply {
            init(tStartX, tStartY, tFloorZ, tStartAngle, initLen)
            assertSegmentsLR(bodySections, bodyAndHeadSections,
                0, 0, 1, radius / 2, 7, radius, 1, radius * 2, 1, radius)

            // summary length is as initialized
            assertEquals(initLen, bodyAndHeadSections.sumByDouble { it.dPrevLength }, testDoubleTolerance)
            assertEquals(initLen - 2, bodySections.sumByDouble { it.dPrevLength }, testDoubleTolerance)

            // 5 sections
            assertCenters(bodyAndHeadSections, tStartAngle, 0, 0, 0, 0)
        }
    }

    /**
     * advance by small length without feeding
     * the neck segment must be added, the tail shortened
     * */
    @Test
    fun `advance 1 segment - short`() {
        val radius = 0.1
        val feedSize = FOODLENGTH_LARGE // not used since we don't feed the snake here
        val initLen = 10.0

        val shaper = TestConicalProportions(doubleArrayOfNumbers(
            0.1, radius/2, 0.8, radius, 0.9, radius*2, 1, radius), 1, feedSize)
        BodyModel(shaper).apply {
            init(tStartX, tStartY, tFloorZ, tStartAngle, initLen)
            advance(0.1, 0.1)

            assertSegmentsLR(
                bodySections, bodyAndHeadSections,
                0, 0, 0.9, radius / 2 * 0.9, 0.1, radius / 2,
                6.9, radius / 2 + radius / 2 * 6.9 / 7,
                0.1, radius, // new segment
                1, radius * 2, 1, radius  // head segments
            )

            val tail = bodySections.first()
            assertEquals(tStartX + 0.1 * cos(tStartAngle), tail.dCenterX, testDoubleTolerance)
            assertEquals(tStartY + 0.1 * sin(tStartAngle), tail.dCenterY, testDoubleTolerance)

            assertCenters(bodyAndHeadSections, tStartAngle, 0, 0, 0, 0.1, 0, 0)
        }
    }

    /**
     * advance by big length so all segments are regenerated.
     * Test that tail is exactly moved
     * */
    @Test
    fun `advance over length - withAngle - test newTailCoords`() {
        val radius = 0.1
        val initLen = 10.0
        val angleDelta = 0.1

        initTestBody1711_L10(initLen = initLen, radius = radius).apply {
            // remember where old neck ended
            val oldNeckCoords = bodySections.last().run { Pair(this.dCenterX, this.dCenterY) }

            // advance under angle! so new sections will be created
            advance(20.0, angleDelta)

            // the tail bust be moved at specified distance (moveDist - tail2neck == 20 - (10-2) == 12)
            val neckToNewTailDist = 12.0
            val tail = bodySections.first()
            assertEquals(oldNeckCoords.first + neckToNewTailDist * cos(tStartAngle + angleDelta), tail.dCenterX, testDoubleTolerance)
            assertEquals(oldNeckCoords.second + neckToNewTailDist * sin(tStartAngle + angleDelta), tail.dCenterY, testDoubleTolerance)
        }
    }



    /**
     * advance by big length so all segments are regenerated.
     * Test segments lengths, raduiuses and angles
     * */
    @Test
    fun `advance over length - withAngle - test segments`() {
        val radius = 0.1
        val initLen = 10.0
        val angleDelta = 0.1

        // add intermediate segment @0.1L w/ half radius
        initTestBody1711_L10(initLen = initLen, radius = radius).apply {
            // under angle!
            advance(20.0, angleDelta)

            // these values are same as after init
            assertSegmentsLR(bodySections, bodyAndHeadSections,
                0, 0, 1, radius / 2, 7, radius, 1, radius * 2, 1, radius)

            // summary length is as initialized
            assertEquals(initLen, bodyAndHeadSections.sumByDouble { it.dPrevLength }, testDoubleTolerance)
            assertEquals(initLen - 2, bodySections.sumByDouble { it.dPrevLength }, testDoubleTolerance)

            // 5 sections
            assertCenters(bodyAndHeadSections, tStartAngle + angleDelta, 0, 0, 0, 0)
        }
    }

    /**
     * tests exact border condition when a segment is entirely removed.
     * new segment added as part of body before neck
     * Only lenggths are checked since we test how the advance works, not reshaping
     */
    @Test
    fun `advance exact tail segment - breakNoAngle - checkLengths`() {
        initTestBody1711_L10(initLen = 10.0, radius = 0.1).apply {
            advance(1.0, 0.0)
            assertArrayEquals(doubleArrayOfNumbers(0, 1, 7, 1, 1), bodyAndHeadSections.toL(), testDoubleTolerance)
        }
    }

    /**
     * tests exact border condition when a segment is entirely removed.
     * Only lenggths are checked since we test how the advance works, not reshaping
     */
    @Test
    fun `advance exact tail segment - breakAngle - checkLengths`() {
        initTestBody1711_L10(initLen = 10.0, radius = 0.1).apply {
            advance(1.0, 0.1)
            assertArrayEquals(doubleArrayOfNumbers(0, 1, 6, 1, 1, 1), bodyAndHeadSections.toL(), testDoubleTolerance)
        }
    }

    /**
     * advance exact as feed: become advanceLength longer
     */
    @Test
    fun `feed advance equals feed`() {
        initTestBodyNonExp811_L10(feedSize = 0.5).apply {
            feed() // feed param should not matter anymore
            advance(0.5, 0.0) // keep direction - doesn't matter here
            // lengths were 8,1,1 . add 0.5 to body (since no angle - no new neck segm), other segments kept the same
            assertArrayEquals(doubleArrayOfNumbers(0, 8.5, 1, 1), bodyAndHeadSections.toL(), testDoubleTolerance)
            assertEquals(tStartX, bodyAndHeadSections.first().dCenterX, testDoubleTolerance)
            assertEquals(tStartY, bodyAndHeadSections.first().dCenterY, testDoubleTolerance)

            // food is consumed totally, next move is plain advancement
            // now advance ahead - samy body, start point moves
            advance(0.1, 0.0)
            assertArrayEquals(doubleArrayOfNumbers(0, 8.5, 1, 1), bodyAndHeadSections.toL(), testDoubleTolerance)
            assertEquals(tStartX + 0.1 * cos(tStartAngle), bodyAndHeadSections.first().dCenterX, testDoubleTolerance)
            assertEquals(tStartY + 0.1 * sin(tStartAngle), bodyAndHeadSections.first().dCenterY, testDoubleTolerance)
        }
    }

    /**
     * move ahead at diff between advance and head
     */
    @Test
    fun `feed advance more than feed`() {
        val advance = 1.0
        val feedSize = 0.2

        initTestBodyNonExp811_L10(feedSize = feedSize).apply {
            feed() // feed param should not matter anymore
            advance(advance, 0.0) // keep direction - doesn't matter here

            assertArrayEquals(doubleArrayOfNumbers(0, 8 + feedSize, 1, 1), bodyAndHeadSections.toL(), testDoubleTolerance)
            assertEquals(tStartX + (advance - feedSize) * cos(tStartAngle), bodyAndHeadSections.first().dCenterX, testDoubleTolerance)
            assertEquals(tStartY + (advance - feedSize) * sin(tStartAngle), bodyAndHeadSections.first().dCenterY, testDoubleTolerance)
        }
    }

    /**
     * multiple advances through multiple feeds
     */
    @Test
    fun `feed advance through accumulated feeds`() {

        initTestBodyNonExp811_L10(feedSize = 1.0).apply {
            feed()
            advance(0.75, 0.0) // keep direction - doesn't matter here
            // start must be same, body += 0.75

            // tail at same place, body becomes longer
            assertEquals(tStartX, bodyAndHeadSections.first().dCenterX, testDoubleTolerance)
            assertEquals(tStartY, bodyAndHeadSections.first().dCenterY, testDoubleTolerance)
            assertArrayEquals(doubleArrayOfNumbers(0, 8.75, 1, 1), bodyAndHeadSections.toL(), testDoubleTolerance)

            // 0.25 kept
            feed()
            // 1.25 kept
            advance(1.75, 0.0)
            // tail must move by 0.5 from original position

            // head moved += 2.5, tail moved +0.5
            assertArrayEquals(doubleArrayOfNumbers(0, 10.0, 1, 1), bodyAndHeadSections.toL(), testDoubleTolerance)
            assertEquals(tStartX + 0.5 * cos(tStartAngle), bodyAndHeadSections.first().dCenterX, testDoubleTolerance)
            assertEquals(tStartY + 0.5 * sin(tStartAngle), bodyAndHeadSections.first().dCenterY, testDoubleTolerance)
        }
    }


    /**
     * the head coords are reported to position camera and to give current straight direction of movement
     * */
    @Test
    fun `neck coordinates`() {
        val angle = PI / 3
        val startX = 2.0
        val startY = 4.0

        BodyModel(TestConicalProportions(doubleArrayOfNumbers(0.8, 1, 0.9, 2, 1, 1),
            0, 0.1)).apply {
            init(startX, startY, 0.0, angle, 10.0)

            // body is segmented as 8:1:1 so first segment is 8
            assertEquals((startX + 8 * cos(angle)).toFloat(), headX, testFloatTolerance)
            assertEquals((startY + 8 * sin(angle)).toFloat(), headY, testFloatTolerance)
            assertEquals(angle.toFloat(), viewDirection, testFloatTolerance)
        }

    }

    // init after food will reset the counter
    @Test
    fun `feed init resets`() {
        val bodyLength = 10.0
        initTestBodyNonExp811_L10(initLen = bodyLength).apply {
            feed()
            init(tStartX, tStartY, tFloorZ, tStartAngle, bodyLength)
            advance(1.0, 0.0)

            // total len must be initial length after advance - the init must reset "feed" counter
            assertEquals(bodyLength, bodyAndHeadSections.sumByDouble { it.dPrevLength }, testDoubleTolerance)
        }

    }


    /**
     * Last segment was added with radius = 0 due to wrong processing of lengths
     * after advance after feeding
     */
    @Test
    fun `test against zero radius last segment`() {
        val props = FeedableProportions(
            doubleArrayOf(0.3, 0.1, 0.65, 0.1, 0.8, 0.07, 0.9, 0.1, 1.0, 0.07),
            1.0,0, 2.0)
        BodyModel(props).apply{
            init(1.0,1.0,1.0, 0.0, 2.0)
            feed()
            advance(0.1, 0.1)

            // lengths must be equal to lengths provided by shaper proportions
            assertEquals(props.fullLength, bodyAndHeadSections.sumByDouble { s-> s.dPrevLength }, testDoubleTolerance)
            assertEquals(props.lengthToNeck, bodySections.sumByDouble { s-> s.dPrevLength }, testDoubleTolerance)
            assertTrue(bodySections.drop(1).all { s -> s.dRadius > 0.0 })
        }

        // same with oversized size (where r is restricted and central section is prolonged)
        BodyModel(props).apply{
            init(1.0,1.0,1.0, 0.0, 100.0)
            feed()
            advance(0.1, 0.1)

            // lengths must be equal to lengths provided by shaper proportions
            assertEquals(props.fullLength, bodyAndHeadSections.sumByDouble { s-> s.dPrevLength }, testDoubleTolerance)
            assertEquals(props.lengthToNeck, bodySections.sumByDouble { s-> s.dPrevLength }, testDoubleTolerance)
            assertTrue(bodySections.drop(1).all { s -> s.dRadius > 0.0 })
        }

        // same but advance bigger than feed (to check tail shortening is processed correctly)
        BodyModel(props).apply{
            init(1.0,1.0,1.0, 0.0, 100.0)
            feed()
            advance(20.0, 0.1) // bigger then feed length

            // lengths must be equal to lengths provided by shaper proportions
            assertEquals(props.fullLength, bodyAndHeadSections.sumByDouble { s-> s.dPrevLength }, testDoubleTolerance)
            assertEquals(props.lengthToNeck, bodySections.sumByDouble { s-> s.dPrevLength }, testDoubleTolerance)
            assertTrue(bodySections.drop(1).all { s -> s.dRadius > 0.0 })
        }
    }

    @Test
    fun `body collision model`() {
        val radius = 0.1

        with(initTestBody1711_L10(initLen = 10.0, radius = radius).collisionModel) {

            assertArrayEquals(floatArrayOfNumbers(0, 0, 1, radius / 2, 7, radius),
                bodySections.toLR(), testFloatTolerance)

            // faceSection
            assertEquals(tStartX + 8 * cos(tStartAngle), faceSection.dCenterX, testDoubleTolerance)
            assertEquals(tStartY + 8 * sin(tStartAngle), faceSection.dCenterY, testDoubleTolerance)
            assertEquals(tStartAngle, faceSection.dAlpha, testDoubleTolerance)

            assertEquals(8.0, lengthTailToFace, testDoubleTolerance)
            assertEquals(0.2, headRadius, testDoubleTolerance)
            assertEquals(1.0, headOffset, testDoubleTolerance)

        }
    }
}
