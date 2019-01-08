package tequilacat.org.snake3d.playfeature.oglgame

import android.graphics.Color
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.util.Log
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import android.opengl.GLES20.GL_COMPILE_STATUS
import tequilacat.org.snake3d.playfeature.Game
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
class OGLGameScene(private val game: Game) {

    interface IResourceHolder {
        fun freeResources()
    }

    private val gameObjects = mutableListOf<AbstractOGLGameObject>()

    private var initialized = false

    // lateinit var defaultProgram: DefaultProgram
    private lateinit var drawContext: DrawContext

    fun initInstance() {
        if (!initialized) {
            initialized = true
            drawContext = DrawContext(DefaultProgram())

            // "Skybox" color
            glClearColor(0.2f, 0.2f, 0.2f, 1f)

            glEnable(GL_DEPTH_TEST)
//            glCullFace(GL_BACK)
//            glEnable(GL_CULL_FACE)
        }

        createLevel()
    }

    private fun freeResources() {
        gameObjects.forEach { if(it is IResourceHolder) it.freeResources() }
    }

    private fun bodyUnit() = (Game.R_HEAD * 2f).toFloat()

    /**
     * creates opengl objects for current game level
     * sets initial camera
     */
    private fun createLevel() {
        freeResources()
        gameObjects.clear()


        val fW = game.fieldWidth
        val fH = game.fieldHeight
        val bodyUnit =  bodyUnit() // height of protagonist

        val floorBuilder = TriangleBuilder()
        val tileSpace = bodyUnit * 3
        val tileSide = tileSpace * 0.95f
        val floorZ = 0f
        var x = 0f

        while (x < fW) {
            var y = 0f

            while (y < fH) {
                floorBuilder.addQuad(
                    x, y, floorZ, // BL
                    x + tileSide, y, floorZ, // BR
                    x + tileSide, y + tileSide, floorZ, // TR
                    x, y + tileSide, floorZ
                )
                y += tileSpace
            }

            x += tileSpace
        }

        gameObjects.add(GOTriangles(floorBuilder.buildVertexBuffer(),
            floorBuilder.buildIndexBuffer(), Color.BLUE.toColorArray()))

        gameObjects.add(GORectPrism(5f, fH/2, 2f, 12, 0f, 1f, Color.CYAN))
        gameObjects.add(GORectPrism(9.5f, fH/2 + 0.5f, 2f, 6, 0f, 1f, Color.RED))

        // (re)create all data from static non-changed during movement
        // TODO create walls
        // TODO create field floor
        // TODO create obstacles
        // TODO create lightings
    }

    private val projectionMatrix = FloatArray(16)
    private val reusedViewMatrix = FloatArray(16)
    private val reusedMvpMatrix = FloatArray(16)

    private fun adjustViewAngle() {
        val eyeH = bodyUnit() * 3
        val fh = game.fieldHeight

        // Set the camera position (View matrix)
        Matrix.setLookAtM(reusedViewMatrix, 0,
//            0f, 0f, eyeH,
            0f, fh/2, eyeH,
            1f, fh/2, eyeH,
            0f, 0.0f, 1.0f);
    }

    fun initView(width: Int, height: Int) {
        glViewport(0, 0, width, height);

        val ratio = width.toFloat() / height
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 0.3f, 200f)
    }

    fun draw() {
        adjustViewAngle()
        // draws OGL scene
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(reusedMvpMatrix, 0, projectionMatrix, 0, reusedViewMatrix, 0);

        gameObjects.forEach { if(it is Drawable) { it.draw(reusedMvpMatrix, drawContext) }}
    }
}
