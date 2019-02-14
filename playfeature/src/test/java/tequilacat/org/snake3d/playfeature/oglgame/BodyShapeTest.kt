package tequilacat.org.snake3d.playfeature.oglgame

import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import tequilacat.org.snake3d.playfeature.*
import tequilacat.org.snake3d.playfeature.glutils.Geometry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BodyShapeTest {
    class TB(private val startX: Double, private val startY: Double, private val floorZ: Double) {

        private class TBS(
            override val startX: Float,
            override val startY: Float,
            floorZ: Float,
            //override val startZ: Float,
            override val length: Float,
            override val startRadius: Float,
            override val endRadius: Float,
            override val alpha: Float
        ) : IBodySegmentModel {

            override var endX: Float = 0f
            override var endY: Float = 0f

            override val startZ = floorZ + startRadius
            override val endZ = floorZ + endRadius

            override val alphaSinus = sin(alpha)
            override val alphaCosinus = cos(alpha)

            init {
                computeEnd()
            }

            private fun computeEnd() {
                endX = startX + length * alphaCosinus
                endY = startY + length * alphaSinus
            }
        }

        val segments = mutableListOf<IBodySegmentModel>()

        fun add(length: Double, endRadius: Double, deltaAngle: Double): TB {
            if(segments.isEmpty()) {
                segments.add(TBS(startX.toFloat(), startY.toFloat(), floorZ.toFloat(), length.toFloat(),
                    0.0f, endRadius.toFloat(), deltaAngle.toFloat()))
            }else {
                val last = segments.last()
                segments.add(TBS(last.endX, last.endY, floorZ.toFloat(), length.toFloat(),
                    last.endRadius, endRadius.toFloat(), last.alpha + deltaAngle.toFloat()))
            }
            return this
        }
    }

    @Before
    fun beforeTests() = mockAndroidStatics()

    @After
    fun afterTests() = unmockkAll()

    @Test
    fun `build basicfeatures`() {
        val geom = BodyShapeBuilder(4, 0f, 1f, 0f)
            .run {
                update(TB(0.0, 0.0, 0.0).add(1.0, 1.0, 0.0).segments)
                geometry
            }

        assertTrue(geom.hasNormals)
        assertTrue(geom.hasTexture)
        assertEquals(8, geom.vertexFloatStride)
    }

    @Test
    fun indexes() {
        // BodyShape(4, testRadius,0f,1f, 0f)
        val geom = BodyShapeBuilder(4, 0f, 1f, 0f)
            .run {
                update(TB(0.0, 0.0, 0.0)
                    .add(1.0, 1.0, 0.0) // 4 segments to fit test data
                    .add(1.0, 1.0, 1.0)
                    .add(1.0, 1.0, 1.0)
                    .add(1.0, 1.0, 1.0)
                    .segments)
                geometry
            }

        // expected indexes for 2 segments: 3 rings and 2 pointed ends
        val expectedIndexes = shortArrayOf(
            0, 1, 2, 0, 2, 3, 0, 3, 4, 0, 4, 1,
            1, 5, 6, 1, 6, 2, 2, 6, 7, 2, 7, 3, 3, 7, 8, 3, 8, 4, 4, 8, 5, 4, 5, 1,
            5, 9, 10, 5, 10, 6, 6, 10, 11, 6, 11, 7, 7, 11, 12, 7, 12, 8, 8, 12, 9, 8, 9, 5,
            9, 13, 14, 9, 14, 10, 10, 14, 15, 10, 15, 11, 11, 15, 16, 11, 16, 12, 12, 16, 13, 12, 13, 9,
            13, 17, 14, 14, 17, 15, 15, 17, 16, 16, 17, 13
        )

        assertArrayEquals(expectedIndexes,  geom.indexes.sliceArray(0 until geom.indexCount))
    }

    /**
     * Tests how index and vertex count are computed
     *
     * Also test (on expected hardcoded sizes) that allocated is increased first (and arrays differ by reference)
     *  then on next allocation sizes differ while array is same
     */
    @Test
    fun `counts and array reallocation`() {
        val makeSegments = {i:Int -> TB(0.0, 0.0, 0.0).run {
            (1..i).forEach { _ -> add(1.0, 1.0, 0.0) }
            this
        }.segments}

        val segments1 = makeSegments(1)
        val nFaces = 4
        val builder = BodyShapeBuilder(nFaces, 0f, 1f, 0f)
        builder.update(segments1)
        val prevIndexes = builder.geometry.indexes
        val prevVertexes = builder.geometry.vertexes

        //assertEquals((segments1.size + 1) * 8 * 3, builder.geometry.indexCount)
        assertEquals(3 * (2 * nFaces + (segments1.size - 1) * nFaces * 2), builder.geometry.indexCount)
        assertEquals(
            segments1.size * builder.segmentFaceCount + 2,
            builder.geometry.vertexCount)

        val segments2 = makeSegments(10)
        builder.update(segments2)

        assertEquals(3 * (2 * nFaces + (segments2.size - 1) * nFaces * 2), builder.geometry.indexCount)
        assertEquals(
            segments2.size * builder.segmentFaceCount + 2,
            builder.geometry.vertexCount)
        assertEquals(prevIndexes, builder.geometry.indexes)
        assertEquals(prevVertexes, builder.geometry.vertexes)

        val segments3 = makeSegments(1000)
        builder.update(segments3)

        assertEquals(3 * (2 * nFaces + (segments3.size - 1) * nFaces * 2), builder.geometry.indexCount)
        assertEquals(
            segments3.size * builder.segmentFaceCount + 2,
            builder.geometry.vertexCount)
        assertNotEquals(prevIndexes, builder.geometry.indexes)
        assertNotEquals(prevVertexes, builder.geometry.vertexes)
    }

    /////////////////////////////////////////////////
    // Vertex test

    /**
     * how the start angle affects vertex positions in section
     */
    @Test
    fun `vertex ring start`() {
        val nFaces = 5
        // floor -1, R 1 means all "ends" have Z=0
        val segments = TB(0.0, 0.0, 0.0)
            .add(10.0, 1.0, 0.0)
            .add(10.0, 1.0, PI / 4)
            .add(10.0, 1.0, PI / 4).segments

        val startAngle1 = 1.0f // just some angle in Q1
        // make 5 so Z is never repeated for same section
        val bodyShape =  BodyShapeBuilder(nFaces, startAngle1, 10f, 0f)
        bodyShape.update(segments)
        val geometry = bodyShape.geometry

        // 3 segments, all have Z
        (0 until segments.size).forEach {
            assertEquals(sin(startAngle1) + 1,
                geometry.vertexes[(1 + nFaces * it) * geometry.vertexFloatStride + 2],
                testFloatTolerance)
        }
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
                    testFloatTolerance)
            }

            expIndex += expectedDataStride
            bodyVertexIndex += bodyGeometry.vertexFloatStride
            testedVertex++
        }
    }

    @Test
    fun `vertex coordinates for 1 segment`() {
        val len = 4.0f
        val r = 2.0f
        val builder = BodyShapeBuilder(4, 0f, 1f, 0f)
        // 2 segments
        builder.update(TB(0.0, 0.0, 0.0)
            .add(len.toDouble(), r.toDouble(), 0.0).segments)

        // vertex count to be tested!
        assertEquals(6, builder.geometry.vertexCount)

        // tail is 0 0 0
        assertArrayEquals(floatArrayOf(0f, 0f, 0f),
            builder.geometry.vertexes.sliceArray(0..2), testFloatTolerance)

        //  4 vertexes of single segment around [4, 0, 2]
        assertVertexData(builder.geometry, 1,0, floatArrayOf(
            len, -r, r, len, 0f, 2 * r, len, r, r, len, 0f, 0f), 3)

        // nose is end of 1st segment
        assertVertexData(builder.geometry, builder.geometry.vertexCount - 1,0,
            floatArrayOf(len, 0f, r), 3)
    }

    @Test
    fun `vertex coordinates for 2 segments`() {

//        val segments = mutableListOf(BodySegment(0.0, 0.0, testRadius.toDouble(),
//            0.0, 10.0))
        //val builder =  bodyShape()
        // BodyShape(4, testRadius,0f,1f, 0f)

        val r1 = 1f
        val r2 = 2f
        val l1 = 10f
        val l2 = 5f
        val builder = BodyShapeBuilder(4, 0f, 1f, 0f)
        builder.update(TB(0.0, 0.0, 0.0)
            .add(l1.toDouble(), r1.toDouble(), 0.0)
            .add(l2.toDouble(), r2.toDouble(), PI / 4).segments)

        val sin45 = sin(PI / 4).toFloat()
        val sinPi8 = sin(PI / 8).toFloat()
        val cosPi8 = cos(PI / 8).toFloat()

        assertEquals(10, builder.geometry.vertexCount)

        // now examine deeper the expected 3d coords:
        // first vertex (tail)

        // tail point
        assertArrayEquals(floatArrayOf(0f, 0f, 0f),
            builder.geometry.vertexes.sliceArray(0..2), testFloatTolerance)


        // skip starting intermediate ring as it's to be adjusted for better visuals

        // then joining
        assertVertexData(builder.geometry, 1,0, floatArrayOf(
            // 1st ring center @L1, 0, r1 = not in this test
            // l1, -r1, r1, l1, 0f, r1*2, l1, r1, r1, l1, 0f, 0f,

            // 1,2,3,4
            // joint (rotated by pi/8) aka start last segment
            l1 + sinPi8 * r1, -cosPi8 * r1, r1,
            l1, 0f, r1*2,
            l1 - sinPi8 * r1, cosPi8 * r1, r1,
            l1, 0f, 0f,

            // 5,6,7,8 (last face)S
            l1 + l2 * sin45 + r2 * sin45, l2 * sin45 - r2 * sin45, r2,
            l1 + l2 * sin45, l2 * sin45, r2*2,
            l1 + l2 * sin45 - r2 * sin45, l2 * sin45 + r2 * sin45, r2,
            l1 + l2 * sin45, l2 * sin45, 0f
            ), 3)


        // last one (nose) is center of last circle
        assertVertexData(builder.geometry, builder.geometry.vertexCount - 1,0, floatArrayOf(
            l1 + l2 * sin45, l2 * sin45, r2
        ), 3)
    }

    /**
     * test that each vertex in a triangle has same normal which is normalized -
     * dont compar to the normal of its triangle!
     * */
    @Test
    fun normals() {
        val r1 = 1f
        val l1 = 5f
        val l2 = 5f

        val builder= BodyShapeBuilder(4, 0f, 1f, 0f)
        // make 3 segments to test ring#2 since testing normals of 1st ring connected to tail is too complex
        builder.update(TB(0.0, 0.0, 0.0)
            .add(l1.toDouble(), r1.toDouble(), 0.0)
            .add(l1.toDouble(), r1.toDouble(), 0.0)
            .add(l2.toDouble(), r1.toDouble(), PI / 4).segments)
        val geometry = builder.geometry

        // check all normals have length = 1
        for (vi in 0 until geometry.vertexCount step geometry.vertexFloatStride) {
            assertEquals("Bad normal at vi=$vi",
                1f, TestUtils.computeVectorLength(geometry.vertexes, vi + 5), testFloatTolerance) // tolerance
        }

        val getNormal = { index: Int ->
            val pos = index * geometry.vertexFloatStride + 5
            floatArrayOf(geometry.vertexes[pos], geometry.vertexes[pos + 1], geometry.vertexes[pos + 2])
        }

        // tail
        assertArrayEquals(floatArrayOf(-1f, 0f, 0f), getNormal(0), testFloatTolerance)
        // nose
        val sin45 = sin(PI / 4).toFloat()
        assertArrayEquals(floatArrayOf(sin45, sin45, 0f), getNormal(13), testFloatTolerance)

        assertArrayEquals(
            floatArrayOf(sin(PI / 8).toFloat(), -cos( PI / 8).toFloat(), 0f),
            getNormal(5), testFloatTolerance)
    }

    private fun customAssertRingU(ringIndex: Int, geometry: Geometry, nFaceSegments: Int, uExpected: Float) {
        val vIndex = nFaceSegments * ringIndex + 1
        (0 until nFaceSegments).forEach {
            assertEquals("Ring #$ringIndex, vertex #$it",
                uExpected,
                geometry.vertexes[(vIndex + it) * geometry.vertexFloatStride + 3],
                testFloatTolerance
            )
        }
    }

    @Test
    fun `texture U 1 segment`() {
        val uPer1Length = 0.1f
        val length = 10.0
        val nFaceSegments = 4

        val builder = BodyShapeBuilder(nFaceSegments, 0f, uPer1Length, 0f)
        builder.update(TB(0.0, 0.0, 0.0)
            .add(length, 2.0, 0.0)
            .segments)
        val geometry = builder.geometry

        // nose
        assertEquals(0f, geometry.vertexes[geometry.vertexCount * geometry.vertexFloatStride - 5], testFloatTolerance)
        // tail
        assertEquals(length.toFloat() * uPer1Length, geometry.vertexes[3], testFloatTolerance)
        // ring (the single one)
        customAssertRingU(0, geometry, nFaceSegments, 0f)
    }


    /**
     * test that each segment starting FROM NOSE (backward) has U (x)
     * according to length and runLength coeff
     */
    @Test
    fun `texture U`() {
        val uPer1Length = 0.1f
        val nFaceSegments = 4
        val l1 = 1f
        val l2 = 0.3f
        val l3 = 2f
        val r1 = 0.5f

        val builder = BodyShapeBuilder(nFaceSegments, 0f, uPer1Length, 0f)
        builder.update(TB(0.0, 0.0, 0.0)
            .add(l1.toDouble(), r1.toDouble(), 0.0)
            .add(l2.toDouble(), r1.toDouble(), PI / 4)
            .add(l3.toDouble(), r1.toDouble(), -PI / 4).segments)

        val geometry = builder.geometry

        // check U (V) - from head to tail, the ring texture maps to run length

        // tailpoint - v#0
        // check nose (last vertex) is 0
        assertEquals(0f, geometry.vertexes[geometry.vertexCount * geometry.vertexFloatStride - 5], testFloatTolerance)
        // tail point
        assertEquals((l1 + l2 + l3) * uPer1Length, geometry.vertexes[3], testFloatTolerance)

        customAssertRingU(2, geometry, nFaceSegments, 0f) // nose ring same as nose point
        customAssertRingU(1, geometry, nFaceSegments, l3 * uPer1Length)
        customAssertRingU(0, geometry, nFaceSegments, (l2 + l3) * uPer1Length)
    }

    /**
     * test that each point has its assigned V according to start U angle.
     * assume texture is painted from top point (V = 0) down (V = 1)
     */
    @Test
    fun `texture V`() {
        val nFaceSegments = 10
        val startAngle = 3 * PI.toFloat() / 2
        val r1 = 0.5f
        val l1 = 1f

        // make 2 segments (2 rings to check)
        val geometry = BodyShapeBuilder(nFaceSegments, startAngle, 1f, 0f).run {
            update(TB(0.0, 0.0, 0.0)
                .add(l1.toDouble(), r1.toDouble(), 1.0)
                .add(l1.toDouble(), r1.toDouble(), 2.0).segments)
            geometry
        }

        val expectedV = FloatArray(10) { it * 0.1f }

        // for 2 segments
        for (nRing in 0..1) {
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
}
