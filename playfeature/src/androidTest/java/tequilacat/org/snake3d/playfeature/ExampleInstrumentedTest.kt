package tequilacat.org.snake3d.playfeature

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("tequilacat.org.snake3d.playfeature.test", appContext.packageName)
    }

/*
    @Test
    fun measureUpdatePerformance() {
        val testRadius = 1f
        val shape = BodyShape(6, testRadius)
        val segments = mutableListOf<IBodySegment>(
            BodySegment(0.0, 0.0, testRadius.toDouble(), 0.0, 10.0))
        // 50 segments
        (1..50).forEach { segments.append(PI / 4, 10.0, true) }

        val count = 1000

        val t0 = System.currentTimeMillis() // SystemClock.uptimeMillis()

        for(i in 0 until count) {
            shape.update(segments)
        }

        val t1 = System.currentTimeMillis()

        Log.d("perf", "Total: ${(t1-t0)} ms")
        Log.d("perf", "Per invocation: ${(t1-t0) / count.toFloat()} ms")
    }
*/
}
