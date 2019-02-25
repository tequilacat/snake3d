package tequilacat.org.snake3d.playfeature

import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.*

class BodyModelCollisionTest {
    @Before
    fun beforeTests() = mockAndroidStatics(true)

    @After
    fun afterTests() = unmockkAll()

    private data class TestScene(
        override val fieldWidth: Float,
        override val fieldHeight: Float,
        override val fieldObjects: Iterable<IFieldObject> = listOf()
    ) : IGameScene {
        override fun remove(collidingObj: IFieldObject) {}
        // Placeholder
        override val bodyModel = BodyModel(TailLenBodyProportions(1.0, 1.0, 0.0, 0.0))
    }

    private val headOffset = 0.5
    private val headR = 2.0
    private val headAngle = PI / 6


    private fun body(headCenterX: Double, headCenterY: Double): BodyModel {
        val bodyLen = 10.0
        val bodyRadius = 1.0

        val offset = bodyLen + headOffset
        return BodyModel(TailLenBodyProportions(bodyRadius, 1.0, headOffset, headR)).apply {
            init(headCenterX - cos(headAngle) * offset, headCenterY - sin(headAngle) * offset, 0.0, headAngle, bodyLen)
        }
    }

    /**
     * tests whether the test code works OK :)
     * once tested
     */
    @Test
    fun `test test harness`() {
        val body = body(5.0, 5.0)
        assertEquals((5 - cos(headAngle) * headOffset).toFloat(), body.bodySections.last().centerX, testFloatTolerance)
        assertEquals((5 - sin(headAngle) * headOffset).toFloat(), body.bodySections.last().centerY, testFloatTolerance)
    }

    private fun cd() = CollisionDetector()

    // active zone is head
    @Test
    fun `collision to field margins`() {
        val scene = TestScene(10f, 10f)
        // R = 2
        assertEquals(CollisionDetector.CollisionType.NONE, cd().check(body(5.0,5.0), scene).type)

        assertEquals(CollisionDetector.CollisionType.WALL, cd().check(body(9.0,5.0), scene).type)
        assertEquals(CollisionDetector.CollisionType.WALL, cd().check(body(11.0,5.0), scene).type)
        assertEquals(CollisionDetector.CollisionType.WALL, cd().check(body(1.0,5.0), scene).type)
        assertEquals(CollisionDetector.CollisionType.WALL, cd().check(body(-1.0,5.0), scene).type)

        assertEquals(CollisionDetector.CollisionType.WALL, cd().check(body(5.0,9.0), scene).type)
        assertEquals(CollisionDetector.CollisionType.WALL, cd().check(body(5.0,11.0), scene).type)
        assertEquals(CollisionDetector.CollisionType.WALL, cd().check(body(5.0,1.0), scene).type)
        assertEquals(CollisionDetector.CollisionType.WALL, cd().check(body(5.0,-1.0), scene).type)
    }

    private data class FO( override val centerX: Float, override val centerY: Float, override val radius: Float) : IFieldObject {
        constructor(x: Double, y: Double, r: Double) : this(x.toFloat(), y.toFloat(), r.toFloat())
        override val type = IFieldObject.Type.OBSTACLE
        companion object {
            const val R = 1.0
        }
    }

    private fun CollisionDetector.Collision.assertNone() {
        assertEquals(CollisionDetector.CollisionType.NONE, this.type)
    }

    private fun CollisionDetector.Collision.assertSelf() {
        assertEquals(CollisionDetector.CollisionType.SELF, this.type)
    }

    private fun CollisionDetector.Collision.assertIs(ct: CollisionDetector.CollisionType) {
        assertEquals(ct, this.type)
    }

    private fun CollisionDetector.Collision.assertCollidesObject() {
        assertTrue(this.isColliding)
        assertEquals(CollisionDetector.CollisionType.GAMEOBJECT, this.type)
        assertNotNull(this.fieldObject)
    }

    @Test
    fun `collision to objects`() {
        // so far test by distance between centers
        val headX = 5.0
        val headY = 5.0
        val body = body(headX, headY)

        assertEquals(
            CollisionDetector.CollisionType.NONE,
            cd().check(body, TestScene(10f, 10f, listOf(FO(0.0, 0.0, FO.R)))).type)

        val foFar = FO(
            headX + (headR + FO.R) * 1.1 * cos(headAngle),
            headY + (headR + FO.R) * 1.1 * sin(headAngle), FO.R)
        // close but too far by exact distance:
        assertEquals(
            CollisionDetector.CollisionType.NONE,
            cd().check(body, TestScene(10f, 10f, listOf(foFar))).type)

        val foNear = FO(
            headX + (headR + FO.R) * 0.9 * cos(headAngle),
            headY + (headR + FO.R) * 0.9 * sin(headAngle), FO.R)

        cd().check(body, TestScene(10f, 10f, listOf(foNear))).apply {
            assertTrue(this.isColliding)
            assertEquals(CollisionDetector.CollisionType.GAMEOBJECT, this.type)
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

        val body = BodyModel(TailLenBodyProportions(bodyRadius, 1.0, headOffset, headRadius)).apply {
            init(5.0, 5.0, 0.0, angle, bodyLen)
        }

        // check head sphere collision:
        // ahead sphere
        // in sphere
        // behind sphere AND body center

        // ahead head center and a bit outside radius
        cd().check(body, TestScene(w, h, listOf(FO(5.0, 16.0 + objR*2, objR))))
            .assertIs(CollisionDetector.CollisionType.NONE)

        // little behind face center, inside body - this check must fail, really checked elsewhere
        cd().check(body, TestScene(w, h, listOf(FO(5.0, 10.0 - 0.2, 0.1))))
            .assertIs(CollisionDetector.CollisionType.NONE)

        // ahead head center but within radius
        cd().check(body, TestScene(w, h, listOf(FO(5.0, 15.0, objR))))
            .assertCollidesObject()



        // between head center and face ring center - AND does not touch head sphere
        cd().check(body, TestScene(w, h, listOf(FO(5.0, 14.0 - headRadius - objR * 2, objR))))
            .assertCollidesObject()

        // exactly at head ring center
        cd().check(body, TestScene(w, h, listOf(FO(5.0, 10.0, objR))))
            .assertCollidesObject()

        // Test touch neck:
        // Offset from neck: [6.25, 11] is on neck line

        // between head/ring centers and within the head cone - COLLIDES
        cd().check(body, TestScene(w, h, listOf(FO(6.25 - 2 * objR, 11.0, objR))))
            .assertCollidesObject()

        // between head/ring centers and out of the head cone - NO collision
        cd().check(body, TestScene(w, h, listOf(FO(6.25 + 2 * objR, 11.0, objR))))
            .assertIs(CollisionDetector.CollisionType.NONE)

        // also tricky - further ahead but within cone (out of head sphere)
        cd().check(body, TestScene(w, h, listOf(FO(7.0, 15.0, objR))))
            .assertIs(CollisionDetector.CollisionType.NONE)
    }

    @Test
    fun `self no curves no collisions`() {
        // trivial
        cd().check(body(5.0, 5.0), TestScene(100f,100f))
            .assertIs(CollisionDetector.CollisionType.NONE)

        // test that closest and smallest segments do not trigger collision
        // 11 segments by 0.2r
        cd().check(
            BodyModel(TailLenBodyProportions(1.0, 1000.0, 1.0, 1.0)).apply {
            init(10.0, 10.0, 10.0, 0.0, 0.2)
            advance(0.2, 0.01)
            advance(0.2, -0.01)
            advance(0.2, 0.01)
            advance(0.2, -0.01)
            advance(0.2, 0.01)
            advance(0.2, -0.01)
            advance(0.2, 0.01)
            advance(0.2, -0.01)
            advance(0.2, 0.01)
            advance(0.2, -0.01)
        }, TestScene(1000f,1000f))
            .assertNone()
    }


    /**
     * check that big head (expanding behind face ring) does not collide with vertebra behind
     */
    @Test
    fun `self big head does not touch first vertebra`() {
        val totalLen = 9.0
        val headR = 10.0 // HUGE head

        cd().check(BodyModel(TailLenBodyProportions(1.0, 1000.0, 0.0, headR)).apply {
            init(100.0, 100.0, 10.0, 0.0, totalLen)
        }, TestScene(1000f, 1000f))
            .assertNone()
    }

    /**
     *
     */
    @Test
    fun `self tail point out of neck`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 2.0
        val radius = 1.0

        cd().check(BodyModelTest.initByCoords(
            radius, hOffset, hRadius,
            52.0, 52.0, // out of cone clearly: no collision
            100.0, 50.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ), scene).assertNone()
    }

    /**
     *
     */
    @Test
    fun `self tail point ahead of head`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 2.0
        val radius = 1.0
        val margin = 0.01

        cd().check(BodyModelTest.initByCoords(
            radius, hOffset, hRadius,
            50.0, 48.0 - margin, // ahead of head
            100.0, 50.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ), scene).assertNone()
    }

    /**
     *
     */
    @Test
    fun `self tail point little out of neck`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 3.0
        val radius = 1.0
        val margin = 0.01

        cd().check(BodyModelTest.initByCoords(
            radius, hOffset, hRadius,
            52 + margin, 55.0, // almost touching cone but little outside
            100.0, 50.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ), scene).assertNone()
    }

    /**
     *
     */
    @Test
    fun `self tail point little inside of neck`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 3.0
        val radius = 1.0
        val margin = 0.01

        cd().check(BodyModelTest.initByCoords(
            radius, hOffset, hRadius,
            52 - margin, 53.0, // exactly touching cone and little inside
            100.0, 50.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ), scene).assertSelf()
    }

    /**
     *
     */
    @Test
    fun `self tail point within head`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 2.0
        val radius = 1.0

        cd().check(BodyModelTest.initByCoords(
            radius, hOffset, hRadius,
            50.0, 49.0, // within head
            100.0, 50.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ), scene).assertSelf()
    }




    /**
     * Segment crosses "neck" between head sphere and face ring
     */
    @Test
    fun `self crossing neck not touching head`() {
        val hOffset = 10.0
        cd().check(BodyModelTest.initByCoords(
            1.0, hOffset, 2.0,
            10.0, 55.0, 20.0, 55.0, // crosses neck without touching head - @y=55
            100.0, 55.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ), TestScene(1000f,1000f)).assertSelf()
    }

    /**
     * crosses neck between head sphere and face ring under angle 45'
     */
    @Test
    fun `self crossing neck diagonal`() {
        val hOffset = 10.0
        // crosses neck without touching head - @y=55
        cd().check(BodyModelTest.initByCoords(
            1.0, hOffset, 2.0,
            10.0, 55.0, 45.0, 55.0, 55.0, 50.0,
            100.0, 50.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ), TestScene(1000f,1000f)).assertSelf()
    }

    /**
     * Segment touches head sphere ahead of center.
     * segment axis is out of sphere but its radius makes segment touch the sphere
     */
    @Test
    fun `self crossing head`() {
        val hOffset = 10.0
        cd().check(BodyModelTest.initByCoords(
            1.0, hOffset, 2.0,
            10.0, 47.5, 20.0, 47.5, // crosses head - y = 47.5
            100.0, 47.5, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ), TestScene(1000f,1000f)).assertSelf()
    }


    /**
     * test crossing by segment! not by ends of segment touching head 
     */
    @Test
    fun `self crossing ahead of head`() {
        val hOffset = 10.0
        cd().check(BodyModelTest.initByCoords(
            1.0, hOffset, 2.0,
            10.0, 46.0, 20.0, 46.0, // crosses AHEAD of head - y = 46
            100.0, 46.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ), TestScene(1000f,1000f)).assertNone()
    }

    // coaxial checks:

    /**
     * segment coaxial with neck lays within neck
     */
    @Test
    fun `self crossing coaxial segment within neck`() {
        val hOffset = 10.0
        cd().check(BodyModelTest.initByCoords(
            1.0, hOffset, 2.0,
            50.0, 54.0, 50.0, 56.0, // within neck
            100.0, 55.0, 50.0, 100.0, 50.0, 50.0 + hOffset
        ), TestScene(1000f,1000f)).assertSelf()
    }

    /**
     * segment coaxial with neck partially overlaps neck
     */
    @Test
    fun `self crossing coaxial segment overlaps neck`() {
        val hOffset = 10.0
        cd().check(BodyModelTest.initByCoords(
            1.0, hOffset, 2.0,
            50.0, 10.0, 50.0, 56.0, // starts outside neck, ends in neck
            100.0, 55.0, 50.0, 100.0, 50.0, 50.0 + hOffset
        ), TestScene(1000f,1000f))
            .assertSelf()
    }

    /**
     * segment coaxial with neck outside neck
     */
    @Test
    fun `self crossing coaxial segment ahead and outside neck`() {
        val hOffset = 10.0
        cd().check(BodyModelTest.initByCoords(
            1.0, hOffset, 2.0,
            50.0, 10.0, 50.0, 12.0, // starts outside neck, ends in neck
            100.0, 55.0, 50.0, 100.0, 50.0, 50.0 + hOffset
        ), TestScene(1000f,1000f))
            .assertNone()
    }

    /**
     * segment coaxial with neck outside neck
     */
    @Test
    fun `self crossing coaxial segment behind and outside neck`() {
        val hOffset = 10.0
        cd().check(BodyModelTest.initByCoords(
            1.0, hOffset, 2.0,
            50.0, 200.0, 60.0, 100.0, 50.0, 110.0,
            50.0, 50.0 + hOffset
        ), TestScene(1000f,1000f))
            .assertNone()
    }

    /**
     * neck is coaxial to large segment and lays within it
     */
    @Test
    fun `self crossing coaxial neck within segment`() {
        val hOffset = 10.0
        cd().check(BodyModelTest.initByCoords(
            1.0, hOffset, 2.0,
            50.0, 20.0, 50.0, 100.0, // overlaps neck
            50.0, 50.0 + hOffset
        ), TestScene(1000f,1000f))
            .assertSelf()
    }

    /**
     * test crossing by segment! not by ends of segment touching head
     */
    @Test
    fun `self crossing behind face ring`() {
        val hOffset = 10.0

        // here no collisiion - body crosses body, not head - no collision here
        cd().check(BodyModelTest.initByCoords(
            1.0, hOffset, 2.0,
            10.0, 62.0, 20.0, 62.0, // crosses BEHIND of face - y = 62
            100.0, 62.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ), TestScene(1000f,1000f))
            .assertNone()
    }

    /**
     *
     */
    @Test
    fun `self crossing rib out of neck`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 3.0
        val radius = 1.0

        cd().check(BodyModelTest.initByCoords(
            radius, hOffset, hRadius,
            70.0, 50.0,
            54.1, 50.0, // testcase: little right of neck (+0.1)
            100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ), scene)
            .assertNone()
    }

    /**
     * rib "sphere" touches slightly head sphere
     */
    @Test
    fun `self crossing rib in neck`() {
        val scene = TestScene(1000f, 1000f)
        val hOffset = 10.0
        val hRadius = 3.0
        val radius = 1.0

        cd().check(BodyModelTest.initByCoords(
            radius, hOffset, hRadius,
            70.0, 50.0,
            53.9, 50.0, // testcase: little into neck (+0.1)
            100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ), scene)
            .assertSelf()
    }
}