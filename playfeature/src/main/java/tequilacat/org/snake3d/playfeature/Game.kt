package tequilacat.org.snake3d.playfeature

import android.graphics.RectF
import android.os.SystemClock
import kotlin.random.Random

/**
 * stores all logic and data of game field
 */
class Game(private val addObstacles: Boolean = true) {
    private data class Level(val fieldWidth: Double, val fieldHeight: Double, val pickableCount: Int, val obstacleCount: Int)
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

    companion object {
        // Assume SI: meters, seconds
        private const val R_HEAD = 1.0

        const val BODY_UNIT = R_HEAD.toFloat() * 2

        const val R_OBSTACLE = 1.0
        const val R_PICKABLE = 1.0

        private const val FIELD_SAFEMARGIN = R_HEAD * 4 // margin along sizes not seeded by objects

        private const val SPEED_M_NS = 10.0 / 1_000 // 10 m/s

        // 10 degrees per head diameter
        private const val ROTATE_ANGLE_PER_LEN = (Math.PI / 5) / (R_HEAD * 2)

        // no faster than 50 FPS - 20 ms
        private val MIN_STEP_MS: Long = 20 // milliseconds

        // device rotation within +- threshold does not cause turns
        private const val TILT_THRESHOLD = 5.0

        private const val FLOOR_Z = 0.0
        private const val BODY_INIT_LEN = R_HEAD * 4
        private const val BODY_TAIL_LEN = R_HEAD * 8
        private const val FEED_SIZE = R_HEAD * 2
    }

    private class GameObject(override val type: IFieldObject.Type,
                             private val centerDblX: Double, private val centerDblY: Double,
                             private val dblRadius: Double) : IFieldObject {
        override val radius = dblRadius.toFloat()
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

    private fun getEffectiveRotateAngle(gameControlImpulse: GameControlImpulse? = null) = when {
        gameControlImpulse == GameControlImpulse.LEFT -> ROTATE_ANGLE_PER_LEN
        gameControlImpulse == GameControlImpulse.RIGHT -> -ROTATE_ANGLE_PER_LEN
        gameControlImpulse == GameControlImpulse.NONE -> 0.0
        // these called when null is provided (2d controls)
        (lastRoll > TILT_THRESHOLD) -> -ROTATE_ANGLE_PER_LEN
        (lastRoll < -TILT_THRESHOLD) -> ROTATE_ANGLE_PER_LEN
        else -> 0.0
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

        scene.bodyModel.advance(step, deltaAngle * step)

        val collision = scene.bodyModel.checkCollisions(scene)

        if (collision.type == BodyModel.CollisionType.WALL || collision.type == BodyModel.CollisionType.SELF) {
            firstLevel()
            tickResult = TickResult.INITGAME

        } else if (collision.type == BodyModel.CollisionType.GAMEOBJECT) {
            val collidingObj = collision.fieldObject

            when(collidingObj?.type) {
                IFieldObject.Type.OBSTACLE -> {
                    firstLevel()
                    tickResult = TickResult.INITGAME
                }
                IFieldObject.Type.PICKABLE -> {
                    scene.remove(collidingObj)
                    scene.bodyModel.feed(FEED_SIZE)
                    tickResult = TickResult.CONSUME
                }
                else -> tickResult = TickResult.MOVE
            }
        } else {
            tickResult = TickResult.MOVE
        }

        lastInteraction = SystemClock.uptimeMillis()
        return tickResult
    }

    private class GameScene(private val level: Level) : IGameScene {

        override val bodyModel = BodyModel(TailLenBodyProportions(R_HEAD, BODY_TAIL_LEN,
            R_HEAD / 2, R_HEAD * 1.5))

        override val fieldWidth: Float = level.fieldWidth.toFloat()
        override val fieldHeight: Float = level.fieldHeight.toFloat()

        val fieldObjectsImpl = mutableListOf<GameObject>()

        override val fieldObjects = fieldObjectsImpl as Iterable<IFieldObject>

        override fun remove(collidingObj: IFieldObject) {
            fieldObjectsImpl.remove(collidingObj)
        }

        init {
            loadLevel()
        }

        /**
         * seeds field with initial
         */
        private fun loadLevel() {
            bodyModel.init(FIELD_SAFEMARGIN / 2,
                FIELD_SAFEMARGIN / 2, FLOOR_Z, 0.0, BODY_INIT_LEN)

            fieldObjectsImpl.clear()

            val fieldWidth = level.fieldWidth
            val fieldHeight = level.fieldHeight

            for (pair in listOf(
                Triple(IFieldObject.Type.PICKABLE, R_PICKABLE, level.pickableCount),
                Triple(IFieldObject.Type.OBSTACLE, R_OBSTACLE, level.obstacleCount)
            )) {
                val newObjRadius = pair.second

                for (i in 0 until pair.third) {
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

                    fieldObjectsImpl.add(GameObject(pair.first, objX, objY, pair.second))
                }
            }
        }
    }
}
