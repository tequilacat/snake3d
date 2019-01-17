package tequilacat.org.snake3d.playfeature.oglgame

import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import tequilacat.org.snake3d.playfeature.BodySegment
import tequilacat.org.snake3d.playfeature.Game
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
class GameRenderer() : GLSurfaceView.Renderer  {

    // override Renderer

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        controlState = Game.GameControlImpulse.NONE
        initOnSurfaceCreated()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        initViewOnSurfaceChanged(width, height)
    }

    var lastDrawMs: Long = 0

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

    class GameDrawableObject(val drawable: Drawable, val gameObject: Game.GameObject? = null)

    /** All things to be drawn are stored here */
    private val gameObjects = mutableListOf<GameDrawableObject>()

    private val bodyObject = BodyShape()

    private lateinit var defaultProgram: DefaultProgram
    private lateinit var phongLightProgram: SemiPhongProgram
    private lateinit var guraudLightProgram: GuraudLightProgram

    private var drawContext = DrawContext().apply {
        // matrix of light position in world
        LIGHT_POSITION.copyInto(lightPosGlobal, 0, 0, 4)
    }

    private fun initOnSurfaceCreated() {
        game.running = true

        defaultProgram = DefaultProgram()
        phongLightProgram = SemiPhongProgram()
        guraudLightProgram = GuraudLightProgram()

        glClearColor(ObjColors.SKY.rgb[0], ObjColors.SKY.rgb[1], ObjColors.SKY.rgb[2], 1f)
        glEnable(GL_DEPTH_TEST)

//            glCullFace(GL_BACK)
//            glEnable(GL_CULL_FACE)

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

        // build floor tiles as single geometry
        val fW = game.fieldWidth
        val fH = game.fieldHeight
        val bodyUnit =  bodyUnit() // height of protagonist

        val floorBuilder = GeometryBuilder()
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

        gameObjects.add(GameDrawableObject(ShadedGeometryPainter(
            floorBuilder.toArrays().run{ explode(first, second)},
            ObjColors.FLOOR.rgb, guraudLightProgram)))

        val objHeight = bodyUnit * 1.5f
        val obstacleGeom = GeometryBuilder.makePrism(0f, 0f, floorZ, objHeight,
            Game.GameObject.Type.OBSTACLE.radius.toFloat(), 8, true)
            .run { explode(first, second) }
        val pickableGeom = GeometryBuilder.makePrism(0f, 0f, floorZ, objHeight,
            Game.GameObject.Type.PICKABLE.radius.toFloat(), 8, true)
            .run { explode(first, second) }
        val headGeom = GeometryBuilder.makePrism(0f, 0f, floorZ, bodyUnit,
            Game.R_HEAD.toFloat(), 8, true)
            .run { explode(first, second) }

        gameObjects.addAll(game.fieldObjects.map {
            GameDrawableObject(
                ShadedGeometryPainter(
                    (if (it.type == Game.GameObject.Type.OBSTACLE) obstacleGeom else pickableGeom),
                    if (it.type == Game.GameObject.Type.OBSTACLE) ObjColors.OBSTACLE.rgb else ObjColors.PICKABLE.rgb,
                    phongLightProgram
                ).apply { position(it.centerX.toFloat(), it.centerY.toFloat(), 0f, 0f) }, it
            )
        })

        headObj = GameDrawableObject(
            ShadedGeometryPainter(
                headGeom, ObjColors.BODY.rgb, phongLightProgram
            ).apply { position(game.headX, game.headY, 0f, 0f) }
        )
        gameObjects.add(headObj!!)

        bodyObject.update(game)
        // gameObjects.add(bodyObject)
    }

    private fun explode(vertexes:FloatArray, indexes:ShortArray) : TriangleGeometryWithNormals {
        return GeometryBuilder.makeFacettedGeometry(vertexes, indexes).run {
            TriangleGeometryWithNormals(first.toBuffer(), ShortArray(0).toBuffer(), second.toBuffer())
        }
    }

    // TODO remove this temp head object and replace with full body
    var headObj: GameDrawableObject? = null

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

        headObj!!.drawable.position(game.headX, game.headY, 0f, game.headAngle)
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
                bodyObject.update(game) }
            Game.TickResult.MOVE -> bodyObject.update(game)
            Game.TickResult.INITGAME -> createLevel()
            Game.TickResult.NONE -> {}
        }

        adjustViewAngle()
        // draws OGL scene
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(drawContext.viewProjectionMatrix, 0, drawContext.projectionMatrix, 0, drawContext.viewMatrix, 0)

        gameObjects.forEach { it.drawable.draw(drawContext) }
    }
}



class BodyShape : Drawable {
    override fun position(x: Float, y: Float, z: Float, rotateAngle: Float) {
    }

    override fun draw(drawContext: DrawContext) {
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
