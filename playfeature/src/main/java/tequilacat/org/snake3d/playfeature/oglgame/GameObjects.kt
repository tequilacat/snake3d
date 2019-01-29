package tequilacat.org.snake3d.playfeature.oglgame

import android.opengl.Matrix
import tequilacat.org.snake3d.playfeature.Game.GameObject
import tequilacat.org.snake3d.playfeature.glutils.*
import kotlin.math.*



// allow for different drawing approaches
abstract class AbstractGameObject(val gameObject: GameObject?) : Drawable

// TODO template class for gameObject, move to Drawables
class DrawableGameObject(primaryColor: FloatArray, private val geometry: Geometry,
                         private val geometryPainter: GeometryPainter,
                         textureId: Int = -1,
                         gameObject: GameObject? = null) : AbstractGameObject(gameObject) {
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

