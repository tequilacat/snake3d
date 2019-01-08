package tequilacat.org.snake3d.playfeature.oglgame

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer


val RECT_VERTEX_ORDER_LIST = shortArrayOf(0, 1, 2, 0, 2, 3)
val RECT_VERTEX_ORDER: ShortBuffer = RECT_VERTEX_ORDER_LIST.toBuffer()


class TriangleBuilder {
    private val storedVertexes = mutableListOf<Float>()
    private val storedIndexes = mutableListOf<Short>()
    private var freeIndex: Short = 0

    fun add(vertexes: FloatArray, indexes: ShortArray) {
        vertexes.forEach { storedVertexes.add(it) }
        val currentBase = freeIndex

        indexes.forEach() {
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
            RECT_VERTEX_ORDER_LIST)
    }

    fun buildVertexBuffer() = storedVertexes.toFloatArray().toBuffer()
    fun buildIndexBuffer() = storedIndexes.toShortArray().toBuffer()
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
