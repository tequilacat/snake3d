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





fun makeFloor(fW: Float, fH: Float, tileSpace: Float, addTexture: Boolean): Geometry {
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

