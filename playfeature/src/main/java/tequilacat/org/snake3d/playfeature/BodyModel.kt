package tequilacat.org.snake3d.playfeature

//import android.util.Log
import tequilacat.org.snake3d.playfeature.glutils.IDirectedSection
import kotlin.math.*

/**
 * Logical representation of snake's body - segments and modification methods
 */
class BodyModel(private val bodyProportions: IBodyProportions) {

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
    //private var faceZ: Double = 0.0
    private var faceR: Double = 0.0

    private var floorZ: Double = 0.0

    private var headDirectionSinus: Double = 0.0
    private var headDirectionCosinus: Double = 0.0

    /** direction of view and further direct movements */
    var viewDirection: Float = 0f
        private set


    // private val tailSection = SingleSection()
    private val headSection = SingleSection()

    private class SingleSection : IDirectedSection {
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
    }

    /**
     * @param cPrevX x of previous point from which this segment is translated by dLength and dAngle
     * @param cPrevY Y of previous point from which this segment is translated by dLength and dAngle
     */
    private class BodySegmentModel(
        cPrevX: Double, cPrevY: Double,
        override val dAlpha: Double, aPrevLength: Double
    ) : IDirectedSection {

        override var dCenterX = 0.0
        override var dCenterY = 0.0
        override var dCenterZ = 0.0
        override var dRadius = 0.0
        override var dPrevLength: Double = aPrevLength
            private set

        var prev: BodySegmentModel? = null
        var next: BodySegmentModel? = null
            private set

        private val alphaSinus = sin(dAlpha)
        private val alphaCosinus = cos(dAlpha)

        constructor(prevSection: BodySegmentModel, horAngle: Double, length: Double)
                : this( prevSection.dCenterX, prevSection.dCenterY, horAngle, length)

        companion object {
            fun createTail(cx: Double, cy: Double, angle: Double) = BodySegmentModel(cx, cy, angle, 0.0)
        }

        init {
            translateCenter(cPrevX, cPrevY, dPrevLength)
        }

        /**
         * inserts this section between specified sections
         * */
        fun insert(prevSection: BodySegmentModel?, nextSection: BodySegmentModel?) {
            prev = prevSection
            next = nextSection
            if(prev != null) {
                prev!!.next = this
            }
            if(next != null) {
                next!!.prev = this
            }
        }

        /**
         * translates new center to position from [fromX,fromY] by dAlpha and specified distance
         */
        private fun translateCenter(fromX: Double, fromY: Double, byDistance: Double) {
            dCenterX = fromX + byDistance * alphaCosinus
            dCenterY = fromY + byDistance * alphaSinus
        }

        fun extendBy(delta: Double) {
            translateCenter(dCenterX, dCenterY, delta)
            dPrevLength += delta
        }


        /**
         * always appends FINAL section (its next is null) from current center
         * */
        fun appendSection(horzAngle: Double, segmentLength: Double) =
            BodySegmentModel(this, horzAngle, segmentLength).also {
                it.insert(this, null)
            }

        fun setRadiuses(newRadius: Double, floorZ: Double) {
            dRadius = newRadius
            dCenterZ = floorZ + dRadius
        }

        /** sets new length keeping same center point */
        fun resizeFromTail(newLength: Double) {
            dPrevLength = newLength
        }

        /**
         * creates a new tail and adds as prev segment of this one
         */
        fun makeTail() =
            BodySegmentModel.createTail(
                dCenterX - dPrevLength * alphaCosinus,
                dCenterY - dPrevLength * alphaSinus, dAlpha).also {
                it.insert(null, this)
            }

    }

    /**
     * is created in init()
     */
    private lateinit var linkedListTailSection: BodySegmentModel

    /**
     *  is created in init()
     */
    private lateinit var linkedListHeadSection: BodySegmentModel

    val bodySections : Sequence<IDirectedSection> get() = sequence {
        var curSection:BodySegmentModel? = linkedListTailSection

        while(curSection != null) {
            yield(curSection as IDirectedSection)
            curSection = curSection.next
        }
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
            val lastSegment = linkedListHeadSection as IDirectedSection
            //val lastSegment = bodySegmentsImpl.last
            val lastR = bodyProportions.headRadius// lastSegment.dEndRadius * 1.5
            val headOffset = bodyProportions.headOffset
            headSection.setValues(
                lastSegment.dCenterX + cos(lastSegment.dAlpha) * headOffset,
                lastSegment.dCenterY + sin(lastSegment.dAlpha) * headOffset, floorZ + lastR,
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
        linkedListTailSection = BodySegmentModel.createTail(startX, startY, startHorzAngle)
        linkedListHeadSection = linkedListTailSection.appendSection(startHorzAngle, totalLength)
        processSegments()
    }

    private fun computeTotalLength() = bodySections.sumByDouble { it.dPrevLength }

    // updates diameters, inserts if needed
    private fun processSegments() {
        //Log.d("segments", "========= process segments")
        bodyProportions.resize(computeTotalLength())

        var index = 0
        val shapeSegmentCount = bodyProportions.segmentCount
        var curShapeSegmentStart = 0.0
        var curShapeSegmentEnd = bodyProportions.segmentEndFromTail(0)
        var remainingLength = curShapeSegmentEnd
        var shapeSegmentR0 = 0.0
        var shapeSegmentR1 = bodyProportions.segmentRadius(0)

        var segment = linkedListTailSection.next!!

//        bodySections.forEach {
//            Log.d("segments", "  R=${it.dRadius}, L=${it.dPrevLength}")
//        }

        // don't allow segments shorter than that
        val minSegmentLength = 0.0001

        while(true) {
            //Log.d("segments", "Segment len: ${segment.dPrevLength} vs remLen = $remainingLength ")

            // if segment is longer than remaining but by too short - consider it equal
            val nearEqual = (segment.dPrevLength - remainingLength).absoluteValue < minSegmentLength

            if(nearEqual || segment.dPrevLength < remainingLength) {
                // segment is OK, switch
                adjustRadiuses(segment, curShapeSegmentStart, curShapeSegmentEnd, shapeSegmentR0, shapeSegmentR1, remainingLength)

                if(nearEqual) {
                    remainingLength = 0.0
                } else {
                    remainingLength -= segment.dPrevLength
                }

                val next = segment.next
                if (next != null) {
                    segment = next
//                    Log.d("segments", "  to next segment [remLength = $remainingLength]")
                } else {
                    linkedListHeadSection = segment
//                    Log.d("segments", "  no more segments, break")
                    break
                }

            } else {
                // segment is (with tolerance) longer than remaining length and has to be split
                // insert between prev and this one. prev is never null - we're starting with 2nd section
//                val inserted = BodySegmentModel(segment.dStartX, segment.dStartY, segment.dAlpha, remainingLength)
                val inserted = segment.prev!!.appendSection(segment.dAlpha, remainingLength)
                inserted.insert(segment.prev, segment)
                segment.resizeFromTail(segment.dPrevLength - remainingLength)
                adjustRadiuses(inserted, curShapeSegmentStart, curShapeSegmentEnd, shapeSegmentR0, shapeSegmentR1, remainingLength)
//                Log.d("segments", "  insert R=${inserted.dRadius} L=${inserted.dPrevLength}, reiterate segment R=${segment.dRadius}")
                remainingLength = 0.0
            }

            if(remainingLength.absoluteValue < minSegmentLength) { // tolerance - don't allow for extra short segments
//                Log.d("segments", "     - $remainingLength too small, on to next shapeSegm #${index+1} (count=$shapeSegmentCount}")

                // switch to next
                index++
                if(index >= shapeSegmentCount) {
                    linkedListHeadSection = segment // probably this should not happen but still update the head section
//                    Log.d("segments", "    break after smallest section [r = ${linkedListHeadSection.dRadius}, l = ${linkedListHeadSection.dPrevLength}]")
                    break
                }

                shapeSegmentR0 = shapeSegmentR1
                shapeSegmentR1 = bodyProportions.segmentRadius(index)

                curShapeSegmentStart = curShapeSegmentEnd
                curShapeSegmentEnd = bodyProportions.segmentEndFromTail(index)
                remainingLength = curShapeSegmentEnd - curShapeSegmentStart
            }
        }

        // ensure tailpoint z/radius
        linkedListTailSection.setRadiuses(0.0, floorZ)
        val last = linkedListHeadSection

        // nose point
        faceX = last.dCenterX
        faceY = last.dCenterY
        //faceZ = last.dCenterZ
        faceR = last.dRadius

        headDirectionSinus = sin(last.dAlpha)
        headDirectionCosinus = cos(last.dAlpha)

        dHeadX = last.dCenterX + headDirectionCosinus * bodyProportions.headOffset
        dHeadY = last.dCenterY + headDirectionSinus * bodyProportions.headOffset

        viewDirection = last.dAlpha.toFloat()

        updateHeadSection()
    }

    private fun adjustRadiuses(
        segment: BodySegmentModel, l0: Double, l1: Double, r0: Double, r1: Double, remainingLength: Double
    ) {
        val rPerL = (r1 - r0) / (l1 - l0)
        //val rStart = r0 + (l1 - l0 - remainingLength) * rPerL
        val rEnd = r0 + (l1 - l0 - remainingLength + segment.dPrevLength) * rPerL
        segment.setRadiuses(rEnd, floorZ)
    }

    /**
     *  moves body forward, shortening tail if shortenBy is > 0
     *  */
    fun advance(distance: Double, angleDelta: Double) {
        // append new one or extend last if angle is 0
        //val last = bodySegmentsImpl.last()

        if(angleDelta == 0.0) {
            linkedListHeadSection.extendBy(distance)
        } else {
            linkedListHeadSection = linkedListHeadSection.appendSection(
                linkedListHeadSection.dAlpha + angleDelta, distance)
//                addLast(BodySegmentModel(last.dEndX, last.dEndY,
//                last.dAlpha + angleDelta, distance))
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

        if(remainingShorten > 0.0) {
            var curSeg = linkedListTailSection.next

            while (curSeg != null && curSeg.dPrevLength <= remainingShorten) {
                remainingShorten -= curSeg.dPrevLength
                curSeg = curSeg.next
            }
            // curSeg == null - subtracted too much, exception?
            // otherwise curSeg.dPrevLength > remainingShorten - means we shorten it
            if (curSeg != null) {
                curSeg.resizeFromTail(curSeg.dPrevLength - remainingShorten)
                linkedListTailSection = curSeg.makeTail()
            }
        }

        processSegments()
//        Log.d("segments", "advance($distance, $angleDelta): " + bodySections.map { it.dRadius }.joinToString(", "))
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
        var prevSection: IDirectedSection? = null

        for (section in bodySections) {
            //val section = floatSection
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