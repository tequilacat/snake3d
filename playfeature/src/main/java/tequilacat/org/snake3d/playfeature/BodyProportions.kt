package tequilacat.org.snake3d.playfeature

data class BodyProportions(
    val maxRadius: Double,
    val lengthToRadius: Double,
    val tailLenToRadius: Double,
    val neckLenToRadius: Double,
    val faceRadiusToRadius:Double
) {

    fun effectiveMaxRadius(totalLength: Double): Double {
        if (totalLength < 0) {
            throw IllegalArgumentException("negative len")
        }

        val effR = totalLength / lengthToRadius
        return if (effR < maxRadius) effR else maxRadius
    }

    fun findRadius(distanceToTail: Double, totalLength: Double): Double {
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
