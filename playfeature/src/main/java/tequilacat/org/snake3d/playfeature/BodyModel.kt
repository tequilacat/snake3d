package tequilacat.org.snake3d.playfeature

import java.util.*
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

interface IBodySegmentModel {
    val startX: Float
    val startY: Float
    val startZ: Float

    val endX: Float
    val endY: Float
    val endZ: Float

    val length: Float
    val startRadius: Float
    val endRadius: Float

    val alpha: Float
    val alphaSinus: Float
    val alphaCosinus: Float
}

/**
 * Logical representation of snake's body - segments and modification methods
 * @param headOffset is how far head is moved forward off the end
 * @param headRadius collision radius of head, defaults to
 */
class BodyModel(private val tailLength: Double, private val radius: Double,
                private val headOffset: Double, private val headRadius: Double) {

    private var foodRunLength: Double = 0.0

    private var dHeadX = 0.0
    private var dHeadY = 0.0

    /** head center X coord according to body end, head offset and head radius */
    val headX get() = dHeadX.toFloat()

    /** head center Y coord */
    val headY get() = dHeadY.toFloat()

    /** direction of view and further direct movements */
    var viewDirection: Float = 0f
        private set

    private class BodySegmentModel(
        var dStartX: Double, var dStartY: Double, val dFloorZ: Double,
        val dAlpha: Double, var dLength: Double
    ) : IBodySegmentModel {

        var dEndX = 0.0
        var dEndY = 0.0
        var dEndZ = 0.0
        var dStartZ = 0.0

        private var dStartRadius = 0.0
        private var dEndRadius = 0.0

        override val alpha: Float = dAlpha.toFloat()
        override val alphaSinus: Float = sin(alpha)
        override val alphaCosinus: Float = cos(alpha)

        override val startX: Float get() = dStartX.toFloat()
        override val startY: Float get() = dStartY.toFloat()
        override val startZ: Float get() = dStartZ.toFloat()
        override val endX: Float get() = dEndX.toFloat()
        override val endY: Float get() = dEndY.toFloat()
        override val endZ: Float get() = dEndZ.toFloat()

        override val length: Float get() = dLength.toFloat()
        override val startRadius: Float get() = dStartRadius.toFloat()
        override val endRadius: Float get() = dEndRadius.toFloat()

        init {
            computeEnd()
        }

        private fun computeEnd() {
            dEndX = dStartX + dLength * alphaCosinus
            dEndY = dStartY + dLength * alphaSinus
        }

        fun extendBy(delta: Double) {
            dLength += delta
            computeEnd()
        }

        /** shortens by specified amount or throws exception if length is >=*/
        fun shortenBy(delta:Double) {
            if (delta >= dLength) {
                throw IllegalArgumentException("Cannot shorten segment having $dLength by $delta")
            }
            dStartX += delta * alphaCosinus
            dStartY += delta * alphaSinus
            dLength -= delta
        }

        fun setRadiuses(rStart: Double, rEnd: Double) {
            dStartRadius = rStart
            dEndRadius = rEnd
            dStartZ = dFloorZ + dStartRadius
            dEndZ = dFloorZ + dEndRadius
        }
    }

    private val bodySegmentsImpl = LinkedList<BodySegmentModel>()

    val bodySegments: Collection<IBodySegmentModel> = bodySegmentsImpl

    /**
     * Z is floor level
     */
    fun init(startX: Double, startY: Double, floorZ: Double,
             startHorzAngle: Double, totalLength: Double) {
        foodRunLength = 0.0
        bodySegmentsImpl.clear()
        bodySegmentsImpl.addFirst(BodySegmentModel(startX, startY, floorZ, startHorzAngle, totalLength))
        processSegments()
    }


    // updates diameters, inserts if needed
    private fun processSegments() {
        // so far only extend is accounted for
        var processedLen = 0.0
        var index = 0

        for (segment in bodySegmentsImpl) {
            if (processedLen < tailLength && processedLen + segment.dLength > tailLength) {
                // split current segment into 2, and stop processing
                // bodySegmentsImpl
                val fragLength = tailLength - processedLen
                val fragment = BodySegmentModel(segment.dStartX, segment.dStartY, segment.dFloorZ, segment.dAlpha,
                    fragLength)
                segment.shortenBy(fragLength)
                bodySegmentsImpl.add(index, fragment)
                break
            }

            processedLen += segment.dLength
            index++
        }

        // TODO benchmark and make in single cycle for performance if needed
        processedLen = 0.0

        for (segment in bodySegmentsImpl) {
            if (processedLen < tailLength) {
                val endSegDistance = processedLen + segment.length
                segment.setRadiuses(
                    processedLen / tailLength * radius,
                    if (endSegDistance < tailLength) endSegDistance / tailLength else radius
                )
            } else {
                segment.setRadiuses(radius, radius)
            }
            // adjust radiuses
            processedLen += segment.dLength
        }
        val last = bodySegmentsImpl.last()

        dHeadX = last.dEndX + cos(last.dAlpha) * headOffset
        dHeadY = last.dEndY + sin(last.dAlpha) * headOffset

        viewDirection = last.dAlpha.toFloat()
    }

    /**
     *  moves body forward, shortening tail if shortenBy is > 0
     *  */
    fun advance(distance: Double, angleDelta: Double) {
        // append new one or extend last if angle is 0
        val last = bodySegmentsImpl.last()

        if(angleDelta == 0.0) {
            last.extendBy(distance)
        } else {
            bodySegmentsImpl.addLast(BodySegmentModel(last.dEndX, last.dEndY,last.dFloorZ,
                last.dAlpha + angleDelta, distance))
        }

        var remainingShorten = when {
            foodRunLength == 0.0 -> distance
            foodRunLength >= distance -> {
                foodRunLength -= distance
                0.0
            }
            else -> {
                // FRL < distance - drag something
                val res = distance - foodRunLength
                foodRunLength = 0.0
                res
            }
        }

        while (remainingShorten > 0.0) {
            val first = bodySegmentsImpl.first()

            if (first.dLength > remainingShorten) {
                // shorten this one
                first.shortenBy(remainingShorten)
                break
            }

            bodySegmentsImpl.removeFirst()
            remainingShorten -= first.dLength
        }

        processSegments()
    }

    companion object {
        private val NO_COLLISION: Collision = Collision(CollisionType.NONE, null)
        private val SELF_COLLISION: Collision = Collision(CollisionType.SELF, null)
        private val WALL_COLLISION: Collision = Collision(CollisionType.WALL, null)
    }

    enum class CollisionType {
        NONE, SELF, WALL, GAMEOBJECT;
    }

    data class Collision(val type: CollisionType, val fieldObject: IFieldObject?){
        val isColliding = type != CollisionType.NONE
    }

    private fun collidesHead(fieldObject: IFieldObject):Boolean {
        return hypot(fieldObject.centerX - headX, fieldObject.centerY - headY) < fieldObject.type.radius + headRadius
    }

    fun checkCollisions(gameScene: IGameScene) : Collision {
        if (dHeadX + headRadius > gameScene.fieldWidth || dHeadX - headRadius < 0
            || dHeadY + headRadius > gameScene.fieldHeight || dHeadY - headRadius < 0)
            return WALL_COLLISION

        val obj = gameScene.fieldObjects.firstOrNull(::collidesHead)

        if (obj != null) {
            return Collision(CollisionType.GAMEOBJECT, obj)
        }

        return NO_COLLISION
    }

    /** how much length to keep while extending */
    fun feed(aFoodRunLength: Double) {
        foodRunLength += aFoodRunLength
    }
}