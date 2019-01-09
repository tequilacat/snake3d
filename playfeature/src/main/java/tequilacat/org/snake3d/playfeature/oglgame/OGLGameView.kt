package tequilacat.org.snake3d.playfeature.oglgame

import android.graphics.Color
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import tequilacat.org.snake3d.playfeature.Game
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * stores opengl data from the game
 */
class GameRenderer() : GLSurfaceView.Renderer  {

    // override Renderer

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        controlState = Game.GameControlImpulse.NONE
        initOnSurfaceCreated()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        initViewOnSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val stt = System.nanoTime()
        draw()
        val time = (System.nanoTime() - stt)/ 1_000_000.0
        // Log.d("render", "onDrawFrame: $time ms")
    }

    // Game related code
    private var controlState: Game.GameControlImpulse = Game.GameControlImpulse.NONE
    private val game = Game()


    private val gameObjects = mutableListOf<AbstractOGLGameObject>()

    // lateinit var defaultProgram: DefaultProgram
    private lateinit var drawContext: DrawContext

    private fun initOnSurfaceCreated() {
        drawContext = DrawContext(DefaultProgram())
        // "Skybox" color
        glClearColor(0.2f, 0.2f, 0.2f, 1f)
        glEnable(GL_DEPTH_TEST)
//            glCullFace(GL_BACK)
//            glEnable(GL_CULL_FACE)

        createLevel()
        updateControls(null, false)
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

        game.fieldObjects.forEach {
            gameObjects.add(
                GORectPrism(
                    it.centerX.toFloat(), it.centerY.toFloat(), it.radius.toFloat(),
                    4, floorZ, bodyUnit * 2,
                    if (it.type == Game.GameObject.Type.OBSTACLE) 0xff0000 else 0x00ff00
                ).apply {
                    if(it.type == Game.GameObject.Type.PICKABLE) {
                        this.gameObject = it
                    }
                }
            )
        }

        // (re)create all data from static non-changed during movement
        // TODO create walls
        // TODO create field floor
        // TODO create obstacles
        // TODO create lightings
    }

    private fun updateConsumables() {
        val gameObjSet = game.fieldObjects.toSet()
        gameObjects.removeAll(
            gameObjects.filter { it.gameObject != null && !gameObjSet.contains(it.gameObject!!) })
    }

    private val projectionMatrix = FloatArray(16)
    private val reusedViewMatrix = FloatArray(16)
    private val reusedMvpMatrix = FloatArray(16)

    /**
     * notifies renderer that user pressed or dragged finger horizontally,
     * -1f to 1f, where 1f is screen width dragged from original touch.
     * Null is passed when user has released pointer.
     * left is negative.
     */
    fun updateControls(horizontalDrag: Float?, startGesture: Boolean) {
        controlState = when {
            horizontalDrag == null || abs(horizontalDrag) < 0.1 ->
                Game.GameControlImpulse.NONE
            horizontalDrag < 0.1 ->
                Game.GameControlImpulse.LEFT
            else ->
                Game.GameControlImpulse.RIGHT
        }
    }

    private fun adjustViewAngle() {
        val eyeH = bodyUnit() * 3

        val cx = game.headX
        val cy = game.headY
        val angle = game.headAngle

        Matrix.setLookAtM(reusedViewMatrix, 0,
            cx, cy, eyeH,
            cx + cos(angle).toFloat(), cy + sin(angle).toFloat(), eyeH,
            0f, 0.0f, 1.0f)
    }

    private fun initViewOnSurfaceChanged(width: Int, height: Int) {
        glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 0.3f, 200f)
    }

    fun draw() {
        // tick
        val tickResult = game.tick(controlState)
//        Log.d("render", "Tick result: $tickResult")

        when(tickResult) {
            Game.TickResult.CONSUME -> updateConsumables()
            Game.TickResult.INITGAME -> createLevel()
            else -> {}
        }

        adjustViewAngle()
        // draws OGL scene
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(reusedMvpMatrix, 0, projectionMatrix, 0, reusedViewMatrix, 0)

        gameObjects.forEach { if(it is Drawable) { it.draw(reusedMvpMatrix, drawContext) }}
    }
}
