package tequilacat.org.snake3d.playfeature.oglgame

import tequilacat.org.snake3d.playfeature.IBodySegmentModel
import tequilacat.org.snake3d.playfeature.glutils.CoordUtils
import tequilacat.org.snake3d.playfeature.glutils.Geometry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

interface IBodyGeometryBuilder {
    val geometry: Geometry
    fun update(segments: Iterable<IBodySegmentModel>)
}

/**
 * @param startAngle at which angle section vertex iteration starts
 * @param uPerLengthUnit how much U per 1.0 of run length
 * @param vStart texture V at angle 0
 */
abstract class AbstractBodyGeometryBuilder(
    val segmentFaceCount: Int,
    private val startAngle: Float,
    protected val uPerLengthUnit: Float, private val vStart: Float
) : IBodyGeometryBuilder {

    private val segmentAngleSinCos = Array(segmentFaceCount) {
        Pair(
            sin(startAngle + PI * 2 / segmentFaceCount * it).toFloat(),
            cos(startAngle + PI * 2 / segmentFaceCount * it).toFloat()
        )
    }

    // last generated geometry
    private lateinit var geom: Geometry
    override val geometry get() = geom

    protected val vertexFloatStride = 8

    // some safe guesses
    protected var vertexes: FloatArray = FloatArray(1000)
    private var indexes: ShortArray = ShortArray(2000)
    private var indexCount = 0
    protected var vertexCount = 0

    protected abstract fun rebuildGeometry(bodySegments: Iterable<IBodySegmentModel>)

    override fun update(segments: Iterable<IBodySegmentModel>) {
        rebuildGeometry(segments)

        //val stNormals = SystemClock.uptimeMillis()
        computeNormals()
        //val stNormals1 = SystemClock.uptimeMillis()
        //Log.d("perf", "Segments: ${segments.size}, Normals: $indexCount, time: ${stNormals1 - stNormals} ms")

        geom = Geometry(
            vertexes, vertexCount,
            indexes, indexCount,
            hasNormals = true,
            hasTexture = true
        )
    }

    private val tmpNormals = FloatArray(3)

    /**
     * (re-)allocates vertex array from known ring count
     * Called from rebuildGeometry when ring count is known
     */
    protected fun allocateVertexArray(ringCount: Int) {
        vertexCount = (segmentFaceCount * ringCount + 2)
        val vertexCoordCount = vertexCount * vertexFloatStride

        if(vertexes.size < vertexCoordCount) {
            vertexes = FloatArray(vertexCoordCount + 1000)
        }
    }

    /**
     * called by computeVertexes when ring count is known
     */
    protected fun rebuildIndexes(ringsCount: Int) {
        // allocate indexes
        // 2 triangle each of 3 vert for each face of a segment
        // indexCount = ringCount * segmentFaceCount * 2 * 3
        // 3*triangles
        indexCount = 3 * (segmentFaceCount * 2 + (ringsCount - 1) * segmentFaceCount * 2)

        // alloc in advance
        if(indexes.size < indexCount) {
            indexes = ShortArray(indexCount + 1000) //
//            Log.d("body", "increase indexes to ${indexes.size}")
        }

        // generate standard triangle grid
        var curVertexIndex = 0

        // start
        for (faceIndex in 0 until segmentFaceCount) {
            indexes[curVertexIndex++] = 0
            indexes[curVertexIndex++] = (faceIndex + 1).toShort()
            indexes[curVertexIndex++] = if (faceIndex == segmentFaceCount - 1) 1 else (faceIndex + 2).toShort()
        }

        var startVertex = 1

        for (segIndex in 0 until ringsCount - 1) {
            for (faceIndex in 0 until segmentFaceCount) {
                val i1 = startVertex + faceIndex
                val i2 = i1 + segmentFaceCount
                val i4 = if (faceIndex == segmentFaceCount - 1) startVertex else (i1 + 1)
                val i3 = i4 + segmentFaceCount

                indexes[curVertexIndex++] = i1.toShort()
                indexes[curVertexIndex++] = i2.toShort()
                indexes[curVertexIndex++] = i3.toShort()

                indexes[curVertexIndex++] = i1.toShort()
                indexes[curVertexIndex++] = i3.toShort()
                indexes[curVertexIndex++] = i4.toShort()
            }

            startVertex += segmentFaceCount
        }

        // end
        val endIndex = startVertex + segmentFaceCount
        for (faceIndex in 0 until segmentFaceCount) {
            indexes[curVertexIndex++] = (startVertex + faceIndex).toShort()
            indexes[curVertexIndex++] = endIndex.toShort()
            indexes[curVertexIndex++] =
                if (faceIndex == segmentFaceCount - 1) startVertex.toShort()
                else (startVertex + faceIndex + 1).toShort()
        }
    }

    private fun computeNormals() {
        // reset normals to 0
        for (vi in vertexFloatStride - 3 until vertexCount step vertexFloatStride) {
            // reset normals
            vertexes[vi] = 0f
            vertexes[vi + 1] = 0f
            vertexes[vi + 2] = 0f
        }

        var i = 0
        while(i < indexCount) {
        //for (i in 0 until indexCount step 3) {
            // compute normals
            CoordUtils.crossProduct(
                tmpNormals, 0, // end of stride (to account for possible UV in between
                vertexes, indexes[i].toInt(), indexes[i + 1].toInt(), indexes[i + 2].toInt(), vertexFloatStride
            )

//            println("updateN triangle ${i / 3}: v[${indexes[i]}, ${indexes[i + 1]}, ${indexes[i + 2]}] += normal ${tmpNormals.contentToString()}")

            var count = 3
            while(count-- > 0) {
            //for (it in 0..2) {
                val vPos = indexes[i] * vertexFloatStride + 5
                vertexes[vPos] += tmpNormals[0]
                vertexes[vPos + 1] += tmpNormals[1]
                vertexes[vPos + 2] += tmpNormals[2]
//                println("   upd. normal of vertex #${indexes[i]} [vPos0 = ${vPos-5}] -> " +
//                        "${vertexes[vPos]}, ${vertexes[vPos + 1]}, ${vertexes[vPos + 2]}")

                i++
            }

            //i += 3
        }
        for(vertexIndex in 0 until vertexCount) {
            val normalPos = vertexIndex * vertexFloatStride + 5
            CoordUtils.normalize(vertexes, normalPos, vertexes, normalPos)
//            println("  V#$vertexIndex: ${vertexes[normalPos]}, ${vertexes[normalPos+1]}, ${vertexes[normalPos+2]}")
        }
    }

    protected fun addRing(atIndex: Int, cx: Float, cy: Float, cz: Float, radius: Float, angle: Float,
                        ringU: Float) {
        val sinus = sin(angle)
        val cosinus = cos(angle)
        var index = atIndex
        val dV = 1f / segmentFaceCount
        var currV = 0f

        for ((aSin, aCos) in segmentAngleSinCos) {
            // compute coord of every vertex of the segment
            val dx0 = radius * aCos
            // val dy0 = bodyRadius * aSin
            vertexes[index] = (cx + dx0 * sinus)
            vertexes[index + 1] = (cy - dx0 * cosinus)
            vertexes[index + 2] = cz + radius * aSin
            vertexes[index + 3] = ringU
            vertexes[index + 4] = currV

            currV += dV
            index += vertexFloatStride
        }
    }
}

class BodyShape(segmentFaceCount: Int, private val bodyRadius: Float, startAngle: Float, uPerLengthUnit: Float, vStart: Float) :
    AbstractBodyGeometryBuilder(segmentFaceCount, startAngle, uPerLengthUnit, vStart) {

    override fun rebuildGeometry(bodySegments: Iterable<IBodySegmentModel>) {
        var totalLength = bodyRadius * 2
        var segmentCount = 0

        // first run is needed anyway, so we compute also segment count
        bodySegments.forEach {
            totalLength += it.length
            segmentCount++
        }
        // and adjust vertex array here
        allocateVertexArray(segmentCount + 3)

        val endRadius = bodyRadius
        val endCorrLen = bodyRadius * 0.7f
        val corrRadius = bodyRadius * 0.7f
        var currentU = totalLength * uPerLengthUnit
        val first = bodySegments.first()

        // add tail point
        vertexes[0] = first.startX - endRadius * first.alphaCosinus
        vertexes[1] = first.startY - endRadius * first.alphaSinus
        vertexes[2] = first.startZ
        vertexes[3] = currentU
        vertexes[4] = 0.5f

        var index = vertexFloatStride
        val ringStride = segmentFaceCount * vertexFloatStride
        var ringCount = 3

        // add first correction ring
        addRing(index,
            first.startX - endCorrLen * first.alphaCosinus,
            first.startY - endCorrLen * first.alphaSinus, first.startZ, corrRadius, first.alpha,
            (totalLength - bodyRadius + endCorrLen) * uPerLengthUnit)
        index += ringStride
        currentU -= bodyRadius * uPerLengthUnit

        var prevSegment: IBodySegmentModel? = null

        for (segment in bodySegments) {
            addRing(
                index, segment.startX, segment.startY, segment.startZ, bodyRadius,
                if (prevSegment != null)
                    (segment.alpha + prevSegment.alpha) / 2
                else segment.alpha,
                currentU
            )
            ringCount++

            currentU -= uPerLengthUnit*segment.length
            prevSegment = segment
            index += ringStride
        }

        val last = bodySegments.last()
        // add last segment end ring
        addRing(index, last.endX, last.endY, last.endZ, bodyRadius, last.alpha, currentU)
        index += ringStride

        // add last correction ring
        addRing(
            index,
            last.endX + endCorrLen * last.alphaCosinus,
            last.endY + endCorrLen * last.alphaSinus, last.endZ, corrRadius, last.alpha,
            (bodyRadius - endCorrLen) * uPerLengthUnit)
        index += ringStride
        // add nose point
        vertexes[index++] = last.endX + endRadius * last.alphaCosinus
        vertexes[index++] = last.endY + endRadius * last.alphaSinus
        vertexes[index++] = last.endZ

        vertexes[index++] = 0f // nose U is always 0
        vertexes[index] = 0.5f // nose V TODO what should be V for end points? middle?

        rebuildIndexes(ringCount)
    }
}


class BodyShapeBuilder(segmentFaceCount: Int, startAngle: Float, uPerLengthUnit: Float, vStart: Float) :
    AbstractBodyGeometryBuilder(segmentFaceCount, startAngle, uPerLengthUnit, vStart) {

    override fun rebuildGeometry(bodySegments: Iterable<IBodySegmentModel>) {
        val iterableSegments = bodySegments as Iterable<IBodySegmentModel>
        val iter = iterableSegments.iterator()
        val first = iter.next() // bodySegments.first()
        var totalLength = 0f
        var segmentCount = 0

        // first run is needed anyway, so we compute also segment count
        iterableSegments.forEach {
            totalLength += it.length
            segmentCount++
        }
        // and adjust vertex array here
        allocateVertexArray(segmentCount)
        rebuildIndexes(segmentCount)

        var currentU = (totalLength) * uPerLengthUnit

        // add tail point
        vertexes[0] = first.startX
        vertexes[1] = first.startY
        vertexes[2] = first.startZ
        vertexes[3] = currentU
        vertexes[4] = 0.5f

        var index = vertexFloatStride
        val ringStride = segmentFaceCount * vertexFloatStride
        var prev = first // assume hasNext
        var ringCount = 1 // anyway last ring is added

        while(iter.hasNext()) {
            val next = iter.next()
            // betveen prev end and next start

            currentU -= uPerLengthUnit * prev.length
            addRing(
                index, prev.endX, prev.endY, prev.endZ, prev.endRadius,
                (next.alpha + prev.alpha) / 2, currentU
            )

            ringCount++
            index += ringStride
            prev = next
        }

        // prev points to last segment, draw closing ring and nose
        addRing(
            index, prev.endX, prev.endY, prev.endZ, prev.endRadius,
            prev.alpha, 0f // nose point has always 0U
        )
        index += ringStride

        // add nose as center of face ring
        vertexes[index++] = prev.endX
        vertexes[index++] = prev.endY
        vertexes[index++] = prev.endZ

        vertexes[index++] = 0f // nose U is always 0
        vertexes[index++] = 0.5f // nose V TODO what should be V for end points? middle?

        // now index points at normal start, increase by 3 to point to next vertex stride
        // and compute vertex count
        vertexCount = (index +3 )/ vertexFloatStride
    }
}