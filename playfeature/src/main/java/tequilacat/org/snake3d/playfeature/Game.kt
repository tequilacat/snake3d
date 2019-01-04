package tequilacat.org.snake3d.playfeature

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * stores all logic and data of game field
 */
class Game {
    // state - as enum
    var dbgStatus: String = "none"

    /// level data
    data class Level(val fieldWidth: Double, val fieldHeight: Double, val pickableCount: Int, val obstacleCount: Int)
    private val levels = listOf(Level(100.0, 100.0, 20, 5))
    private var currentLevelIndex = 0
    private var currentLevel: Level = levels[currentLevelIndex]

    val fieldWidth get() = currentLevel.fieldWidth.toFloat()
    val fieldHeight get() = currentLevel.fieldHeight.toFloat()

    // objects on field
    private val fieldObjects = mutableListOf<GameObject>()
    private val bodySegments = LinkedList<BodySegment>()

    private var lastInteractionTimeNs: Long = 0
    private var lastRoll = 0.0

    /**
     * length to add while moving head - if positive we add head but don't cut tail
     */
    var pendingLength: Double = 0.0

    companion object {
        // Assume SI: meters, seconds

        const val R_HEAD = 1.0
        const val R_OBSTACLE = 1.0
        const val R_PICKABLE = 1.0
        const val OBJCENTER_MIN_DISTANCE = 4.0 // object centers cannot be closer than this

        const val MARGIN = R_HEAD * 4 // margin along sizes not seeded by objects

        const val SPEED_M_NS = 10.0 / 1_000_000_000 // 10 m/s

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
            var dx = centerX - x
            var dy = centerY - y
            var sqDistance = dx * dx + dy * dy
            var twoRadiuses = radius + otherRadius
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
            fieldObjects.clear()
        }

        bodySegments.clear()
        bodySegments.addFirst(BodySegment(MARGIN / 2, MARGIN / 2, 0.0, R_HEAD * 4))

        // just set initial timestamp
        continueGame()
    }


    /**
     * seeds field with initial
     */
    private fun seed() {
        fieldObjects.clear()
//        fieldObjects.addAll(listOf(
//            GameObject(GameObject.Type.PICKABLE, 50.0, 50.0),
//            GameObject(GameObject.Type.PICKABLE, 50.0, 20.0),
//            GameObject(GameObject.Type.PICKABLE, 50.0, 30.0),
//            GameObject(GameObject.Type.OBSTACLE, 30.0, 20.0),
//            GameObject(GameObject.Type.OBSTACLE, 40.0, 20.0)
//        ))

        for (pair in listOf(
            Pair(GameObject.Type.PICKABLE, currentLevel.pickableCount),
            Pair(GameObject.Type.OBSTACLE, currentLevel.obstacleCount))) {

            for (i in 0 until pair.second) {
                var tries = 20 // if cannot find a free cell for 20 tries we fail miserably
                var objX: Double
                var objY: Double
                var objPlaced: Boolean

                do {
                    objX = Random.nextDouble(MARGIN, fieldWidth - MARGIN)
                    objY = Random.nextDouble(MARGIN, fieldHeight - MARGIN)
                    tries--
                    objPlaced = !fieldObjects.any() { it.isColliding(objX, objY, pair.first.radius) }
                } while (!objPlaced && tries > 0)

                if(!objPlaced) {
                    throw IllegalArgumentException("Cannot randomly place that much game objects")
                }

                fieldObjects.add(GameObject(pair.first, objX, objY))
            }
        }
    }

    /**
     * sets initial timestamp
     */
    fun continueGame() {
        lastInteractionTimeNs = System.nanoTime()
    }

    fun Float.f(digits: Int) = java.lang.String.format("%.${digits}f", this)

    /// updates state controlled by device position
    fun updatePositionalControls(azimuth: Float, pitch: Float, roll: Float) {
//        dbgStatus = "${azimuth.f(3)} / ${pitch.f(3)} / ${roll.f(3)}"
        lastRoll = roll.toDouble()
    }

    private fun getEffectiveRotateAngle() = when {
        (lastRoll > TILT_THRESHOLD) -> -ROTATE_ANGLE_PER_LEN
        (lastRoll < -TILT_THRESHOLD) -> ROTATE_ANGLE_PER_LEN
        else -> 0.0
    }

    val fillPainter = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
    }

    fun drawGameObject(c:Canvas, gameObject: GameObject, ratio: Double) {
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
    fun drawGameField(c: Canvas, viewWidth: Int, viewHeight: Int) {

    }

    fun drawGameScreen(c: Canvas, viewWidth: Int, viewHeight: Int) {
        c.drawColor(0xff3affbd.toInt())

        // fit width to screen: pix to logic
        val ratio: Float = (viewWidth / fieldWidth)

        c.drawRect(0f, 0f, (fieldWidth * ratio), (fieldHeight * ratio), Paints.linePaint)

        for (obj in fieldObjects) {
            drawGameObject(c, obj, ratio.toDouble())
        }

        for (segment in bodySegments) {
            val x0 = (segment.startX * ratio).toFloat()
            val y0 = (segment.startY * ratio).toFloat()
            val x1 = (segment.endX * ratio).toFloat()
            val y1 = (segment.endY * ratio).toFloat()
            c.drawLine(x0, y0, x1, y1, Paints.bodyPaint)
            c.drawCircle(x0, y0, (R_HEAD / 2 * ratio).toFloat(), Paints.bodyPaint)
        }

        val head = bodySegments.last
        val headX = head.endX * ratio
        val headY = head.endY * ratio
        // draw head with direction as
        c.drawCircle(headX.toFloat(), headY.toFloat(), (Game.R_HEAD * ratio).toFloat(), Paints.headPaint)
        c.drawLine(
            headX.toFloat(), headY.toFloat(), (headX + Game.R_HEAD * ratio * cos(head.angle)).toFloat(),
            (headY + Game.R_HEAD * ratio * sin(head.angle)).toFloat(), Paints.linePaint
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

        // draw circle on distance from center
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

    fun tick() {
        val curNanoTime = System.nanoTime()
        // check movement since last analysis
        val step = SPEED_M_NS * (curNanoTime - lastInteractionTimeNs)
        // check angle delta
        val deltaAngle = getEffectiveRotateAngle()

        processBody(step, deltaAngle*step)
        dbgStatus = "${bodySegments.size}"

        val last = bodySegments.last

        if (last.endX < 0 || last.endX >= fieldWidth || last.endY < 0 || last.endY >= fieldHeight) {
            init()
        } else {
            var collidingObj = fieldObjects.firstOrNull() { it.isColliding(last.endX, last.endY, R_HEAD) }

            if (collidingObj?.type == GameObject.Type.OBSTACLE) {
                init()
            } else if (collidingObj?.type == GameObject.Type.PICKABLE) {
                fieldObjects.remove(collidingObj)
                pendingLength += collidingObj.radius * 2
            }
        }


        lastInteractionTimeNs = System.nanoTime()
    }

    // moves a body along the
    fun processBody(step: Double, deltaAngle: Double) {

        // have at least one - extend or add it, then subtract from tail
        if (deltaAngle == 0.0) {
            bodySegments.last.extend(step)
        } else {
            bodySegments.addLast(BodySegment(bodySegments.last.endX, bodySegments.last.endY,
                bodySegments.last.angle + deltaAngle, step))
        }

        pendingLength -= step

        // no more pending length and some cut must be done from the tail
        if (pendingLength < 0) {
            pendingLength = -pendingLength

            while(bodySegments.first.length <= pendingLength) {
                pendingLength -= bodySegments.first.length
                bodySegments.removeFirst()
            }

            if(pendingLength > 0) {
                // partial remove of first element
                bodySegments.first.cutTail(pendingLength)
            }

            pendingLength = 0.0
        }
    }

}

class BodySegment(var startX: Double, var startY: Double, val angle: Double, var length: Double) {
    private var sinus = sin(angle)
    private var cosinus = cos(angle)

    var endX: Double = 0.0
    var endY: Double = 0.0

    init {
        computeEnd()
    }

    private fun computeEnd() {
        endX = startX + length * cosinus
        endY = startY + length * sinus
    }

    fun extend(step: Double) {
        length += step
        computeEnd()
    }

    fun cutTail(minusLength: Double) {
        length -= minusLength
        startX += minusLength * cosinus
        startY += minusLength * sinus
    }
}
