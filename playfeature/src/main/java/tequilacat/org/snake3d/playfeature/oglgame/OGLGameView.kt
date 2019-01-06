package tequilacat.org.snake3d.playfeature.oglgame

import android.graphics.Color
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import android.opengl.GLES20.GL_COMPILE_STATUS
import tequilacat.org.snake3d.playfeature.Game
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

    }
}

/**
 * stores opengl data from the game
 */
class OGLGameScene(val game: Game) {

    data class DrawContext(val program: DefaultProgram)

    interface Drawable {
        fun draw(mvpMatrix: FloatArray, drawContext: DrawContext)
    }

    interface IResourceHolder {
        fun freeResources()
    }

    abstract class AbstractOGLGameObject

    private val gameObjects = mutableListOf<AbstractOGLGameObject>()

    var initialized = false

    // lateinit var defaultProgram: DefaultProgram
    lateinit var drawContext: DrawContext

    fun initInstance() {
        if (!initialized) {
            initialized = true
            drawContext = OGLGameScene.DrawContext(DefaultProgram())
        }

        createLevel()
    }

    fun freeResources() {
        gameObjects.forEach { if(it is IResourceHolder) it.freeResources() }
    }

    /**
     * creates opengl objects for current game level
     * sets initial camera
     */
    private fun createLevel() {
        freeResources()
        gameObjects.clear()


        val fW = game.fieldWidth
        val fH = game.fieldHeight
        val wH = Game.R_HEAD.toFloat() * 3f

        var levelFloor = floatArrayOf(
            0f, fH, 0.0f,      // bottom left
            0f, 0f, 0.0f,      // top left
            fW, 0f, 0.0f,      // bottom right
            fW, fH, 0.0f       // top right
        )

        val eyeH = 3f

        val tileCount = 40
        val fieldSide = 100f
        val tileSpace = (fieldSide / tileCount) * 0.9f
        val floorZ = 0f

        for (xCount in 0 until tileCount) {
            val x = fieldSide * xCount / tileCount

            for (yCount in 0 until tileCount) {
                val y = fieldSide * yCount / tileCount

                gameObjects.add(GORect(floatArrayOf(
                    x, y, floorZ, // BL
                    x + tileSpace, y, floorZ, // BR
                    x + tileSpace, y + tileSpace, floorZ, // TR
                    x, y + tileSpace, floorZ), // TL
                    when {
                        (xCount == 0) -> Color.BLUE
                        (yCount == 0) -> Color.RED
                        else -> Color.WHITE
                    }.toColorArray()))
            }
        }


        gameObjects.add(GORectPrism(5f, fieldSide/2, 2f, 6, 0f, 1f, Color.CYAN))


        // Set the camera position (View matrix)
        Matrix.setLookAtM(reusedViewMatrix, 0,
//            0f, 0f, eyeH,
            0f, fieldSide/2, eyeH,
            1f, fieldSide/2, eyeH,
//            1f, 1f, eyeH,
//            h, h, eyeH,
            0f, 0.0f, 1.0f);

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
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 0.3f, 200f)
    }

    fun draw() {
        // draws OGL scene
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(reusedMvpMatrix, 0, projectionMatrix, 0, reusedViewMatrix, 0);

        gameObjects.forEach { if(it is Drawable) { it.draw(reusedMvpMatrix, drawContext) }}
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
    constructor() : this(DEF_VERTEX_SHADER_CODE, DEF_FRAGMENT_SHADER_CODE) {}

    companion object {
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

open class GOTriangles(private val vertexBuffer: FloatBuffer,
                  private val indexBuffer: ShortBuffer,
                  private val color: FloatArray) :
    OGLGameScene.AbstractOGLGameObject(),
    OGLGameScene.Drawable {

    override fun draw(mvpMatrix: FloatArray, drawContext: OGLGameScene.DrawContext) {
        glUseProgram(drawContext.program.id)

        glEnableVertexAttribArray(drawContext.program.positionHandle);
        glVertexAttribPointer(
            drawContext.program.positionHandle, OGLUtils.COORDS_PER_VERTEX,
            GL_FLOAT, false,
            OGLUtils.VERTEX_STRIDE, vertexBuffer);

        glUniform4fv(drawContext.program.colorHandle, 1, color, 0);
        glUniformMatrix4fv(drawContext.program.mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the square
        glDrawElements(
            GL_TRIANGLES, indexBuffer.capacity(),
            GL_UNSIGNED_SHORT, indexBuffer
        );

        // Disable vertex array
        glDisableVertexAttribArray(drawContext.program.positionHandle)
    }
}

/**
 * displays rect as 2 triangles
 */
class GORect(
    rectCoords: FloatArray,
    color: FloatArray
) : GOTriangles(rectCoords.toBuffer(), RECT_VERTEX_ORDER, color) {

    private companion object {
        val RECT_VERTEX_ORDER: ShortBuffer = shortArrayOf(0, 1, 2, 0, 2, 3).toBuffer()
    }
}

/**
 *
 */
class GORectPrism(cx: Float, cy: Float, radius: Float, sides: Int,
                  zBase: Float, height: Float, color: Int) :
    GOTriangles(
        composePrismVertexes(cx, cy, radius, sides, zBase, height).toBuffer(),
        composePrismIndexes(sides).toBuffer(), color.toColorArray()) {

    companion object {

        fun composePrismVertexes(
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

                val pt0 = side * 2

                alpha += deltaAlpha
            }

            return coords
        }


        /**
         * creates prism indexes for specified sides count,
         * assume prism along 0Z, vertexes follow around center counter-clockwise (increasing alpha)
         */
        fun composePrismIndexes(sides: Int) : ShortArray {
            val array = ShortArray(sides * 6)
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

            return array
        }

    }

}
