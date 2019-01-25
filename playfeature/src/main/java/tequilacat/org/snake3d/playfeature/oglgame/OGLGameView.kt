package tequilacat.org.snake3d.playfeature.oglgame

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import tequilacat.org.snake3d.playfeature.BodySegment
import tequilacat.org.snake3d.playfeature.Game
import tequilacat.org.snake3d.playfeature.R
import tequilacat.org.snake3d.playfeature.glutils.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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
        draw()
        val curMs = SystemClock.uptimeMillis()
        //val time = (System.nanoTime() - stt)/ 1_000_000.0
        Log.d("render", "MS Since last draw: ${curMs - lastDrawMs}, drawn within ${curMs - preDrawMs}")
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

    private val LIGHT_POSITION = floatArrayOf(0f, 0f, 20f, 1f)

    // defines FOV
    private val nearPlaneDist = 1.2f
    // how far behind the head the eye is
    private val eyeRearDistance = bodyUnit() * 5



    private var controlState: Game.GameControlImpulse = Game.GameControlImpulse.NONE
    private val game = Game()


    // allow for different drawing approaches
    abstract class AbstractGameObject(val gameObject: Game.GameObject?) : Drawable

    // TODO template class for gameObject, move to Drawables
    class DrawableGameObject(primaryColor: FloatArray, private val geometry: Geometry,
                             private val geometryPainter: GeometryPainter,
                             textureId: Int = -1,
                             gameObject: Game.GameObject? = null) : AbstractGameObject(gameObject) {
        private val objectContext = ObjectContext(primaryColor, textureId)

        private val modelMatrix = FloatArray(16).also {
            Matrix.setIdentityM(it, 0)
        }

        override fun position(x: Float, y: Float, z: Float, rotateAngle: Float) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, y, z)
            Matrix.rotateM(modelMatrix, 0, (rotateAngle * 180 / PI).toFloat(), 0.0f, 0.0f, 1.0f)
        }

        override fun draw(sceneContext: SceneContext) {
            geometryPainter.paint(geometry, objectContext, modelMatrix, sceneContext)
        }
    }

    /** All things to be drawn are stored here */
    private val gameObjects = mutableListOf<AbstractGameObject>()

    private var drawContext = SceneContext().apply {
        // matrix of light position in world
        LIGHT_POSITION.copyInto(lightPosGlobal, 0, 0, 4)
    }

    private val bodyObject = BodyShape()

    // compute all geometry once as number arrays, recreate OGL data on each surfaceCreated

    private val pickableGeometryData: GeometryData = GeometryBuilder(textures = true).makePrism(0f, 0f, 0f,
        bodyUnit() * 2f, Game.GameObject.Type.PICKABLE.radius.toFloat(), 12, true)

    private val obstacleGeometryData: GeometryData = GeometryBuilder(textures = true).makePrism(0f, 0f, 0f,
        bodyUnit() * 1.3f, Game.GameObject.Type.OBSTACLE.radius.toFloat(), 12, true)

    private var headGeometryData: GeometryData = GeometryBuilder(textures = true).makePrism(0f, 0f, 0f,
        bodyUnit(), Game.R_HEAD.toFloat(), 6, true)

    private lateinit var obstacleGeometry: Geometry
    private lateinit var pickableGeometry: Geometry
    private lateinit var headGeometry: Geometry
    private lateinit var floorGeometry: Geometry

    private lateinit var phongPainter: GeometryPainter
    private lateinit var guraudPainter: GeometryPainter
    private lateinit var texturePainter: TexturePainter
    private var obstacleTextureId: Int = 0
    private var pickableTextureId: Int = 0

    private fun initOnSurfaceCreated() {
        game.running = true

        //headTextureId = loadTexture(context, R.drawable.takyr_floor)
        obstacleTextureId = loadTexture(context, R.raw.cokecan_graphics)
        pickableTextureId = loadTexture(context, R.raw.guinnes)

        // no need to recreate
        phongPainter = ShadedPainter(SemiPhongProgram(context))
        guraudPainter = ShadedPainter(GuraudLightProgram(context))
        texturePainter = TexturePainter(TextureProgram(context))

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

    private fun makeFloor(fW: Float, fH: Float): GeometryData {
        val floorBuilder = GeometryBuilder()
        val tileSpace = bodyUnit() * 3
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

        return floorBuilder.build()
    }

    /**
     * creates opengl objects for current game level
     * sets initial camera
     */
    private fun createLevel() {
        freeResources()
        gameObjects.clear()

        // create dynamically depending on game size (current level), not static data
        if(::floorGeometry.isInitialized) {
            floorGeometry.release()
        }

        floorGeometry = Geometry(makeFloor(game.fieldWidth, game.fieldHeight))
        gameObjects.add(DrawableGameObject(ObjColors.FLOOR.rgb, floorGeometry, guraudPainter))

        // add game objects
        gameObjects.addAll(game.fieldObjects.map {
            DrawableGameObject(
                if (it.type == Game.GameObject.Type.OBSTACLE) ObjColors.OBSTACLE.rgb else ObjColors.PICKABLE.rgb,
                if (it.type == Game.GameObject.Type.OBSTACLE) obstacleGeometry else pickableGeometry,
                texturePainter, gameObject = it,
                textureId = if (it.type == Game.GameObject.Type.OBSTACLE) obstacleTextureId else pickableTextureId
            ).apply { position(it.centerX.toFloat(), it.centerY.toFloat(), 0f, 0f) }})

        headObj = DrawableGameObject(ObjColors.BODY.rgb, headGeometry,
            phongPainter)
        gameObjects.add(headObj!!)

        updateBody()
        // gameObjects.add(bodyObject)
    }

    private fun updateBody() {
        bodyObject.update(game)
    }

    // TODO remove this temp head object and replace with full body
    var headObj: AbstractGameObject? = null

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
        val eyeH = bodyUnit() * 3

        val cx: Float = (game.headX - game.headCosinus * eyeRearDistance)
        val cy: Float = (game.headY - game.headSinus * eyeRearDistance)

        Matrix.setLookAtM(drawContext.viewMatrix, 0,
            cx, cy, eyeH,
            cx + game.headCosinus, cy + game.headSinus, eyeH,
            0f, 0.0f, 1.0f)

        // compute light in eye pos
        Matrix.multiplyMV(drawContext.lightPosInEyeSpace, 0, drawContext.viewMatrix, 0, drawContext.lightPosGlobal, 0)

        headObj!!.position(game.headX, game.headY, 0f, game.headAngle)
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

    fun draw() {
        // tick
        val tickResult = game.tick(controlState)
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
    }

    /*
    fun loadAsset() {
        try {
        val json_string = application.assets.open(file_name).bufferedReader().use{
            it.readText()
        }

            val inputStream = assets.open("news_data_file.json")
            val inputString = inputStream.bufferedReader().use{it.readText()}
            Log.d("res",inputString)
        } catch (e:Exception){
            Log.d("res", e.toString())
        }
    }*/

    fun loadTexture(context: Context, resourceId: Int): Int {
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
}



class BodyShape : Drawable {
    override fun position(x: Float, y: Float, z: Float, rotateAngle: Float) {
    }

    override fun draw(sceneContext: SceneContext) {
    }

    private companion object {
        private const val BS_SIDE_COUNT = 6
        private val BS_ANGLES = Array(BS_SIDE_COUNT) {
            Pair(
                sin(PI * 2 / BS_SIDE_COUNT * it).toFloat(),
                cos(PI * 2 / BS_SIDE_COUNT * it).toFloat())
        }
    }

    // some safe guesses
    private var vertexes: FloatArray = FloatArray(1000)
    private var indexes: ShortArray = ShortArray(2000)

    private fun initSizes(segmentCount: Int) {
        val vertexCoordCount = BS_SIDE_COUNT * (segmentCount + 1) * 3

        if(vertexes.size < vertexCoordCount) {
            vertexes = FloatArray(vertexCoordCount * 2)
        }

        // 2 triangle each of 3 vert for each face of a segment
        val indexSize = segmentCount * BS_SIDE_COUNT * 2 * 3

        if(indexes.size < indexSize) {
            indexes = ShortArray(indexSize * 2)
        }
    }

    fun update(game: Game) {

    }

    /**
     * rebuilds geometry from current body
     */
    private fun updateInt(game: Game) {
        initSizes(game.bodySegments.size)

        val bodyRadius = Game.R_HEAD.toFloat()
        var vPos = 0
        // var dAlpla: Float = (PI * 2 / BS_SIDE_COUNT).toFloat()
        var curSegmentStartVertexIndex = 0
        var indexPos = 0 // pos in the vertex buffer

        // iterate and create segments
        val iter = game.bodySegments.iterator()

        while (iter.hasNext()) {
            val cx: Float
            val cy: Float
            val segment: BodySegment

            if (vPos == 0) {
                segment = game.bodySegments.first()
                cx = segment.startX.toFloat()
                cy = segment.startY.toFloat()
            } else {
                segment = iter.next()
                cx = segment.endX.toFloat()
                cy = segment.endY.toFloat()

                // for each segment add triangles between its start and end slice
                // vPos points to first pos of the (nonfilled) end slice of current segment
                for (i in 0 until BS_SIDE_COUNT) {
                    val p0 = curSegmentStartVertexIndex + i - 1
                    var p1 = p0 + 1
                    if (i == BS_SIDE_COUNT - 1) {
                        p1 -= BS_SIDE_COUNT
                    }
                    val np0 = p0 + BS_SIDE_COUNT
                    val np1 = p1 + BS_SIDE_COUNT
                    //if (i < BS_SIDE_COUNT - 1) i + 1 else 0
                    indexes[indexPos++] = p0.toShort()
                    indexes[indexPos++] = np0.toShort()
                    indexes[indexPos++] = np1.toShort()

                    indexes[indexPos++] = p0.toShort()
                    indexes[indexPos++] = np1.toShort()
                    indexes[indexPos++] = p1.toShort()

                }
                curSegmentStartVertexIndex += BS_SIDE_COUNT
            }

            // val angle = segment.angle.toFloat()

            for ((aSin, aCos) in BS_ANGLES) {
                // compute coord of every vertex of the segment
                val dx0 = bodyRadius * aCos
                // val dy0 = bodyRadius * aSin
                vertexes[vPos++] = (cx + dx0 * segment.angleSinus).toFloat()
                vertexes[vPos++] = (cy - dx0 * segment.angleCosinus).toFloat()
                vertexes[vPos++] = bodyRadius + bodyRadius * aSin
            }
        }
    }
}
