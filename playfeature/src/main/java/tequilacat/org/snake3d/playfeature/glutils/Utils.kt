package tequilacat.org.snake3d.playfeature.glutils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.*


class CoordUtils {
    companion object {
        /**
         * cross product between p2-p1 and p3-p1
         */
        fun crossProduct(target: FloatArray, targetPos: Int, coords: FloatArray, pos1: Short, pos2: Short, pos3: Short) {
            val p1 = pos1 * 3
            val p2 = pos2 * 3
            val p3 = pos3 * 3

            val uX = coords[p2] - coords[p1]
            val uY = coords[p2 + 1] - coords[p1 + 1]
            val uZ = coords[p2 + 2] - coords[p1 + 2]

            val vX = coords[p3] - coords[p1]
            val vY = coords[p3 + 1] - coords[p1 + 1]
            val vZ = coords[p3 + 2] - coords[p1 + 2]

            target[targetPos] = uY * vZ - uZ * vY
            target[targetPos + 1] = uZ * vX - uX * vZ
            target[targetPos + 2] = uX * vY - uY * vX
            /*
            Nx = UyVz - UzVy
            Ny = UzVx - UxVz
            Nz = UxVy - UyVx*/
        }

        fun distance(p1: FloatArray, p2: FloatArray): Float {
            val dx = p1[0] - p2[0]
            val dy = p1[1] - p2[1]
            val dz = p1[2] - p2[2]

            return Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
        }

        fun length(v: FloatArray, pos: Int = 0) =
            sqrt(v[pos] * v[pos] + v[pos + 1] * v[pos + 1] + v[pos + 2] * v[pos + 2])

        /** src and dst may be same array at same position (but don't overlap!)*/
        fun normalize(dst: FloatArray, dstPos: Int, src: FloatArray, srcPos: Int) {
            val l = 1.0f / length(src, srcPos)
            dst[dstPos] = src[srcPos] * l
            dst[dstPos + 1] = src[srcPos + 1] * l
            dst[dstPos + 2] = src[srcPos + 2] * l
        }
    }
}

open class TriangleGeometry(val vertexes: FloatBuffer, val indexes: ShortBuffer) {

    constructor(vertexArray: FloatArray, indexesArray: ShortArray) : this(
        vertexArray.toBuffer(),
        indexesArray.toBuffer()
    )

    // TODO consider dynamic buffers
    val indexCount by lazy { indexes.capacity() }

    companion object {
        const val COORDS_PER_VERTEX = 3
        /** 4 = bytes per vertex */
        const val VERTEX_STRIDE = COORDS_PER_VERTEX * 4
    }
}

class TriangleGeometryWithNormals(vertexes: FloatBuffer, indexes: ShortBuffer, val normals: FloatBuffer)
    : TriangleGeometry(vertexes, indexes){
}

class GeometryBuilder {
    companion object {
        private val RECT_VERTEX_ORDER_LIST = shortArrayOf(0, 1, 2, 0, 2, 3)

        fun makePrism(
            cx: Float, cy: Float, zBase: Float,
            height: Float, radius: Float, sides: Int,
            addCap: Boolean = false
        ) = Pair(
            makePrismVertexes(
                cx,
                cy,
                radius,
                sides,
                zBase,
                height
            ),
            makePrismIndexes(sides, addCap)
        )

        private fun makePrismVertexes(
            cx: Float, cy: Float, radius: Float, sides: Int,
            zBase: Float, height: Float
        ): FloatArray {
            val deltaAlpha: Float = PI.toFloat() * 2 / sides.toFloat()
            val coords = FloatArray(sides * 2 * 3)
            var pos = 0
            var alpha = 0f

            for (side in 0 until sides) {
                val x = cx + radius * cos(alpha)
                val y = cy + radius * sin(alpha)

                coords[pos++] = x
                coords[pos++] = y
                coords[pos++] = zBase

                coords[pos++] = x
                coords[pos++] = y
                coords[pos++] = zBase + height

                alpha += deltaAlpha
            }

            return coords
        }


        /**
         * creates prism indexes for specified sides count,
         * assume prism along 0Z, vertexes follow around center counter-clockwise (increasing alpha)
         */
        private fun makePrismIndexes(sides: Int, addCap: Boolean) : ShortArray {
            val array = ShortArray(sides * 6 + if (addCap) (sides - 2) * 3 else 0)
            var pos = 0

            for (sideVertex in 0 until sides) {
                val p0: Short = (sideVertex * 2).toShort()
                val p1: Short = (p0 + 1).toShort()
                val p2: Short = if (sideVertex < sides - 1) (p0 + 2).toShort() else 0
                val p3: Short = (p2 + 1).toShort()
                array[pos++] = p0
                array[pos++] = p2
                array[pos++] = p1
                array[pos++] = p2
                array[pos++] = p3
                array[pos++] = p1
            }

            if (addCap) {
                val first: Short = 1
                var next: Short = 3 // first + 1

                for(i in 0 until sides - 2) {
                    array[pos++] = first
                    array[pos++] = next
                    next = (next + 2).toShort()
                    array[pos++] = next
                }
            }
            return array
        }

        /**
         * generates separate vertices and a normal for each vertex, without indexing
         */
        fun makeFacettedGeometry(
            vertexes: FloatArray,
            indexes: ShortArray
        ): Pair<FloatArray, FloatArray> {

            // make target vertexes
            // no reuse of vertexes - facets will be visible
            val resVertexCount = indexes.size // for each index there will be a vertex
            val outVertexes = FloatArray(resVertexCount * 3)
            val normals = FloatArray(resVertexCount * 3)
            val singleNormalCoords = FloatArray(3) // temp out array
            var coordPtr = 0 //

            for (i in 0 until indexes.size step 3) {
                // for all 3 face vertices the normal is same so compute once
                CoordUtils.crossProduct(singleNormalCoords, 0,
                    vertexes, indexes[i], indexes[i + 1], indexes[i + 2])
                CoordUtils.normalize(singleNormalCoords,0, singleNormalCoords, 0)

                for(vI in 0 .. 2) {
                    val cPos = indexes[i + vI] * 3 // position of coords
                    normals[coordPtr] = singleNormalCoords[0]
                    normals[coordPtr + 1] = singleNormalCoords[1]
                    normals[coordPtr + 2] = singleNormalCoords[2]

                    outVertexes[coordPtr] = vertexes[cPos]
                    outVertexes[coordPtr + 1] = vertexes[cPos + 1]
                    outVertexes[coordPtr + 2] = vertexes[cPos + 2]

                    coordPtr += 3
                }
            }

            return Pair(outVertexes, normals)
        }
    }

    private val storedVertexes = mutableListOf<Float>()
    private val storedIndexes = mutableListOf<Short>()
    private var freeIndex: Short = 0

    fun add(vertexes: FloatArray, indexes: ShortArray) {
        vertexes.forEach { storedVertexes.add(it) }
        val currentBase = freeIndex

        indexes.forEach {
            val index : Short = (currentBase + it).toShort()
            if (freeIndex < index) {
                freeIndex = index
            }
            storedIndexes.add(index)
        }
        freeIndex++
    }

    fun addQuad(x0: Float, y0: Float, z0: Float,
                x1: Float, y1: Float, z1: Float,
                x2: Float, y2: Float, z2: Float,
                x3: Float, y3: Float, z3: Float) {
        add(floatArrayOf(x0,y0,z0,x1,y1,z1,x2,y2,z2,x3,y3,z3),
            RECT_VERTEX_ORDER_LIST
        )
    }

    fun buildVertexBuffer() = storedVertexes.toFloatArray().toBuffer()
    fun buildIndexBuffer() = storedIndexes.toShortArray().toBuffer()
    fun build() = TriangleGeometry(buildVertexBuffer(), buildIndexBuffer())

    fun toArrays() = Pair(storedVertexes.toFloatArray(), storedIndexes.toShortArray())

    // fun buildWithNormals() = TriangleGeometryWithNormals(storedVertexes.toFloatArray(), storedIndexes.toShortArray())
}

// initialize vertex byte buffer for shape coordinates
fun FloatArray.toBuffer(): FloatBuffer {
    val floatArray: FloatArray = this
    // (# of coordinate values * 4 bytes per float)
    return ByteBuffer.allocateDirect(floatArray.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(floatArray)
            position(0)
        }
    }
}


// initialize byte buffer for the draw list
fun ShortArray.toBuffer(): ShortBuffer {
    val shortArray: ShortArray = this

// (# of coordinate values * 2 bytes per short)
    return ByteBuffer.allocateDirect(shortArray.size * 2).run {
        order(ByteOrder.nativeOrder())
        asShortBuffer().apply {
            put(shortArray)
            position(0)
        }
    }
}

fun Int.toColorArray() : FloatArray = floatArrayOf(
    ((this ushr 16) and 0xff) / 255f,
    ((this ushr 8) and 0xff) / 255f,
    (this and 0xff) / 255f,
    1f)
