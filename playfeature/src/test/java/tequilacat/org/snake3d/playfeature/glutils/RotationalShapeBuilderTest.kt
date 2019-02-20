package tequilacat.org.snake3d.playfeature.glutils

import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import tequilacat.org.snake3d.playfeature.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class RotationalShapeBuilderTest {
    private class TDS(
        override val centerX: Float,
        override val centerY: Float,
        override val centerZ: Float,
        override val prevLength: Float,
        override val radius: Float,
        var mutableAlpha: Float
    ) : IDirectedSection {
        override val alpha: Float get() = mutableAlpha
    }

    private class TB(private val startX: Number, private val startY: Number, private val floorZ: Number) {
        //val segments = mutableListOf<IDirectedSection>()
        private val segmentsList = mutableListOf<IDirectedSection>()
        val segments get() = segmentsList.asSequence()

        fun add(length: Number, radius: Number, deltaAngle: Number): TB {
            val last: IDirectedSection
            val newAngle: Double

            if(segmentsList.isEmpty()) {
                last = TDS(startX.toFloat(), startY.toFloat(), floorZ.toFloat(), 0f,
                    0f, deltaAngle.toFloat())
                segmentsList.add(last)
                newAngle = deltaAngle.toDouble()
            } else {
                last = segmentsList.last()
                newAngle = last.alpha + deltaAngle.toDouble()
                (last as TDS).mutableAlpha = newAngle.toFloat()
                // modify last alpha
            }

            segmentsList.add(TDS(
                (last.centerX + length.toDouble() * cos(newAngle)).toFloat(),
                (last.centerY + length.toDouble() * sin(newAngle)).toFloat(),
                floorZ.toFloat() + radius.toFloat(),
                length.toFloat(), radius.toFloat(), newAngle.toFloat()))

            return this
        }
    }

    @Before
    fun beforeTests() = mockAndroidStatics()

    @After
    fun afterTests() = unmockkAll()

    @Test
    fun selftest() {

        // test angles
        val body2sections = TB(0.0, 0.0, 0.0)
            .add(10, 1, 2).segments //

        assertArrayEquals(floatArrayOf(2f,2f),
            body2sections.toList().map { s -> s.alpha }.toFloatArray(), testFloatTolerance)

        val body4sections = TB(0.0, 0.0, 0.0)
            .add(1, 1, 1)
            .add(1, 1, 1)
            .add(1, 1, 1)
            .segments
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 3f),
            body4sections.toList().map { s -> s.alpha }.toFloatArray(), testFloatTolerance)



        // test coords
        val body3sectionsCoords = TB(1,2,0)
            .add(1, 2, PI/2)
            .add(2, 1, PI/2)
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

    @Test
    fun `build basicfeatures`() {
        val geom = RotationalShapeBuilder(4, 0f, 1f, 0f)
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
        val geom = RotationalShapeBuilder(4, 0f, 1f, 0f)
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

        val NSEGMENTS_1 = 1
        val segments1 = makeSegments(NSEGMENTS_1)
        val nFaces = 4
        val builder = RotationalShapeBuilder(nFaces, 0f, 1f, 0f)
        builder.update(segments1)
        val prevIndexes = builder.geometry.indexes
        val prevVertexes = builder.geometry.vertexes

        //assertEquals((segments1.size + 1) * 8 * 3, builder.geometry.indexCount)
        assertEquals(3 * (2 * nFaces + (NSEGMENTS_1 - 1) * nFaces * 2), builder.geometry.indexCount)
        assertEquals(
            NSEGMENTS_1 * builder.segmentFaceCount + 2,
            builder.geometry.vertexCount)

        val NSEGMENTS_10 = 10
        val segments2 = makeSegments(NSEGMENTS_10)
        builder.update(segments2)

        assertEquals(3 * (2 * nFaces + (NSEGMENTS_10 - 1) * nFaces * 2), builder.geometry.indexCount)
        assertEquals(
            NSEGMENTS_10 * builder.segmentFaceCount + 2,
            builder.geometry.vertexCount)
        assertEquals(prevIndexes, builder.geometry.indexes)
        assertEquals(prevVertexes, builder.geometry.vertexes)

        val NSEGMENTS_1000 = 1000
        val segments3 = makeSegments(NSEGMENTS_1000)
        builder.update(segments3)

        assertEquals(3 * (2 * nFaces + (NSEGMENTS_1000 - 1) * nFaces * 2), builder.geometry.indexCount)
        assertEquals(
            NSEGMENTS_1000 * builder.segmentFaceCount + 2,
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
        val bodyShape = RotationalShapeBuilder(nFaces, startAngle1, 10f, 0f)
        bodyShape.update(segments)
        val geometry = bodyShape.geometry

        // 3 segments [0-2], all have Z
        (0..2).forEach {
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
        val builder = RotationalShapeBuilder(4, 0f, 1f, 0f)
        // 2 segments
        builder.update(TB(0.0, 0.0, 0.0)
            .add(len.toDouble(), r.toDouble(), 0.0).segments)

        // vertex count to be tested!
        assertEquals(6, builder.geometry.vertexCount)

        // tail is 0 0 0
        assertArrayEquals(floatArrayOf(0f, 0f, 0f),
            builder.geometry.vertexes.sliceArray(0..2), testFloatTolerance)

        // nose is end of 1st segment
        assertVertexData(builder.geometry, builder.geometry.vertexCount - 1,0,
            floatArrayOf(len, 0f, r), 3)

        //  4 vertexes of single segment around [4, 0, 2]
        assertVertexData(builder.geometry, 1,0, floatArrayOf(
            len, -r, r, len, 0f, 2 * r, len, r, r, len, 0f, 0f), 3)

    }

    @Test
    fun `vertex coordinates for 2 segments`() {
        val r1 = 1f
        val r2 = 2f
        val l1 = 10f
        val l2 = 5f
        val builder = RotationalShapeBuilder(4, 0f, 1f, 0f)
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
     * check outer vertexes
     */
    @Test
    fun `vertex rotated coordinates for 3 segments`() {
        val builder = RotationalShapeBuilder(4, 0f, 1f, 0f)
        builder.update(TB(0.0, 0.0, 0.0)
            .add(10, 1, 0.0)
            .add(10, 1, PI/2)
            .add(10, 1, PI/2).segments)
        val stride = builder.geometry.vertexFloatStride
        val vertexes = builder.geometry.vertexes
        val sin45 = sin(PI/4).toFloat()

        assertArrayEquals(floatArrayOf(0f, 0f, 0f), vertexes.sliceArray(0..2), testFloatTolerance)

        // 1st ring
        assertArrayEquals(
            floatArrayOf(10f + sin45, -sin45, 1f),
            vertexes.sliceArray((1 * stride)..(1 * stride + 2)), testFloatTolerance)
        assertArrayEquals(
            floatArrayOf(10f, 0f, 2f),
            vertexes.sliceArray((2 * stride)..(2 * stride + 2)), testFloatTolerance)

        // 2nd ring
        assertArrayEquals(
            floatArrayOf(10f + sin45, 10f + sin45, 1f),
            vertexes.sliceArray((5 * stride)..(5 * stride + 2)), testFloatTolerance)
        assertArrayEquals(
            floatArrayOf(10f, 10f, 2f),
            vertexes.sliceArray((6 * stride)..(6 * stride + 2)), testFloatTolerance)

        // 3nd ring
        assertArrayEquals(
            floatArrayOf(0f, 11f, 1f),
            vertexes.sliceArray((9 * stride)..(9 * stride + 2)), testFloatTolerance)
        assertArrayEquals(
            floatArrayOf(0f, 10f, 2f),
            vertexes.sliceArray((10 * stride)..(10 * stride + 2)), testFloatTolerance)

        // nose
        assertArrayEquals(
            floatArrayOf(0f, 10f, 1f),
            vertexes.sliceArray((13 * stride)..(13 * stride + 2)), testFloatTolerance)
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

        val builder= RotationalShapeBuilder(4, 0f, 1f, 0f)
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

        val builder = RotationalShapeBuilder(nFaceSegments, 0f, uPer1Length, 0f)
        builder.update(TB(0.0, 0.0, 0.0)
            .add(length, 2.0, 0.0)
            .segments)
        val geometry = builder.geometry

        // tail
        assertEquals(0f, geometry.vertexes[3], testFloatTolerance)
        // nose
        assertEquals(length.toFloat() * uPer1Length, geometry.vertexes[geometry.vertexCount * geometry.vertexFloatStride - 5], testFloatTolerance)
        // ring (the single one)
        customAssertRingU(0, geometry, nFaceSegments, length.toFloat() * uPer1Length)
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

        val builder = RotationalShapeBuilder(nFaceSegments, 0f, uPer1Length, 0f)
        builder.update(TB(0.0, 0.0, 0.0)
            .add(l1.toDouble(), r1.toDouble(), 0.0)
            .add(l2.toDouble(), r1.toDouble(), PI / 4)
            .add(l3.toDouble(), r1.toDouble(), -PI / 4).segments)

        val geometry = builder.geometry

        // check U (V) - from tail to had, the ring texture maps to run length

        // head:
        assertEquals((l1 + l2 + l3) * uPer1Length, geometry.vertexes[geometry.vertexCount * geometry.vertexFloatStride - 5], testFloatTolerance)
        // tail:
        assertEquals(0f, geometry.vertexes[3], testFloatTolerance)

        customAssertRingU(2, geometry, nFaceSegments, (l1 + l2 + l3) * uPer1Length) // nose ring same as nose point
        customAssertRingU(1, geometry, nFaceSegments, (l1 + l2) * uPer1Length)
        customAssertRingU(0, geometry, nFaceSegments, l1*uPer1Length)
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
        val geometry = RotationalShapeBuilder(nFaceSegments, startAngle, 1f, 0f).run {
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


    /**
     * how angles are computed purely from coordinates
     * */
   /* @Test
    fun `3d rotation`() {
        val sections = listOf(
            TDS(0f,0f,0f, 0f, 0f, 0f), // v0
            TDS(10f,0f,0f, 0f, 1f, 0f), // v1..4
            TDS(20f,10f,10f, 0f, 1f, 0f), // v5..8
            TDS(20f,10f,20f, 0f, 1f, 0f)) // v9..12 completely upstream
        val geometry = RotationalShapeBuilder(4, 0f, 1f, 0f)
            .run {
                update(sections)
                geometry
            }
        // #1 vertex normal is
    }*/
}
