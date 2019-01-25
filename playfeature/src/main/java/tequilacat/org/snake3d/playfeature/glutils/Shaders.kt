package tequilacat.org.snake3d.playfeature.glutils

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.Matrix
import tequilacat.org.snake3d.playfeature.R
/**
 * adds number of attributes used by sample lighting program used by learnopengles.com
 */
open class LightingProgram(vertexShader: String, fragmentShader: String) : OGLProgram(vertexShader, fragmentShader) {
    constructor(context:Context, vertexShaderResId: Int, fragmentShaderResId: Int) :
            this(readResourceText(context, vertexShaderResId), readResourceText(context, fragmentShaderResId))

    val uMVMatrix = uniform("u_MVMatrix")
    val uMVPMatrix = uniform("u_MVPMatrix")
    val uLightPos = uniform("u_LightPos")
    val uColor = uniform("u_Color")

    val aPosition = attr("a_Position")
    val aNormal = attr("a_Normal")
}

/**
 * simpler shading
 */
class GuraudLightProgram(context:Context) : LightingProgram(context, R.raw.guraud_1, R.raw.guraud_2)

/**
 * near Phong shading
 */
class SemiPhongProgram(context:Context) : LightingProgram(context, R.raw.phong_1, R.raw.phong_2)

class ShadedPainter(private val program: LightingProgram) : GeometryPainter {
    private val mMVMatrix = FloatArray(16)
    private val mMVPMatrix = FloatArray(16)

    override fun paint(geometry: Geometry, objectContext: ObjectContext, modelMatrix: FloatArray, sceneContext: SceneContext) {
        // current impl does not paint indexes!

        glUseProgram(program.id)

        //////////////////////////////
        // prepare data

        // set uniform color
        glUniform4fv(program.uColor.id, 1, objectContext.primaryColor, 0)
        glUniform3f(
            program.uLightPos.id, sceneContext.lightPosInEyeSpace[0],
            sceneContext.lightPosInEyeSpace[1], sceneContext.lightPosInEyeSpace[2]
        )

        // uniforms

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVMatrix, 0, sceneContext.viewMatrix, 0, modelMatrix, 0)

        // Pass in the modelview matrix.
        glUniformMatrix4fv(program.uMVMatrix.id, 1, false, mMVMatrix, 0)

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, sceneContext.projectionMatrix, 0, mMVMatrix, 0)

        // Pass in the combined matrix.
        glUniformMatrix4fv(program.uMVPMatrix.id, 1, false, mMVPMatrix, 0)


        ///////////////////////////
        // draw VBOs

        // start position for coords: 0
        // start position for normals:

        glBindBuffer(GL_ARRAY_BUFFER, geometry.vertexBufferId)

        // Bind Attributes
        glVertexAttribPointer(program.aPosition.id, geometry.coordinatesPerVertex, GL_FLOAT,
            false, geometry.vertexStride, geometry.coordBytesOffset)
        glEnableVertexAttribArray(program.aPosition.id)

        glVertexAttribPointer(program.aNormal.id, geometry.coordinatesPerVertex, GL_FLOAT,
            false, geometry.vertexStride, geometry.normalBytesOffset)
        glEnableVertexAttribArray(program.aNormal.id)

        // Draw
        if (geometry.hasIndexes) {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, geometry.indexBufferId)
            glDrawElements(GL_TRIANGLE_STRIP, geometry.indexCount, GL_UNSIGNED_SHORT, 0)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        } else {
            glDrawArrays(GL_TRIANGLES, 0, geometry.vertexCount)
        }

        glDisableVertexAttribArray(program.aPosition.id)
        glDisableVertexAttribArray(program.aNormal.id)
        checkGlErr()

        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }
}


class TextureProgram(context: Context) :
    OGLProgram(context, R.raw.simple_tex_1 , R.raw.simple_tex_2) {

    val uMVMatrix = uniform("u_MVMatrix")
    val uMVPMatrix = uniform("u_MVPMatrix")
    val uLightPos = uniform("u_LightPos")
    //val uColor = uniform("u_Color")

    val aPosition = attr("a_Position")
    val aNormal = attr("a_Normal")

    val uTexture = uniform("u_Texture")
    val aTexCoordinate = attr("a_TexCoordinate")

    // note the vec4 position instead of vec3 as before!
}

class TexturePainter(private val program: TextureProgram) : GeometryPainter {
    private val mMVMatrix = FloatArray(16)
    private val mMVPMatrix = FloatArray(16)

    override fun paint(geometry: Geometry, objectContext: ObjectContext, modelMatrix: FloatArray, sceneContext: SceneContext) {

        glUseProgram(program.id)

        //////////////////////////////
        // prepare data

        // set uniform color
//        glUniform4fv(program.uColor.id, 1, objectContext.primaryColor, 0)
        glUniform3f(
            program.uLightPos.id, sceneContext.lightPosInEyeSpace[0],
            sceneContext.lightPosInEyeSpace[1], sceneContext.lightPosInEyeSpace[2]
        )

        // uniforms

        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVMatrix, 0, sceneContext.viewMatrix, 0, modelMatrix, 0)

        // Pass in the modelview matrix.
        glUniformMatrix4fv(program.uMVMatrix.id, 1, false, mMVMatrix, 0)

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, sceneContext.projectionMatrix, 0, mMVMatrix, 0)

        // Pass in the combined matrix.
        glUniformMatrix4fv(program.uMVPMatrix.id, 1, false, mMVPMatrix, 0)


        /// texture activate
        // Set the active texture unit to texture unit 0.
        glActiveTexture(GL_TEXTURE0);
        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, objectContext.textureId);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        glUniform1i(program.uTexture.id, 0);

        ///////////////////////////
        // draw VBOs


        glBindBuffer(GL_ARRAY_BUFFER, geometry.vertexBufferId)

        // Bind Attributes
        glVertexAttribPointer(program.aPosition.id, geometry.coordinatesPerVertex, GL_FLOAT,
            false, geometry.vertexStride, geometry.coordBytesOffset)
        glEnableVertexAttribArray(program.aPosition.id)

        glVertexAttribPointer(program.aNormal.id, geometry.coordinatesPerVertex, GL_FLOAT,
            false, geometry.vertexStride, geometry.normalBytesOffset)
        glEnableVertexAttribArray(program.aNormal.id)

        glVertexAttribPointer(program.aTexCoordinate.id, geometry.floatsPerTexUV, GL_FLOAT,
            false, geometry.vertexStride, geometry.texUvBytesOffset)
        glEnableVertexAttribArray(program.aTexCoordinate.id)

        // Draw
        if (geometry.hasIndexes) {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, geometry.indexBufferId)
            glDrawElements(GL_TRIANGLE_STRIP, geometry.indexCount, GL_UNSIGNED_SHORT, 0)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        } else {
            glDrawArrays(GL_TRIANGLES, 0, geometry.vertexCount)
        }

        glDisableVertexAttribArray(program.aTexCoordinate.id)
        glDisableVertexAttribArray(program.aPosition.id)
        glDisableVertexAttribArray(program.aNormal.id)
        checkGlErr()

        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }
}
