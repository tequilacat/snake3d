package tequilacat.org.snake3d.playfeature.oglgame

import android.opengl.GLES20


open class OGLProgram(vertexShader: String, fragmentShader: String) {
    val id: Int = GLES20.glCreateProgram().also {
        GLES20.glAttachShader(it, OGLUtils.loadShader(GLES20.GL_VERTEX_SHADER, vertexShader))
        GLES20.glAttachShader(it, OGLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader))
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
