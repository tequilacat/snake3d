package tequilacat.org.snake3d.playfeature.oglgame

import android.opengl.GLES20.*
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


data class DrawContext(val program: DefaultProgram)

interface Drawable {
    fun draw(mvpMatrix: FloatArray, drawContext: DrawContext)
}

abstract class AbstractOGLGameObject


open class GOTriangles(private val vertexBuffer: FloatBuffer,
                       private val indexBuffer: ShortBuffer,
                       private val color: FloatArray) :
    AbstractOGLGameObject(),
    Drawable {

    override fun draw(mvpMatrix: FloatArray, drawContext: DrawContext) {
        glUseProgram(drawContext.program.id)

        glEnableVertexAttribArray(drawContext.program.positionHandle)
        glVertexAttribPointer(
            drawContext.program.positionHandle, OGLUtils.COORDS_PER_VERTEX,
            GL_FLOAT, false,
            OGLUtils.VERTEX_STRIDE, vertexBuffer)

        glUniform4fv(drawContext.program.colorHandle, 1, color, 0)
        glUniformMatrix4fv(drawContext.program.mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Draw the square
        glDrawElements(GL_TRIANGLES, indexBuffer.capacity(),
            GL_UNSIGNED_SHORT, indexBuffer)

        // Disable vertex array
        glDisableVertexAttribArray(drawContext.program.positionHandle)
    }
}

/**
 * displays rect as 2 triangles
 */
class GORect(
    rectCoords: FloatArray,
    color: FloatArray
) : GOTriangles(rectCoords.toBuffer(), RECT_VERTEX_ORDER, color)

/**
 *
 */
class GORectPrism(cx: Float, cy: Float, radius: Float, sides: Int,
                  zBase: Float, height: Float, color: Int) :
    GOTriangles(
        composePrismVertexes(cx, cy, radius, sides, zBase, height).toBuffer(),
        composePrismIndexes(sides).toBuffer(), color.toColorArray()) {

    companion object {

        fun composePrismVertexes(
            cx: Float, cy: Float, radius: Float, sides: Int,
            zBase: Float, height: Float
        ): FloatArray {
            val deltaAlpha: Float = PI.toFloat() * 2 / sides.toFloat()
            val coords = FloatArray(sides * 2 * 3)
            var pos = 0
            var alpha = 0f

            for (side in 0 until sides) {
                val x = cx + radius * cos(alpha)
                val y = cy + radius * sin(alpha)

                coords[pos++] = x
                coords[pos++] = y
                coords[pos++] = zBase

                coords[pos++] = x
                coords[pos++] = y
                coords[pos++] = zBase + height

                alpha += deltaAlpha
            }

            return coords
        }


        /**
         * creates prism indexes for specified sides count,
         * assume prism along 0Z, vertexes follow around center counter-clockwise (increasing alpha)
         */
        fun composePrismIndexes(sides: Int) : ShortArray {
            val array = ShortArray(sides * 6)
            var pos = 0

            for (sideVertex in 0 until sides) {
                val p0: Short = (sideVertex * 2).toShort()
                val p1: Short = (p0 + 1).toShort()
                val p2: Short = if (sideVertex < sides - 1) (p0 + 2).toShort() else 0
                val p3: Short = (p2 + 1).toShort()
                array[pos++] = p0
                array[pos++] = p2
                array[pos++] = p1
                array[pos++] = p2
                array[pos++] = p3
                array[pos++] = p1
            }

            return array
        }
    }
}
