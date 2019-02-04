package tequilacat.org.snake3d.playfeature

import java.util.*
import kotlin.math.min

/**
 * Logical representation of snake's body - segments and modification methods
 */
class BodyModel(private val tailLength: Double, private val radius: Double) {
    private val bodySegmentsImpl = LinkedList<BodySegment>()

    val bodySegments: Collection<IBodySegment> = bodySegmentsImpl

    /**
     * Z is floor level
     */
    fun init(startX: Double, startY: Double, floorZ: Double,
             startHorzAngle: Double, startVertAngle: Double, length: Double) {
        bodySegmentsImpl.clear()

// TODO process start/endZ and radius in body segment

        val first = BodySegment(startX, startY, floorZ, startHorzAngle, startVertAngle, min(tailLength, length))
            .apply {
//                dblEndRadius = radius * (if (length >= tailLength) 1.0 else length / tailLength)
//                dblEndZ = floorZ + dblEndRadius
            }
        bodySegmentsImpl.addFirst(first)

        if (length > tailLength) {
            // append 2nd at same conditions
            bodySegmentsImpl.addLast(BodySegment(
                first.dblEndX, first.dblEndY, first.dblEndZ,
                startHorzAngle, startVertAngle, length - tailLength
            ).apply {
//                dblEndZ = floorZ + radius
//                dblEndRadius = radius
            })
        }
    }

    /**
     *  moves body forward, shortening tail if shortenBy is > 0
     *  */
    fun advance(distance: Double, angleDelta: Double, shortenBy: Double) {
        val originalLength = bodySegmentsImpl.sumByDouble { it.dblLength }
        var remainingDistance = distance
        // extend segment only if dA == 0 && isFirst
        var isFirst = true

        while (remainingDistance > 0.0) {
            // check if we increase or append another one
            val step = if (originalLength >= tailLength) {
                remainingDistance
            } else if (originalLength + remainingDistance < tailLength) {
                remainingDistance
            } else {
                tailLength - originalLength
            }

            val last = bodySegmentsImpl.last()

            if (isFirst && angleDelta == 0.0) {
                last.extend(step)
            } else {
                // append another one
                bodySegmentsImpl.addLast(BodySegment(last.dblEndX, last.dblEndY, last.dblEndZ,
                    last.angle + if(isFirst) angleDelta else 0.0,
                    last.dblBeta, step))
            }

            isFirst = false
            remainingDistance -= step
        }

        // TODO shorten after growing
    }
}