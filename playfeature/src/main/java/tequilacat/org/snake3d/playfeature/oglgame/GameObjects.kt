package tequilacat.org.snake3d.playfeature.oglgame

import android.opengl.Matrix
import tequilacat.org.snake3d.playfeature.BodySegment
import tequilacat.org.snake3d.playfeature.Game
import tequilacat.org.snake3d.playfeature.glutils.*
import kotlin.math.*



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

fun makeFloor(fW: Float, fH: Float, tileSpace: Float, addTexture: Boolean): GeometryData {
    val floorBuilder = GeometryBuilder()
    val floorZ = 0f

    floorBuilder.addQuad(
        0f, 0f, floorZ, // BL
        fW, 0f, floorZ, // BR
        fW, fH, floorZ, // TR
        0f, fH, floorZ,
        if (addTexture) floatArrayOf(0f, 0f, 20f, 20f) else Empty.FloatArray
    )

    /*
    val texArray = if (addTexture) floatArrayOf(0f, 0f, 1f, 1f) else FloatArray(0)
    val tileSide = tileSpace // * 0.95f No gaps!
    var x = 0f

    while (x < fW) {
        var y = 0f

        while (y < fH) {
            floorBuilder.addQuad(
                x, y, floorZ, // BL
                x + tileSide, y, floorZ, // BR
                x + tileSide, y + tileSide, floorZ, // TR
                x, y + tileSide, floorZ,
                texArray
            )
            y += tileSpace
        }

        x += tileSpace
    }
*/
    return floorBuilder.build()
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
