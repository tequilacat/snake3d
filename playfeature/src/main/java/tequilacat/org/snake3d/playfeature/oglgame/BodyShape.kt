package tequilacat.org.snake3d.playfeature.oglgame

import tequilacat.org.snake3d.playfeature.IBodySegment
import tequilacat.org.snake3d.playfeature.glutils.CoordUtils
import tequilacat.org.snake3d.playfeature.glutils.Geometry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * @param startAngle at which angle section vertex iteration starts
 * @param uPerLengthUnit how much U per 1.0 of run length
 * @param vStart texture V at angle 0
 */
class BodyShape(
    val segmentFaceCount: Int, private val bodyRadius: Float,
    private val startAngle: Float,
    private val uPerLengthUnit: Float, private val vStart: Float
) {

    // always even count
    private val vertexFloatStride = 8

    private val segmentAngleSinCos = Array(segmentFaceCount) {
        Pair(
            sin(startAngle + PI * 2 / segmentFaceCount * it).toFloat(),
            cos(startAngle + PI * 2 / segmentFaceCount * it).toFloat()
        )
    }

    // last generated geometry
    lateinit var geometry: Geometry private set

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
//            Log.d("body", "increase vertexes to ${vertexes.size}")
        }

        // allocate indexes
        // 2 triangle each of 3 vert for each face of a segment
        indexCount = (segmentCount + 3) * segmentFaceCount * 2 * 3

        // alloc in advance
        if(indexes.size < indexCount) {
            indexes = ShortArray(indexCount + 1000) //
//            Log.d("body", "increase indexes to ${indexes.size}")
        }
    }

    /**
     *
     */
    fun update(segments: Collection<IBodySegment>) {
        allocateArrays(segments.size)
        rebuildIndexes(segments.size + 2)
        rebuildVertexes(segments)

        //val stNormals = SystemClock.uptimeMillis()
        computeNormals()
        //val stNormals1 = SystemClock.uptimeMillis()
        //Log.d("perf", "Segments: ${segments.size}, Normals: $indexCount, time: ${stNormals1 - stNormals} ms")

        geometry = Geometry(
            vertexes, vertexCount,
            indexes, indexCount,
            hasNormals = true,
            hasTexture = true
        )
    }

    private val tmpNormals = FloatArray(3)

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

            var count = 3
            while(count-- > 0) {
            //for (it in 0..2) {
                val vPos = indexes[i] * vertexFloatStride + 5
                vertexes[vPos] += tmpNormals[0]
                vertexes[vPos + 1] += tmpNormals[1]
                vertexes[vPos + 2] += tmpNormals[2]
                i++
            }

            //i += 3
        }

        for(vertexIndex in 0 until vertexCount) {
            val normalPos = vertexIndex * vertexFloatStride + 5
            CoordUtils.normalize(vertexes, normalPos, vertexes, normalPos)
        }
    }

    private fun rebuildIndexes(ringsCount: Int) {
        // generate standard triangle grid
        var curVertexIndex = 0

        // start
        for (faceIndex in 0 until segmentFaceCount) {
            indexes[curVertexIndex++] = 0
            indexes[curVertexIndex++] = (faceIndex + 1).toShort()
            indexes[curVertexIndex++] = if (faceIndex == segmentFaceCount - 1) 1 else (faceIndex + 2).toShort()
        }

        var startVertex = 1

        for (segIndex in 0 until ringsCount) {
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

    private fun addRing(atIndex: Int, cx: Float, cy: Float, cz: Float, radius: Float, angle: Float,
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

    private fun rebuildVertexes(bodySegments: Collection<IBodySegment>) {
        val endRadius = bodyRadius
        val endCorrLen = bodyRadius * 0.7f
        val corrRadius = bodyRadius * 0.7f
        val totalLength = bodySegments.sumByDouble { it.length.toDouble() }.toFloat() + bodyRadius * 2
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

        // add first correction ring
        addRing(index,
            first.startX - endCorrLen * first.alphaCosinus,
            first.startY - endCorrLen * first.alphaSinus, first.startZ, corrRadius, first.alpha,
            (totalLength - bodyRadius + endCorrLen) * uPerLengthUnit)
        index += ringStride
        currentU -= bodyRadius * uPerLengthUnit

        var prevSegment: IBodySegment? = null

        for (segment in bodySegments) {
            addRing(
                index, segment.startX, segment.startY, segment.startZ, bodyRadius,
                if (prevSegment != null)
                    (segment.alpha + prevSegment.alpha) / 2
                else segment.alpha,
                currentU
            )

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
    }
}