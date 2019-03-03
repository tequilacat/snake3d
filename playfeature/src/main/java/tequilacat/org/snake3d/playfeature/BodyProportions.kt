package tequilacat.org.snake3d.playfeature

import kotlin.math.min

interface IFeedableBodyProportions {
    val segmentCount: Int
    val neckIndex: Int
    val fullLength: Double
    val lengthToNeck get() = segmentEndFromTail(neckIndex)

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

    fun printable(): String  {
        val posAndR = (0 until segmentCount)
            .map { i-> "[${segmentEndFromTail(i)}, ${segmentRadius(i)}]" }
            .joinToString(", ")
        return "l = $fullLength all: $segmentCount neck @$neckIndex, lr = $posAndR"
    }
}

class FeedableProportions(
    private val lenRadiusPairs: DoubleArray,
    private val maxRadius: Double,
    private val fullRadiusFirstIndex: Int,
    private val feedByFullRadiusRatio: Double // TODO remove default
) : IFeedableBodyProportions {

    private var ratio: Double = 0.0
    private var foodToConsume: Double = 0.0

    override var fullLength = 0.0
//    override val fullLength get() = segmentEndFromTail(segmentCount - 1)
    override val segmentCount = lenRadiusPairs.size / 2
    override val neckIndex = segmentCount - 3

    init {
        if(fullRadiusFirstIndex < 0 || fullRadiusFirstIndex >= segmentCount - 1
            || lenRadiusPairs[fullRadiusFirstIndex * 2 + 1] != lenRadiusPairs[(fullRadiusFirstIndex + 1) * 2 + 1])
            throw IllegalArgumentException("Bad index of continous segment $fullRadiusFirstIndex")
    }

    override fun resize(bodyLength: Double) {
        adjustSize(bodyLength)
        foodToConsume = 0.0
    }

    override fun segmentEndFromTail(index: Int): Double {
        val segDistance: Double
        val fromTail = lenRadiusPairs[index * 2]

        //
        if(index <= fullRadiusFirstIndex || ratio == fullLength) {
            segDistance = fromTail * ratio
        } else {
            segDistance = fullLength - (1 - fromTail) * ratio
        }

        return segDistance
    }

    override fun segmentRadius(index: Int) = lenRadiusPairs[index * 2 + 1] * ratio

    override fun advance(distance: Double) {
        if(foodToConsume > 0) {
            val deltaLength = min(distance, foodToConsume)
            foodToConsume -= deltaLength
            adjustSize(fullLength + deltaLength)
        }
    }

    override fun feed() {
        foodToConsume += feedByFullRadiusRatio * segmentRadius(fullRadiusFirstIndex)
    }

    private fun adjustSize(bodyLength: Double) {
        fullLength = bodyLength
        val refR = lenRadiusPairs[fullRadiusFirstIndex * 2 + 1]

        if (refR * bodyLength <= maxRadius) {
            ratio = bodyLength
        } else {
            ratio = maxRadius / refR
        }
    }
}