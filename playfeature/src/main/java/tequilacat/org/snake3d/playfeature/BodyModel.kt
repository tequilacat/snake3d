package tequilacat.org.snake3d.playfeature

//import android.util.Log
import tequilacat.org.snake3d.playfeature.glutils.IDirectedSection
import kotlin.math.*

/**
 * Logical representation of snake's body - segments and modification methods
 */
class BodyModel(private val bodyProportions: IFeedableBodyProportions) {

    // TODO move into body proportions
    private var foodRunLength: Double = 0.0

    private var floorZ: Double = 0.0

    /** direction of view and further direct movements */
    val viewDirection: Float get() = linkedListNeckSection.alpha

    val headX: Float get() = linkedListNeckSection.centerX
    val headY: Float get() = linkedListNeckSection.centerY

    private class SingleSection : IDirectedSection {
        override var dCenterX: Double = 0.0
        override var dCenterY: Double = 0.0
        override var dCenterZ: Double = 0.0
        override var dAlpha: Double = 0.0

        override var dRadius: Double = 0.0
        override var dPrevLength: Double = 0.0

        fun setValues(prevSection: IDirectedSection, r: Double, length: Double) {
            dCenterX = prevSection.dCenterX + length * cos(prevSection.dAlpha)
            dCenterY = prevSection.dCenterY + length * sin(prevSection.dAlpha)
            dCenterZ = prevSection.dCenterZ - prevSection.dRadius + r
            dAlpha = prevSection.dAlpha
            dRadius = r
            dPrevLength = length
        }
    }

    /**
     * No neck collision model
     */
    private class MyCollisionModel : IBodyCollisionModel {

        override var headOffset: Double = 0.0
        override var headRadius: Double = 0.0
        override var lengthTailToFace: Double = 0.0
        override lateinit var faceSection: IDirectedSection
        override lateinit var bodySections: Sequence<IDirectedSection>

        fun init(body: BodyModel, neckSection: IDirectedSection) {
            bodySections = body.bodySections
            this.faceSection = neckSection
            val propos = body.bodyProportions
            lengthTailToFace = propos.lengthToNeck
            // always consider head ring is next to Neck
            headRadius = propos.segmentRadius(propos.neckIndex + 1)
            headOffset = propos.segmentEndFromTail(propos.neckIndex + 1) - lengthTailToFace
        }
    }

    private val collisionModelImpl = MyCollisionModel()

    val collisionModel: IBodyCollisionModel = collisionModelImpl

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

    private val headSectionsList =
        (bodyProportions.neckIndex + 1 until bodyProportions.segmentCount).map { SingleSection() }.toList()

    /**
     * is created in init()
     */
    private lateinit var linkedListTailSection: BodySegmentModel

    /**
     *  is created in init()
     */
    private lateinit var linkedListNeckSection: BodySegmentModel

    // all segments up to bodyProportions.neckIndex
    val bodySections : Sequence<IDirectedSection> get() = sequence {
        var curSection:BodySegmentModel? = linkedListTailSection

        while(curSection != null) {
            yield(curSection as IDirectedSection)
            curSection = curSection.next
        }
    }

    // all segments
    val bodyAndHeadSections : Sequence<IDirectedSection> get() = sequence {
        yieldAll(bodySections)
        yieldAll(headSectionsList)
    }
    // body sections with extra section for head


    private fun updateHeadSection() {
        var prev = linkedListNeckSection as IDirectedSection
        var nSeg = bodyProportions.neckIndex + 1
        var lastLenFromTail = bodyProportions.segmentEndFromTail(bodyProportions.neckIndex)

        for (headSection in headSectionsList) {
            val curLenFromTail = bodyProportions.segmentEndFromTail(nSeg)
            headSection.setValues(prev, bodyProportions.segmentRadius(nSeg), curLenFromTail - lastLenFromTail)
            prev = headSection
            nSeg++
            lastLenFromTail = curLenFromTail
        }
    }


    /**
     * Z is floor level
     */
    fun init(startX: Double, startY: Double, floorZ: Double,
             startHorzAngle: Double, totalLength: Double) {
        this@BodyModel.floorZ = floorZ
        foodRunLength = 0.0
        bodyProportions.resize(totalLength)
        // find out initial straight section length
        val tailToNeck = bodyProportions.segmentEndFromTail(bodyProportions.neckIndex)
        linkedListTailSection = BodySegmentModel.createTail(startX, startY, startHorzAngle)
        linkedListNeckSection = linkedListTailSection.appendSection(startHorzAngle, tailToNeck)
        processSegments()
    }

    private fun Sequence<IDirectedSection>.printable() =
        this.map { s-> "[${s.dPrevLength}, ${s.dRadius}]" }.joinToString(", ")

    // updates diameters, inserts if needed, computes length - and does not change it
    private fun processSegments() {
        var index = 0
        val neckIndex = bodyProportions.neckIndex
        var curShapeSegmentStart = 0.0
        var curShapeSegmentEnd = bodyProportions.segmentEndFromTail(0)
        var remainingLength = curShapeSegmentEnd
        var shapeSegmentR0 = 0.0
        var shapeSegmentR1 = bodyProportions.segmentRadius(0)

        var segment = linkedListTailSection.next!!

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
                    linkedListNeckSection = segment
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
                if(index > neckIndex) {
                    linkedListNeckSection = segment // probably this should not happen but still update the head section
//                    Log.d("segments", "    break after smallest section [r = ${linkedListNeckSection.dRadius}, l = ${linkedListNeckSection.dPrevLength}]")
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

        updateHeadSection()
        collisionModelImpl.init(this, linkedListNeckSection)
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
        //println("=========== begin advance: body is ${bodyAndHeadSections.printable()}")

        //println("bef. bodyProportions.advance($distance): ${bodyProportions.lengthToNeck}; ${bodyProportions.fullLength}")
        bodyProportions.advance(distance)
        //println("aft. bodyProportions.advance($distance): ${bodyProportions.lengthToNeck}; ${bodyProportions.fullLength}")

        val neckExtension: Double = distance
        // equalize BODY segments length with proportions size  by extending neck and cutting tail if needed

        if(angleDelta == 0.0) {
            linkedListNeckSection.extendBy(neckExtension)
        } else {
            linkedListNeckSection = linkedListNeckSection.appendSection(
                linkedListNeckSection.dAlpha + angleDelta, neckExtension)
        }

        var remainingShorten = bodySections.sumByDouble { s -> s.dPrevLength } - bodyProportions.lengthToNeck

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

        processSegments()
//        Log.d("segments", "advance($distance, $angleDelta): " + bodySections.map { it.dRadius }.joinToString(", "))
    }

    /** how much length to keep while extending */
    fun feed() {
        bodyProportions.feed()
        // foodRunLength += aFoodRunLength
    }
}