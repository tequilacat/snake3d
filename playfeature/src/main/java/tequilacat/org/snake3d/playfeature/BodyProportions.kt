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

interface IFeedableBodyProportions {
    val segmentCount: Int
    val neckIndex: Int

    fun resize(bodyLength: Double)
    fun segmentEndFromTail(index: Int): Double
    fun segmentRadius(index: Int): Double

    /**
     * notifies that snake advances forward (lives another time span)
     * so the eaten food will move to tail and dissolve
     * */
    fun advance(distance: Double)

    /** feeds a unit of food to the snake so it starts digesting it */
    fun feed()
}

// TODO move TailLenBodyProportions to test utils after body shaping is truly dynamic
/**
 * previously hardcoded behaviour when theres tail length expanding to maxRadius,
 * and same radius is kept to the nose point
 * */
class TailLenBodyProportions(
    private val maxRadius: Double, private val tailLength: Double,
    override val headOffset: Double, private val headRadiusToNeckRatio: Double) : IBodyProportions {

    override var headRadius: Double = 0.0

    private var curLength: Double = 0.0

    override fun resize(bodyLength: Double) {
        if(curLength < 0)
            throw IllegalArgumentException("Distance to tail $curLength is negative")
        curLength = bodyLength

        val lastR = if (curLength >= tailLength) maxRadius else maxRadius * curLength / tailLength
        headRadius = if (headOffset == 0.0) lastR else lastR * headRadiusToNeckRatio
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

class FeedableProportions(
    private val lenRadiusPairs: DoubleArray,
    private val maxRadius: Double,
    private val fullRadiusFirstIndex: Int
) : IFeedableBodyProportions {

    private var bodyLength: Double = -1.0
    private var ratio: Double = 0.0

    override val segmentCount = lenRadiusPairs.size / 2
    override val neckIndex = segmentCount - 3

    init {
        if(fullRadiusFirstIndex < 0 || fullRadiusFirstIndex >= segmentCount - 1
            || lenRadiusPairs[fullRadiusFirstIndex * 2 + 1] != lenRadiusPairs[(fullRadiusFirstIndex + 1) * 2 + 1])
            throw IllegalArgumentException("Bad index of continous segment $fullRadiusFirstIndex")
    }

    override fun resize(bodyLength: Double) {
        this.bodyLength = bodyLength
        val refR = lenRadiusPairs[fullRadiusFirstIndex * 2 + 1]

        if (refR * bodyLength <= maxRadius) {
            ratio = bodyLength
        } else {
            ratio = maxRadius / refR
        }
    }

    override fun segmentEndFromTail(index: Int): Double {
        if(bodyLength < 0) {
            throw IllegalStateException("bodyLength not assigned")
        }

        val segDistance: Double
        val fromTail = lenRadiusPairs[index * 2]

        if(index <= fullRadiusFirstIndex || ratio == bodyLength) {
            segDistance = fromTail * ratio
        } else {
            segDistance = bodyLength - (1 - fromTail) * ratio
        }

        return segDistance
    }

    override fun segmentRadius(index: Int): Double {
        if(bodyLength < 0) {
            throw IllegalStateException("bodyLength not assigned")
        }

        val relR = lenRadiusPairs[index * 2 + 1]

        return relR * ratio
    }

    override fun advance(distance: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun feed() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}