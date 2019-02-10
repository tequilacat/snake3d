package tequilacat.org.snake3d.playfeature

class GameGeometry {
    companion object {
        // TODO hide geometry radiuses in BodyModel after reworking BodyShape
        //const val R_HEAD = 1.0
        const val R_OBSTACLE = 1.0
        const val R_PICKABLE = 1.0
    }
}

interface IFieldObject {
    enum class Type(val dblRadius:Double) {
        OBSTACLE( GameGeometry.R_OBSTACLE), PICKABLE( GameGeometry.R_PICKABLE);
        // TODO IFieldObject.Type.radius to double
        val radius = dblRadius.toFloat()
    }

    val type: Type
    val centerX: Float
    val centerY: Float
}

interface IGameScene {

    val fieldWidth: Float
    val fieldHeight: Float

    val fieldObjects: Iterable<IFieldObject>
    fun remove(collidingObj: IFieldObject)

    val bodyModel: BodyModel
}
