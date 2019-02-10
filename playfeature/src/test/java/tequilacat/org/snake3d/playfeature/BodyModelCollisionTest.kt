package tequilacat.org.snake3d.playfeature

import org.junit.Assert.*
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
        override fun remove(collidingObj: IFieldObject) {}
        // Placeholder
        override val bodyModel = BodyModel(1.0, 1.0, 0.0, 0.0)
    }

    private val headOffset = 0.5
    private val headR = 2.0
    private val headAngle = PI / 6


    private fun body(headCenterX: Double, headCenterY: Double): BodyModel {
        val bodyLen = 10.0
        val bodyRadius = 1.0

        val offset = bodyLen + headOffset
        return BodyModel(1.0, bodyRadius, headOffset, headR).apply {
            init(headCenterX - cos(headAngle) * offset, headCenterY - sin(headAngle) * offset, 0.0, headAngle, bodyLen)
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
        assertEquals(BodyModel.CollisionType.NONE, body(5.0,5.0).checkCollisions(scene).type)

        assertEquals(BodyModel.CollisionType.WALL, body(9.0,5.0).checkCollisions(scene).type)
        assertEquals(BodyModel.CollisionType.WALL, body(11.0,5.0).checkCollisions(scene).type)
        assertEquals(BodyModel.CollisionType.WALL, body(1.0,5.0).checkCollisions(scene).type)
        assertEquals(BodyModel.CollisionType.WALL, body(-1.0,5.0).checkCollisions(scene).type)

        assertEquals(BodyModel.CollisionType.WALL, body(5.0,9.0).checkCollisions(scene).type)
        assertEquals(BodyModel.CollisionType.WALL, body(5.0,11.0).checkCollisions(scene).type)
        assertEquals(BodyModel.CollisionType.WALL, body(5.0,1.0).checkCollisions(scene).type)
        assertEquals(BodyModel.CollisionType.WALL, body(5.0,-1.0).checkCollisions(scene).type)
    }

    private data class FO( override val centerX: Float, override val centerY: Float, override val radius: Float) : IFieldObject {
        constructor(x: Double, y: Double, r: Double) : this(x.toFloat(), y.toFloat(), r.toFloat())
        override val type = IFieldObject.Type.OBSTACLE
        companion object {
            const val R = 1.0
        }
    }

    private fun BodyModel.Collision.assertIs(ct: BodyModel.CollisionType) {
        assertEquals(ct, this.type)
    }

    private fun BodyModel.Collision.assertCollidesObject() {
        assertTrue(this.isColliding)
        assertEquals(BodyModel.CollisionType.GAMEOBJECT, this.type)
        assertNotNull(this.fieldObject)
    }

    @Test
    fun `collision to objects`() {
        // TODO test by distance between head center and body end, not my proximity
        // so far test by distance between centers
        val headX = 5.0
        val headY = 5.0
        val body = body(headX, headY)

        assertEquals(
            BodyModel.CollisionType.NONE,
            body.checkCollisions(TestScene(10f, 10f, listOf(FO(0.0, 0.0, FO.R)))).type)

        val foFar = FO(
            headX + (headR + FO.R) * 1.1 * cos(headAngle),
            headY + (headR + FO.R) * 1.1 * sin(headAngle), FO.R)
        // close but too far by exact distance:
        assertEquals(
            BodyModel.CollisionType.NONE,
            body.checkCollisions(TestScene(10f, 10f, listOf(foFar))).type)

        val foNear = FO(
            headX + (headR + FO.R) * 0.9 * cos(headAngle),
            headY + (headR + FO.R) * 0.9 * sin(headAngle), FO.R)

        body.checkCollisions(TestScene(10f, 10f, listOf(foNear))).apply {
            assertTrue(this.isColliding)
            assertEquals(BodyModel.CollisionType.GAMEOBJECT, this.type)
            assertEquals(foNear, this.fieldObject)
        }
    }

    /**
     * the object is between head center and head disk
     */
    @Test
    fun `collision to object past head center`() {
        val w = 1000.0f
        val h = 1000.0f
        val bodyLen = 5.0
        val bodyRadius = 1.0
        val headRadius = 2.0
        val headOffset = 4.0
        val angle = PI / 2 // up ahead

        val objR = 0.01 // very small for test purposes

        val body = BodyModel(1.0, bodyRadius, headOffset, headRadius).apply {
            init(5.0, 5.0, 0.0, angle, bodyLen)
        }

        // check head sphere collision:
        // ahead sphere
        // in sphere
        // behind sphere AND body center

        // ahead head center and a bit outside radius
        body.checkCollisions(TestScene(w, h, listOf(FO(5.0, 16.0 + objR*2, objR))))
            .assertIs(BodyModel.CollisionType.NONE)

        // little behind face center, inside body - this check must fail, really checked elsewhere
        body.checkCollisions(TestScene(w, h, listOf(FO(5.0, 10.0 - 0.2, 0.1))))
            .assertIs(BodyModel.CollisionType.NONE)

        // ahead head center but within radius
        body.checkCollisions(TestScene(w, h, listOf(FO(5.0, 15.0, objR))))
            .assertCollidesObject()



        // between head center and face ring center - AND does not touch head sphere
        body.checkCollisions(TestScene(w, h, listOf(FO(5.0, 14.0 - headRadius - objR * 2, objR))))
            .assertCollidesObject()

        // exactly at head ring center
        body.checkCollisions(TestScene(w, h, listOf(FO(5.0, 10.0, objR))))
            .assertCollidesObject()

        // Test touch neck:
        // Offset from neck: [6.25, 11] is on neck line

        // between head/ring centers and within the head cone - COLLIDES
        body.checkCollisions(TestScene(w, h, listOf(FO(6.25 - 2 * objR, 11.0, objR))))
            .assertCollidesObject()

        // between head/ring centers and out of the head cone - NO collision
        body.checkCollisions(TestScene(w, h, listOf(FO(6.25 + 2 * objR, 11.0, objR))))
            .assertIs(BodyModel.CollisionType.NONE)

        // also tricky - further ahead but within cone (out of head sphere)
        body.checkCollisions(TestScene(w, h, listOf(FO(7.0, 15.0, objR))))
            .assertIs(BodyModel.CollisionType.NONE)
    }


    @Test
    fun `collision to self`() {
//        fail("Collision to self is not tested")
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