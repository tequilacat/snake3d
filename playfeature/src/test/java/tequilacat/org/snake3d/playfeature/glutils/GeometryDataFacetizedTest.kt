package tequilacat.org.snake3d.playfeature.glutils


import org.junit.Assert.*
import org.junit.Test

class GeometryDataFacetizedTest {
    @Test(expected = IllegalArgumentException::class)
    fun `facetize noindexes argumentexception`() {
        // no indexes
        GeometryData(
            floatArrayOf(
                0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f
            ), false, false
        ).facetize()
    }

    @Test
    fun `facetize noUV noNormals`() {
        // no indexes
        val geom = GeometryData(
            floatArrayOf(
                0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f
            ), false, false,
            shortArrayOf(0, 1, 2, 0, 3, 1, 0, 2, 3)
        )

        val facetized = geom.facetize()
        assertEquals(geom.hasTexture, facetized.hasTexture)
        assertEquals(6, facetized.vertexFloatStride)
        assertFacettedNormals(facetized)
    }


    @Test
    fun `facetize UV noNormals`() {
        // no indexes
        val geom = GeometryData(
            floatArrayOf(
                0f, 0f, 0f, 0.1f, 0.2f,
                1f, 0f, 0f, 0.3f, 0.4f,
                0f, 1f, 0f, 0.5f, 0.6f,
                0f, 0f, 1f, 0.7f, 0.8f
            ), false, true,
            shortArrayOf(0, 1, 2, 0, 3, 1, 0, 2, 3)
        )

        val facetized = geom.facetize()
        assertEquals(geom.hasTexture, facetized.hasTexture)
        assertEquals(8, facetized.vertexFloatStride)
        assertFacettedNormals(facetized)

        // test that all these vertexes have UV copied correctly
        val assertUV = { vertexIndex: Int, u: Float, v: Float ->
            val pos = vertexIndex * facetized.vertexFloatStride
            facetized.vertexes[pos + 3] == u &&
                    facetized.vertexes[pos + 4] == v

        }

        assertTrue(assertUV(0, 0.1f, 0.2f)) // 0
        assertTrue(assertUV(1, 0.3f, 0.4f)) // 1
        assertTrue(assertUV(2, 0.5f, 0.6f)) // 2
        assertTrue(assertUV(3, 0.1f, 0.2f)) // 0
        assertTrue(assertUV(4, 0.7f, 0.8f)) // 3
        assertTrue(assertUV(5, 0.3f, 0.4f)) // 1
        assertTrue(assertUV(6, 0.1f, 0.2f)) // 0
        assertTrue(assertUV(7, 0.5f, 0.6f)) // 2
        assertTrue(assertUV(8, 0.7f, 0.8f)) // 3
    }

    @Test
    fun `facetize noUV hasNormals`() {
        // no indexes
        val geom = GeometryData(
            floatArrayOf(
                0f, 0f, 0f, 11f, 22f, 33f, // reset all these
                1f, 0f, 0f, 11f, 22f, 33f,
                0f, 1f, 0f, 11f, 22f, 33f,
                0f, 0f, 1f, 11f, 22f, 33f
            ), true, false,
            shortArrayOf(0, 1, 2, 0, 3, 1, 0, 2, 3)
        )

        val facetized = geom.facetize()
        assertEquals(geom.hasTexture, facetized.hasTexture)
        assertEquals(6, facetized.vertexFloatStride)
        assertFacettedNormals(facetized)
    }


    private fun assertFacettedNormals(geometry: GeometryData) {
        assertTrue(geometry.hasNormals)
        // 3 triangles - each has 3 vertexes, generates 3 unique indexes
        assertEquals(9, geometry.vertexCount)
        assertEquals(9, geometry.indexCount)

        assertTrue(geometry.indexes contentEquals shortArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8))

        val checkCoords = { vertexIndex: Int, coords: FloatArray ->
            val pos = vertexIndex * geometry.vertexFloatStride
            geometry.vertexes[pos] == coords[0] &&
                    geometry.vertexes[pos + 1] == coords[1] &&
                    geometry.vertexes[pos + 2] == coords[2]
                    &&
                    geometry.vertexes[pos + geometry.vertexFloatStride - 3] == coords[3] &&
                    geometry.vertexes[pos + geometry.vertexFloatStride - 2] == coords[4] &&
                    geometry.vertexes[pos + geometry.vertexFloatStride - 1] == coords[5]

        }

        // triangle for 0, 1, 2
        // triangle for 0, 3, 1
        // triangle for 0, 2, 3

        // normal is 0,0,1 upZ
        assertTrue(checkCoords(0, floatArrayOf(0f, 0f, 0f, 0f, 0f, 1f))) // 0
        assertTrue(checkCoords(1, floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f))) // 1
        assertTrue(checkCoords(2, floatArrayOf(0f, 1f, 0f, 0f, 0f, 1f))) // 2

        // normal is 0, 1, 0
        assertTrue(checkCoords(3, floatArrayOf(0f, 0f, 0f, 0f, 1f, 0f))) // 0
        assertTrue(checkCoords(4, floatArrayOf(0f, 0f, 1f, 0f, 1f, 0f))) // 3
        assertTrue(checkCoords(5, floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f))) // 1

        // normal is 1,0,0
        assertTrue(checkCoords(6, floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f))) // 0
        assertTrue(checkCoords(7, floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f))) // 2
        assertTrue(checkCoords(8, floatArrayOf(0f, 0f, 1f, 1f, 0f, 0f))) // 3
    }
}