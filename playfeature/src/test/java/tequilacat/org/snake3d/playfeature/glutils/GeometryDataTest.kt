package tequilacat.org.snake3d.playfeature.glutils

import org.junit.Test

import org.junit.Assert.*

class GeometryDataTest {

    // 3 vertexes having texture UV (5 floats per)
    val vertexes_3_hasUV = FloatArray(3 * 5) { i -> i.toFloat() }

    @Test
    fun `vertexStride noNormal noUV`() {
        val sampleVertexCount = 3
        assertEquals(3, GeometryData(
            FloatArray(sampleVertexCount * 3 /* 3 coords only*/),
            hasNormals = false, hasTexture = false).vertexFloatStride)
    }

    @Test
    fun `vertexStride hasNormal noUV`() {
        val sampleVertexCount = 3
        assertEquals(6, GeometryData(
            FloatArray(sampleVertexCount * 6 /* 3 coords + 3 normalvec coords*/),
            hasNormals = true, hasTexture = false).vertexFloatStride)
    }

    @Test
    fun `vertexStride noNormal hasUV`() {
        val sampleVertexCount = 3
        assertEquals(5, GeometryData(
            FloatArray(sampleVertexCount * 5 /* 3 coords + 2 UV*/),
            hasNormals = false, hasTexture = true).vertexFloatStride)
    }

    @Test
    fun `vertexStride hasNormal hasUV`() {
        val sampleVertexCount = 3
        assertEquals(8, GeometryData(
            FloatArray(sampleVertexCount * 8 /* 3 coords + 3 normalvec coords + 2UV*/),
            hasNormals = true, hasTexture = true).vertexFloatStride)
    }





    // checks that when nonspecified size, vertex size is used otherwise what's specified
    @Test
    fun `vertexCount UV defaultSize`() {
        // no indexes
        val data = GeometryData(vertexes_3_hasUV, hasNormals = false, hasTexture = true)
        assertEquals(3, data.vertexCount)
    }


    @Test
    fun `vertexCount UV specifiedSize`() {
        // no indexes
        val data = GeometryData(
            FloatArray(100), 2,
            Empty.ShortArray, 0, false, true)
        assertEquals(2, data.vertexCount)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `vertexCount UV badrange`() {
        // array contains 4 vertexes, specify 5
        GeometryData(FloatArray(3 * 4), 5,
            Empty.ShortArray, 0, false, false)
    }

    @Test
    fun `indexCount default empty`() {
        // no indexes
        assertEquals(0, GeometryData(FloatArray(3), hasNormals = false, hasTexture = false).indexCount)
    }

    @Test
    fun `indexCount default count`() {
        // default indexes
        assertEquals(10,
            GeometryData(FloatArray(3), hasNormals = false, hasTexture = false, indexes = ShortArray(10)).indexCount)
    }

    @Test
    fun `indexCount overridden`() {
        // ovrride indexes
        assertEquals(5, GeometryData(FloatArray(3), 1, // xyz one inst - we need to specify
            indexes = ShortArray(10), indexCount = 5,
            hasNormals = false, hasTexture = false).indexCount)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `indexCount badrange`() {
        // ovrride indexes
        GeometryData(FloatArray(3), 1, // xyz one inst - we need to specify
            indexes = ShortArray(5), indexCount = 10,
            hasNormals = false, hasTexture = false)
    }

}