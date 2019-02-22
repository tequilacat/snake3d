package tequilacat.org.snake3d.playfeature

class NotImplProportions(tailLen: Double, tRadius: Double, tHeadOffset: Double, tHeadR: Double) :
    IBodyProportions {
    override val segmentCount: Int get() = TODO("not implemented")
    override val headOffset: Double get() = TODO("not implemented")
    override val headRadius: Double get() = TODO("not implemented")
    override fun resize(bodyLength: Double) = TODO("must fail")
    override fun segmentEndFromTail(index: Int): Double = TODO("must fail")
    override fun segmentRadius(index: Int): Double = TODO("must fail")
}

/** whatever length is given, it's conical: tail 0, radius head */
class NotSegmentedProportions(private val radius: Double,
                              override val headOffset:Double = 0.0,
                              override val headRadius:Double = radius): IBodyProportions {
    override val segmentCount = 1
    private var length = 0.0

    override fun resize(bodyLength: Double) {
        length = bodyLength
    }

    override fun segmentEndFromTail(index: Int): Double = length
    override fun segmentRadius(index: Int): Double = radius
}

class ArrayProportions(private vararg val relSizes: Number) : IBodyProportions {
    override val segmentCount = relSizes.size / 2
    override val headOffset: Double = 0.0
    override val headRadius: Double = relSizes.last().toDouble()
    private var lastLen = 0.0

    override fun resize(bodyLength: Double) {
        lastLen = bodyLength
    }

    override fun segmentEndFromTail(index: Int) = relSizes[index * 2].toDouble() * lastLen
    override fun segmentRadius(index: Int) = relSizes[index * 2 + 1].toDouble()
}

/** used to exchange implementation for testing purposes to split body on different steps by different algos */
class FacadeProportions(var backer: IBodyProportions) : IBodyProportions {
    override val segmentCount get() = backer.segmentCount
    override val headOffset get() = backer.headOffset
    override val headRadius get() = backer.headRadius
    override fun resize(bodyLength: Double) = backer.resize(bodyLength)
    override fun segmentEndFromTail(index: Int) = backer.segmentEndFromTail(index)
    override fun segmentRadius(index: Int) = backer.segmentRadius(index)
}