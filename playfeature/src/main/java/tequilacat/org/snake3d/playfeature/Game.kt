package tequilacat.org.snake3d.playfeature

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * stores all logic and data of game field
 */
class Game(private val addObstacles: Boolean = true) {
    // state - as enum
    var dbgStatus: String = "none"

    /// level data
    data class Level(val fieldWidth: Double, val fieldHeight: Double, val pickableCount: Int, val obstacleCount: Int)
    private val levels = listOf(Level(100.0, 100.0, 20, 5))
    private var currentLevelIndex = 0
    private var currentLevel: Level = levels[currentLevelIndex]

    val fieldWidth get() = currentLevel.fieldWidth.toFloat()
    val fieldHeight get() = currentLevel.fieldHeight.toFloat()

    val fieldObjects get() = fieldObjectList as Collection<GameObject>

    // objects on field
    private val fieldObjectList = mutableListOf<GameObject>()
    private val bodySegmentsList = LinkedList<BodySegment>()

    val bodySegments get() = bodySegmentsList as Collection<BodySegment>

    //private var lastInteractionTimeNs: Long = 0

    private var lastInteraction: Long = -1L

    var running: Boolean
        get() = lastInteraction >= 0
        set(value) {
            lastInteraction = if (value) SystemClock.uptimeMillis() else -1
        }

    private var lastRoll = 0.0

    /**
     * length to add while moving head - if positive we add head but don't cut tail
     */
    private var pendingLength: Double = 0.0

    companion object {
        // Assume SI: meters, seconds

        const val R_HEAD = 1.0
        const val R_OBSTACLE = 1.0
        const val R_PICKABLE = 1.0
        const val OBJCENTER_MIN_DISTANCE = 4.0 // object centers cannot be closer than this

        const val MARGIN = R_HEAD * 4 // margin along sizes not seeded by objects

        const val SPEED_M_NS = 10.0 / 1_000 // 10 m/s

        // 10 degrees per head diameter
        const val ROTATE_ANGLE_PER_LEN = (Math.PI / 5) / (R_HEAD * 2)

        // device rotation within +- threshold does not cause turns
        const val TILT_THRESHOLD = 5.0

    }


    class GameObject(val type: Type, val centerX: Double, val centerY: Double) {
        enum class Type(val radius: Double){
            OBSTACLE(R_OBSTACLE), PICKABLE(R_PICKABLE)
        }

        val radius: Double get() = type.radius

        private val boundingBox = RectF(
            (centerX - radius).toFloat(),
            (centerY - radius).toFloat(),
            (centerX + radius).toFloat(),
            (centerY + radius).toFloat()
        )

        // for performace first check rect overlapping
        fun isColliding(x: Double, y: Double, otherRadius: Double) = boundingBox.intersects(
            (x - otherRadius).toFloat(),
            (y - otherRadius).toFloat(),
            (x + otherRadius).toFloat(),
            (y + otherRadius).toFloat() )
                && run {
            val dx = centerX - x
            val dy = centerY - y
            val sqDistance = dx * dx + dy * dy
            val twoRadiuses = radius + otherRadius
            sqDistance < twoRadiuses * twoRadiuses
        }
    }

    init {
        init()
    }

    fun init() {
        // fills field with objs and stores initial time.
        // continueGame always at 0x0 + head radius + safe margin for rounding errors, and looking to center

        try {
            seed()
        } catch (e: Exception) {
            fieldObjectList.clear()
        }

        bodySegmentsList.clear()
        bodySegmentsList.addFirst(BodySegment(MARGIN / 2, MARGIN / 2, R_HEAD, 0.0, R_HEAD * 4))

        // just set initial timestamp
        //continueGame()
    }


    /**
     * seeds field with initial
     */
    private fun seed() {
        fieldObjectList.clear()
//        fieldObjectList.addAll(listOf(
//            GameObject(GameObject.Type.PICKABLE, 50.0, 50.0),
//            GameObject(GameObject.Type.PICKABLE, 50.0, 20.0),
//            GameObject(GameObject.Type.PICKABLE, 50.0, 30.0),
//            GameObject(GameObject.Type.OBSTACLE, 30.0, 20.0),
//            GameObject(GameObject.Type.OBSTACLE, 40.0, 20.0)
//        ))

        if(addObstacles) {
            for (pair in listOf(
                Pair(GameObject.Type.PICKABLE, currentLevel.pickableCount),
                Pair(GameObject.Type.OBSTACLE, currentLevel.obstacleCount)
            )) {

                for (i in 0 until pair.second) {
                    var tries = 20 // if cannot find a free cell for 20 tries we fail miserably
                    var objX: Double
                    var objY: Double
                    var objPlaced: Boolean

                    do {
                        objX = Random.nextDouble(MARGIN, fieldWidth - MARGIN)
                        objY = Random.nextDouble(MARGIN, fieldHeight - MARGIN)
                        tries--
                        objPlaced = !fieldObjectList.any() { it.isColliding(objX, objY, pair.first.radius) }
                    } while (!objPlaced && tries > 0)

                    if (!objPlaced) {
                        throw IllegalArgumentException("Cannot randomly place that much game objects")
                    }

                    fieldObjectList.add(GameObject(pair.first, objX, objY))
                }
            }
        }
    }

    /**
     * sets initial timestamp
     */
//    fun continueGame() {
//        lastInteractionTimeNs = System.nanoTime()
//    }

    fun Float.f(digits: Int) = java.lang.String.format("%.${digits}f", this)

    /// updates state controlled by device position
    fun updatePositionalControls(azimuth: Float, pitch: Float, roll: Float) {
//        dbgStatus = "${azimuth.f(3)} / ${pitch.f(3)} / ${roll.f(3)}"
        lastRoll = roll.toDouble()
    }

    private fun getEffectiveRotateAngle(gameControlImpulse: GameControlImpulse? = null) = when {
        gameControlImpulse == GameControlImpulse.LEFT -> ROTATE_ANGLE_PER_LEN
        gameControlImpulse == GameControlImpulse.RIGHT -> -ROTATE_ANGLE_PER_LEN
        gameControlImpulse == GameControlImpulse.NONE -> 0.0
        // these called when null is provided (2d controls)
        (lastRoll > TILT_THRESHOLD) -> -ROTATE_ANGLE_PER_LEN
        (lastRoll < -TILT_THRESHOLD) -> ROTATE_ANGLE_PER_LEN
        else -> 0.0
    }

    private val fillPainter = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
    }

    private fun drawGameObject(c:Canvas, gameObject: GameObject, ratio: Double) {
        fillPainter.color = when(gameObject.type) {
            GameObject.Type.OBSTACLE -> 0xffff0000.toInt()
            GameObject.Type.PICKABLE -> 0xff0000ff.toInt()
        }

        c.drawCircle(
            (gameObject.centerX * ratio).toFloat(), (gameObject.centerY * ratio).toFloat(),
            (gameObject.radius * ratio).toFloat(), fillPainter)
    }

    object Paints {
        val headPaint = Paint().apply {
            color = 0xffff0000.toInt()
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = 0xff0000ff.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3.0f
            isAntiAlias = true
            textSize = 40f
        }

        val bodyPaint = Paint().apply {
            color = 0xff000000.toInt()
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
    }

    /**
     * draws onto canvas in specified rect, with specified ratio
     * centered on head
     */
    private fun drawGameField(c: Canvas, viewWidth: Int, viewHeight: Int) {

    }

    val head: IBodySegment get() = bodySegmentsList.last

    fun drawGameScreen(c: Canvas, viewWidth: Int, viewHeight: Int) {
        c.drawColor(0xff3affbd.toInt())

        val R_HEAD = Game.R_HEAD.toFloat()
        // fit width to screen: pix to logic
        val ratio: Float = (viewWidth / fieldWidth)

        c.drawRect(0f, 0f, (fieldWidth * ratio), (fieldHeight * ratio), Paints.linePaint)

        for (obj in fieldObjectList) {
            drawGameObject(c, obj, ratio.toDouble())
        }

        for (segment in bodySegmentsList) {
            val x0 = (segment.startX * ratio)
            val y0 = (segment.startY * ratio)
            val x1 = (segment.endX * ratio)
            val y1 = (segment.endY * ratio)
            c.drawLine(x0, y0, x1, y1, Paints.bodyPaint)
            c.drawCircle(x0, y0, R_HEAD / 2 * ratio, Paints.bodyPaint)
        }

        val headX = head.endX * ratio
        val headY = head.endY * ratio
        // drawGameFrame head with direction as
        c.drawCircle(headX, headY, R_HEAD * ratio, Paints.headPaint)
        c.drawLine(
            headX, headY, (headX + R_HEAD * ratio * head.alphaCosinus),
            (headY + R_HEAD * ratio * head.alphaSinus), Paints.linePaint
        )

        c.drawText(dbgStatus, 0f, fieldHeight * ratio + Paints.linePaint.textSize, Paints.linePaint)

        // rotation ruler
        // along screen bottom
        val rcHeight = viewWidth / 20
        val rcSegmentWidth = viewWidth / 3f
        val deltaAngle = getEffectiveRotateAngle()
        val segColorActive: Int = 0xff0000ff.toInt()
        val segColorInactive: Int = 0xff00ccff.toInt()

        val rcRect = RectF(0f, (viewHeight - rcHeight).toFloat(), rcSegmentWidth, viewHeight.toFloat())
        c.drawRect(rcRect, fillPainter.apply { color = if (deltaAngle < 0) segColorActive else segColorInactive })

        rcRect.offset(rcRect.width() * 2, 0f)
        c.drawRect(rcRect, fillPainter.apply { color = if (deltaAngle > 0) segColorActive else segColorInactive })

        // drawGameFrame circle on distance from center
        val rotateBallRadius = rcHeight * 0.35f
        var rotateBallX = (viewWidth / 2 - lastRoll / TILT_THRESHOLD * rcSegmentWidth / 2).toFloat()

        if(rotateBallX < rotateBallRadius){
            rotateBallX = rotateBallRadius
        } else if (rotateBallX > viewWidth - rotateBallRadius) {
            rotateBallX = viewWidth - rotateBallRadius
        }

        c.drawCircle(rotateBallX,
            (viewHeight - rcHeight / 2).toFloat(), rotateBallRadius, fillPainter.apply { color = 0xff000000.toInt() })
    }


    /**
     *  makes one step forward,
     *  computing the next position and checking all obstacles/pickables on the way to the new location
     */

    enum class TickResult {
        NONE,
        MOVE,
        CONSUME,
        INITGAME
    }

    // no faster than 50 FPS - 20 ms
    val MIN_STEP_MS: Long = 20 // milliseconds

    enum class GameControlImpulse {
        LEFT, RIGHT, NONE
    }

    fun tick(gameControlImpulse: GameControlImpulse): TickResult {
        val tickResult: TickResult

        if(!running) return TickResult.NONE

        val curMsTime = SystemClock.uptimeMillis()
        if (curMsTime - lastInteraction < MIN_STEP_MS) return TickResult.NONE

        // Log.d("render", "${(curNanoTime - lastInteractionTimeNs) / 1_000_000f} ms")

        // check movement since last analysis
        val step = SPEED_M_NS * (curMsTime - lastInteraction)
        // check angle delta
        val deltaAngle = getEffectiveRotateAngle(gameControlImpulse)

        processBody(step, deltaAngle * step)
        dbgStatus = "${bodySegmentsList.size}"

        val last = bodySegmentsList.last

        if (last.dblEndX < 0 || last.dblEndX >= fieldWidth || last.dblEndY < 0 || last.dblEndY >= fieldHeight) {
            init()
            tickResult = TickResult.INITGAME

        } else {
            val collidingObj = fieldObjectList.firstOrNull { it.isColliding(last.dblEndX, last.dblEndY, R_HEAD) }

            when(collidingObj?.type) {
                GameObject.Type.OBSTACLE -> {
                    init()
                    tickResult = TickResult.INITGAME
                }
                GameObject.Type.PICKABLE -> {
                    fieldObjectList.remove(collidingObj)
                    pendingLength += collidingObj.radius * 2
                    tickResult = TickResult.CONSUME
                }
                else -> tickResult = TickResult.MOVE
            }
        }

        lastInteraction = SystemClock.uptimeMillis()
        return tickResult
    }

    // moves a body along the
    private fun processBody(step: Double, deltaAngle: Double) {

        // have at least one - extend or add it, then subtract from tail
        if (deltaAngle == 0.0) {
            bodySegmentsList.last.extend(step)
        } else {
            bodySegmentsList.addLast(BodySegment(
                bodySegmentsList.last.dblEndX, bodySegmentsList.last.dblEndY, bodySegmentsList.last.dblEndZ,
                bodySegmentsList.last.angle + deltaAngle, step))
        }

        pendingLength -= step

        // no more pending length and some cut must be done from the tail
        if (pendingLength < 0) {
            pendingLength = -pendingLength

            while(bodySegmentsList.first.dblLength <= pendingLength) {
                pendingLength -= bodySegmentsList.first.dblLength
                bodySegmentsList.removeFirst()
            }

            if(pendingLength > 0) {
                // partial remove of first element
                bodySegmentsList.first.cutTail(pendingLength)
            }

            pendingLength = 0.0
        }
    }

}

interface IBodySegment {
    val startX: Float
    val startY: Float
    val startZ: Float

    val endX: Float
    val endY: Float
    val endZ: Float

    val length: Float

    val alpha: Float
    val alphaSinus: Float
    val alphaCosinus: Float

    val beta: Float
    val betaSinus: Float
    val betaCosinus: Float
}

/**
 * appends a segment to the list
 */
fun MutableList<IBodySegment>.append(
    angle: Double,
    length: Double,
    angleRelative: Boolean = true
): MutableList<IBodySegment> {

    val last = this.last()
    this.add(
        BodySegment(
            last.endX.toDouble(), last.endY.toDouble(), last.endZ.toDouble(),
            angle + if(angleRelative) last.alpha else 0f, length
        )
    )
    return this
}

class BodySegment(var dblStartX: Double, var dblStartY: Double, var dblStartZ: Double, val angle: Double, var dblLength: Double) : IBodySegment {

//    constructor(dblStartX: Double, dblStartY: Double, angle: Double, dblLength: Double) :
//            this(dblStartX, dblStartY, 0.0, angle, dblLength)
    val angleSinus = sin(angle)
    val angleCosinus = cos(angle)

    var dblEndX: Double = 0.0
    var dblEndY: Double = 0.0
    var dblEndZ: Double = dblStartZ // so far parallel to the ground

    override val startX: Float get() = dblStartX.toFloat()
    override val startY: Float get() = dblStartY.toFloat()
    override val startZ: Float get() = dblStartZ.toFloat()
    override val endX: Float get() = dblEndX.toFloat()
    override val endY: Float get() = dblEndY.toFloat()
    override val endZ: Float get() = dblEndZ.toFloat()

    override val length: Float get() = dblLength.toFloat()

    override val alpha = angle.toFloat()
    override val alphaSinus = angleSinus.toFloat()
    override val alphaCosinus = angleCosinus.toFloat()

    // for 3d spacing - use default for flat location
    override val beta = 0f
    override val betaSinus = sin(beta)
    override val betaCosinus= cos(beta)



    init {
        computeEnd()
    }

    private fun computeEnd() {
        dblEndX = dblStartX + dblLength * angleCosinus
        dblEndY = dblStartY + dblLength * angleSinus
    }

    fun extend(step: Double) {
        dblLength += step
        computeEnd()
    }

    fun cutTail(minusLength: Double) {
        dblLength -= minusLength
        dblStartX += minusLength * angleCosinus
        dblStartY += minusLength * angleSinus
    }
}
