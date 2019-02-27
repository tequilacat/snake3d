package tequilacat.org.snake3d.playfeature

interface IFieldObject {
    enum class Type {
        OBSTACLE, PICKABLE
    }

    val type: Type
    val radius: Float
    val centerX: Float
    val centerY: Float
}

interface IGameScene {

    val fieldWidth: Float
    val fieldHeight: Float

    val fieldObjects: Iterable<IFieldObject>
    fun remove(collidingObj: IFieldObject)
}
