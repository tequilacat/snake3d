package tequilacat.org.snake3d.playfeature.oglgame

import org.junit.Test

import org.junit.Assert.*
import tequilacat.org.snake3d.playfeature.*
import tequilacat.org.snake3d.playfeature.glutils.CoordUtils
import tequilacat.org.snake3d.playfeature.glutils.Geometry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BodyShapeTest {

    private val testRadius = 1f
    private val beta = 0.0

    private fun bodyShape() = BodyShape(4, testRadius,0f,1f, 0f)

    /**
     * adds count copies of last element
     */
    private fun expandBy(list: MutableList<BodySegment>, count: Int) =
        1.rangeTo(count).forEach { _ -> list.add(list.last()) }

    private fun singleSegmentGeom() = bodyShape()
        .run {
            update(listOf(BodySegment(0.0, 0.0, 0.0, 0.0, beta, 1.0)))
            geometry
        }


    @Test
    fun `build basicfeatures`() {
        val geom = singleSegmentGeom()

        assertTrue(geom.hasNormals)
        assertTrue(geom.hasTexture)
        assertEquals(8, geom.vertexFloatStride)
    }

    @Test
    fun indexes() {
        val geom = singleSegmentGeom()
        // 32 triangles of these vertexes
        // compare provided count, not size of array since it's allocated in advance
        assertEquals(32 * 3, geom.indexCount)

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
                "diff @${vi.index}: exp. ${vi.value} != [${geom.indexes[vi.index]}]",
                vi.value, geom.indexes[vi.index])
        }
    }

    /**
     *  test (on expected hardcoded sizes) that size is increased first (and arrays differ by reference)
     *  then on next allocation sizes differ while array is same
     */
    @Test
    fun `index array reallocation`() {
        val segments = mutableListOf(BodySegment(0.0, 0.0, 0.0, 0.0, beta, 1.0))

        val builder = bodyShape()
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
     * how the start angle affects vertex positions in section
     */
    @Test
    fun `vertex ring start`() {
        val nFaces = 5
        val segments = mutableListOf<IBodySegment>(
            BodySegment(0.0, 0.0, 0.0, 0.0, beta, 10.0))
            .append(PI / 4, 10.0, true)
            .append(PI / 4, 10.0, true)

        val startAngle1 = 1.0f // just some angle in Q1
        // make 5 so Z is never repeated for same section
        val bodyShape =  BodyShape(nFaces, testRadius, startAngle1, 10f, 0f)
                bodyShape.update(segments)
        val geometry = bodyShape.geometry

        // 3 main segments + 2 smaller rings - we check main segments only
        // 1 + nRing*4 (starting) - all Z coords are sin(startAngle)
        (1..3).forEach {
            assertEquals(sin(startAngle1),
                geometry.vertexes[(1 + nFaces * it) * geometry.vertexFloatStride + 2],
                testFloatTolerance)
        }
    }

    /**
     *  test (on expected hardcoded sizes) that size is increased first (and arrays differ by reference)
     *  then on next allocation sizes differ while array is same
     */
    @Test
    fun `vertex array reallocation`() {
        val segments = mutableListOf(BodySegment(0.0, 0.0, 0.0, 0.0, beta, 1.0))

        val builder =  bodyShape()
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

    private fun assertVertexData(bodyGeometry: Geometry, firstVertex: Int,
                                 geomOffset: Int, // offset within vertex subarray
                                 expectedVertexData: FloatArray,
                                 // how many in expected array per vertex
                                 expectedDataStride: Int) {
        // assume always 8 floats per vertex - as body always has
        var expIndex = 0
        //
        var bodyVertexIndex = firstVertex * bodyGeometry.vertexFloatStride + geomOffset
        var testedVertex = firstVertex

        while (expIndex < expectedVertexData.size) {
            for(i in 0 until expectedDataStride) {
                assertEquals("Diff [${expIndex + i}] and geom[${bodyVertexIndex + i}] (vertex $testedVertex)",
                    expectedVertexData[i + expIndex], bodyGeometry.vertexes[bodyVertexIndex + i],
                    0.0001f)
            }

            expIndex += expectedDataStride
            bodyVertexIndex += bodyGeometry.vertexFloatStride
            testedVertex++
        }
    }

    // check vertex count for single segment
    @Test
    fun `vertex coordinates`() {
        val segments = mutableListOf(BodySegment(0.0, 0.0, testRadius.toDouble(),
            0.0, beta, 10.0))

        val builder =  bodyShape()
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
                PI / 4, beta,
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

    /**
     * test that each vertex in a triangle has same normal which is normalized -
     * dont compar to the normal of its triangle!
     * */
    @Test
    fun normals() {
        val geometry =  bodyShape().run {
            update(mutableListOf<IBodySegment>(
                BodySegment(0.0, 0.0, testRadius.toDouble(), 0.0, beta, 10.0))
                .append(PI / 4, 10.0, true)
            )
            geometry
        }

        for (vi in 0 until geometry.vertexCount step geometry.vertexFloatStride) {
            assertEquals("Bad normal at vi=$vi", 1f, CoordUtils.length(geometry.vertexes, vi + 5), 0.0001f) // tolerance
        }

        val getNormal = { index: Int ->
            val pos = index * geometry.vertexFloatStride + 5
            floatArrayOf(geometry.vertexes[pos], geometry.vertexes[pos + 1], geometry.vertexes[pos + 2])
        }

        val sin45 = sin(PI / 4).toFloat()

        assertArrayEquals(floatArrayOf(-1f, 0f, 0f), getNormal(0), testFloatTolerance)
        assertArrayEquals(
            floatArrayOf(cos(3 * PI / 8).toFloat(), -sin(3 * PI / 8).toFloat(), 0f),
            getNormal(9), testFloatTolerance)
        assertArrayEquals(floatArrayOf(sin45, sin45, 0f), getNormal(21), testFloatTolerance)
    }

    /**
     * due to bug, end stub ring differs from intermediate ring
     */
    @Test
    fun `intermediate ring equals last ring`() {
        val getCoords = { g: Geometry, indexes: Iterable<Int> ->
            indexes.map { idx ->
                val pos = idx * g.vertexFloatStride
                listOf(g.vertexes[pos], g.vertexes[pos + 1], g.vertexes[pos + 2])
            }.flatten()
        }

        val geom1 = bodyShape().run {
            update(mutableListOf<IBodySegment>(
                BodySegment(
                    10.0, 4.0, 1.0,
                    PI/4, beta, 2.0)))
            geometry
        }
        val v1 = getCoords(geom1, (0..12))

        val geom2 = bodyShape().run {
            update(mutableListOf<IBodySegment>(
                BodySegment(
                    10.0, 4.0, 1.0,
                    PI/4, beta, 2.0))
                .append(PI/4, 2.0, false))
            geometry
        }

        val v2 = getCoords(geom2, (0..12))
        assertArraysEqual(v1.toFloatArray(), v2.toFloatArray())
    }

    /**
     * test that each segment starting FROM NOSE (backward) has U (x)
     * according to length and runLength coeff
     */
    @Test
    fun `texture U`() {
        val uPer1Length = 0.1f // 1
        val nFaceSegments = 4
        val segment = mutableListOf<IBodySegment>(
            BodySegment(0.0, 0.0, 0.0, 0.0, beta, 1.0))
            .append(PI / 4, 0.3, true)
            .append(-PI / 4, 1.0, true)

        val geometry =  BodyShape(nFaceSegments, 0.5f, 0f, uPer1Length, 0f).run {
            update(segment);geometry}

        // 3 segments, 6 rings (0-5), check main rings
        val checkRing = { ringIndex: Int, uExpected: Float ->
            val vIndex = nFaceSegments * ringIndex + 1
            (0 until nFaceSegments).forEach {
                assertEquals("Ring #$ringIndex, vertex #$it",
                    uExpected,
                    geometry.vertexes[(vIndex + it) * geometry.vertexFloatStride + 3],
                    testFloatTolerance
                )
            }
        }
        // check U (V) - from head to tail, the ring texture maps to run length

        // tailpoint - v#0
        assertEquals(0.05f + 0.1f + 0.03f + 0.1f + 0.05f, geometry.vertexes[3], testFloatTolerance)
        // check nose (last vertex) is 0
        assertEquals(0f, geometry.vertexes[geometry.vertexCount*geometry.vertexFloatStride - 5], testFloatTolerance)

        // nose/tail are 0.7R from segment end points
        checkRing(5, 0.05f - 0.05f * 0.7f) // nose small ring

        checkRing(4, 0.05f) // nose ring
        checkRing(3, 0.05f + 0.1f)
        checkRing(2, 0.05f + 0.1f + 0.03f) // nose ring
        checkRing(1, 0.05f + 0.1f + 0.03f + 0.1f) // nose ring

        checkRing(0, 0.05f + 0.1f + 0.03f + 0.1f + 0.05f * 0.7f) // tail small ring
    }

    /**
     * test that each point has its assigned V according to start U angle.
     * assume texture is painted from top point (V = 0) down (V = 1)
     */
    @Test
    fun `texture V`() {
        val nFaceSegments = 10
        val geometry = BodyShape(nFaceSegments, 0.5f, 3 * PI.toFloat() / 2, 1f, 0f).run {
            update(mutableListOf<IBodySegment>(
                BodySegment(0.0, 0.0, 0.0, 0.0, beta, 1.0)
            ));geometry
        }

        val expectedV = FloatArray(10) { it * 0.1f}

        for (nRing in 0..3) {
            val ringVI = 1 + nRing * nFaceSegments
            assertArrayEquals(expectedV,
                (0 until nFaceSegments).map { fsi ->
                    geometry.vertexes[(ringVI + fsi) * geometry.vertexFloatStride + 4]
                }.toFloatArray(), testFloatTolerance)
        }

        // nose and tail have 0.5 so far - TODO understand what to put here
        assertEquals(0.5f, geometry.vertexes[4])
        assertEquals(0.5f, geometry.vertexes[geometry.vertexCount*geometry.vertexFloatStride - 4])
    }



    // TODO Test natural shape generation
    /*
    when tailLenth > totalLength - headR = HEAD_R
      straight: tail, 1 ring (of r = bodyRadius), nose (at center of nose ring)

     */

}
