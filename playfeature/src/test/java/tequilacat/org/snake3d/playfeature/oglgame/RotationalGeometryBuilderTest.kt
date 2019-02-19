package tequilacat.org.snake3d.playfeature.oglgame

import io.mockk.unmockkAll
import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import tequilacat.org.snake3d.playfeature.glutils.RotationalGeometryBuilder
import tequilacat.org.snake3d.playfeature.mockAndroidStatics
import tequilacat.org.snake3d.playfeature.testFloatTolerance
import kotlin.math.hypot

class RotationalGeometryBuilderTest {
    @Before
    fun beforeTests() = mockAndroidStatics()
    @After
    fun afterTests() = unmockkAll()

    private val aX = 0f
    private val aY = 0f
    private val aZ = 1f
    private val faceCount = 4

    // @Test
    // TODO activate rotation tests when implemented
    fun `simple case`() {
        // this adds 2 points at the ends implicitly, the rings are start and end of U
        // <d,r>, <d,r>
        val radiuses = floatArrayOf(0f, 1f, 2f, 1f)
        val geom = RotationalGeometryBuilder()
            .build(radiuses, aX, aY, aZ, faceCount)

        // added ultra-thin with U:1->0
        val realFC = faceCount + 1

        // check vertex count, assuming start/end vertexes are added
        // vertexes: given faces + 2 + ends
        assertEquals(2 * realFC + 2, geom.vertexCount)

        // index count: everything is smooth so (ringcount-1)*fc*2 + fc*2 (ends)
        assertEquals((realFC * 2 + realFC * 2) * 3, geom.indexCount)

        // check end/head coords
        assertArrayEquals(floatArrayOf(0f, 0f, 0f),
            geom.vertexes.sliceArray(0..2), testFloatTolerance
        )

        val lastIndex = (geom.vertexCount - 1) * geom.vertexFloatStride
        assertArrayEquals(
            floatArrayOf(0f, 0f, 2f),
            geom.vertexes.sliceArray(lastIndex..lastIndex + 2),
            testFloatTolerance)

        // check is symmetric around given axis and have predefined distance from 0Z

        for (i in 1..5) {
            // all have x,y equidistant and Z=0
            assertEquals(1f, hypot( geom.vertexes[i * geom.vertexFloatStride],
                geom.vertexes[i * geom.vertexFloatStride + 1]), testFloatTolerance)
            assertEquals(0f, geom.vertexes[i * geom.vertexFloatStride + 2], testFloatTolerance)
        }

        for (i in 6..10) {
            // all have x,y equidistant and Z=2
            assertEquals(1f, hypot( geom.vertexes[i * geom.vertexFloatStride],
                geom.vertexes[i * geom.vertexFloatStride + 1]), testFloatTolerance)
            assertEquals(2f, geom.vertexes[i * geom.vertexFloatStride + 2], testFloatTolerance)
        }

        // check that one face is extra thin

        // check normals of ring vertexes are smooth,
        // check normals of ends are coaxial to specified axis
    }
}