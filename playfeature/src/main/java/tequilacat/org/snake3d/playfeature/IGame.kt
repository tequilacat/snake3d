package tequilacat.org.snake3d.playfeature

class GameGeometry {
    companion object {
        const val R_HEAD = 1.0
        const val R_OBSTACLE = 1.0
        const val R_PICKABLE = 1.0
    }
}

interface IFieldObject {
    enum class Type(val dblRadius:Double) {
        OBSTACLE( GameGeometry.R_OBSTACLE), PICKABLE( GameGeometry.R_PICKABLE);
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
    val bodySegments: Collection<BodySegment>
}

interface IBodySegment {
    val startX: Float
    val startY: Float
    val startZ: Float

    val endX: Float
    val endY: Float
    val endZ: Float

    val length: Float

    val alpha: Float
    val alphaSinus: Float
    val alphaCosinus: Float

    val beta: Float
    val betaSinus: Float
    val betaCosinus: Float
}

/**
 * appends a segment to the list
 */
fun MutableList<IBodySegment>.append(
    angle: Double,
    length: Double,
    angleRelative: Boolean = true
): MutableList<IBodySegment> {

    val last = this.last()
    this.add(
        BodySegment(
            last.endX.toDouble(), last.endY.toDouble(), last.endZ.toDouble(),
            angle + if(angleRelative) last.alpha else 0f, length
        )
    )
    return this
}
