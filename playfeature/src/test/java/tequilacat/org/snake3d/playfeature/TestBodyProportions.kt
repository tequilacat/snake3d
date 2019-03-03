package tequilacat.org.snake3d.playfeature

import org.junit.Assert
import org.junit.Test
import kotlin.math.min

class TestConicalProportionsTest {

    @Test
    fun `selftest TestConicalProportions advance more than feed`() {
        val radius = 0.1
        val feedSize = 0.5

        // 1 segment body, 2 segments head
        val props = TestConicalProportions(doubleArrayOfNumbers(0.1, radius/2, 0.8, radius, 0.9, radius*2, 1, radius),
            1, feedSize)
        props.resize(10.0)

        Assert.assertEquals(8.0, props.lengthToNeck, testDoubleTolerance)
        Assert.assertEquals(10.0, props.fullLength, testDoubleTolerance)

        Assert.assertArrayEquals(
            doubleArrayOfNumbers(1, radius / 2, 8, radius, 9, radius * 2, 10, radius),
            (0 until props.segmentCount).flatMap { i -> listOf(props.segmentEndFromTail(i), props.segmentRadius(i)) }
                .toDoubleArray(), testDoubleTolerance)

        props.feed()
        props.advance(1.0) // more than feed - same

        Assert.assertEquals(8.5, props.lengthToNeck, testDoubleTolerance)
        Assert.assertEquals(10.5, props.fullLength, testDoubleTolerance)
        Assert.assertArrayEquals(
            doubleArrayOfNumbers(1, radius / 2, 8.5, radius, 9.5, radius * 2, 10.5, radius),
            (0 until props.segmentCount).flatMap { i -> listOf(props.segmentEndFromTail(i), props.segmentRadius(i)) }
                .toDoubleArray(), testDoubleTolerance)
    }

    @Test
    fun `selftest TestConicalProportions advance less than feed`() {
        val radius = 0.1
        val feedSize = 1.0

        // 1 segment body, 2 segments head
        val props = TestConicalProportions(doubleArrayOfNumbers(0.1, radius/2, 0.8, radius, 0.9, radius*2, 1, radius),
            1, feedSize)
        props.resize(10.0)

        props.feed()
        props.advance(0.5) // more than feed - same

        Assert.assertEquals(8.5, props.lengthToNeck, testDoubleTolerance)
        Assert.assertEquals(10.5, props.fullLength, testDoubleTolerance)
        Assert.assertArrayEquals(
            doubleArrayOfNumbers(1, radius / 2, 8.5, radius, 9.5, radius * 2, 10.5, radius),
            (0 until props.segmentCount).flatMap { i -> listOf(props.segmentEndFromTail(i), props.segmentRadius(i)) }
                .toDoubleArray(), testDoubleTolerance)
    }

}

/**
 * used for tests
 * */
class TestConicalProportions(
    private val relativeLR: DoubleArray,
    private val firstFarRing: Int,
    private val feedSize: Double
) : IFeedableBodyProportions {

    override var fullLength = 0.0
    override val segmentCount = relativeLR.size / 2
    override val neckIndex = segmentCount - 3 // neck then nose/headR

    private var remainingFoodLen = 0.0
    private var firstLength = 0.0

    override fun resize(bodyLength: Double) {
        firstLength = bodyLength
        fullLength = bodyLength
        remainingFoodLen = 0.0
    }

    override fun segmentEndFromTail(index: Int) = if (index < firstFarRing)
        relativeLR[index * 2] * firstLength
    else
        fullLength - (firstLength - relativeLR[index * 2] * firstLength)

    override fun segmentRadius(index: Int) = relativeLR[index * 2 + 1]

    override fun advance(distance: Double) {
        if(remainingFoodLen > 0) {
            val consumedLength = min(remainingFoodLen, distance)
            remainingFoodLen -= consumedLength
            fullLength += consumedLength
        }
    }

    override fun feed() {
        remainingFoodLen += feedSize
    }

}
