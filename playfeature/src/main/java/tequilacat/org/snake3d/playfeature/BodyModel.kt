package tequilacat.org.snake3d.playfeature

import tequilacat.org.snake3d.playfeature.glutils.IDirectedSection
import java.util.*
import kotlin.math.*

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

    private class TailSection : IDirectedSection {
        fun setFrom(tailSegment: BodySegmentModel) {
            centerX = tailSegment.dStartX.toFloat()
            centerY = tailSegment.dStartY.toFloat()
            centerZ = tailSegment.dStartZ.toFloat()
            alpha = tailSegment.alpha
        }

        override var alpha: Float = 0f
        override var centerX: Float = 0f
        override var centerY: Float = 0f
        override var centerZ: Float = 0f
        override val radius = 0f
        override val prevLength = 0f
    }

    private class BodySegmentModel(
        var dStartX: Double, var dStartY: Double, val dFloorZ: Double,
        val dAlpha: Double, var dLength: Double
    ) : IDirectedSection {

        var dEndX = 0.0
        var dEndY = 0.0
        var dEndZ = 0.0
        var dStartZ = 0.0

        var dStartRadius = 0.0
        var dEndRadius = 0.0

        override val alpha: Float = dAlpha.toFloat()

        private val alphaSinus: Float = sin(alpha)
        private val alphaCosinus: Float = cos(alpha)

        // IDirectedSection
        override val centerX: Float get() = dEndX.toFloat()
        override val centerY: Float get() = dEndY.toFloat()
        override val centerZ: Float get() = dEndZ.toFloat()
        override val radius: Float get() = dEndRadius.toFloat()
        override val prevLength: Float get() = dLength.toFloat()

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

    // TODO generate body sections from BodyProportions without BodySegment insertion when radius changes

    private val tailSection = TailSection()

    val bodySections : Sequence<IDirectedSection> get() = sequence {
        tailSection.setFrom(bodySegmentsImpl.first)
        yield(tailSection)
        yieldAll(bodySegmentsImpl)
    }

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
                val endSegDistance = processedLen + segment.prevLength
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
        val rotX = offX * headDirectionCosinus - offY * (-headDirectionSinus)
        val rotY = offX * (-headDirectionSinus) + offY * headDirectionCosinus

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

    /**
     * whether line crosses. we don't check if line ends touch cone (checked elsewhere)
     * only whether the line crosses "neck" axis between face ring and head sphere,
     *   or ahead of head sphere, accounting for segment thickness (extrapolated between r0 and r1)
     *
     */
    private fun crossesHead(x0: Double, y0: Double, r0: Double, x1: Double, y1: Double, r1: Double): Boolean {
        val offX0 = x0 - faceX
        val offY0 = y0 - faceY

        // rotate around face center
        val rotX0 = offX0 * headDirectionCosinus -  offY0 * (-headDirectionSinus)
        val rotY0 = offX0 * (-headDirectionSinus) + offY0 * headDirectionCosinus


        val offX1 = x1 - faceX
        val offY1 = y1 - faceY

        // rotate around face center
        val rotX1 = offX1 * headDirectionCosinus -  offY1 * (-headDirectionSinus)
        val rotY1 = offX1 * (-headDirectionSinus) + offY1 * headDirectionCosinus

        // if diff signs of Y they]re on different sides,

        val tolerance = 0.0001
        // when coaxial we test if both are within
        if(abs(rotY0) < tolerance && abs(rotY1) < tolerance) {
            val xMin = min(rotX0, rotX1)
            val xMax = max(rotX0, rotX1)
            return (xMax >= 0.0 && xMin <= headOffset + headRadius)
        }

        // on same side (the 0 is checked above
        if (sign(rotY0) == sign(rotY1)) {
            return false
        }

        // now points are on diff sides:
        // look for cross coordinate on 0x and check if it is within neck and head sphere

        val factor = abs(rotY0 / (rotY1 - rotY0))
        val crossX = rotX0 + (rotX1 - rotX0) * factor
        val crossR = r0 + (r1 - r0) * factor

        return crossX >= 0 && crossX - crossR < headOffset + headRadius
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

            // segment end is already far enough to deserve test
            if(checkThis && crossesHead(segment.dStartX, segment.dStartY, segment.dStartRadius,
                    segment.dEndX, segment.dEndY, segment.dEndRadius)) {
                return SELF_COLLISION
            }

            distanceFromFace += segment.dLength
            // start checking vertebras further than 2*faceR from face
            checkThis = checkThis || distanceFromFace > faceR * 2

            if (checkThis) {
                // start ting of the segment cannot touch face center - check from here
                val startRadius = if(i==0) 0.0 else bodySegmentsImpl[i - 1].dEndRadius
                if (collidesHead(segment.dStartX, segment.dStartY, startRadius)) {
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