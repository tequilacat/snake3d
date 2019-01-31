package tequilacat.org.snake3d.playfeature.glutils

import android.opengl.GLES20
import java.nio.Buffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * Geometry stored in VBOs, destroyed with OpenGL context
 * */
class GeometryBuffer(data: Geometry) {
    // convert vertexes and indexes into
    val vertexBufferId: Int
    val indexBufferId: Int

    val indexCount: Int = data.indexCount
    val vertexCount: Int = data.vertexCount
    val vertexByteStride = data.vertexFloatStride * BYTES_PER_FLOAT
    val coordinatesPerVertex = 3
    val floatsPerTexUV = 2

    val coordBytesOffset = 0
    val normalBytesOffset = (if (data.hasTexture) 5 else 3) * BYTES_PER_FLOAT
    val texUvBytesOffset = 3 * BYTES_PER_FLOAT

    // initialize
    init {
        vertexBufferId = bindBuffer(data.vertexes.toBuffer(data.vertexCount * data.vertexFloatStride))
        indexBufferId = if(indexCount == 0) 0 else bindBuffer(data.indexes.toBuffer(data.indexCount))
    }

    // after we init{} the indexCount is defined
    val hasIndexes = indexCount > 0

    private fun bindBuffer(buffer: Buffer): Int {
        val isVertexBuffer = when (buffer) {
            is FloatBuffer -> true
            is ShortBuffer -> false
            else -> throw IllegalArgumentException("Buffer must be FloatBuffer or ShortBuffer")
        }
        val bufferId = if(isVertexBuffer) GLES20.GL_ARRAY_BUFFER else GLES20.GL_ELEMENT_ARRAY_BUFFER
        val bufferIdArray = IntArray(1)
        GLES20.glGenBuffers(1, bufferIdArray, 0)

        //if(bufferIdArray[0] == 0) throw

        // Bind to the buffer. Future commands will affect this buffer specifically.
        GLES20.glBindBuffer(bufferId, bufferIdArray[0])
        // Transfer data from client memory to the buffer.
        // We can release the client memory after this call.
        GLES20.glBufferData(
            bufferId,
            buffer.capacity() * (if (isVertexBuffer) BYTES_PER_FLOAT else BYTES_PER_SHORT),
            buffer,
            GLES20.GL_STATIC_DRAW
        )

        // IMPORTANT: Unbind from the buffer when we're done with it.
        GLES20.glBindBuffer(bufferId, 0)
        return bufferIdArray[0]
    }

    fun release() {
        if (vertexBufferId != 0) {
            GLES20.glDeleteBuffers(1, intArrayOf(vertexBufferId), 0)
        }
        if (indexBufferId != 0) {
            GLES20.glDeleteBuffers(1, intArrayOf(indexBufferId), 0)
        }
    }
}