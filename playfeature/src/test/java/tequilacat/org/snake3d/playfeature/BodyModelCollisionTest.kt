package tequilacat.org.snake3d.playfeature

import junit.framework.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BodyModelCollisionTest {

    private data class TestScene(
        override val fieldWidth: Float,
        override val fieldHeight: Float,
        override val fieldObjects: Iterable<IFieldObject> = listOf()
    ) : IGameScene {
        override val bodySegments: Collection<BodySegment> = listOf()
    }

    private val headOffset = 0.5
    private val headR = 2.0
    private val headAngle = PI / 6


    fun body(x: Double, y: Double): BodyModel {
        val bodyLen = 10.0
        val bodyRadius = 1.0

        val offset = bodyLen + headOffset
        return BodyModel(1.0, bodyRadius, headOffset, headR).apply {
            init(x - cos(headAngle) * offset, y - sin(headAngle) * offset, 0.0, headAngle, bodyLen)
        }
    }

    /**
     * tests whether the test code works OK :)
     */
    @Test
    fun `test test harness`() {
        val body = body(5.0, 5.0)
        assertEquals((5 - cos(headAngle) * headOffset).toFloat(), body.bodySegments.last().endX, testFloatTolerance)
        assertEquals((5 - sin(headAngle) * headOffset).toFloat(), body.bodySegments.last().endY, testFloatTolerance)
    }

    // active zone is head
    @Test
    fun `collision to field margins`() {
        val scene = TestScene(10f, 10f)
        // R = 2
        assertEquals(BodyModel.NO_COLLISION, body(5.0,5.0).checkCollisions(scene))

        assertEquals(BodyModel.WALL_COLLISION, body(9.0,5.0).checkCollisions(scene))
        assertEquals(BodyModel.WALL_COLLISION, body(11.0,5.0).checkCollisions(scene))
        assertEquals(BodyModel.WALL_COLLISION, body(1.0,5.0).checkCollisions(scene))
        assertEquals(BodyModel.WALL_COLLISION, body(-1.0,5.0).checkCollisions(scene))

        assertEquals(BodyModel.WALL_COLLISION, body(5.0,9.0).checkCollisions(scene))
        assertEquals(BodyModel.WALL_COLLISION, body(5.0,11.0).checkCollisions(scene))
        assertEquals(BodyModel.WALL_COLLISION, body(5.0,1.0).checkCollisions(scene))
        assertEquals(BodyModel.WALL_COLLISION, body(5.0,-1.0).checkCollisions(scene))
    }

    private data class FO( override val centerX: Float, override val centerY: Float) : IFieldObject {
        constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
        override val type = IFieldObject.Type.OBSTACLE
        companion object {
            val R = IFieldObject.Type.OBSTACLE.dblRadius
        }
    }

    @Test
    fun `collision to objects`() {
        // TODO test by distance between head center and body end, not my proximity
        // so far test by distance between centers
        val headX = 5.0
        val headY = 5.0
        val body = body(headX, headY)

        assertEquals(
            BodyModel.NO_COLLISION,
            body.checkCollisions(TestScene(10f, 10f, listOf(FO(0.0f, 0.0f)))))

        val foFar = FO(
            headX + (headR + FO.R) * 1.1 * cos(headAngle),
            headY + (headR + FO.R) * 1.1 * sin(headAngle)
        )
        // close but too far by exact distance:
        assertEquals(
            BodyModel.NO_COLLISION,
            body.checkCollisions(TestScene(10f, 10f, listOf(foFar))))

        val foNear = FO(
            headX + (headR + FO.R) * 0.9 * cos(headAngle),
            headY + (headR + FO.R) * 0.9 * sin(headAngle)
        )

        body.checkCollisions(TestScene(10f, 10f, listOf(foNear))).apply {
            assertTrue(this.isColliding)
            assertEquals(BodyModel.CollisionType.GAMEOBJECT, this.type)
            assertEquals(foNear, this.fieldObject)
        }
    }

    @Test
    fun `collision to self`() {
        fail("Collision to self is not tested")
        /*val tailLen = 1.0
        val initLen = 0.2
        val delta = 2.0

        BodyModel(tailLen, tRadius)
            .apply {
                init(tStartX, tStartY, tStartZ, tStartAngle, initLen)
                advance(delta, NO_ROTATE, delta)
            }.assertBodySegments(tailLen, doubleArrayOf(initLen),
                doubleArrayOf(tStartAngle),
                tStartX + delta* cos(tStartAngle), tStartY + delta* cos(tStartAngle)
            )*/
    }
}