package tequilacat.org.snake3d.playfeature

import org.junit.Assert.*
import org.junit.Test
import tequilacat.org.snake3d.playfeature.glutils.IDirectedSection
import kotlin.math.*

class TestSections(private val startX: Number, private val startY: Number, private val floorZ: Number) {
    private val segmentsList = mutableListOf<IDirectedSection>()
    val segments get() = segmentsList.asSequence()

    fun add(length: Number, radius: Number, deltaAngle: Number): TestSections {
        val last: IDirectedSection
        val newAngle: Double

        if(segmentsList.isEmpty()) {
            last = TDS(startX.toDouble(), startY.toDouble(), floorZ.toDouble(), 0.0,
                0.0, deltaAngle.toFloat())
            segmentsList.add(last)
            newAngle = deltaAngle.toDouble()
        } else {
            last = segmentsList.last()
            newAngle = last.alpha + deltaAngle.toDouble()
            (last as TDS).mutableAlpha = newAngle.toFloat()
            // modify last alpha
        }

        segmentsList.add(TDS(
            (last.centerX + length.toDouble() * cos(newAngle)),
            (last.centerY + length.toDouble() * sin(newAngle)),
            floorZ.toDouble() + radius.toDouble(),
            length.toDouble(), radius.toDouble(), newAngle.toFloat()))

        return this
    }

    private class TDS(
        override val dCenterX: Double,
        override val dCenterY: Double,
        override val dCenterZ: Double,
        override val dPrevLength: Double,
        override val dRadius: Double,
        var mutableAlpha: Float
    ) : IDirectedSection {
        override val dAlpha: Double get() = mutableAlpha.toDouble()
    }

    companion object {
        fun fromCoords(radius: Number, vararg coords: Number): List<IDirectedSection> {
            //val model = this
            val floorZ = 0.0

            var lastX = coords[0].toDouble()
            var lastY = coords[1].toDouble()

            val r = radius.toDouble()
            val list = mutableListOf<IDirectedSection>()

            for (i in 2 until coords.size step 2) {

                val x = coords[i].toDouble()
                val y = coords[i+1].toDouble()

                val angle = atan2(y - lastY, x - lastX)
                val length = hypot(y - lastY, x - lastX)

                if(i == 2) {
                    list.add(TDS(lastX, lastY, floorZ, 0.0, 0.0, angle.toFloat()))
                }

                list.add(TDS(x, y, floorZ + r, length, r, angle.toFloat()))

                lastX = x
                lastY = y
            }

            return list
        }
    }

}

class TestSectionGenerator {

    @Test
    fun `create by coords onesegment`() {
        val onesegment = TestSections.fromCoords(1, 10, 10, 10, 20, 9, 19)
        assertEquals(3, onesegment.size)

        assertEquals(10.0, onesegment[0].dCenterX, testDoubleTolerance)
        assertEquals(10.0, onesegment[0].dCenterY, testDoubleTolerance)
        assertEquals(0.0, onesegment[0].dCenterZ, testDoubleTolerance)
        assertEquals(0.0, onesegment[0].dPrevLength, testDoubleTolerance)
        assertEquals(0.0, onesegment[0].dRadius, testDoubleTolerance)
        assertEquals(PI / 2, onesegment[0].dAlpha, testDoubleTolerance)

        assertEquals(10.0, onesegment[1].dCenterX, testDoubleTolerance)
        assertEquals(20.0, onesegment[1].dCenterY, testDoubleTolerance)
        assertEquals(1.0, onesegment[1].dCenterZ, testDoubleTolerance)
        assertEquals(10.0, onesegment[1].dPrevLength, testDoubleTolerance)
        assertEquals(1.0, onesegment[1].dRadius, testDoubleTolerance)
        assertEquals(PI / 2, onesegment[1].dAlpha, testDoubleTolerance)
        
        assertEquals(9.0, onesegment[2].dCenterX, testDoubleTolerance)
        assertEquals(19.0, onesegment[2].dCenterY, testDoubleTolerance)
        assertEquals(1.0, onesegment[2].dCenterZ, testDoubleTolerance)
        assertEquals(hypot(1.0, 1.0), onesegment[2].dPrevLength, testDoubleTolerance)
        assertEquals(1.0, onesegment[2].dRadius, testDoubleTolerance)
        assertEquals(PI + PI / 4, PI * 2 + onesegment[2].dAlpha, testDoubleTolerance)
    }

    @Test
    fun selftest() {

        // test angles
        val body2sections = TestSections(0.0, 0.0, 0.0)
            .add(10, 1, 2).segments //

        assertArrayEquals(floatArrayOf(2f, 2f),
            body2sections.toList().map { s -> s.alpha }.toFloatArray(), testFloatTolerance
        )

        val body4sections = TestSections(0.0, 0.0, 0.0)
            .add(1, 1, 1)
            .add(1, 1, 1)
            .add(1, 1, 1)
            .segments
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 3f),
            body4sections.toList().map { s -> s.alpha }.toFloatArray(), testFloatTolerance
        )



        // test coords
        val body3sectionsCoords = TestSections(1,2,0)
            .add(1, 2, PI /2)
            .add(2, 1, PI /2)
            .segments.toList()
        assertEquals(3, body3sectionsCoords.size)

        val sec1 = body3sectionsCoords.first()
        assertEquals(1f, sec1.centerX, testFloatTolerance)
        assertEquals(2f, sec1.centerY, testFloatTolerance)
        assertEquals(0f, sec1.centerZ, testFloatTolerance)
        assertEquals(0f, sec1.radius, testFloatTolerance)
        assertEquals(0f, sec1.prevLength, testFloatTolerance)
        assertEquals(PI.toFloat() / 2, sec1.alpha, testFloatTolerance)

        val sec2 = body3sectionsCoords[1]
        assertEquals(1f, sec2.centerX, testFloatTolerance)
        assertEquals(3f, sec2.centerY, testFloatTolerance)
        assertEquals(2f, sec2.centerZ, testFloatTolerance)
        assertEquals(2f, sec2.radius, testFloatTolerance)
        assertEquals(1f, sec2.prevLength, testFloatTolerance)
        // have rotated 2nd face
        assertEquals(PI.toFloat(), sec2.alpha, testFloatTolerance)

        val sec3 = body3sectionsCoords[2]
        assertEquals(-1f, sec3.centerX, testFloatTolerance)
        assertEquals(3f, sec3.centerY, testFloatTolerance)
        assertEquals(1f, sec3.centerZ, testFloatTolerance)
        assertEquals(1f, sec3.radius, testFloatTolerance)
        assertEquals(2f, sec3.prevLength, testFloatTolerance)
        // final face has same rotation
        assertEquals(PI.toFloat(), sec3.alpha, testFloatTolerance)
    }

}