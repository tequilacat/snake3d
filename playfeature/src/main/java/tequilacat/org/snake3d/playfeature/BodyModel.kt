package tequilacat.org.snake3d.playfeature

import tequilacat.org.snake3d.playfeature.glutils.IDirectedSection
import java.util.*
import kotlin.math.*

/**
 * Logical representation of snake's body - segments and modification methods
 */
class BodyModel(private val bodyProportions: IBodyProportions) {

    /*
     * @param headOffset is how far head is moved forward off the end
     * @param headRadius collision radius of head, defaults to
     */
//    constructor(tailLength: Double, radius: Double,
//                headOffset: Double, headRadius: Double)
//            : this(TailLenBodyProportions(radius, tailLength, headOffset, headRadius))

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

    private var floorZ: Double = 0.0

    private var headDirectionSinus: Double = 0.0
    private var headDirectionCosinus: Double = 0.0

    /** direction of view and further direct movements */
    var viewDirection: Float = 0f
        private set


    interface IDoubleDirectedSection {
        val dCenterX: Double
        val dCenterY: Double
        val dCenterZ: Double

        val dRadius: Double

        val dPrevLength: Double
        // TODO get rid of alpha here
        val dAlpha: Double
    }


    private val tailSection = SingleSection()
    private val headSection = SingleSection()

    private class SingleSection : IDirectedSection, IDoubleDirectedSection {
        override var dCenterX: Double = 0.0
        override var dCenterY: Double = 0.0
        override var dCenterZ: Double = 0.0
        override var dAlpha: Double = 0.0

        override var dRadius: Double = 0.0
        override var dPrevLength: Double = 0.0

        fun setValues(x: Double, y: Double, z: Double, r: Double, alpha: Double, length: Double) {
            dCenterX = x
            dCenterY = y
            dCenterZ = z
            dAlpha = alpha
            dRadius = r
            dPrevLength = length
        }

        fun setFrom(tailSegment: BodySegmentModel) {
            setValues(tailSegment.dStartX, tailSegment.dStartY, tailSegment.dStartZ, 0.0, tailSegment.dAlpha,
                0.0)
        }

        override val alpha get() = dAlpha.toFloat()
        override val centerX get() = dCenterX.toFloat()
        override val centerY get() = dCenterY.toFloat()
        override val centerZ get() = dCenterZ.toFloat()
        override val radius get() = dRadius.toFloat()
        override val prevLength get() = dPrevLength.toFloat()
    }

    private class BodySegmentModel(
        var dStartX: Double, var dStartY: Double,
        override val dAlpha: Double, var dLength: Double
    ) : IDirectedSection, IDoubleDirectedSection {

        var dEndX = 0.0
        var dEndY = 0.0
        var dEndZ = 0.0
        var dStartZ = 0.0

        //var dStartRadius = 0.0
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

        override val dCenterX get() = dEndX
        override val dCenterY get() = dEndY
        override val dCenterZ get() = dEndZ
        override val dRadius get() = dEndRadius
        override val dPrevLength get() = dLength


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

        fun setRadiuses(rStart: Double, rEnd: Double, floorZ: Double) {
//            if(rStart == 0.0) {
//                dStartZ = 0.0 // for zero radius we assume it's tail and it lays onn the floor
//            }
            dEndRadius = rEnd
            dStartZ = floorZ + rStart
            dEndZ = floorZ + dEndRadius
        }
    }

    private val bodySegmentsImpl = LinkedList<BodySegmentModel>()

    val bodySections : Sequence<IDirectedSection> get() = sequence {
        tailSection.setFrom(bodySegmentsImpl.first)
        yield(tailSection as IDirectedSection)
        yieldAll(bodySegmentsImpl)
    }

    // body sections with extra section for head
    val bodyAndHeadSections : Sequence<IDirectedSection> get() = sequence {
        yieldAll(bodySections)
        // add section of head R ahead
        if (bodyProportions.headOffset > 0) {
            yield(headSection)
        }
    }

    private fun updateHeadSection() {
        if (bodyProportions.headOffset > 0) {
            val lastSegment = bodySegmentsImpl.last
            val lastR = bodyProportions.headRadius// lastSegment.dEndRadius * 1.5
            val headOffset = bodyProportions.headOffset
            headSection.setValues(
                lastSegment.dEndX + cos(lastSegment.dAlpha) * headOffset,
                lastSegment.dEndY + sin(lastSegment.dAlpha) * headOffset, floorZ + lastR,
                lastR, lastSegment.dAlpha, headOffset)
        }
    }

    /**
     * Z is floor level
     */
    fun init(startX: Double, startY: Double, floorZ: Double,
             startHorzAngle: Double, totalLength: Double) {
        this@BodyModel.floorZ = floorZ
        foodRunLength = 0.0
        bodySegmentsImpl.clear()
        bodySegmentsImpl.addFirst(BodySegmentModel(startX, startY, startHorzAngle, totalLength))
        processSegments()
    }

    private fun computeTotalLength() = bodySegmentsImpl.sumByDouble { it.dPrevLength }

    // updates diameters, inserts if needed
    private fun processSegments() {
        bodyProportions.resize(computeTotalLength())

        var index = 0
        val shapeSegmentCount = bodyProportions.segmentCount
        var curShapeSegmentStart = 0.0
        var curShapeSegmentEnd = bodyProportions.segmentEndFromTail(0)
        var remainingLength = curShapeSegmentEnd
        var shapeSegmentR0 = 0.0
        var shapeSegmentR1 = bodyProportions.segmentRadius(0)

        val segIter = bodySegmentsImpl.iterator()
        var segment = segIter.next() // on first time always assume hasNext = true
        val newSegments = mutableListOf<BodySegmentModel>()

        while(true) {
            if(segment.dPrevLength <= remainingLength) {
                adjustRadiuses(segment, curShapeSegmentStart, curShapeSegmentEnd, shapeSegmentR0, shapeSegmentR1, remainingLength)
                newSegments.add(segment)
                remainingLength -= segment.dPrevLength
                if(!segIter.hasNext())
                    break
                segment = segIter.next()

            } else { // segment length > remaining: split segment
                val inserted = BodySegmentModel(segment.dStartX, segment.dStartY, segment.dAlpha, remainingLength)
                adjustRadiuses(inserted, curShapeSegmentStart, curShapeSegmentEnd, shapeSegmentR0, shapeSegmentR1, remainingLength)
                newSegments.add(inserted) // TODO adjust radiuses
                segment.shortenBy(remainingLength)
                remainingLength = 0.0
            }

            if(remainingLength.absoluteValue < 0.001) { // tolerance - don't allow for extra short segments
                // switch to next
                index++
                if(index >= shapeSegmentCount) {
                    break
                }

                shapeSegmentR0 = shapeSegmentR1
                shapeSegmentR1 = bodyProportions.segmentRadius(index)

                curShapeSegmentStart = curShapeSegmentEnd
                curShapeSegmentEnd = bodyProportions.segmentEndFromTail(index)
                remainingLength = curShapeSegmentEnd - curShapeSegmentStart
            }
        }

        bodySegmentsImpl.clear()
        bodySegmentsImpl.addAll(newSegments)

        val last = bodySegmentsImpl.last()

        // nose point
        faceX = last.dEndX
        faceY = last.dEndY
        faceZ = last.dEndZ
        faceR = last.dEndRadius

        headDirectionSinus = sin(last.dAlpha)
        headDirectionCosinus = cos(last.dAlpha)

        dHeadX = last.dEndX + headDirectionCosinus * bodyProportions.headOffset
        dHeadY = last.dEndY + headDirectionSinus * bodyProportions.headOffset

        viewDirection = last.dAlpha.toFloat()

        updateHeadSection()
    }

    private fun adjustRadiuses(
        segment: BodySegmentModel, l0: Double, l1: Double, r0: Double, r1: Double, remainingLength: Double
    ) {
        val rPerL = (r1 - r0) / (l1 - l0)
        val rStart = r0 + (l1 - l0 - remainingLength) * rPerL
        val rEnd = r0 + (l1 - l0 - remainingLength + segment.dPrevLength) * rPerL
        segment.setRadiuses(rStart, rEnd, floorZ)
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
            bodySegmentsImpl.addLast(BodySegmentModel(last.dEndX, last.dEndY,
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
        val headOffset = bodyProportions.headOffset
        val headRadius = bodyProportions.headRadius

        if(rotX < -objR || rotX >  headOffset + headRadius + objR)
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
        val headOffset = bodyProportions.headOffset
        val headRadius = bodyProportions.headRadius
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
        val headRadius = bodyProportions.headRadius

        if (dHeadX + headRadius > gameScene.fieldWidth || dHeadX - headRadius < 0
            || dHeadY + headRadius > gameScene.fieldHeight || dHeadY - headRadius < 0)
            return WALL_COLLISION

        val obj = gameScene.fieldObjects.firstOrNull(::collidesHead)

        if (obj != null) {
            return Collision(CollisionType.GAMEOBJECT, obj)
        }

        var remainingLenToFace = computeTotalLength()
        var prevSection: IDoubleDirectedSection? = null

        for (floatSection in bodySections) {
            val section = floatSection as IDoubleDirectedSection
            remainingLenToFace -= section.dPrevLength
            // if too close break it
            if(remainingLenToFace < faceR * 2) {
                break
            }

            // check if segment center(with radus) collides head
            if (collidesHead(section.dCenterX, section.dCenterY, section.dRadius)) {
                return SELF_COLLISION
            }

            // check if segment between this and prev section crosses head
            if(prevSection != null && crossesHead(section.dCenterX, section.dCenterY, section.dRadius,
                    prevSection.dCenterX, prevSection.dCenterY, prevSection.dRadius)) {
                return SELF_COLLISION
            }

            prevSection = section
        }

        return NO_COLLISION
    }

    /** how much length to keep while extending */
    fun feed(aFoodRunLength: Double) {
        foodRunLength += aFoodRunLength
    }
}