package tequilacat.org.snake3d.playfeature.glutils

import org.junit.Assert.*
import org.junit.Test

class GeometryBuilderTest{

    private fun testinstance_basicQuad() = GeometryBuilder()
        .apply {
            addQuad(
                0f, 0f, 0f,
                2f, 0f, 0f,
                2f, 2f, 0f,
                0f, 2f, 0f
            )
        }.build()

    private fun testinstance_texturedQuad() = GeometryBuilder()
        .apply {
            addQuad(
                0f, 0f, 0f,
                2f, 0f, 0f,
                2f, 2f, 0f,
                0f, 2f, 0f,
                // for all corners add U and V: U0, V0, U2, V2 (0 and 2 are corners)
                floatArrayOf(1.1f, 1.2f, 2.1f, 2.2f)
            )
        }.build()

    // by default has normals - always has normals!
    @Test
    fun `addQuad basic hasNormals`() {
        assertTrue(testinstance_basicQuad().hasNormals)
    }

    // check it has indexes exactly as for quad of 2 triangles
    @Test
    fun `addQuad basic hasIndexes`() {
        // check add quad w/o normals and
        val geom = testinstance_basicQuad()
        assertTrue(geom.hasIndexes)
        assertTrue(shortArrayOf(0, 1, 2, 0, 2, 3) contentEquals geom.indexes)
        // assert indexes are specified correctly
    }

    // by default no texture
    @Test
    fun `addQuad basic hasTexture no`() {
        // check add quad w/o normals and
        assertFalse(testinstance_basicQuad().hasTexture)
    }

    // by default 2 coordsets by 3 coord each
    @Test
    fun `addQuad basic vertexStride`() {
        // check add quad w/o normals and
        assertEquals(6 * BYTES_PER_FLOAT, testinstance_basicQuad().vertexStride)
    }

    // test basic vertexes
    @Test
    fun `addQuad basic vertexes`() {
        // check add quad w/o normals and
        val geom = testinstance_basicQuad()

        // 0,1,2,0,2,3
        // normals: 0f,0f,1f
        val expectedVertexes = floatArrayOf(
            0f, 0f, 0f, 0f, 0f, 1f, // last 3 are normals
            2f, 0f, 0f, 0f, 0f, 1f,
            2f, 2f, 0f, 0f, 0f, 1f,
//            0f, 0f, 0f, 0f, 0f, 1f,
//            2f, 2f, 0f, 0f, 0f, 1f,
            0f, 2f, 0f, 0f, 0f, 1f
        )

        assertArraysEqual(expectedVertexes, geom.vertexes)
    }

    private fun assertArraysEqual(v1: FloatArray, v2: FloatArray) {
        assertEquals(v1.size,v2.size)
        if (! (v1 contentEquals v2)) {
            val s1 = v1.contentToString()
            val s2 = v2.contentToString()

            for(i in v1.indices) {
                assertEquals("Elem $i: expected = ${v1[i]} real = ${v2[i]} \n#1: $s1 \n#2: $s2",
                    v1[i], v2[i])
            }
        }
    }

    //////////////////////////////////
    // textured - results are same but the sizes and contents of vertexes differ


    // by default has normals - always has normals!
    @Test
    fun `addQuad textured hasNormals`() {
        assertTrue(testinstance_texturedQuad().hasNormals)
    }

    // check it has indexes exactly as for quad of 2 triangles
    @Test
    fun `addQuad textured hasIndexes`() {
        // check add quad w/o normals and
        val geom = testinstance_texturedQuad()
        assertTrue(geom.hasIndexes)
        assertTrue(shortArrayOf(0, 1, 2, 0, 2, 3) contentEquals geom.indexes)
        // assert indexes are specified correctly
    }

    // by default no texture
    @Test
    fun `addQuad textured hasTexture`() {
        // check add quad w/o normals and
        assertTrue(testinstance_texturedQuad().hasTexture)
    }

    // by default 2 coordsets by 3 coord each + 2 for UV
    @Test
    fun `addQuad textured vertexStride`() {
        // check add quad w/o normals and
        assertEquals(8 * BYTES_PER_FLOAT, testinstance_texturedQuad().vertexStride)
    }

    // test basic vertexes
    @Test
    fun `addQuad textured vertexes`() {
        // check add quad w/o normals and
        val geom = testinstance_texturedQuad()


        // 1.1f, 1.2f, 2.1f, 2.2f
        // 0,1,2,0,2,3
        // normals: 0f,0f,1f
        val expectedVertexes = floatArrayOf(
            0f, 0f, 0f,  1.1f, 1.2f,     0f, 0f, 1f, // last 3 are normals
            2f, 0f, 0f,  2.1f, 1.2f,     0f, 0f, 1f,
            2f, 2f, 0f,  2.1f, 2.2f,    0f, 0f, 1f,
            0f, 2f, 0f,  1.1f, 2.2f,    0f, 0f, 1f
        )

        assertArraysEqual(expectedVertexes, geom.vertexes)
    }

}