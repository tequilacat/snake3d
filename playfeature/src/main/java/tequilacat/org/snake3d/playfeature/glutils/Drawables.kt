package tequilacat.org.snake3d.playfeature.glutils

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES20.*

import android.util.Log
import java.lang.IllegalArgumentException
import java.nio.Buffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer


const val BYTES_PER_SHORT = 2
const val BYTES_PER_FLOAT = 4

// new classes for VBOs

/**
 * Geometry stored as float array, possibly with indexes
 */
data class GeometryData(val vertexes: FloatArray, val hasNormals: Boolean, val hasTexture: Boolean, val indexes: ShortArray = Static.noindexes) {
    object Static {
        val noindexes = ShortArray(0)
    }

    /** vertex stride in bytes */
    val vertexStride: Int
        get() {
            var floatStride = 3
            if (hasNormals) floatStride += 3
            if (hasTexture) floatStride += 2
            return floatStride * BYTES_PER_FLOAT
        }
}

/**
 * Geometry stored in VBOs
 * */
class Geometry(data: GeometryData) {
    // convert vertexes and indexes into
    val vertexBufferId: Int
    val indexBufferId: Int

    val indexCount: Int = data.indexes.size
    val vertexCount: Int = data.vertexes.size * BYTES_PER_FLOAT / data.vertexStride
    val vertexStride = data.vertexStride
    val coordinatesPerVertex = 3
    val floatsPerTexUV = 2

    val coordBytesOffset = 0
    val normalBytesOffset = (if (data.hasTexture) 5 else 3) * BYTES_PER_FLOAT
    val texUvBytesOffset = 3 * BYTES_PER_FLOAT

    // initialize
    init {
        vertexBufferId = bindBuffer(data.vertexes.toBuffer())
        indexCount
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

class ObjectContext(val primaryColor: FloatArray, val textureId: Int) {

}


open class OGLProgram(vertexShader: String, fragmentShader: String, vararg attNames: String) {
    constructor(context:Context, vertexShaderResId: Int, fragmentShaderResId: Int) :
            this(readResourceText(context, vertexShaderResId), readResourceText(context, fragmentShaderResId))

    enum class PAType {
        ATTRIBUTE, UNIFORM
    }

    data class PA(val name: String, val type: PAType, val program: OGLProgram) {
        val id by lazy {
            when (type) {
                PAType.ATTRIBUTE -> glGetAttribLocation(program.id, name)
                PAType.UNIFORM -> glGetUniformLocation(program.id, name)
            }
        }
    }

    /*
    val u = uniform("")
    val a = attr("")
     */

    protected fun attr(name: String) = PA(name, PAType.ATTRIBUTE, this)
    protected fun uniform(name: String) = PA(name, PAType.UNIFORM, this)

    companion object {
        fun readResourceText(context: Context, resourceId: Int): String {
            return context.resources.openRawResource(resourceId).use {
                    r -> r.readBytes().toString(Charsets.UTF_8)
            }
        }
    }

    private fun loadShader(type: Int, shaderCode: String) = GLES20.glCreateShader(type)
        .also { shader ->
            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.e("render", "error compiling shader #$shader: ${glGetShaderInfoLog(shader)}")
                glDeleteShader(shader)
            }

        }

    val id: Int = GLES20.glCreateProgram().also {
        GLES20.glAttachShader(it, loadShader(GLES20.GL_VERTEX_SHADER, vertexShader))
        GLES20.glAttachShader(it, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader))
        GLES20.glLinkProgram(it)

        for (i in attNames.indices) {
            glBindAttribLocation(it, i, attNames[i])
        }
    }
}
