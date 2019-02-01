package tequilacat.org.snake3d.playfeature.glutils

import org.junit.Test

import org.junit.Assert.*
import tequilacat.org.snake3d.playfeature.testFloatTolerance

class UtilsKtTest {
    // test that capacity is respected or default, with exceptions also
    @Test
    fun `ShortArray toBuffer default`() {
        assertEquals(10, FloatArray(10).toBuffer().capacity())
    }

    @Test
    fun `ShortArray toBuffer explicit`() {
        assertEquals(2, FloatArray(10).toBuffer(2).capacity())
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `ShortArray toBuffer badSize`() {
        FloatArray(2).toBuffer(10)
    }

    // test that capacity is respected or default, with exceptions also
    @Test
    fun `FloatArray toBuffer default`() {
        assertEquals(10, FloatArray(10).toBuffer().capacity())
    }

    @Test
    fun `FloatArray toBuffer explicit`() {
        assertEquals(2, FloatArray(10).toBuffer(2).capacity())
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `FloatArray toBuffer badSize`() {
        FloatArray(2).toBuffer(10)
    }

    @Test
    fun `crossproduct of point`() {
        val vertexes = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val normal = FloatArray(3)
        CoordUtils.crossProduct(normal, 0, vertexes, 0, 1, 2, 3)
        assertArrayEquals("crossproduct of point must be 0,0,0",
            floatArrayOf(0f, 0f, 0f), normal, testFloatTolerance)
    }

    @Test
    fun `crossproduct of line`() {
        val vertexes = floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f)
        val normal = FloatArray(3)
        CoordUtils.crossProduct(normal, 0, vertexes, 0, 1, 2, 3)
        assertArrayEquals("crossproduct of line must be 0,0,0",
            floatArrayOf(0f, 0f, 0f), normal, testFloatTolerance)
    }
}