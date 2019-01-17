package tequilacat.org.snake3d.playfeature.glutils

import android.opengl.GLES20.*
import android.opengl.Matrix
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI


class DrawContext {
    /** screen dimension and frustum */
    val projectionMatrix = FloatArray(16)

    /** view according to eye position */
    val viewMatrix = FloatArray(16)

    /** projection view matrix to screen also according to eye position*/
    val viewProjectionMatrix = FloatArray(16)

    /** Location of light source in scene*/
    val lightPosGlobal = FloatArray(4)

    val lightPosInEyeSpace = FloatArray(4)
}

interface Drawable {
    fun position(x: Float, y: Float, z: Float, rotateAngle: Float)
    fun draw(drawContext: DrawContext)
}

class FlatGeometryPainter(private val geometry: TriangleGeometry,
                          private val color: FloatArray,
                          private val program: DefaultProgram
) :
    Drawable {

    private val mMVMatrix = FloatArray(16)
    private val mMVPMatrix = FloatArray(16)

    private val modelMatrix = FloatArray(16).also {
        Matrix.setIdentityM(it, 0)
    }

    /** do nothing - current impl assumes abs coords, not model coords */
    override fun position(x: Float, y: Float, z: Float, rotateAngle: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, (rotateAngle * 180 / PI).toFloat(), 0.0f, 0.0f, 1.0f)
    }

    override fun draw(drawContext: DrawContext) {
        glUseProgram(program.id)

        glEnableVertexAttribArray(program.aPosition)
        glVertexAttribPointer(
            program.aPosition,
            TriangleGeometry.COORDS_PER_VERTEX,
            GL_FLOAT, false,
            TriangleGeometry.VERTEX_STRIDE, geometry.vertexes)

        glUniform4fv(program.aColor, 1, color, 0)


        // translate modelMatrix according to viewProjectionMatrix
        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVMatrix, 0, drawContext.viewMatrix, 0, modelMatrix, 0)
        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, drawContext.projectionMatrix, 0, mMVMatrix, 0)
        // Pass in the combined matrix.
        glUniformMatrix4fv(program.uMvpMatrix, 1, false, mMVPMatrix, 0)


        glDrawElements(GL_TRIANGLES, geometry.indexCount,
            GL_UNSIGNED_SHORT, geometry.indexes)

        // Disable vertex array
        glDisableVertexAttribArray(program.aPosition)
    }
}

class ShadedGeometryPainter(private val geometry: TriangleGeometryWithNormals,
                            private val colorArray: FloatArray,
                            private val program: LightingProgram
) : Drawable {

    private val modelMatrix = FloatArray(16).also {
        Matrix.setIdentityM(it, 0)
    }

    private val mMVMatrix = FloatArray(16)
    private val mMVPMatrix = FloatArray(16)

    /**
     * coords are in model space. translates to world and displays shaded
     */
    override fun position(x: Float, y: Float, z: Float, rotateAngle: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, (rotateAngle * 180 / PI).toFloat(), 0.0f, 0.0f, 1.0f)
    }

    override fun draw(drawContext: DrawContext) {
        glUseProgram(program.id)


        // set uniform color
        glUniform4fv(program.attColorHandle, 1, colorArray, 0)
        glUniform3f(
            program.attLightPosHandle, drawContext.lightPosInEyeSpace[0],
            drawContext.lightPosInEyeSpace[1], drawContext.lightPosInEyeSpace[2]
        )

        // uniforms

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVMatrix, 0, drawContext.viewMatrix, 0, modelMatrix, 0)

        // Pass in the modelview matrix.
        glUniformMatrix4fv(program.attMVMatrixHandle, 1, false, mMVMatrix, 0)

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, drawContext.projectionMatrix, 0, mMVMatrix, 0)

        // Pass in the combined matrix.
        glUniformMatrix4fv(program.attMVPMatrixHandle, 1, false, mMVPMatrix, 0)

        if(geometry.indexCount > 0)
            drawIndexed()
        else
            drawTriangles()
    }

    private fun drawTriangles() {
        glEnableVertexAttribArray(program.attNormalHandle)

        geometry.normals.position(0)
        glVertexAttribPointer(program.attNormalHandle, 3 /*mNormalDataSize*/, GL_FLOAT, false,
            0, geometry.normals) // 3 coords * 4 bytes per float


        glEnableVertexAttribArray(program.attPositionHandle)
        glVertexAttribPointer(
            program.attPositionHandle,
            TriangleGeometry.COORDS_PER_VERTEX,
            GL_FLOAT, false,
            TriangleGeometry.VERTEX_STRIDE, geometry.vertexes)

        // Draw the square
        glDrawArrays(GL_TRIANGLES, 0, geometry.vertexes.capacity() / 3) // n floats / 3 = nVertexes

        // Disable vertex array
        //glDisableVertexAttribArray(program.attPositionHandle)
    }

    private fun drawIndexed() {

        glEnableVertexAttribArray(program.attNormalHandle)

        geometry.normals.position(0)
        glVertexAttribPointer(program.attNormalHandle, 3 /*mNormalDataSize*/, GL_FLOAT, false,
            0, geometry.normals)


        glEnableVertexAttribArray(program.attPositionHandle)
        glVertexAttribPointer(
            program.attPositionHandle,
            TriangleGeometry.COORDS_PER_VERTEX,
            GL_FLOAT, false,
            TriangleGeometry.VERTEX_STRIDE, geometry.vertexes)

        // Draw the square
        glDrawElements(GL_TRIANGLES, geometry.indexCount,
            GL_UNSIGNED_SHORT, geometry.indexes)

        // Disable vertex array
        glDisableVertexAttribArray(program.attPositionHandle)
    }
}

