package tequilacat.org.snake3d.playfeature

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

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
     * once tested
     */
    @Test
    fun `test test harness`() {
        val body = body(5.0, 5.0)
        assertEquals((5 - cos(headAngle) * headOffset).toFloat(), body.bodySegments.last().endX, testFloatTolerance)
        assertEquals((5 - sin(headAngle) * headOffset).toFloat(), body.bodySegments.last().endY, testFloatTolerance)
    }

    private fun initByCoords(radius: Double, headOffset: Double, headRadius: Double, vararg coords: Double): BodyModel {
        //val model = this
        val floorZ = 0.0

        var lastX = coords[0]
        var lastY = coords[1]

        val firstSegmentLen = hypot(coords[2]-coords[0], coords[3] - coords[1])
        val model = BodyModel(firstSegmentLen, radius, headOffset, headRadius)

        for (i in 2 until coords.size step 2) {

            val x = coords[i]
            val y = coords[i+1]

            val angle = atan2(y - lastY, x - lastX)
            val length = hypot(y - lastY, x - lastX)

            if(i == 2) {
                model.init(lastX, lastY, floorZ, angle, length)
                // never shrink
                model.feed(1000000.0)
            } else {
                model.advance(length, angle - model.viewDirection)
            }

            lastX = x
            lastY = y
        }

        // self test: all coords must be exactly same - set tail length to extra larg
        assertEquals(coords.size / 2 - 1, model.bodySegments.size)

        val tolerance = 0.001f
        var i = 0
        for(segment in model.bodySegments){
            assertEquals(coords[i].toFloat(), segment.startX, tolerance)
            assertEquals(coords[i+1].toFloat(), segment.startY, tolerance)

            if (i + 2 < coords.size) {
                assertEquals(coords[i + 2].toFloat(), segment.endX, tolerance)
                assertEquals(coords[i + 3].toFloat(), segment.endY, tolerance)
            }

            i += 2
        }

        assertEquals(model.bodySegments.last().endRadius, radius.toFloat(), testFloatTolerance)
        return model
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

    private fun BodyModel.Collision.assertNone() {
        assertEquals(BodyModel.CollisionType.NONE, this.type)
    }

    private fun BodyModel.Collision.assertSelf() {
        assertEquals(BodyModel.CollisionType.SELF, this.type)
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
    fun `self no curves no collisions`() {
        // trivial
        body(5.0, 5.0).checkCollisions(TestScene(100f,100f))
            .assertIs(BodyModel.CollisionType.NONE)

        // test that closest and smallest segments do not trigger collision
        // 11 segments by 0.2r

        BodyModel(1000.0, 1.0, 1.0, 1.0).apply {
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
        }.checkCollisions(TestScene(1000f,1000f))
            .assertNone()
    }


    /**
     * check that big head (expanding behind face ring) does not collide with vertebra behind
     */
    @Test
    fun `self big head does not touch first vertebra`() {
        val totalLen = 9.0
        val headR = 10.0 // HUGE head

        BodyModel(1000.0, 1.0, 0.0, headR).apply {
            init(100.0, 100.0, 10.0, 0.0, totalLen)
        }.checkCollisions(TestScene(1000f,1000f))
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

        initByCoords(
            radius, hOffset, hRadius,
            52.0, 52.0, // out of cone clearly: no collision
            100.0, 50.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertNone()
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


        initByCoords(
            radius, hOffset, hRadius,
            50.0, 48.0 - margin, // ahead of head
            100.0, 50.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertNone()
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

        initByCoords(
            radius, hOffset, hRadius,
            52 + margin, 55.0, // almost touching cone but little outside
            100.0, 50.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertNone()
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

        initByCoords(
            radius, hOffset, hRadius,
            52 - margin, 53.0, // exactly touching cone and little inside
            100.0, 50.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertSelf()
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

        initByCoords(
            radius, hOffset, hRadius,
            50.0, 49.0, // within head
            100.0, 50.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertSelf()
    }

    /**
     * test crossing by segment! not by ends of segment touching head
     */
    @Test
    fun `self crossing neck not touching head`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 2.0
        val radius = 1.0

        //////////////////////
        // Crossing
        initByCoords(
            radius, hOffset, hRadius,
            10.0, 55.0, 20.0, 55.0, // crosses neck without touching head - @y=55
            100.0, 55.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertSelf()
    }

    /**
     ** test crossing by segment! not by ends of segment touching head
     */
    @Test
    fun `self crossing head`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 2.0
        val radius = 1.0

        initByCoords(
            radius, hOffset, hRadius,
            10.0, 48.0, 20.0, 48.0, // crosses head - y = 48
            100.0, 48.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertSelf()
    }

    /**
     * test crossing by segment! not by ends of segment touching head 
     */
    @Test
    fun `self crossing ahead of head`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 2.0
        val radius = 1.0

        initByCoords(
            radius, hOffset, hRadius,
            10.0, 46.0, 20.0, 46.0, // crosses AHEAD of head - y = 46
            100.0, 46.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertNone()
    }

    /**
     * test crossing by segment! not by ends of segment touching head
     */
    @Test
    fun `self crossing behind face ring`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 2.0
        val radius = 1.0

        // here no collisiion - body crosses body, not head - no collision here
        initByCoords(
            radius, hOffset, hRadius,
            10.0, 62.0, 20.0, 62.0, // crosses BEHIND of face - y = 62
            100.0, 62.0, 100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertNone()
    }

    // neck at y=55: [52,55]

    /**
     *
     */
    @Test
    fun `self crossing rib out of neck`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 3.0
        val radius = 1.0

        initByCoords(
            radius, hOffset, hRadius,
            70.0, 50.0,
            54.1, 50.0, // testcase: little right of neck (+0.1)
            100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertNone()
    }

    /**
     * rib "sphere" touches slightly head sphere
     */
    @Test
    fun `self crossing rib in neck`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 3.0
        val radius = 1.0

        initByCoords(
            radius, hOffset, hRadius,
            70.0, 50.0,
            53.9, 50.0, // testcase: little into neck (+0.1)
            100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertSelf()
    }

/*    TODO Especial check that head does not cross any segment

    @Test
    fun `self crossing rib out of head`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 3.0
        val radius = 1.0

        initByCoords(
            radius, hOffset, hRadius,
            50.0, 10.0,
            52.0, 50.0, // testcase: TODO check rib coords
            100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertNone()
    }

    @Test
    fun `self crossing rib IN head`() {
        val scene = TestScene(1000f,1000f)
        val hOffset = 10.0
        val hRadius = 3.0
        val radius = 1.0

        initByCoords(
            radius, hOffset, hRadius,
            70.0, 50.0,
            52.0, 50.0, // testcase: TODO check rib coords
            100.0, 100.0, 50.0, 100.0,
            50.0, 50.0 + hOffset
        ).checkCollisions(scene)
            .assertSelf()
    }*/
}