package tequilacat.org.snake3d.playfeature.glutils

import java.lang.IllegalArgumentException
import java.nio.Buffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer

import android.opengl.GLES20.*

/**
 * Geometry stored as float array, possibly with indexes.
 * Does not reference OpenGL structures
 */
class GeometryData(val vertexes: FloatArray, val vertexCount: Int,
                   val indexes: ShortArray, val indexCount: Int,
                   val hasNormals: Boolean, val hasTexture: Boolean) {

    constructor(vertexes: FloatArray, hasNormals: Boolean, hasTexture: Boolean,
                indexes: ShortArray = Empty.ShortArray) :
            this(vertexes, vertexes.size * BYTES_PER_FLOAT / computeVertexStride(hasNormals, hasTexture),
                indexes, indexes.size,
                hasNormals, hasTexture)

    companion object {
        fun computeVertexStride(hasNormals: Boolean, hasTexture: Boolean): Int {
            var floatStride = 3
            if (hasNormals) floatStride += 3
            if (hasTexture) floatStride += 2
            return floatStride * BYTES_PER_FLOAT
        }
    }

    /** vertex stride in bytes */
    val vertexStride = computeVertexStride(hasNormals, hasTexture)

    val hasIndexes = indexCount > 0
    // val vertexCount = vertexes.size * BYTES_PER_FLOAT / vertexStride
    init {
        if(indexCount > indexes.size) {
            throw IndexOutOfBoundsException("Index count $indexCount exceeds array size ${indexes.size}")
        }
        if(vertexCount * vertexStride / BYTES_PER_FLOAT > vertexes.size) {
            throw IndexOutOfBoundsException(
                "Vertex count $vertexCount (*floats per vertex) exceeds array size ${vertexes.size} ")
        }
    }
}


/**
 * Geometry stored in VBOs, destroyed with OpenGL context
 * */
class Geometry(data: GeometryData) {
    // convert vertexes and indexes into
    val vertexBufferId: Int
    val indexBufferId: Int

    val indexCount: Int = data.indexCount
    val vertexCount: Int = data.vertexCount
    val vertexStride = data.vertexStride
    val coordinatesPerVertex = 3
    val floatsPerTexUV = 2

    val coordBytesOffset = 0
    val normalBytesOffset = (if (data.hasTexture) 5 else 3) * BYTES_PER_FLOAT
    val texUvBytesOffset = 3 * BYTES_PER_FLOAT

    // initialize
    init {
        // TODO both buffers length - from provided size not from buffered array size!
        vertexBufferId = bindBuffer(data.vertexes.toBuffer())
        indexBufferId = if(indexCount == 0) 0 else bindBuffer(data.indexes.toBuffer())
    }

    // after we init{} the indexCount is defined
    val hasIndexes = indexCount > 0

    private fun bindBuffer(buffer: Buffer): Int {
        val isVertexBuffer = when (buffer) {
            is FloatBuffer -> true
            is ShortBuffer -> false
            else -> throw IllegalArgumentException("Buffer must be FloatBuffer or ShortBuffer")
        }
        val bufferId = if(isVertexBuffer) GL_ARRAY_BUFFER else GL_ELEMENT_ARRAY_BUFFER
        val bufferIdArray = IntArray(1)
        glGenBuffers(1, bufferIdArray, 0)

        //if(bufferIdArray[0] == 0) throw

        // Bind to the buffer. Future commands will affect this buffer specifically.
        glBindBuffer(bufferId, bufferIdArray[0])
        // Transfer data from client memory to the buffer.
        // We can release the client memory after this call.
        glBufferData(bufferId, buffer.capacity() * (if(isVertexBuffer) BYTES_PER_FLOAT else BYTES_PER_SHORT), buffer, GL_STATIC_DRAW)

        // IMPORTANT: Unbind from the buffer when we're done with it.
        glBindBuffer(bufferId, 0)
        return bufferIdArray[0]
    }

    fun release() {
        if (vertexBufferId != 0) {
            glDeleteBuffers(1, intArrayOf(vertexBufferId), 0)
        }
        if (indexBufferId != 0) {
            glDeleteBuffers(1, intArrayOf(indexBufferId), 0)
        }
    }
}