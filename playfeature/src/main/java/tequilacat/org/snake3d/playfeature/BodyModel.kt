package tequilacat.org.snake3d.playfeature

//import android.util.Log
import tequilacat.org.snake3d.playfeature.glutils.IDirectedSection
import kotlin.math.*

/**
 * Logical representation of snake's body - segments and modification methods
 */
class BodyModel(val bodyProportions: IBodyProportions) {

    private var foodRunLength: Double = 0.0

    private var floorZ: Double = 0.0

    /** direction of view and further direct movements */
    var viewDirection: Float = 0f
        private set

    val headX: Float get() = linkedListHeadSection.centerX
    val headY: Float get() = linkedListHeadSection.centerY

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

    val neckSection get() = linkedListHeadSection as IDirectedSection

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

    var bodyLength: Double = 0.0
        private set

    // updates diameters, inserts if needed, computes length - and does not change it
    private fun processSegments() {
        //Log.d("segments", "========= process segments")
        bodyLength = computeTotalLength()
        bodyProportions.resize(bodyLength)

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


        viewDirection = linkedListHeadSection.dAlpha.toFloat()

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

        // notify digesting system that it may move food further
        if (bodyProportions is IFeedableBodyProportions) {
            bodyProportions.advance(distance)
        }

        processSegments()
//        Log.d("segments", "advance($distance, $angleDelta): " + bodySections.map { it.dRadius }.joinToString(", "))
    }

    /** how much length to keep while extending */
    fun feed(aFoodRunLength: Double) {
        if (bodyProportions is IFeedableBodyProportions) {
            // food is ready!
            bodyProportions.feed()
        }
        foodRunLength += aFoodRunLength
    }
}