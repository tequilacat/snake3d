package tequilacat.org.snake3d.playfeature


interface IBodyProportions {
    fun findRadius(distanceToTail: Double, totalLength: Double) : Double
}

data class TailLenBodyProportions(val maxRadius: Double, val tailLength: Double) : IBodyProportions {
    override fun findRadius(distanceToTail: Double, totalLength: Double): Double {
        return when {
            distanceToTail < 0 || distanceToTail > totalLength ->
                throw IllegalArgumentException("Distance to tail $distanceToTail is out of body length $totalLength")
            distanceToTail < tailLength -> maxRadius * distanceToTail / tailLength
            else -> maxRadius
        }
    }
}

data class TailNeckBodyProportions(
    val maxRadius: Double,
    val lengthToRadius: Double,
    val tailLenToRadius: Double,
    val neckLenToRadius: Double,
    val faceRadiusToRadius:Double
) : IBodyProportions {

    private fun effectiveMaxRadius(totalLength: Double): Double {
        if (totalLength < 0) {
            throw IllegalArgumentException("negative len")
        }

        val effR = totalLength / lengthToRadius
        return if (effR < maxRadius) effR else maxRadius
    }

    override fun findRadius(distanceToTail: Double, totalLength: Double): Double {
        if (distanceToTail < 0 || distanceToTail > totalLength) {
            throw IllegalArgumentException("negative len")
        }

        val effR = effectiveMaxRadius(totalLength)
        val tailLen = tailLenToRadius * effR

        if(distanceToTail < tailLen) {
            return effR * distanceToTail / tailLen
        }

        val distanceToNose = totalLength - distanceToTail
        val neckLen = neckLenToRadius * effR

        if(distanceToNose < neckLen) {
            return effR * (faceRadiusToRadius + (1 - faceRadiusToRadius) * distanceToNose / neckLen)
        }

        return effR
    }
}
