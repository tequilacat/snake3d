package tequilacat.org.snake3d.playfeature

import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

const val testFloatTolerance = 0.00001f
const val testDoubleTolerance = 0.000001

fun mockAndroidStatics() {
    mockkStatic(SystemClock::class)
    mockkStatic(Matrix::class)

    every { SystemClock.uptimeMillis() } answers { System.currentTimeMillis() }
    val slotX = slot<Float>()
    val slotY = slot<Float>()
    val slotZ = slot<Float>()

    every { Matrix.length(capture(slotX), capture(slotY), capture(slotZ)) } answers {
        TestUtils.computeVectorLength(floatArrayOf(slotX.captured, slotY.captured, slotZ.captured), 0)
    }

    mockkStatic(Log::class)
    every { Log.d(any(),any()) } returns 0
}

fun assertArraysEqual(v1: FloatArray, v2: FloatArray) {
    Assert.assertEquals(v1.size, v2.size)
    if (! (v1 contentEquals v2)) {
        val s1 = v1.contentToString()
        val s2 = v2.contentToString()

        for(i in v1.indices) {
            Assert.assertEquals(
                "Elem $i: expected = ${v1[i]} real = ${v2[i]} \n#1: $s1 \n#2: $s2",
                v1[i], v2[i]
            )
        }
    }
}

class TestUtils {
    companion object {
        fun computeVectorLength(v: FloatArray, pos: Int = 0) =
            sqrt(v[pos] * v[pos] + v[pos + 1] * v[pos + 1] + v[pos + 2] * v[pos + 2])
    }

    @Before
    fun beforeTests() = mockAndroidStatics()

    @After
    fun afterTests() = unmockkAll()

    /** Test self*/
    @Test
    fun `mock statics`() {
        val t0 = SystemClock.uptimeMillis()
        Thread.sleep(10)
        val t1 = SystemClock.uptimeMillis()
        Assert.assertTrue(t1 > t0)

        val singleMatrix = floatArrayOf(1f, 2f, 3f)

        Assert.assertEquals(
            computeVectorLength(singleMatrix, 0),
            Matrix.length(singleMatrix[0], singleMatrix[1], singleMatrix[2])
        )

        Log.d("just", "0")
    }
}