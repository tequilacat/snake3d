package tequilacat.org.snake3d.playfeature

import tequilacat.org.snake3d.playfeature.glutils.IDirectedSection
import kotlin.math.*

interface IBodyCollisionModel {
    val faceSection: IDirectedSection

    val headOffset: Double
    val headRadius: Double
    val lengthTailToFace: Double

    val bodySections: Sequence<IDirectedSection>
}

class CollisionDetector {

    companion object {
        private val NO_COLLISION: Collision = Collision(CollisionType.NONE, null)
        private val SELF_COLLISION: Collision = Collision(CollisionType.SELF, null)
        private val WALL_COLLISION: Collision = Collision(CollisionType.WALL, null)
    }

    enum class CollisionType {
        NONE, SELF, WALL, GAMEOBJECT;
    }

    data class Collision(val type: CollisionType, val fieldObject: IFieldObject?){
        val isColliding = type != CollisionType.NONE
    }

    private fun collidesHead(fieldObject: IFieldObject): Boolean {
        return collidesHead(
            fieldObject.centerX.toDouble(),
            fieldObject.centerY.toDouble(),
            fieldObject.radius.toDouble()
        )
    }

    /**
     * whether the point with radius touches the head "neck" or head sphere
     */
    private fun collidesHead(cx: Double, cy: Double, objR: Double): Boolean {
        // check how near to the face ring->head center line:
        // rotate obj center around last ring center, check distance to ring->head centers
        // translate to face CS
        val face = collisionData.faceSection
        val offX = cx - face.dCenterX
        val offY = cy - face.dCenterY

        // rotate around face center
        val rotX = offX * collisionData.headDirectionCosinus - offY * (-collisionData.headDirectionSinus)
        val rotY = offX * (-collisionData.headDirectionSinus) + offY * collisionData.headDirectionCosinus

        // after rotation, behind face center or ahead of head sphere
        val headOffset = collisionData.headOffset
        val headRadius = collisionData.headRadius

        if(rotX < -objR || rotX >  headOffset + headRadius + objR)
            return false

        // Test if between face and head we're close to neck than radius
        // within neck (face-head segment): find radius at coords of tested obj
        // foolproof for head offset 0
        val neckR = face.dRadius + if (headOffset == 0.0) 0.0 else (headRadius - face.dRadius) * rotX / headOffset
        // only if center is within neck
        if (abs(rotY) - objR < neckR && rotX >= 0.0 && rotX < headOffset)
            return true

        val dx=cx - collisionData.dHeadX
        val dy = cy - collisionData.dHeadY
        val headAndObj = objR + headRadius
        // don't use square root, compare squares
        if (dx * dx + dy * dy < headAndObj * headAndObj) {
            return true
        }
//        if(hypot(cx - headX, cy - headY) < objR + headRadius){}
//            return true

        return false
    }

    /**
     * whether line crosses. we don't check if line ends touch cone (checked elsewhere)
     * only whether the line crosses "neck" axis between face ring and head sphere,
     *   or ahead of head sphere, accounting for segment thickness (extrapolated between r0 and r1)
     *
     */
    private fun crossesHead(x0: Double, y0: Double, r0: Double, x1: Double, y1: Double, r1: Double): Boolean {
        val face = collisionData.faceSection
        val offX0 = x0 - face.dCenterX
        val offY0 = y0 - face.dCenterY

        // rotate around face center
        val rotX0 = offX0 * collisionData.headDirectionCosinus -  offY0 * (-collisionData.headDirectionSinus)
        val rotY0 = offX0 * (-collisionData.headDirectionSinus) + offY0 * collisionData.headDirectionCosinus


        val offX1 = x1 - face.dCenterX
        val offY1 = y1 - face.dCenterY

        // rotate around face center
        val rotX1 = offX1 * collisionData.headDirectionCosinus -  offY1 * (-collisionData.headDirectionSinus)
        val rotY1 = offX1 * (-collisionData.headDirectionSinus) + offY1 * collisionData.headDirectionCosinus

        // if diff signs of Y they]re on different sides,
        val tolerance = 0.0001

        // when coaxial we test if both are within
        if(abs(rotY0) < tolerance && abs(rotY1) < tolerance) {
            val xMin = min(rotX0, rotX1)
            val xMax = max(rotX0, rotX1)
            return (xMax >= 0.0 && xMin <= collisionData.headOffset + collisionData.headRadius)
        }

        // on same side (the 0 is checked above
        if (sign(rotY0) == sign(rotY1)) {
            return false
        }

        // now points are on diff sides:
        // look for cross coordinate on 0x and check if it is within neck and head sphere

        val factor = abs(rotY0 / (rotY1 - rotY0))
        val crossX = rotX0 + (rotX1 - rotX0) * factor
        val crossR = r0 + (r1 - r0) * factor

        return crossX >= 0 && crossX - crossR < collisionData.headOffset + collisionData.headRadius
    }

    private class CollisionBodyData {
        var dHeadX: Double = 0.0
        var dHeadY: Double = 0.0
        var headDirectionSinus: Double = 0.0
        var headDirectionCosinus: Double = 0.0

        var headOffset: Double = 0.0
        var headRadius: Double = 0.0

        lateinit var faceSection: IDirectedSection

        fun init(collisionModel: IBodyCollisionModel) {
            faceSection = collisionModel.faceSection
            headOffset = collisionModel.headOffset
            headRadius = collisionModel.headRadius

            headDirectionSinus = sin(collisionModel.faceSection.dAlpha)
            headDirectionCosinus = cos(collisionModel.faceSection.dAlpha)

            dHeadX = collisionModel.faceSection.dCenterX + headDirectionCosinus * collisionModel.headOffset
            dHeadY = collisionModel.faceSection.dCenterY + headDirectionSinus * collisionModel.headOffset
        }
    }

    private val collisionData = CollisionBodyData()

    fun check(collisionModel: IBodyCollisionModel, gameScene: IGameScene) : Collision {
        collisionData.init(collisionModel)

        val face = collisionModel.faceSection
        val headRadius = collisionModel.headRadius

        if (collisionData.dHeadX + headRadius > gameScene.fieldWidth || collisionData.dHeadX - headRadius < 0
            || collisionData.dHeadY + headRadius > gameScene.fieldHeight || collisionData.dHeadY - headRadius < 0) {
            return WALL_COLLISION
        }

        val obj = gameScene.fieldObjects.firstOrNull(::collidesHead)

        if (obj != null) {
            return Collision(CollisionType.GAMEOBJECT, obj)
        }

        var remainingLenToFace = collisionModel.lengthTailToFace
        var prevSection: IDirectedSection? = null

        for (section in collisionModel.bodySections) {
            //val section = floatSection
            remainingLenToFace -= section.dPrevLength
            // if too close break it
            if(remainingLenToFace < face.dRadius * 2) {
                break
            }

            // check if segment center(with radus) collides head
            if (collidesHead(section.dCenterX, section.dCenterY, section.dRadius)) {
                return SELF_COLLISION
            }

            // check if segment between this and prev section crosses head
            if(prevSection != null && crossesHead(section.dCenterX, section.dCenterY, section.dRadius,
                    prevSection.dCenterX, prevSection.dCenterY, prevSection.dRadius)) {
                return SELF_COLLISION
            }

            prevSection = section
        }

        return NO_COLLISION
    }

}