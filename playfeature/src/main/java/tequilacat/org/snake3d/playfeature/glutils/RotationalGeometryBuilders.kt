package tequilacat.org.snake3d.playfeature.glutils

import android.os.SystemClock
import android.util.Log
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

interface IDirectedSection {
    val centerX: Float
    val centerY: Float
    val centerZ: Float

    val radius: Float

    val prevLength: Float
    // TODO get rid of alpha here
    val alpha: Float
}

interface IRotationalGeometryBuilder {
    val geometry: Geometry
    fun update(segments: Sequence<IDirectedSection>)
}

/**
 * @param startAngle at which angle section vertex iteration starts
 * @param uPerLengthUnit how much U per 1.0 of run length
 * @param vStart texture V at angle 0
 */
abstract class AbstractRotationalGeometryBuilder(
    val segmentFaceCount: Int,
    private val startAngle: Float,
    protected val uPerLengthUnit: Float, private val vStart: Float
) : IRotationalGeometryBuilder {

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
    protected var vertexes: FloatArray = FloatArray(2000)
    private var indexes: ShortArray = ShortArray(2000)
    private var indexCount = 0
    protected var vertexCount = 0

    protected abstract fun rebuildGeometry(bodySegments: Sequence<IDirectedSection>)

    override fun update(segments: Sequence<IDirectedSection>) {
        val t1 = SystemClock.uptimeMillis()
        rebuildGeometry(segments)
        val t2 = SystemClock.uptimeMillis()
        //val stNormals = SystemClock.uptimeMillis()
        computeNormals()
        val t3 = SystemClock.uptimeMillis()
        //val stNormals1 = SystemClock.uptimeMillis()
        Log.d("perf", "update[$indexCount]: geom ${t2 - t1}, normals ${t3 - t2} ms")

        geom = Geometry(
            vertexes, vertexCount,
            indexes, indexCount,
            hasNormals = true,
            hasTexture = true
        )
    }

    private val tmpNormals = FloatArray(3)

    /**
     * allocates vertex array assuming we're about to storie vertex at specified index.
     * makes sure there's at least 1000 vertex floats allowed to be stored after the call
     */
    protected fun ensureVertexCapacity(currentVertexIndex: Int) {
        val newSize = currentVertexIndex + 1000

        if (vertexes.size < newSize) {
            val newArray = FloatArray(newSize)
            vertexes.copyInto(newArray, 0, 0, currentVertexIndex)
            vertexes = newArray
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
        val t0 = SystemClock.uptimeMillis()

        // reset normals to 0
        for (vi in vertexFloatStride - 3 until vertexCount step vertexFloatStride) {
            // reset normals
            vertexes[vi] = 0f
            vertexes[vi + 1] = 0f
            vertexes[vi + 2] = 0f
        }

        val t1 = SystemClock.uptimeMillis()

        var i = 0
        while(i < indexCount) {
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
        }

        val t2 = SystemClock.uptimeMillis()

        var normalPos = 5
        for(vertexIndex in 0 until vertexCount) {
            //val normalPos = vertexIndex * vertexFloatStride + 5
            CoordUtils.normalize(vertexes, normalPos, vertexes, normalPos)
            normalPos += vertexFloatStride
//            println("  V#$vertexIndex: ${vertexes[normalPos]}, ${vertexes[normalPos+1]}, ${vertexes[normalPos+2]}")
        }

        val t3 = SystemClock.uptimeMillis()
        // computeNormals large count: 6,15,0
        Log.d("perf", "computeNormals: ${t3-t2}, ${t2-t1}, ${t1-t0}")
    }

    protected fun addRing(atIndex: Int, cx: Float, cy: Float, cz: Float, radius: Float, angle: Float,
                        ringU: Float) {
        ensureVertexCapacity(atIndex)

        val sinus = sin(angle)
        val cosinus = cos(angle)
        var index = atIndex
        val dV = 1f / segmentFaceCount
        var currV = 0f

        for ((aSin, aCos) in segmentAngleSinCos) {
            // compute coord of every vertex of the segment
            val dx0 = radius * aCos
            // TODO use sum of vectors instead of trigon ops - that also should also make 3d easy
            storeVertex(index,cx + dx0 * sinus, cy - dx0 * cosinus, cz + radius * aSin, ringU, currV)
            currV += dV
            index += vertexFloatStride
        }
    }

    // TODO remove explicit rotation after suppressing alpha in IDirectedSection
    private var rotateAroundAxis = false

    /**
     *  when all are 3 we don't rotate
     */
    fun setAxis(axisX: Float, axisY: Float, axisZ: Float) {
        rotateAroundAxis = !(axisX == 0f && axisY == 0f && axisZ == 0f)
    }

    /**
     * Stores coords at index taking rotation into account
     * */
    protected fun storeVertex(vertexFloatIndex:Int, x: Float, y: Float, z: Float, u: Float, v: Float) {
        var i = vertexFloatIndex
//         println("storeVertex @$vertexFloatIndex [#${vertexFloatIndex / 8}]: $x, $y, $z; uv = $u $v")

        if(rotateAroundAxis) {
            // hardcoded! replace OX with 0Z

            vertexes[i++] = y
            vertexes[i++] = z
            vertexes[i++] = x

        } else {
            vertexes[i++] = x
            vertexes[i++] = y
            vertexes[i++] = z
        }

        vertexes[i++] = u
        vertexes[i] = v
    }
}

class RotationalShapeBuilder(segmentFaceCount: Int, startAngle: Float, uPerLengthUnit: Float, vStart: Float,
                             private val useWrapFace: Boolean = false) :
    AbstractRotationalGeometryBuilder(segmentFaceCount, startAngle, uPerLengthUnit, vStart) {

    override fun rebuildGeometry(bodySegments: Sequence<IDirectedSection>) {
        val iter = bodySegments.iterator()
        val first = iter.next()
        var segmentCount = 0

        // make sure we have enough at the start (although there will be checks in addRing)
        ensureVertexCapacity(0)

        var currentU = 0.0f

        // Z: very crude assume that start segment has no start radius at all
        storeVertex(0, first.centerX, first.centerY, first.centerZ, currentU, 0.5f)

        var index = vertexFloatStride
        val ringStride = segmentFaceCount * vertexFloatStride
        var prev = first

        // always must have at least 2 rings (at least 1 segment) !
        while(iter.hasNext()) {
            val next = iter.next()
            // betveen prev end and next start

            currentU += uPerLengthUnit * next.prevLength
            addRing(
                index, next.centerX, next.centerY, next.centerZ, next.radius,
                (next.alpha + prev.alpha) / 2, currentU
            )
            segmentCount++
            index += ringStride
            prev = next
        }

        // add nose as center of face ring
        // nose V TODO what should be V for end points? middle (0.5)?
        storeVertex(index, prev.centerX, prev.centerY, prev.centerZ, currentU, 0.5f)

        // index pts to last one, so actual count is +1
        vertexCount = index / vertexFloatStride + 1
        rebuildIndexes(segmentCount)
    }
}


class RotationalGeometryBuilder {
    /**
     * @param distancesAndRadiuses <d,r>, <d,r>...  the 0 is appended automatically
     */
    fun build(distancesAndRadiuses: FloatArray, axisX: Float, axisY: Float, axisZ: Float, faceCount: Int): Geometry {
        TODO("replace with RotationalShapeBuilder or refactor anyway")
        /*val rotationBuilder = RotationalShapeBuilder(faceCount + 1, 0f, 1f, 0f,
            true)
        rotationBuilder.setAxis(axisX, axisY, axisZ)

        val segments = mutableListOf<Segment>()// (Segment(0f,0f,0f, ))
        var x = 0f

        for (i in 0 until distancesAndRadiuses.size step 2) {
            val length = distancesAndRadiuses[i]
            val endRadius = distancesAndRadiuses[i+1]
            segments.add(Segment(x, x + length, length, endRadius))
            x += length
        }

        rotationBuilder.update(segments)
        return rotationBuilder.geometry*/
    }
}