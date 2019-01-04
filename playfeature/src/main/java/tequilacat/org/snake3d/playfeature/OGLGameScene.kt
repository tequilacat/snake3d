package tequilacat.org.snake3d.playfeature


import android.graphics.Color
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import android.opengl.GLES20.GL_COMPILE_STATUS


fun Int.toColorArray() : FloatArray = floatArrayOf(
    ((this ushr 16) and 0xff) / 255f,
    ((this ushr 8) and 0xff) / 255f,
    (this and 0xff) / 255f,
    1f)

class OGLUtils {
    companion object {
        const val COORDS_PER_VERTEX = 3
        /** 4 = bytes per vertex */
        const val VERTEX_STRIDE = COORDS_PER_VERTEX * 4

        fun loadShader(type: Int, shaderCode: String) = glCreateShader(type)
            .also { shader ->
                // add the source code to the shader and compile it
                glShaderSource(shader, shaderCode)
                glCompileShader(shader)

                val compileStatus = IntArray(1)
                glGetShaderiv(shader, GL_COMPILE_STATUS, compileStatus, 0)

                // If the compilation failed, delete the shader.
                if (compileStatus[0] == 0) {
                    Log.e("render", "error compiling shader $shader")
                    // glDeleteShader(shader)
                }
            }

        // initialize vertex byte buffer for shape coordinates
        fun createVertexBuffer(floatArray: FloatArray): FloatBuffer =
        // (# of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(floatArray.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(floatArray)
                    position(0)
                }
            }

        // initialize byte buffer for the draw list
        fun createDrawListBuffer(shortArray: ShortArray): ShortBuffer =
        // (# of coordinate values * 2 bytes per short)
            ByteBuffer.allocateDirect(shortArray.size * 2).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    put(shortArray)
                    position(0)
                }
            }
    }
}

/**
 * stores opengl data from the game
 */
class OGLGameScene(val game: Game) {

    interface Drawable {
        fun draw(mvpMatrix: FloatArray)
    }

    interface IResourceHolder {
        fun freeResources()
    }

    abstract class AbstractOGLGameObject

    private val gameObjects = mutableListOf<AbstractOGLGameObject>()

    fun freeResources() {
        gameObjects.forEach { if(it is IResourceHolder) it.freeResources() }
    }

    /**
     * creates opengl objects for current game level
     * sets initial camera
     */
    fun createLevel() {
        freeResources()
        gameObjects.clear()

        val stdRect = floatArrayOf(
            -0.5f,  0.5f, 0.0f,      // top left
            -0.5f, -0.5f, 0.0f,      // bottom left
            0.5f, -0.5f, 0.0f,      // bottom right
            0.5f,  0.5f, 0.0f       // top right
        )
        var levelFloor = floatArrayOf(
            0f, game.fieldHeight, 0.0f,      // bottom left
            0f, 0f, 0.0f,      // top left
            game.fieldWidth, 0f, 0.0f,      // bottom right
            game.fieldWidth, game.fieldHeight, 0.0f       // top right
        )

        gameObjects.add(GORect(DefaultProgram(), stdRect, Color.MAGENTA.toColorArray()))


        glClearColor(0.2f, 0.2f, 0.2f, 1f)

        // (re)create all data from static non-changed during movement
        // TODO create walls
        // TODO create field floor
        // TODO create obstacles
        // TODO create lightings
    }

    private val projectionMatrix = FloatArray(16)
    private val reusedViewMatrix = FloatArray(16)
    private val reusedMvpMatrix = FloatArray(16)

    fun initView(width: Int, height: Int) {
        glViewport(0, 0, width, height);

        val ratio = width.toFloat() / height
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

    fun draw() {
        // draws OGL scene
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Set the camera position (View matrix)
        Matrix.setLookAtM(reusedViewMatrix, 0,
            0f, 0f, -3f,
            0f, 0f, 0f,
            0f, 1.0f, 0.0f);
        // Calculate the projection and view transformation
        Matrix.multiplyMM(reusedMvpMatrix, 0, projectionMatrix, 0, reusedViewMatrix, 0);

        gameObjects.forEach { if(it is Drawable) { it.draw(reusedMvpMatrix) }}
    }
}

open class OGLProgram(vertexShader: String, fragmentShader: String) {
    val id: Int = glCreateProgram().also {
        glAttachShader(it, OGLUtils.loadShader(GL_VERTEX_SHADER, vertexShader))
        glAttachShader(it, OGLUtils.loadShader(GL_FRAGMENT_SHADER, fragmentShader))
        glLinkProgram(it)
    }
}

class DefaultProgram(vertexShader: String, fragmentShader: String)
    : OGLProgram(vertexShader, fragmentShader) {
    constructor() : this(Const.DEF_VERTEX_SHADER_CODE, Const.DEF_FRAGMENT_SHADER_CODE) {}

    object Const {
        val DEF_VERTEX_SHADER_CODE = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """.trimIndent()

        val DEF_FRAGMENT_SHADER_CODE = """
            precision mediump float;
            uniform vec4 vColor;
            void main() {
              gl_FragColor = vColor;
            }
        """.trimIndent()
    }

    val positionHandle: Int by lazy { glGetAttribLocation(id, "vPosition") }

    val colorHandle: Int by lazy { glGetUniformLocation(id, "vColor") }

    val mvpMatrixHandle: Int by lazy { glGetUniformLocation(id, "uMVPMatrix") }
}

/*
var squareCoords = floatArrayOf(
    -0.5f,  0.5f, 0.0f,      // top left
    -0.5f, -0.5f, 0.0f,      // bottom left
    0.5f, -0.5f, 0.0f,      // bottom right
    0.5f,  0.5f, 0.0f       // top right
)
*/

/**
 * displays rect as 2 triangles
 */
class GORect(
    private val program: DefaultProgram,
    rectCoords: FloatArray,
    private val color: FloatArray
) : OGLGameScene.AbstractOGLGameObject(), OGLGameScene.Drawable {


    companion object {
        const val DRAW_ORDER_LEN = 6

        val drawListBuffer: ShortBuffer = OGLUtils.createDrawListBuffer(
            shortArrayOf(0, 1, 2, 0, 2, 3)).apply {
            Log.d("ogl", "create draw list for rect")
        }
    }

    // initialize vertex byte buffer for shape coordinates
    private val vertexBuffer: FloatBuffer = OGLUtils.createVertexBuffer(rectCoords)

    override fun draw(mvpMatrix: FloatArray) {
        glUseProgram(program.id)

        glEnableVertexAttribArray(program.positionHandle);
        glVertexAttribPointer(
            program.positionHandle, OGLUtils.COORDS_PER_VERTEX,
            GL_FLOAT, false,
            OGLUtils.VERTEX_STRIDE, vertexBuffer);


        glUniform4fv(program.colorHandle, 1, color, 0);
        glUniformMatrix4fv(program.mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the square
        glDrawElements(
            GL_TRIANGLES, DRAW_ORDER_LEN,
            GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        glDisableVertexAttribArray(program.positionHandle)
    }

}