package tequilacat.org.snake3d.playfeature

import java.util.*
import kotlin.math.abs
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

    // center of face (end of last segment)
    private var faceX: Double = 0.0
    private var faceY: Double = 0.0
    private var faceZ: Double = 0.0
    private var faceR: Double = 0.0

    private var headDirectionSinus: Double = 0.0
    private var headDirectionCosinus: Double = 0.0

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
        var dEndRadius = 0.0

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

        // nose point
        faceX = last.dEndX
        faceY = last.dEndY
        faceZ = last.dEndZ
        faceR = last.dEndRadius

        headDirectionSinus = sin(last.dAlpha)
        headDirectionCosinus = cos(last.dAlpha)

        dHeadX = last.dEndX + headDirectionCosinus * headOffset
        dHeadY = last.dEndY + headDirectionSinus * headOffset


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
        return collidesHead(
            fieldObject.centerX.toDouble(),
            fieldObject.centerY.toDouble(),
            fieldObject.radius.toDouble()
        )
    }

    /**
     * whether the point with radius touches the head "neck" or head sphere
     */
    private fun collidesHead(cx: Double, cy: Double, objR: Double): Boolean {
        // check how near to the face ring->head center line:
        // rotate obj center around last ring center, check distance to ring->head centers
        // translate to face CS
        val offX = cx - faceX
        val offY = cy - faceY

        // rotate around face center
        val rotX = offX * headDirectionCosinus - offY * (-headDirectionSinus);
        val rotY = offX * (-headDirectionSinus) + offY * headDirectionCosinus;

        // after rotation, behind face center or ahead of head sphere
        if(rotX < -objR || rotX > headOffset + headRadius + objR)
            return false

        // Test if between face and head we're close to neck than radius
        // within neck (face-head segment): find radius at coords of tested obj
        // foolproof for head offset 0
        val neckR = faceR + if (headOffset == 0.0) 0.0 else (headRadius - faceR) * rotX / headOffset
        // only if center is within neck
        if (abs(rotY) - objR < neckR && rotX >= 0.0 && rotX < headOffset)
            return true

        if(hypot(cx - headX, cy - headY) < objR + headRadius)
            return true

        return false
    }


    fun checkCollisions(gameScene: IGameScene) : Collision {
        if (dHeadX + headRadius > gameScene.fieldWidth || dHeadX - headRadius < 0
            || dHeadY + headRadius > gameScene.fieldHeight || dHeadY - headRadius < 0)
            return WALL_COLLISION

        val obj = gameScene.fieldObjects.firstOrNull(::collidesHead)

        if (obj != null) {
            return Collision(CollisionType.GAMEOBJECT, obj)
        }

        // reverse iteration
        var distanceFromFace = 0.0
        var checkThis = false

        for (i in bodySegmentsImpl.size - 1 downTo 0) {
            val segment = bodySegmentsImpl[i]
            distanceFromFace += segment.dLength
            // TODO fix collision to sphere behind face ring, remove*2
            checkThis = checkThis || segment.startRadius * 2 < distanceFromFace

            if (checkThis) {
                // start ting of the segment cannot touch face center - check from here
                if (collidesHead(segment.dStartX, segment.dStartY, segment.startRadius.toDouble())) {
                    return SELF_COLLISION
                }
            }
        }

        return NO_COLLISION
    }

    /** how much length to keep while extending */
    fun feed(aFoodRunLength: Double) {
        foodRunLength += aFoodRunLength
    }
}