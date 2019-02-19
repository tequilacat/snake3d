package tequilacat.org.snake3d.playfeature.oglgame

import android.opengl.Matrix
import tequilacat.org.snake3d.playfeature.BodyModel
import tequilacat.org.snake3d.playfeature.glutils.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// bodyPainter: GeometryPainter, bodyTextureId: Int
class DebugScene {
    // when not empty defines static camera

    private fun positionCamera(x: Number, y: Number, z: Number, alpaDegrees: Int): FloatArray {
        val x0 = x.toFloat()
        val y0 = y.toFloat()
        val z0 = z.toFloat()
        val alpha = PI / 2 * alpaDegrees / 90
        return floatArrayOf(x0, y0, z0, (x0 + cos(alpha)).toFloat(), (y0 + sin(alpha)).toFloat(), z0)
    }

    val addObstacles = false

    private val tRadius = 1.0
    private val TAILLEN = 5.0 // use expanding tail - 1 segment

    val cameraPosition = positionCamera(8, 9, 1.0, 270)

    private val bodies = arrayOf(
        DebugBodyObj(
            BodyModel(TAILLEN, tRadius, 0.0, tRadius)
                .run {
                    init(6.0, 4.0, 2.1, 0.5, 2.0)
                    feed(1000.0)
                    advance(2.0, -0.5)
                    bodySections
                }
        ),

        DebugBodyObj(BodyModel(TAILLEN, tRadius, 0.0, tRadius)
            .run {
                init(6.0, 4.0, 0.0, 0.0, 4.0)
                feed(1000.0)
                //advance(3.0, 0.0)
                bodySections
            }
        )
    )

    data class TestObjectContext(val painter:GeometryPainter, val textureId: Int)

    fun draw(sceneContext: SceneContext, testObjectContext: TestObjectContext) {
        bodies.forEach { it.update() }
        bodies.forEach { it.draw(sceneContext, testObjectContext) }
    }

    /**
     * displays specified segments as body
     * // val
     */
    class DebugBodyObj(private val segments: Sequence<IDirectedSection>) {
        private val modelMatrix = FloatArray(16).also {
            Matrix.setIdentityM(it, 0)
        }

        private val geomBuilder = RotationalShapeBuilder(
            6,
            3 * PI.toFloat() / 2,
            0.2f, // for yellow brown
            //0.5f, // good for grayscale
            0f
        ) as IRotationalGeometryBuilder

        lateinit var geometryBuffer: GeometryBuffer

        // recreates geometry buffer from geometry generated by body
        fun update() {
            /*if (::geometryBuffer.isInitialized) {
                geometryBuffer.release()
            }
            geomBuilder.update(segments)
            val bodyGeometry = geomBuilder.geometry
            geometryBuffer = GeometryBuffer(bodyGeometry)*/
        }

        fun draw(sceneContext: SceneContext, gfx: TestObjectContext) {
            /*val objectContext = ObjectContext(GameRenderer.ObjColors.BODY.rgb, gfx.textureId)
            gfx.painter.paint(geometryBuffer, objectContext, modelMatrix, sceneContext)*/
        }

    }

}