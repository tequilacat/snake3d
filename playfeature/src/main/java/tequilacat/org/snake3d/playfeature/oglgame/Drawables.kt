package tequilacat.org.snake3d.playfeature.oglgame

import android.opengl.GLES20.*
import tequilacat.org.snake3d.playfeature.BodySegment
import tequilacat.org.snake3d.playfeature.Game
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


data class DrawContext(val program: DefaultProgram) {
    val mvpMatrix = FloatArray(16)
}

interface Drawable {
    fun draw(drawContext: DrawContext)
}

interface IResourceHolder {
    fun freeResources()
}

abstract class AbstractOGLGameObject() {
    var gameObject: Game.GameObject? = null
}

open class GOTriangles(private val vertexBuffer: FloatBuffer,
                       private val indexBuffer: ShortBuffer,
                       private val color: FloatArray) :
    AbstractOGLGameObject(),
    Drawable {

    companion object {
        const val COORDS_PER_VERTEX = 3
        /** 4 = bytes per vertex */
        const val VERTEX_STRIDE = COORDS_PER_VERTEX * 4
    }

    override fun draw(drawContext: DrawContext) {
        glUseProgram(drawContext.program.id)

        glEnableVertexAttribArray(drawContext.program.positionHandle)
        glVertexAttribPointer(
            drawContext.program.positionHandle, COORDS_PER_VERTEX,
            GL_FLOAT, false,
            VERTEX_STRIDE, vertexBuffer)

        glUniform4fv(drawContext.program.colorHandle, 1, color, 0)
        glUniformMatrix4fv(drawContext.program.mvpMatrixHandle, 1, false, drawContext.mvpMatrix, 0)

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


class BodyShape :
    AbstractOGLGameObject(),
    Drawable {

    override fun draw(drawContext: DrawContext) {
    }

    private companion object {
        private val BS_SIDE_COUNT = 6
        private val BS_ANGLES = Array(BS_SIDE_COUNT) {
            Pair(sin(PI * 2 / BS_SIDE_COUNT * it).toFloat(),
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
