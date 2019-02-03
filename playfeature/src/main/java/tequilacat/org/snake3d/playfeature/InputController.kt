package tequilacat.org.snake3d.playfeature

import android.util.Log
import android.view.MotionEvent
import tequilacat.org.snake3d.playfeature.glutils.f
import kotlin.math.PI
import kotlin.math.abs

class InputController() {
    // private val queryDirCb: () -> Float
    private enum class State {
        WAIT, PRESSED, DRAGGED
    }
    private var state = State.WAIT
    private var viewWidth:Int = 0
    private var viewHeight: Int = 0

    private var lastX = 0f
    private var lastY = 0f

    fun initialize(viewWidth:Int, viewHeight: Int) {
        this.viewWidth = viewWidth
        this.viewHeight = viewHeight
        resetInput()
    }

    fun resetInput() {
        state = State.WAIT
    }

    fun processTouchEvent(event: MotionEvent) {
        val x = event.x
        val y = event.y

        if (event.action == MotionEvent.ACTION_UP
            || event.action == MotionEvent.ACTION_POINTER_UP) {
            // RELEASE POINTER
            resetInput()

        } else if (event.action == MotionEvent.ACTION_DOWN
            || event.action == MotionEvent.ACTION_POINTER_DOWN) {
            // down

            // START DRAG

            lastX = x
            lastY = y
            state = State.PRESSED

        } else if (event.action == MotionEvent.ACTION_MOVE) {
            // DO DRAG: remember currently dragged distance
            horizontalDrag = -(x - lastX) / viewWidth
            state = State.DRAGGED
        }
    }

    private val toDeg = 180 / PI.toFloat()

    // on which head direction angle the drag started
    private var startDragAngle: Float = 0f

    // how much angle
    private val rotateAngleRatio = 150 / toDeg  // whole screen is 60'

    /**
     * Relative distance dragged from starting point.
     * The drag across whole screen width equals to 1.0.
     * The left drag is positive, the right is negative
     */
    private var horizontalDrag: Float = 0f

    /** Deltas less than this will not be corrected to avoid jitter.
     * Default is 5 degrees
     * */
    private val minCorrectableDelta = 5 * PI.toFloat() / 180

    /** minimal drag distance (in screen width fraction) to be registered as drag */
    private var minDragDistance = 0.1f

    /**
     * from current input (finger drag) and current head position decides to which direction to rotate head
     */
    fun computeImpulse(headAngle: Float) =
        when(state) {
            State.WAIT -> Game.GameControlImpulse.NONE
            State.PRESSED -> {
                startDragAngle = headAngle
                Game.GameControlImpulse.NONE
            }
            State.DRAGGED -> {
                when{
                    abs(horizontalDrag) < minDragDistance -> Game.GameControlImpulse.NONE
                    horizontalDrag < 0 -> Game.GameControlImpulse.RIGHT
                    else -> Game.GameControlImpulse.LEFT
                }
               // computeIncrementalImpulse(headAngle)
            }
        }

    private fun computeIncrementalImpulse(headAngle: Float): Game.GameControlImpulse {
        val deltaAngle = horizontalDrag * rotateAngleRatio
        val targetAngle = startDragAngle + deltaAngle
        //Log.d("control", "Delta angle: ${targetAngle - currentDirAngle} ")

        val controlState = when {
            abs(targetAngle - headAngle) < minCorrectableDelta ->{
                Log.d("control", "skip jitter...")
                Game.GameControlImpulse.NONE }
            targetAngle > headAngle ->
                Game.GameControlImpulse.LEFT
            else ->
                Game.GameControlImpulse.RIGHT
        }

        Log.d(
            "control",
            "Current: ${headAngle.times(toDeg).f(3)}, target: ${targetAngle.times(toDeg).f(3)}" +
                    " -> $controlState"
        )
        return controlState
    }
}