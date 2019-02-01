package tequilacat.org.snake3d.playfeature.oglgame

import tequilacat.org.snake3d.playfeature.BodySegment
import tequilacat.org.snake3d.playfeature.IBodySegment
import tequilacat.org.snake3d.playfeature.append
import kotlin.math.PI

class DebugScene {
    // when not empty defines static camera
    val cameraPosition = floatArrayOf(0f, 12f, 5f, 10f, 8f, 5f)

    // when not empty defines body shape
    val bodySegments =
        mutableListOf<IBodySegment>(
            BodySegment(20.0, 4.0, 1.0, PI/12, 0.47)
        ).apply {
            (1..96).forEach {
                append(PI/12, 0.47, true)
            }
        }


    //private val LIGHT_POSITION = floatArrayOf(-10f, 5f, 10f, 1f)

/*

        mutableListOf<IBodySegment>(
            BodySegment(
                10.0, 4.0, 1.0,
                PI/4, 2.0))
            .append(PI/4, 2.0, false)
//            .append(PI/4, 2.0, false)
//            .append(PI/4, 2.0, false)
*/


}