package tequilacat.org.snake3d.playfeature.oglgame

import android.opengl.Matrix
import tequilacat.org.snake3d.playfeature.IFieldObject
import tequilacat.org.snake3d.playfeature.glutils.*
import kotlin.math.*



// allow for different drawing approaches
abstract class AbstractGameObject(val gameObject: IFieldObject?) : Drawable

// TODO template class for gameObject, move to Drawables
abstract class AbstractDrawableGameObject(
    private val geometryPainter: GeometryPainter,
    private val objectContext: ObjectContext,
    gameObject: IFieldObject? = null) : AbstractGameObject(gameObject) {

    protected abstract val geometryBuffer: GeometryBuffer

    private val modelMatrix = FloatArray(16).also {
        Matrix.setIdentityM(it, 0)
    }

    override fun position(x: Float, y: Float, z: Float, rotateAngle: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, (rotateAngle * 180 / PI).toFloat(), 0.0f, 0.0f, 1.0f)
    }

    override fun draw(sceneContext: SceneContext) {
        geometryPainter.paint(geometryBuffer, objectContext, modelMatrix, sceneContext)
    }
}

class DrawableGameObject(
    override val geometryBuffer: GeometryBuffer,
    geometryPainter: GeometryPainter,
    primaryColor: FloatArray,
    textureId: Int = -1,
    gameObject: IFieldObject? = null
) : AbstractDrawableGameObject(
    geometryPainter, ObjectContext(primaryColor, textureId), gameObject
)





fun makeFloor(fW: Float, fH: Float, wallHeight: Float, rimWidth: Float): Geometry {
    val floorBuilder = GeometryBuilder()
    val floorZ = 0f

    floorBuilder.addQuad(
        0f, 0f, floorZ,
        0f, fH, floorZ,
        0f, fH, floorZ + wallHeight,
        0f, 0f, floorZ + wallHeight,
        floatArrayOf(0f, 0f, 20f, 1f)
    )

    floorBuilder.addQuad(
        0f, 0f, floorZ,
        0f, 0f, floorZ + wallHeight,
        fW, 0f, floorZ + wallHeight,
        fW, 0f, floorZ,
        floatArrayOf(0f, 0f, 1f, 20f)
    )

    floorBuilder.addQuad(
        fW, 0f, floorZ,
        fW, 0f, floorZ + wallHeight,
        fW, fH, floorZ + wallHeight,
        fW, fH, floorZ,
        floatArrayOf(0f, 0f, 1f, 20f)
    )

    floorBuilder.addQuad(
        fW, fH, floorZ,
        fW, fH, floorZ + wallHeight,
        0f, fH, floorZ + wallHeight,
        0f, fH, floorZ,
        floatArrayOf(0f, 0f, 1f, 20f)
    )

    val x0 = -rimWidth
    val y0 = -rimWidth
    val x1 = fW + rimWidth
    val y1 = fH + rimWidth
    val rimH = floorZ + wallHeight
    val wallfactor = 0.5f
    // rims 0x
    floorBuilder.addQuad(
        x0, 0f, rimH,
        x0, y0, rimH,
        x1, y0, rimH,
        x1, 0f, rimH,
        floatArrayOf(0f, 0f, wallfactor, 20f)
    )

    floorBuilder.addQuad(
        x0, 0f, rimH,
        0f, 0f, rimH,
        0f, fH, rimH,
        x0, fH, rimH,
        floatArrayOf(0f, 0f, wallfactor, 20f)
    )

    floorBuilder.addQuad(
        x0, fH, rimH,
        x1, fH, rimH,
        x1, y1, rimH,
        x0, y1, rimH,
        floatArrayOf(0f, 0f, 20f, wallfactor)
    )

    floorBuilder.addQuad(
        fW, 0f, rimH,
        x1, 0f, rimH,
        x1, fH, rimH,
        fW, fH, rimH,
        floatArrayOf(0f, 0f, wallfactor, 20f)
    )

    // the floor
    floorBuilder.addQuad(
        0f, 0f, floorZ, // BL
        fW, 0f, floorZ, // BR
        fW, fH, floorZ, // TR
        0f, fH, floorZ,
        floatArrayOf(0f, 0f, 20f, 20f)
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

