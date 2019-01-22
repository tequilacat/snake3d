package tequilacat.org.snake3d.playfeature.glutils

import android.opengl.GLES20.*
import android.opengl.Matrix
import android.util.Log
import java.lang.IllegalArgumentException
import java.nio.Buffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer


// new classes for VBOs

data class GeometryData(val vertexes: FloatArray, val hasNormals: Boolean, val indexes: ShortArray = Static.noindexes) {
    object Static {
        val noindexes = ShortArray(0)
    }
}

const val BYTES_PER_SHORT = 2
const val BYTES_PER_FLOAT = 4

class Geometry(data: GeometryData) {

    // convert vertexes and indexes into
    val vertexBufferId: Int
    val indexBufferId: Int

    // TODO consider dynamic buffers
    val indexCount: Int
    val vertexCount: Int
    val hasNormals = data.hasNormals

    /**
     * if no normals in vertex array use 0 as tightly packed
     * otherwise use 24 bytes (3floats * 4bytesperfloat * 2 )
     * */
    val vertexStride = if(hasNormals) 24 else 0

    val coordinatesPerVertex = 3

    // initialize
    init {
        vertexCount = data.vertexes.size / coordinatesPerVertex // 3 per
        vertexBufferId = bindBuffer(data.vertexes.toBuffer())
        indexCount = data.indexes.size
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


fun checkGlErr() {
    val err = glGetError()
    if (err != GL_NO_ERROR) {
        Log.e("render","OGL Error: $err")
    }
}

class SceneContext {
    /** screen dimension and frustum */
    val projectionMatrix = FloatArray(16)

    /** view according to eye position */
    val viewMatrix = FloatArray(16)

    /** projection view matrix to screen also according to eye position*/
    val viewProjectionMatrix = FloatArray(16)

    /** Location of light source in scene*/
    val lightPosGlobal = FloatArray(4)

    val lightPosInEyeSpace = FloatArray(4)
}

interface Drawable {
    fun position(x: Float, y: Float, z: Float, rotateAngle: Float)
    fun draw(sceneContext: SceneContext)
}

interface GeometryPainter {
    fun paint(geometry: Geometry, objectContext: ObjectContext, modelMatrix: FloatArray, sceneContext: SceneContext)
}

class ObjectContext(val primaryColor: FloatArray)

class ShadedPainter(private val program: LightingProgram) : GeometryPainter {
    private val mMVMatrix = FloatArray(16)
    private val mMVPMatrix = FloatArray(16)

    override fun paint(geometry: Geometry, objectContext: ObjectContext, modelMatrix: FloatArray, sceneContext: SceneContext) {
        // current impl does not paint indexes!

        glUseProgram(program.id)

        //////////////////////////////
        // prepare data

        // set uniform color
        glUniform4fv(program.attColorHandle, 1, objectContext.primaryColor, 0)
        glUniform3f(
            program.attLightPosHandle, sceneContext.lightPosInEyeSpace[0],
            sceneContext.lightPosInEyeSpace[1], sceneContext.lightPosInEyeSpace[2]
        )

        // uniforms

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVMatrix, 0, sceneContext.viewMatrix, 0, modelMatrix, 0)

        // Pass in the modelview matrix.
        glUniformMatrix4fv(program.attMVMatrixHandle, 1, false, mMVMatrix, 0)

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, sceneContext.projectionMatrix, 0, mMVMatrix, 0)

        // Pass in the combined matrix.
        glUniformMatrix4fv(program.attMVPMatrixHandle, 1, false, mMVPMatrix, 0)


        ///////////////////////////
        // draw VBOs


        glBindBuffer(GL_ARRAY_BUFFER, geometry.vertexBufferId)

        // Bind Attributes
        glVertexAttribPointer(program.attPositionHandle, geometry.coordinatesPerVertex, GL_FLOAT,
            false, geometry.vertexStride, 0)
        glEnableVertexAttribArray(program.attPositionHandle)

        glVertexAttribPointer(program.attNormalHandle, geometry.coordinatesPerVertex, GL_FLOAT,
            false, geometry.vertexStride, geometry.coordinatesPerVertex * BYTES_PER_FLOAT)
        glEnableVertexAttribArray(program.attNormalHandle)

        // Draw
        if (geometry.hasIndexes) {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, geometry.indexBufferId)
            glDrawElements(GL_TRIANGLE_STRIP, geometry.indexCount, GL_UNSIGNED_SHORT, 0)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        } else {
            glDrawArrays(GL_TRIANGLES, 0, geometry.vertexCount)
        }

        glDisableVertexAttribArray(program.attPositionHandle)
        glDisableVertexAttribArray(program.attNormalHandle)
        checkGlErr()

        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }
}