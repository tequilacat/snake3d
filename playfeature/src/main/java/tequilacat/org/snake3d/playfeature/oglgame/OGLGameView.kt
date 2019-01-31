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
import kotlin.math.abs

/**
 * stores opengl data from the game
 */
class GameRenderer(private val context: Context) : GLSurfaceView.Renderer  {

    // override Renderer

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        controlState = Game.GameControlImpulse.NONE
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
        Log.d("render", "MS Since last drawGameFrame: ${curMs - lastDrawMs}, drawn within ${curMs - preDrawMs}")
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

    /** vysota v holke - 2 head radiuses */
    private fun bodyUnit() = (Game.R_HEAD * 2f).toFloat()

    // positioned according to currently used skybox images
    private val LIGHT_POSITION = floatArrayOf(0f, 100f, 20f, 1f)

    // defines FOV
    private val nearPlaneDist = 1.2f
    // how far behind the head the eye is
    private val eyeRearDistance = bodyUnit() * 5

    private val debugScene: DebugScene? = null


    private var controlState: Game.GameControlImpulse = Game.GameControlImpulse.NONE
    private val game = Game()

    /** All things to be drawn are stored here */
    private val gameObjects = mutableListOf<AbstractGameObject>()

    private var drawContext = SceneContext().apply {
        // matrix of light position in world
        LIGHT_POSITION.copyInto(lightPosGlobal, 0, 0, 4)
    }


    class BodyShapeObject(painter: GeometryPainter) :
        AbstractDrawableGameObject(painter, ObjectContext(ObjColors.BODY.rgb, -1)) {

        private val bodyShape = BodyShape(6, Game.R_HEAD.toFloat())

        override lateinit var geometry: Geometry

        fun update(segments: Collection<IBodySegment>) {
            bodyShape.update(segments)
            geometry = Geometry(bodyShape.geometry)
        }
    }

    // compute all geometry once as number arrays, recreate OGL data on each surfaceCreated

    private val pickableGeometryData: GeometryData = PrimitiveBuilder.makePrism(
        0f, 0f, 0f,
        bodyUnit() * 2f, Game.GameObject.Type.PICKABLE.radius.toFloat(), 12, true, true)

    private val obstacleGeometryData: GeometryData = PrimitiveBuilder.makePrism(0f, 0f, 0f,
        bodyUnit() * 1.3f, Game.GameObject.Type.OBSTACLE.radius.toFloat(), 12, true, true)

    private var headGeometryData: GeometryData = PrimitiveBuilder.makePrism(0f, 0f, 0f,
        bodyUnit(), Game.R_HEAD.toFloat(), 6, true, true)

    private lateinit var obstacleGeometry: Geometry
    private lateinit var pickableGeometry: Geometry
    private lateinit var headGeometry: Geometry
    private lateinit var floorGeometry: Geometry

    private lateinit var phongPainter: GeometryPainter
    private lateinit var guraudPainter: GeometryPainter
    private lateinit var texturePainter: TexturePainter
    private lateinit var skyboxPainter: SkyboxProgramPainter

    private lateinit var bodyShapeObject: BodyShapeObject

    private var obstacleTextureId: Int = 0
    private var pickableTextureId: Int = 0
    private var floorTileTextureId: Int = 0

    private fun initOnSurfaceCreated() {
        game.running = true

        //headTextureId = loadTexture(context, R.drawable.takyr_floor)
        obstacleTextureId = loadTexture(context, R.raw.cokecan_graphics)
        pickableTextureId = loadTexture(context, R.raw.guinnes)
        floorTileTextureId = loadTexture(context, R.raw.oldtiles)

        // no need to recreate
        phongPainter = ShadedPainter(SemiPhongProgram(context))
        guraudPainter = ShadedPainter(GuraudLightProgram(context))
        texturePainter = TexturePainter(TextureProgram(context))
        skyboxPainter = SkyboxProgramPainter(context)

        // build VBO buffers for head and obstacles- in onSurfaceCreated old buffers should be freed
        // TODO check in docs whether VBOs are really freed on restart
        obstacleGeometry = Geometry(obstacleGeometryData)
        headGeometry = Geometry(headGeometryData)
        pickableGeometry = Geometry(pickableGeometryData)

        glClearColor(ObjColors.SKY.rgb[0], ObjColors.SKY.rgb[1], ObjColors.SKY.rgb[2], 1f)
        glEnable(GL_DEPTH_TEST)

        glCullFace(GL_BACK)
        glEnable(GL_CULL_FACE)

        createLevel()
        updateControls(null,null, false)
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
        if (::floorGeometry.isInitialized) {
            floorGeometry.release()
        }

        floorGeometry = Geometry(makeFloor(game.fieldWidth, game.fieldHeight, bodyUnit() * 3, true))
        gameObjects.add(
            DrawableGameObject(
                floorGeometry,
                texturePainter,
                ObjColors.FLOOR.rgb,
                textureId = floorTileTextureId
            )
        )


        // add game objects
        gameObjects.addAll(game.fieldObjects.map {
            DrawableGameObject(
                if (it.type == Game.GameObject.Type.OBSTACLE) obstacleGeometry else pickableGeometry,
                texturePainter,
                if (it.type == Game.GameObject.Type.OBSTACLE) ObjColors.OBSTACLE.rgb else ObjColors.PICKABLE.rgb,
                gameObject = it,
                textureId = if (it.type == Game.GameObject.Type.OBSTACLE) obstacleTextureId else pickableTextureId
            ).apply { position(it.centerX.toFloat(), it.centerY.toFloat(), 0f, 0f) }
        })

        headObj = DrawableGameObject(headGeometry, phongPainter, ObjColors.BODY.rgb)
        // gameObjects.add(headObj!!) // don't show the head

        bodyShapeObject = BodyShapeObject(guraudPainter)
        updateBody()
        gameObjects.add(bodyShapeObject)
    }

    private fun updateBody() {
        if (debugScene != null) {
            bodyShapeObject.update(debugScene.bodySegments)
        } else {
            bodyShapeObject.update(game.bodySegments)
        }
    }

    // TODO remove this temp head object and replace with full body
    private var headObj: AbstractGameObject? = null

    private fun updateConsumables() {
        val gameObjSet = game.fieldObjects.toSet()
        gameObjects.retainAll {  it.gameObject == null || gameObjSet.contains(it.gameObject) }
    }

    /**
     * notifies renderer that user pressed or dragged finger horizontally,
     * -1f to 1f, where 1f is screen width dragged from original touch.
     * Null is passed when user has released pointer.
     * left is negative.
     */
    fun updateControls(relativeXPos: Float?, horizontalDrag: Float?, startGesture: Boolean) {
        controlState = when {
            horizontalDrag == null || abs(horizontalDrag) < 0.1 ->
                Game.GameControlImpulse.NONE
            horizontalDrag < 0.1 ->
                Game.GameControlImpulse.LEFT
            else ->
                Game.GameControlImpulse.RIGHT
        }

//        if (relativeXPos != null) {
//            if (relativeXPos < 0.3) {
//                nearPlaneDist /= 1.1f
//            } else if (relativeXPos > 0.7) {
//                nearPlaneDist *= 1.1f
//            }
//            Log.d("render", "Change near plane dist to $nearPlaneDist")
//        }
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

            val eyeH = bodyUnit() * 3
            val head = game.head

            val cx: Float = head.endX - head.alphaCosinus * eyeRearDistance
            val cy: Float = head.endY - head.alphaSinus * eyeRearDistance

            Matrix.setLookAtM(
                drawContext.viewMatrix, 0,
                cx, cy, eyeH,
                cx + head.alphaCosinus, cy + head.alphaSinus, eyeH,
                0f, 0.0f, 1.0f
            )

            headObj!!.position(head.endX, head.endY, 0f, head.alpha)
        }

        // compute light in eye pos
        Matrix.multiplyMV(drawContext.lightPosInEyeSpace, 0, drawContext.viewMatrix, 0, drawContext.lightPosGlobal, 0)
    }

    /**
     * adjusts to screen dimension ratio
     */
    private fun initViewOnSurfaceChanged(width: Int, height: Int) {
        glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(drawContext.projectionMatrix, 0, -ratio, ratio, -1f, 1f, nearPlaneDist, 200f)
    }

    private fun drawGameFrame() {
        // tick
        val tickResult = if(debugScene == null) game.tick(controlState) else Game.TickResult.NONE
//        Log.d("render", "Tick result: $tickResult")

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

        gameObjects.forEach { it.draw(drawContext) }

        skyboxPainter.paint(drawContext)
    }

}

