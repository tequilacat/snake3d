package tequilacat.org.snake3d.playfeature.oglgame

import tequilacat.org.snake3d.playfeature.IBodySegment
import tequilacat.org.snake3d.playfeature.glutils.GeometryData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BodyShape(requestedSegmentFaceCount: Int, private val bodyRadius: Float) {

    // always even count
    val segmentFaceCount = requestedSegmentFaceCount - (requestedSegmentFaceCount and 1)
    private val vertexFloatStride = 8

    private val segmentAngleSinCos = Array(segmentFaceCount) {
        Pair(
            sin(PI * 2 / segmentFaceCount * it).toFloat(),
            cos(PI * 2 / segmentFaceCount * it).toFloat())
    }

    // last generated geometry
    lateinit var geometry: GeometryData private set

    // some safe guesses
    private var vertexes: FloatArray = FloatArray(1000)
    private var indexes: ShortArray = ShortArray(2000)
    private var indexCount = 0
    private var vertexCount = 0

    private fun allocateArrays(segmentCount: Int) {
        // allocate vertexes
        vertexCount = (segmentFaceCount * (segmentCount + 3) + 2)
        val vertexCoordCount = vertexCount * vertexFloatStride

        if(vertexes.size < vertexCoordCount) {
            vertexes = FloatArray(vertexCoordCount + 1000)
        }

        // allocate indexes
        // 2 triangle each of 3 vert for each face of a segment
        indexCount = (segmentCount + 3) * segmentFaceCount * 2 * 3

        // alloc in advance
        if(indexes.size < indexCount) {
            indexes = ShortArray(indexCount + 1000) //
        }
    }

    /**
     *
     */
    fun update(segments: Collection<IBodySegment>) {
        allocateArrays(segments.size)
        rebuildIndexes(segments)
        rebuildVertexes(segments)
        geometry = GeometryData(
            vertexes, vertexCount,
            indexes, indexCount,
            hasNormals = true,
            hasTexture = true
        )
    }

    private fun rebuildIndexes(bodySegments: Collection<IBodySegment>) {
        // generate standard triangle grid
        var curVertexIndex = 0

        // start
        for (faceIndex in 0 until segmentFaceCount) {
            indexes[curVertexIndex++] = 0
            indexes[curVertexIndex++] = (faceIndex + 1).toShort()
            indexes[curVertexIndex++] = if (faceIndex == segmentFaceCount - 1) 1 else (faceIndex + 2).toShort()
        }

        var startVertex = 1

        for (segIndex in 0 until bodySegments.size + 2) {
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

    private fun addRing(atIndex: Int, cx: Float, cy: Float, cz: Float, radius: Float, angle: Float) {
        val sinus = sin(angle)
        val cosinus = cos(angle)
        var index = atIndex // 1 + ringIndex * segmentFaceCount * vertexFloatStride

        for ((aSin, aCos) in segmentAngleSinCos) {
            // compute coord of every vertex of the segment
            val dx0 = radius * aCos
            // val dy0 = bodyRadius * aSin
            vertexes[index] = (cx + dx0 * sinus)
            vertexes[index + 1] = (cy - dx0 * cosinus)
            vertexes[index + 2] = cz + radius * aSin

            index += vertexFloatStride
        }
    }

    private fun rebuildVertexes(bodySegments: Collection<IBodySegment>) {
        val endRadius = bodyRadius
        val endCorrLen = bodyRadius * 0.7f
        val corrRadius = bodyRadius * 0.7f

        val first = bodySegments.first()

        // add tail point
        vertexes[0] = first.startX - endRadius * first.alphaCosinus
        vertexes[1] = first.startY - endRadius * first.alphaSinus
        vertexes[2] = first.startZ

        var index = vertexFloatStride
        val ringStride = segmentFaceCount * vertexFloatStride

        // add first correction ring
        addRing(index,
            first.startX - endCorrLen * first.alphaCosinus,
            first.startY - endCorrLen * first.alphaSinus, first.startZ, corrRadius, first.alpha)
        index += ringStride

        var prevSegment: IBodySegment? = null

        for (segment in bodySegments) {
            addRing(
                index, segment.startX, segment.startY, segment.startZ, bodyRadius,
                if (prevSegment != null)
                    (segment.alpha - prevSegment.alpha) / 2
                else segment.alpha
            )

            prevSegment = segment
            index += ringStride
        }

        val last = bodySegments.last()
        // add last segment end ring
        addRing(index, last.endX, last.endY, last.endZ, bodyRadius, last.alpha)
        index += ringStride

        // add last correction ring
        addRing(index,
            last.endX + endCorrLen * last.alphaCosinus,
            last.endY + endCorrLen * last.alphaSinus, last.endZ, corrRadius, last.alpha)
        index += ringStride
        // add nose point
        vertexes[index++] = last.endX + endRadius * last.alphaCosinus
        vertexes[index++] = last.endY + endRadius * last.alphaSinus
        vertexes[index] = last.endZ
    }
}