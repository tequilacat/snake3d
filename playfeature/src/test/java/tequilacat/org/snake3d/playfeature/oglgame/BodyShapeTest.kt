package tequilacat.org.snake3d.playfeature.oglgame

import org.junit.Test

import org.junit.Assert.*
import tequilacat.org.snake3d.playfeature.BodySegment
import tequilacat.org.snake3d.playfeature.glutils.BYTES_PER_FLOAT
import tequilacat.org.snake3d.playfeature.glutils.GeometryData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BodyShapeTest {

    private val testRadius = 1f

    /**
     * adds count copies of last element
     */
    private fun expandBy(list: MutableList<BodySegment>, count: Int) =
        1.rangeTo(count).forEach { _ -> list.add(list.last()) }

    private val singleSegmentGeom = BodyShape(4, testRadius)
        .run {
            update(listOf(BodySegment(0.0, 0.0, 0.0, 0.0, 1.0)))
            geometry
        }


    @Test
    fun `build basicfeatures`() {
        assertTrue(singleSegmentGeom.hasNormals)
        assertTrue(singleSegmentGeom.hasTexture)
        assertEquals(8 * BYTES_PER_FLOAT, singleSegmentGeom.vertexStride)
    }

    @Test
    fun `facesegments count always even`() {
        assertEquals(4, BodyShape(4, testRadius).segmentFaceCount)
        assertEquals(4, BodyShape(5, testRadius).segmentFaceCount)
    }

    @Test
    fun indexes() {
        // 32 triangles of these vertexes
        // compare provided count, not size of array since it's allocated in advance
        assertEquals(32 * 3, singleSegmentGeom.indexCount)

        // expected indexes
        val expectedIndexes = shortArrayOf(
            0, 1, 2, 0, 2, 3, 0, 3, 4, 0, 4, 1,
            1, 5, 6, 1, 6, 2, 2, 6, 7, 2, 7, 3, 3, 7, 8, 3, 8, 4, 4, 8, 5, 4, 5, 1,
            5, 9, 10, 5, 10, 6, 6, 10, 11, 6, 11, 7, 7, 11, 12, 7, 12, 8, 8, 12, 9, 8, 9, 5,
            9, 13, 14, 9, 14, 10, 10, 14, 15, 10, 15, 11, 11, 15, 16, 11, 16, 12, 12, 16, 13, 12, 13, 9,
            13, 17, 14, 14, 17, 15, 15, 17, 16, 16, 17, 13
        )
        // check
        for(vi in expectedIndexes.withIndex()){
            assertEquals(
                "diff @${vi.index}: exp. ${vi.value} != [${singleSegmentGeom.indexes[vi.index]}]",
                vi.value, singleSegmentGeom.indexes[vi.index])
        }
    }

    /**
     *  test (on expected hardcoded sizes) that size is increased first (and arrays differ by reference)
     *  then on next allocation sizes differ while array is same
     */
    @Test
    fun `index array reallocation`() {
        val segments = mutableListOf(BodySegment(0.0, 0.0, 0.0, 0.0, 1.0))

        val builder =  BodyShape(4, testRadius)
        builder.update(segments)

        assertEquals((segments.size + 3) * 8 * 3, builder.geometry.indexCount)
        val prevIndexes = builder.geometry.indexes

        expandBy(segments, 10)
        builder.update(segments)
        assertEquals((segments.size + 3) * 8 * 3, builder.geometry.indexCount)
        assertEquals(prevIndexes, builder.geometry.indexes)

        // must exceed range that requires expansion
        expandBy(segments, 1000)
        builder.update(segments)
        assertEquals((segments.size + 3) * 8 * 3, builder.geometry.indexCount)
        assertNotEquals(prevIndexes, builder.geometry.indexes)
    }



    /////////////////////////////////////////////////
    // Vertex test


    /**
     *  test (on expected hardcoded sizes) that size is increased first (and arrays differ by reference)
     *  then on next allocation sizes differ while array is same
     */
    @Test
    fun `vertex array reallocation`() {
        val segments = mutableListOf(BodySegment(0.0, 0.0, 0.0, 0.0, 1.0))

        val builder =  BodyShape(4, testRadius)
        builder.update(segments)

        assertEquals(
            (segments.size + 3) * builder.segmentFaceCount + 2,
            builder.geometry.vertexCount)
        val prevIndexes = builder.geometry.vertexes

        expandBy(segments, 10)
        builder.update(segments)
        assertEquals(
            (segments.size + 3) * builder.segmentFaceCount + 2,
            builder.geometry.vertexCount)
        assertEquals(prevIndexes, builder.geometry.vertexes)

        // must exceed range that requires expansion
        expandBy(segments, 1000)
        builder.update(segments)
        assertEquals((segments.size+3) * builder.segmentFaceCount + 2,
            builder.geometry.vertexCount)
        assertNotEquals(prevIndexes, builder.geometry.vertexes)
    }

    private fun assertVertexData(bodyGeometry: GeometryData, firstVertex: Int,
                                 geomOffset: Int, // offset within vertex subarray
                                 expectedVertexData: FloatArray,
                                 // how many in expected array per vertex
                                 expectedDataStride: Int) {
        // assume always 8 floats per vertex - as body always has
        var expIndex = 0
        //
        var bodyVertexIndex = firstVertex * bodyGeometry.vertexStrideInFloats + geomOffset
        var testedVertex = firstVertex

        while (expIndex < expectedVertexData.size) {
            for(i in 0 until expectedDataStride) {
                assertEquals("Diff [${expIndex + i}] and geom[${bodyVertexIndex + i}] (vertex $testedVertex)",
                    expectedVertexData[i + expIndex], bodyGeometry.vertexes[bodyVertexIndex + i],
                    0.0001f)
            }

            expIndex += expectedDataStride
            bodyVertexIndex += bodyGeometry.vertexStrideInFloats
            testedVertex++
        }
    }

    // check vertex count for single segment
    @Test
    fun `vertex coordinates`() {
        val segments = mutableListOf(BodySegment(0.0, 0.0, testRadius.toDouble(),
            0.0, 10.0))

        val builder =  BodyShape(4, testRadius)
        builder.update(segments)

        // Vertexes (single segment) - 4 on each side, 4 in intermediate to ends, 1+1 ends
        // 18 vertexes
        // compare provided count, not size of array since it's allocated in advance
        assertEquals(2 + (segments.size + 3) * builder.segmentFaceCount, builder.geometry.vertexCount)

        // add rotated segment
        segments.add(
            BodySegment(
                segments.first().dblEndX,
                segments.first().dblEndY,
                segments.first().dblEndZ,
                PI / 4,
                10.0
            )
        )
        builder.update(segments)
        assertEquals(2 + (segments.size + 3) * builder.segmentFaceCount, builder.geometry.vertexCount)

        val sin45 = sin(PI / 4).toFloat()
        val sinPi8 = sin(PI / 8).toFloat()
        val cosPi8 = cos(PI / 8).toFloat()

        // now examine deeper the expected 3d coords:
        // first vertex (tail)
        assertVertexData(builder.geometry, 0,0,
            floatArrayOf(-testRadius, 0f, testRadius), 3)

        // skip starting intermediate ring as it's to be adjusted for better visuals

        // test vertexes at starting 1st segment: 5,6,7,8,
        // then joining
        assertVertexData(builder.geometry, 5,0, floatArrayOf(
            0f, -testRadius, testRadius, 0f, 0f, testRadius*2, 0f, testRadius, testRadius, 0f, 0f, 0f,
            // joint (rotated by pi/8) aka start last segment
            10 + sinPi8 * testRadius, -cosPi8 * testRadius, testRadius,
            10f, 0f, testRadius*2,
            10 - sinPi8 * testRadius, cosPi8 * testRadius, testRadius,
            10f, 0f, 0f,
            // end of head
            10 + 10 * sin45 + testRadius * sin45, 10 * sin45 - testRadius * sin45, testRadius,
            10 + 10 * sin45, 10 * sin45, testRadius*2,
            10 + 10 * sin45 - testRadius * sin45, 10 * sin45 + sin45, testRadius,
            10 + 10 * sin45, 10 * sin45, 0f
            ), 3)


        // last one (nose)
        assertVertexData(builder.geometry, builder.geometry.vertexCount - 1,0, floatArrayOf(
            10 + 10 * sin45 + testRadius * sin45, 10 * sin45 + testRadius * sin45, testRadius
        ), 3)
    }

}
