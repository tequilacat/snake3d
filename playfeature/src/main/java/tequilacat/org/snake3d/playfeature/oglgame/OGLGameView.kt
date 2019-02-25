package tequilacat.org.snake3d.playfeature.oglgame

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import tequilacat.org.snake3d.playfeature.*
import tequilacat.org.snake3d.playfeature.glutils.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * stores opengl data from the game
 */
class GameRenderer(private val context: Context) : GLSurfaceView.Renderer  {

    // override Renderer

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        initOnSurfaceCreated()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        initViewOnSurfaceChanged(width, height)
    }

    private var lastDrawMs: Long = 0

    override fun onDrawFrame(gl: GL10?) {
        val preDrawMs = SystemClock.uptimeMillis()
        drawGameFrame()
        val curMs = SystemClock.uptimeMillis()
        //val time = (System.nanoTime() - stt)/ 1_000_000.0
//        Log.d("render", "MS Since last drawGameFrame: ${curMs - lastDrawMs}, drawn within ${curMs - preDrawMs}")
        lastDrawMs = curMs
    }

    // Game related code

    // constants:

    enum class ObjColors(private val rgbInt: Int) {
        BODY(0x1e9634), FLOOR(0x9b693e),
        PICKABLE(0x3244ff), //0x8afc32),
        OBSTACLE(0xff6a2b), SKY(0xaacbff);

        val rgb by lazy { rgbInt.toColorArray() }
    }

    // positioned according to currently used skybox images
    private val LIGHT_POSITION = floatArrayOf(0f, 100f, 20f, 1f)

    // defines FOV
    private val nearPlaneDist = 1.2f
    // how far behind the head the eye is
    private val eyeRearDistance = Game.BODY_UNIT * 5

//    private val debugScene: DebugScene? = DebugScene()
    private val debugScene: DebugScene? = null


    //private var controlState: Game.GameControlImpulse = Game.GameControlImpulse.NONE
    private val game = Game(debugScene?.addObstacles ?: true)

    /** All things to be drawn are stored here */
    private val gameObjects = mutableListOf<AbstractGameObject>()

    private var drawContext = SceneContext().apply {
        // matrix of light position in world
        LIGHT_POSITION.copyInto(lightPosGlobal, 0, 0, 4)
    }


    class BodyShapeObject(painter: GeometryPainter, bodyTextureId: Int) :
        AbstractDrawableGameObject(painter, ObjectContext(ObjColors.BODY.rgb, bodyTextureId)) {

        // start texture V at bottom lowest point (3 * PI / 2)
        private val bodyShape = RotationalShapeBuilder(
            10,
            3 * PI.toFloat() / 2,
            0.2f, // for yellow brown
            //0.5f, // good for grayscale
            0f
        ) as IRotationalGeometryBuilder

        override lateinit var geometryBuffer: GeometryBuffer

        fun update(segments: Sequence<IDirectedSection>) {
            val time0 = SystemClock.uptimeMillis()
            bodyShape.update(segments)

            val time1 = SystemClock.uptimeMillis()


            // TODO replace contents of buffer with glBufferSubData to decrease GC
            if (::geometryBuffer.isInitialized) {
                geometryBuffer.release()
            }

            val time2 = SystemClock.uptimeMillis()

            val bodyGeometry = bodyShape.geometry // .facetize()

            val time3 = SystemClock.uptimeMillis()

            geometryBuffer = GeometryBuffer(bodyGeometry)

            val time4 = SystemClock.uptimeMillis()

            Log.d("render","body[segments unknown] update: ${time1 - time0} / ${time2 - time1} / ${time3 - time2} / ${time4 - time3}")
        }
    }

    // compute all geometry once as number arrays, recreate OGL data on each surfaceCreated

    private val pickableGeometry: Geometry = PrimitiveBuilder.makePrism(
        0f, 0f, 0f,
        Game.R_PICKABLE.toFloat() * 4, Game.R_PICKABLE.toFloat(), 12, true, true)

    private val obstacleGeometry: Geometry = PrimitiveBuilder.makePrism(0f, 0f, 0f,
        Game.R_OBSTACLE.toFloat() * 3, Game.R_OBSTACLE.toFloat(), 12, true, true)

    private lateinit var obstacleGeometryBuffer: GeometryBuffer
    private lateinit var pickableGeometryBuffer: GeometryBuffer
    private lateinit var floorGeometryBuffer: GeometryBuffer

    private lateinit var phongPainter: GeometryPainter
    private lateinit var guraudPainter: GeometryPainter
    private lateinit var texturePainter: TexturePainter
    private lateinit var skyboxPainter: SkyboxProgramPainter

    private lateinit var bodyShapeObject: BodyShapeObject

    private var obstacleTextureId: Int = 0
    private var pickableTextureId: Int = 0
    private var floorTileTextureId: Int = 0
    private var bodyTextureId: Int = 0

    private fun initOnSurfaceCreated() {
        game.running = true

        obstacleTextureId = loadTexture(context, R.raw.cokecan_graphics)
        pickableTextureId = loadTexture(context, R.raw.penguinness)
        floorTileTextureId = loadTexture(context, R.raw.oldtiles)
        bodyTextureId = loadTexture(context, R.raw.snake_yellowbrown)
//        bodyTextureId = loadTexture(context, R.raw.snake_grayscale_hires)

        // no need to recreate
        phongPainter = ShadedPainter(SemiPhongProgram(context))
        guraudPainter = ShadedPainter(GuraudLightProgram(context))
        texturePainter = TexturePainter(TextureProgram(context))
        skyboxPainter = SkyboxProgramPainter(context)

        // build VBO buffers for head and obstacles- in onSurfaceCreated old buffers should be freed
        obstacleGeometryBuffer = GeometryBuffer(obstacleGeometry)
        pickableGeometryBuffer = GeometryBuffer(pickableGeometry)

        glClearColor(ObjColors.SKY.rgb[0], ObjColors.SKY.rgb[1], ObjColors.SKY.rgb[2], 1f)
        glEnable(GL_DEPTH_TEST)

        glCullFace(GL_BACK)
        glEnable(GL_CULL_FACE)

        createLevel()
    }

    private fun freeResources() {
        // gameObjects.forEach { if(it is IResourceHolder) it.freeResources() }
    }

    /**
     * creates opengl objects for current game level
     * sets initial camera
     */
    private fun createLevel() {
        freeResources()
        gameObjects.clear()

        // create dynamically depending on game size (current level), not static data
        if (::floorGeometryBuffer.isInitialized) {
            floorGeometryBuffer.release()
        }

        floorGeometryBuffer = GeometryBuffer(makeFloor(game.scene.fieldWidth, game.scene.fieldHeight,
            Game.BODY_UNIT, Game.BODY_UNIT * 2))
        gameObjects.add(
            DrawableGameObject(
                floorGeometryBuffer,
                texturePainter,
                ObjColors.FLOOR.rgb,
                textureId = floorTileTextureId
            )
        )


        // add game objects
        gameObjects.addAll(game.scene.fieldObjects.map {
            DrawableGameObject(
                if (it.type == IFieldObject.Type.OBSTACLE) obstacleGeometryBuffer else pickableGeometryBuffer,
                texturePainter,
                if (it.type == IFieldObject.Type.OBSTACLE) ObjColors.OBSTACLE.rgb else ObjColors.PICKABLE.rgb,
                gameObject = it,
                textureId = if (it.type == IFieldObject.Type.OBSTACLE) obstacleTextureId else pickableTextureId
            ).apply { position(it.centerX, it.centerY, 0f, 0f) }
        })

        bodyShapeObject = BodyShapeObject(texturePainter, bodyTextureId)
        updateBody()
        gameObjects.add(bodyShapeObject)
    }

    private fun updateBody() {
        if (debugScene != null) {
            // bodyShapeObject.update(debugScene.bodySegments)
        } else {
            bodyShapeObject.update(game.scene.bodyModel.bodyAndHeadSections)
        }
    }

    private fun updateConsumables() {
        val gameObjSet = game.scene.fieldObjects.toSet()
        gameObjects.retainAll {  it.gameObject == null || gameObjSet.contains(it.gameObject) }
    }


    private fun adjustViewAngle() {
        if(debugScene != null) {
            Matrix.setLookAtM(
                drawContext.viewMatrix, 0,
                debugScene.cameraPosition[0], debugScene.cameraPosition[1], debugScene.cameraPosition[2],
                debugScene.cameraPosition[3], debugScene.cameraPosition[4], debugScene.cameraPosition[5],
                0f, 0.0f, 1.0f
            )
        } else {

            val viewSin = sin(game.scene.bodyModel.viewDirection)
            val viewCos = cos(game.scene.bodyModel.viewDirection)
            val eyeH = Game.BODY_UNIT * 3
            val cx: Float = game.scene.bodyModel.headX - viewCos * eyeRearDistance
            val cy: Float = game.scene.bodyModel.headY - viewSin * eyeRearDistance

            Matrix.setLookAtM(
                drawContext.viewMatrix, 0,
                cx, cy, eyeH,
                cx + viewCos, cy + viewSin, eyeH,
                0f, 0.0f, 1.0f
            )
        }

        // compute light in eye pos
        Matrix.multiplyMV(drawContext.lightPosInEyeSpace, 0, drawContext.viewMatrix, 0, drawContext.lightPosGlobal, 0)
    }

    /**
     * adjusts to screen dimension ratio
     */
    private fun initViewOnSurfaceChanged(width: Int, height: Int) {
        glViewport(0, 0, width, height)

        inputController.initialize(width, height)
        inputController.resetInput()

        val ratio = width.toFloat() / height
        Matrix.frustumM(drawContext.projectionMatrix, 0, -ratio, ratio, -1f, 1f, nearPlaneDist, 200f)
    }

    val inputController = InputController()

    private fun drawGameFrame() {
        val tickResult = if (debugScene == null) {
            game.tick(inputController.computeImpulse(game.scene.bodyModel.viewDirection))
        } else Game.TickResult.NONE

        when(tickResult) {
            Game.TickResult.CONSUME -> {
                updateConsumables()
                updateBody() }
            Game.TickResult.MOVE -> updateBody()
            Game.TickResult.INITGAME -> createLevel()
            Game.TickResult.NONE -> {}
        }

        adjustViewAngle()
        // draws OGL scene
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(drawContext.viewProjectionMatrix, 0, drawContext.projectionMatrix, 0, drawContext.viewMatrix, 0)

        if(debugScene != null) {
            debugScene.draw(drawContext, DebugScene.TestObjectContext(texturePainter, bodyTextureId))
        } else {
            gameObjects.forEach { it.draw(drawContext) }
        }

        skyboxPainter.paint(drawContext)
    }

}

