package tequilacat.org.snake3d.playfeature.oglgame

import tequilacat.org.snake3d.playfeature.BodySegment
import tequilacat.org.snake3d.playfeature.IBodySegment
import tequilacat.org.snake3d.playfeature.append
import kotlin.math.PI

class DebugScene {
    // when not empty defines static camera
    //val cameraPosition = floatArrayOf(0f, 15f, 2f, 10f, 10f, 8f)
    val cameraPosition = floatArrayOf(5f,6f,5f,100f,100f,100f)


    val addObstacles = false

    // when not empty defines body shape
    val bodySegments1 =
        mutableListOf<IBodySegment>(
            BodySegment(30.0, 4.0, 1.0, PI/12, 0.47, 0.0)
        ).apply {
            (1..96).forEach {
                append(PI/12, 0.47, true)
            }
        }

    val bodySegments =
        mutableListOf<IBodySegment>(
            BodySegment(
                7.0, 8.0, 10.0,
                PI / 6, 0.0, 2.0))
            .append(PI/4, 2.0, true)
            .append(PI/4, 2.0, true)
            .append(PI/4, 2.0, true)
            .append(PI/4, 2.0, true)

    //private val LIGHT_POSITION = floatArrayOf(-10f, 5f, 10f, 1f)

/*

*/


}