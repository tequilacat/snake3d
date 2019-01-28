package tequilacat.org.snake3d.playfeature.glutils

import org.junit.Test

import org.junit.Assert.*

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
}