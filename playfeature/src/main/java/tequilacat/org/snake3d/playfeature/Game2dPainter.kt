package tequilacat.org.snake3d.playfeature

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

class Game2dPainter {

    fun drawGameScreen(c: Canvas, gameScene: IGameScene, viewWidth: Int, viewHeight: Int,
                       // TODO remove thee as they should not affect view
                       // game.lastRoll, game.TILT_THRESHOLD, game.getEffectiveRotateAngle
                       lastRoll: Float, tiltRange: Float, effRotateAngle: Float) {
        c.drawColor(0xff3affbd.toInt())

        val R_HEAD = GameGeometry.R_HEAD.toFloat()
        // fit width to screen: pix to logic
        val ratio: Float = (viewWidth / gameScene.fieldWidth)

        c.drawRect(0f, 0f, (gameScene.fieldWidth * ratio), (gameScene.fieldHeight * ratio), Paints.linePaint)

        for (obj in gameScene.fieldObjects) {
            drawGameObject(c, obj, ratio.toDouble())
        }

        for (segment in gameScene.bodyModel.bodySegments) {
            val x0 = (segment.startX * ratio)
            val y0 = (segment.startY * ratio)
            val x1 = (segment.endX * ratio)
            val y1 = (segment.endY * ratio)
            c.drawLine(x0, y0, x1, y1, Paints.bodyPaint)
            c.drawCircle(x0, y0, R_HEAD / 2 * ratio, Paints.bodyPaint)
        }

        val headX = gameScene.bodyModel.headX * ratio
        val headY = gameScene.bodyModel.headY * ratio
        // drawGameFrame head with direction as
        c.drawCircle(headX, headY, R_HEAD * ratio, Paints.headPaint)
        c.drawLine(
            headX, headY, (headX + R_HEAD * ratio * cos(gameScene.bodyModel.viewDirection)),
            (headY + R_HEAD * ratio * sin(gameScene.bodyModel.viewDirection)), Paints.linePaint
        )

        // c.drawText(dbgStatus, 0f, fieldHeight * ratio + Paints.linePaint.textSize, Paints.linePaint)

        // rotation ruler
        // along screen bottom
        val rcHeight = viewWidth / 20
        val rcSegmentWidth = viewWidth / 3f
        val deltaAngle = effRotateAngle
        val segColorActive: Int = 0xff0000ff.toInt()
        val segColorInactive: Int = 0xff00ccff.toInt()

        val rcRect = RectF(0f, (viewHeight - rcHeight).toFloat(), rcSegmentWidth, viewHeight.toFloat())
        c.drawRect(rcRect, fillPainter.apply { color = if (deltaAngle < 0) segColorActive else segColorInactive })

        rcRect.offset(rcRect.width() * 2, 0f)
        c.drawRect(rcRect, fillPainter.apply { color = if (deltaAngle > 0) segColorActive else segColorInactive })

        // drawGameFrame circle on distance from center
        val rotateBallRadius = rcHeight * 0.35f
        var rotateBallX = (viewWidth / 2 - lastRoll / tiltRange * rcSegmentWidth / 2).toFloat()

        if(rotateBallX < rotateBallRadius){
            rotateBallX = rotateBallRadius
        } else if (rotateBallX > viewWidth - rotateBallRadius) {
            rotateBallX = viewWidth - rotateBallRadius
        }

        c.drawCircle(rotateBallX,
            (viewHeight - rcHeight / 2).toFloat(), rotateBallRadius, fillPainter.apply { color = 0xff000000.toInt() })
    }

    private val fillPainter = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
    }

    private fun drawGameObject(c: Canvas, gameObject: IFieldObject, ratio: Double) {
        fillPainter.color = when(gameObject.type) {
            IFieldObject.Type.OBSTACLE -> 0xffff0000.toInt()
            IFieldObject.Type.PICKABLE -> 0xff0000ff.toInt()
        }

        c.drawCircle(
            (gameObject.centerX * ratio).toFloat(), (gameObject.centerY * ratio).toFloat(),
            (gameObject.type.radius * ratio).toFloat(), fillPainter)
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

}

