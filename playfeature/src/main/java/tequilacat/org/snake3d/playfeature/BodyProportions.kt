package tequilacat.org.snake3d.playfeature

/**
 * defines "breaks" in linear advance of radius on imaginary straight body. */
interface IBodyProportions {
    val segmentCount: Int

    val headOffset: Double
    val headRadius: Double

    fun resize(bodyLength: Double)
    fun segmentEndFromTail(index: Int): Double
    fun segmentRadius(index: Int): Double
}

// TODO remove TailLenBodyProportions after body shaping is truly dynamic
/**
 * previously hardcoded behaviour when theres tail length expanding to maxRadius,
 * and same radius is kept to the nose point
 * */
class TailLenBodyProportions(
    private val maxRadius: Double, private val tailLength: Double,
    override val headOffset: Double, override val headRadius: Double) : IBodyProportions {

    private var curLength: Double = 0.0

    override fun resize(bodyLength: Double) {
        if(curLength < 0)
            throw IllegalArgumentException("Distance to tail $curLength is negative")
        curLength = bodyLength
    }

    override val segmentCount get() = if(curLength > tailLength) 2 else 1

    override fun segmentEndFromTail(index: Int): Double {
        val maxSegments = segmentCount
        if (index < 0 || index >= maxSegments)
            throw IllegalArgumentException("Bad segment index $index of allowed $maxSegments")

        return if(maxSegments == 1 || index == 1) {
            curLength
        } else {
            tailLength
        }
    }

    override fun segmentRadius(index: Int): Double {
        val maxSegments = segmentCount
        if (index < 0 || index >= maxSegments)
            throw IllegalArgumentException("Bad segment index $index of allowed $maxSegments")

        return if(curLength >= tailLength) maxRadius
        else maxRadius * curLength / tailLength
    }
}
