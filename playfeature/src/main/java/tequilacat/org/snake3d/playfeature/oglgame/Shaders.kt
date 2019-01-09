package tequilacat.org.snake3d.playfeature.oglgame

import android.opengl.GLES20
import android.util.Log


open class OGLProgram(vertexShader: String, fragmentShader: String) {
    private fun loadShader(type: Int, shaderCode: String) = GLES20.glCreateShader(type)
        .also { shader ->
            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.e("render", "error compiling shader $shader")
                // glDeleteShader(shader)
            }
        }

    val id: Int = GLES20.glCreateProgram().also {
        GLES20.glAttachShader(it, loadShader(GLES20.GL_VERTEX_SHADER, vertexShader))
        GLES20.glAttachShader(it, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader))
        GLES20.glLinkProgram(it)
    }
}

class DefaultProgram(vertexShader: String, fragmentShader: String)
    : OGLProgram(vertexShader, fragmentShader) {
    constructor() : this(DEF_VERTEX_SHADER_CODE, DEF_FRAGMENT_SHADER_CODE) {}

    companion object {
        val DEF_VERTEX_SHADER_CODE = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """.trimIndent()

        val DEF_FRAGMENT_SHADER_CODE = """
            precision mediump float;
            uniform vec4 vColor;
            void main() {
              gl_FragColor = vColor;
            }
        """.trimIndent()
    }

    val positionHandle: Int by lazy { GLES20.glGetAttribLocation(id, "vPosition") }

    val colorHandle: Int by lazy { GLES20.glGetUniformLocation(id, "vColor") }

    val mvpMatrixHandle: Int by lazy { GLES20.glGetUniformLocation(id, "uMVPMatrix") }
}
