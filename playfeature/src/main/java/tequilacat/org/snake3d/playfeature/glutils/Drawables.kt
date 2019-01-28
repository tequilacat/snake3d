package tequilacat.org.snake3d.playfeature.glutils

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
// TODO check whether needed for GLES20?
import android.opengl.GLES30.GL_TEXTURE_WRAP_R
import android.opengl.GLUtils

import android.util.Log

// new classes for VBOs

fun checkGlErr() {
    val err = glGetError()
    if (err != GL_NO_ERROR) {
        Log.e("render","OGL Error: $err")
    }
}

fun loadSkybox(context: Context, vararg faceIds: Int): Int {
    glActiveTexture(GL_TEXTURE0);
    //checkGlErr()
//    glEnable(GL_TEXTURE_CUBE_MAP);
//    checkGlErr()

    val textureHandle = IntArray(1)
    glGenTextures(1, textureHandle, 0)
    glBindTexture(GL_TEXTURE_CUBE_MAP, textureHandle[0]);

    for ((i, faceId) in faceIds.withIndex()) {
        val bitmap = BitmapFactory.decodeResource(context.resources, faceId, BitmapFactory.Options()
            .apply { inScaled = false })
        GLUtils.texImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, bitmap, 0)
//        glTexImage2D(
//                GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
//                0, GL_RGB, bitmap.width, bitmap.height, 0, GL_RGB, GL_UNSIGNED_BYTE, bitmap
//            )

        bitmap.recycle()
    }

    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)


    // added from CommonMistakes
//    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_BASE_LEVEL, 0)
//    glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAX_LEVEL, 0)

    checkGlErr()

    // TODO do we need glBindTexture(GL_TEXTURE_CUBE_MAP, 0) here ?
    return textureHandle[0]
}

fun loadTexture(context:Context, resourceId: Int): Int {
    val textureHandle = IntArray(1)
    glGenTextures(1, textureHandle, 0)

    // if (textureHandle[0] == 0) throw RuntimeException("Error generating texture name.")

    val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, BitmapFactory.Options()
        .apply { inScaled = false })

    // Bind to the texture in OpenGL
    glBindTexture(GL_TEXTURE_2D, textureHandle[0])
    // Set filtering
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    // Load the bitmap into the bound texture.
    GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
    // Recycle the bitmap, since its data has been loaded into OpenGL.
    bitmap.recycle()
    return textureHandle[0]
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

class ObjectContext(val primaryColor: FloatArray, val textureId: Int)



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

    private fun loadShader(type: Int, shaderCode: String) = glCreateShader(type)
        .also { shader ->
            // add the source code to the shader and compile it
            glShaderSource(shader, shaderCode)
            glCompileShader(shader)

            val compileStatus = IntArray(1)
            glGetShaderiv(shader, GL_COMPILE_STATUS, compileStatus, 0)

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.e("render", "error compiling shader #$shader: ${glGetShaderInfoLog(shader)}")
                glDeleteShader(shader)
            }

        }

    val id: Int = glCreateProgram().also {
        glAttachShader(it, loadShader(GL_VERTEX_SHADER, vertexShader))
        glAttachShader(it, loadShader(GL_FRAGMENT_SHADER, fragmentShader))
        glLinkProgram(it)

        for (i in attNames.indices) {
            glBindAttribLocation(it, i, attNames[i])
        }
    }
}
