package tequilacat.org.snake3d.playfeature

import android.graphics.RectF
import android.os.SystemClock
import tequilacat.org.snake3d.playfeature.GameGeometry.Companion.R_HEAD
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
    // TODO make Level class private
    data class Level(val fieldWidth: Double, val fieldHeight: Double, val pickableCount: Int, val obstacleCount: Int)
    private val levels = listOf(Level(100.0, 100.0, 20, 5))
    private var currentLevelIndex = 0

    private lateinit var sceneImpl: GameScene

    val scene: IGameScene get() { return sceneImpl }

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

//        const val R_HEAD = 1.0
//        const val R_OBSTACLE = 1.0
//        const val R_PICKABLE = 1.0
        const val OBJCENTER_MIN_DISTANCE = 4.0 // object centers cannot be closer than this

        const val FIELD_SAFEMARGIN = R_HEAD * 4 // margin along sizes not seeded by objects

        const val SPEED_M_NS = 10.0 / 1_000 // 10 m/s

        // 10 degrees per head diameter
        const val ROTATE_ANGLE_PER_LEN = (Math.PI / 5) / (R_HEAD * 2)

        // device rotation within +- threshold does not cause turns
        const val TILT_THRESHOLD = 5.0
    }

    private class GameObject(override val type: IFieldObject.Type, private val centerDblX: Double, private val centerDblY: Double) : IFieldObject {
        val radius = type.radius

        override val centerX = centerDblX.toFloat()
        override val centerY = centerDblY.toFloat()

        private val boundingBox = RectF(
            (centerDblX - radius).toFloat(),
            (centerDblY - radius).toFloat(),
            (centerDblX + radius).toFloat(),
            (centerDblY + radius).toFloat()
        )

        // for performace first check rect overlapping
        fun isColliding(x: Double, y: Double, otherRadius: Double) = boundingBox.intersects(
            (x - otherRadius).toFloat(),
            (y - otherRadius).toFloat(),
            (x + otherRadius).toFloat(),
            (y + otherRadius).toFloat() )
                && run {
            val dx = centerDblX - x
            val dy = centerDblY - y
            val sqDistance = dx * dx + dy * dy
            val twoRadiuses = radius + otherRadius
            sqDistance < twoRadiuses * twoRadiuses
        }
    }

    init {
        firstLevel()
    }

    // Just reload same level
    private fun nextLevel() = initLevel(0)
    private fun firstLevel() = initLevel(0)

    /**
     * creates a scene from current level
     */
    private fun initLevel(levelIndex: Int) {
        currentLevelIndex = levelIndex
        sceneImpl = GameScene(levels[levelIndex])

        if(!addObstacles) {
            sceneImpl.fieldObjectsImpl.clear()
        }
    }

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

    // val head: IBodySegment get() = bodySegmentsImpl.last


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
        val bodySegmentsImpl = sceneImpl.bodySegmentsImpl
        dbgStatus = "${bodySegmentsImpl.size}"

        val last = bodySegmentsImpl.last

        if (last.dblEndX < 0 || last.dblEndX >= scene.fieldWidth || last.dblEndY < 0 || last.dblEndY >= scene.fieldHeight) {
            firstLevel()
            tickResult = TickResult.INITGAME

        } else {
            val fieldObjectList = (scene as GameScene).fieldObjectsImpl
            val collidingObj = fieldObjectList.firstOrNull { it.isColliding(last.dblEndX, last.dblEndY, R_HEAD) }

            when(collidingObj?.type) {
                IFieldObject.Type.OBSTACLE -> {
                    firstLevel()
                    tickResult = TickResult.INITGAME
                }
                IFieldObject.Type.PICKABLE -> {
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
        val segments = sceneImpl.bodySegmentsImpl

        // have at least one - extend or add it, then subtract from tail
        if (deltaAngle == 0.0) {
            segments.last.extend(step)
        } else {
            with(segments.last) {
                segments.addLast(BodySegment(dblEndX, dblEndY, dblEndZ,angle + deltaAngle, step))
            }
        }

        pendingLength -= step

        // no more pending length and some cut must be done from the tail
        if (pendingLength < 0) {
            pendingLength = -pendingLength

            while(segments.first.dblLength <= pendingLength) {
                pendingLength -= segments.first.dblLength
                segments.removeFirst()
            }

            if(pendingLength > 0) {
                // partial remove of first element
                segments.first.cutTail(pendingLength)
            }

            pendingLength = 0.0
        }
    }





    private class GameScene(private val level: Level) : IGameScene {
        internal val bodySegmentsImpl = LinkedList<BodySegment>()
        override val bodySegments: Collection<BodySegment> = bodySegmentsImpl

        override val fieldWidth: Float = level.fieldWidth.toFloat()
        override val fieldHeight: Float = level.fieldHeight.toFloat()

        val fieldObjectsImpl = mutableListOf<GameObject>()
        override val fieldObjects = fieldObjectsImpl as Iterable<IFieldObject>

        init {
            loadLevel()
        }

        /**
         * seeds field with initial
         */
        private fun loadLevel() {
            // body
            bodySegmentsImpl.clear()
            //bodySegmentsImpl.addFirst(BodySegment(FIELD_SAFEMARGIN / 2, FIELD_SAFEMARGIN / 2, R_HEAD, 0.0, R_HEAD * 4))
            // TODO debug location of body
            bodySegmentsImpl.addFirst(BodySegment(level.fieldWidth / 2, level.fieldHeight / 2, R_HEAD, 0.0, R_HEAD * 4))

            // field objects

            fieldObjectsImpl.clear()
            // fieldObjectList.addAll(listOf(GameObject(GameObject.Type.PICKABLE, 50.0, 50.0)))

            val fieldWidth = level.fieldWidth
            val fieldHeight = level.fieldHeight

            for (pair in listOf(
                Pair(IFieldObject.Type.PICKABLE, level.pickableCount),
                Pair(IFieldObject.Type.OBSTACLE, level.obstacleCount)
            )) {
                val newObjRadius = pair.first.dblRadius

                for (i in 0 until pair.second) {
                    var tries = 20 // if cannot find a free cell for 20 tries we fail miserably
                    var objX: Double
                    var objY: Double
                    var objPlaced: Boolean

                    do {
                        objX = Random.nextDouble(FIELD_SAFEMARGIN, fieldWidth - FIELD_SAFEMARGIN)
                        objY = Random.nextDouble(FIELD_SAFEMARGIN, fieldHeight - FIELD_SAFEMARGIN)
                        tries--
                        objPlaced = !fieldObjectsImpl.any() { it.isColliding(objX, objY, newObjRadius) }
                    } while (!objPlaced && tries > 0)

                    if (!objPlaced) {
                        throw IllegalArgumentException("Cannot randomly place that much game objects")
                    }

                    fieldObjectsImpl.add(GameObject(pair.first, objX, objY))
                }
            }
        }
    }

}

class BodySegment(var dblStartX: Double, var dblStartY: Double, var dblStartZ: Double, val angle: Double, var dblLength: Double) : IBodySegment {
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

