package tequilacat.org.snake3d.playfeature.glutils

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.util.Log

open class OGLProgram(vertexShader: String, fragmentShader: String, vararg attNames: String) {
    private fun loadShader(type: Int, shaderCode: String) = GLES20.glCreateShader(type)
        .also { shader ->
            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.e("render", "error compiling shader #$shader: ${glGetShaderInfoLog(shader)}")
                glDeleteShader(shader)
            }

        }

    val id: Int = GLES20.glCreateProgram().also {
        GLES20.glAttachShader(it, loadShader(GLES20.GL_VERTEX_SHADER, vertexShader))
        GLES20.glAttachShader(it, loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader))
        GLES20.glLinkProgram(it)

        for (i in attNames.indices) {
            glBindAttribLocation(it, i, attNames[i])
        }
    }
}


class DefaultProgram: OGLProgram(
    DEF_VERTEX_SHADER_CODE,
    DEF_FRAGMENT_SHADER_CODE
) {

    companion object {
        val DEF_VERTEX_SHADER_CODE = """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;

            void main() {
                gl_Position = uMVPMatrix * aPosition;
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

    val aPosition: Int by lazy { GLES20.glGetAttribLocation(id, "aPosition") }
    val aColor: Int by lazy { GLES20.glGetUniformLocation(id, "vColor") }
    val uMvpMatrix: Int by lazy { GLES20.glGetUniformLocation(id, "uMVPMatrix") }
}

/**
 * adds number of attributes used by sample lighting program used by learnopengles.com
 */
open class LightingProgram(
    vertexShader: String, fragmentShader: String,
    private val attNamesMap: Map<String, String> = mapOf()
) : OGLProgram(vertexShader, fragmentShader) {

    val attMVPMatrixHandle by lazy { glGetUniformLocation(id, "u_MVPMatrix") }
    val attMVMatrixHandle by lazy { glGetUniformLocation(id, "u_MVMatrix") }
    val attLightPosHandle by lazy { glGetUniformLocation(id, "u_LightPos") }
    val attPositionHandle by lazy { glGetAttribLocation(id, "a_Position") }
    val attColorHandle by lazy { glGetUniformLocation(id, "u_Color") }
    val attNormalHandle by lazy { glGetAttribLocation(id, "a_Normal") }
}

/**
 * simpler shading
 */
class GuraudLightProgram(vertexShader: String, fragmentShader: String)  : LightingProgram(vertexShader,fragmentShader) {
constructor() : this(
    """
          uniform mat4 u_MVPMatrix;     // A constant representing the combined model/view/projection matrix.
          uniform mat4 u_MVMatrix;     // A constant representing the combined model/view matrix.
          uniform vec3 u_LightPos;     // The position of the light in eye space.
          uniform vec4 u_Color;

          attribute vec4 a_Position;     // Per-vertex position information we will pass in.

          // attribute vec4 a_Color;     // Per-vertex color information we will pass in.
          attribute vec3 a_Normal;     // Per-vertex normal information we will pass in.

          varying vec4 v_Color;     // This will be passed into the fragment shader.

          void main()     // The entry point for our vertex shader.
          {
        // Transform the vertex into eye space.
             vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);
        // Transform the normal's orientation into eye space.
             vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));
        // Will be used for attenuation.
             // float distance = length(u_LightPos - modelViewVertex);

        // Get a lighting direction vector from the light to the vertex.
             vec3 lightVector = normalize(u_LightPos - modelViewVertex);

             // hardcode in eye space
             // lightVector = normalize( vec3(0.0, 0.0, 10.0) );

        // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
        // pointing in the same direction then it will get max illumination.
             float diffuse = max(dot(modelViewNormal, lightVector), 0.4);


             // avdo: don't use distance in measurements - assume megalight

        // Attenuate the light based on distance.

             // diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));

        // Multiply the color by the illumination level. It will be interpolated across the triangle.
             v_Color = u_Color * diffuse;
        // gl_Position is a special variable used to store the final position.
        // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
             gl_Position = u_MVPMatrix * a_Position;
          }
            """.trimIndent(),

    """
            precision mediump float;
            varying vec4 v_Color;

            void main() {
              gl_FragColor = v_Color;
            }
        """.trimIndent())
}

/**
 * near Phong shading
 */
class SemiPhongProgram(vertexShader: String, fragmentShader: String) :
    LightingProgram(vertexShader, fragmentShader) {
    constructor() : this(
        /// vertex shader
        """
        uniform mat4 u_MVPMatrix;      // A constant representing the combined model/view/projection matrix.
        uniform mat4 u_MVMatrix;       // A constant representing the combined model/view matrix.

        attribute vec4 a_Position;     // Per-vertex position information we will pass in.

        // attribute vec4 a_Color;        // Per-vertex color information we will pass in.
        uniform vec4 u_Color;

        attribute vec3 a_Normal;       // Per-vertex normal information we will pass in.

        varying vec3 v_Position;       // This will be passed into the fragment shader.
        varying vec4 v_Color;          // This will be passed into the fragment shader.
        varying vec3 v_Normal;         // This will be passed into the fragment shader.

        // The entry point for our vertex shader.
        void main()
        {
            // Transform the vertex into eye space.
            v_Position = vec3(u_MVMatrix * a_Position);

            // Pass through the color.
            v_Color = u_Color;

            // Transform the normal's orientation into eye space.
            v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));

            // gl_Position is a special variable used to store the final position.
            // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
            gl_Position = u_MVPMatrix * a_Position;
        }
    """.trimIndent(),

        // fragment shader
        """
        precision mediump float;       // Set the default precision to medium. We don't need as high of a
                                       // precision in the fragment shader.
        uniform vec3 u_LightPos;       // The position of the light in eye space.

        varying vec3 v_Position;       // Interpolated position for this fragment.
        varying vec4 v_Color;          // This is the color from the vertex shader interpolated across the
                                       // triangle per fragment.
        varying vec3 v_Normal;         // Interpolated normal for this fragment.

        // The entry point for our fragment shader.
        void main()
        {

            // Get a lighting direction vector from the light to the vertex.
            vec3 lightVector = normalize(u_LightPos - v_Position);

            // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
            // pointing in the same direction then it will get max illumination.
            float diffuse = max(dot(v_Normal, lightVector), 0.0);

            // Add attenuation.
            // Will be used for attenuation.
            // avdo: do neither-
            // float distance = length(u_LightPos - v_Position);
            // diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));

            // light up
            diffuse = diffuse + 0.3;
            // Multiply the color by the diffuse illumination level to get final output color.
            gl_FragColor = v_Color * diffuse;
        }
    """.trimIndent())
}

class TextureProgram(vertexShader: String, fragmentShader: String) :
    OGLProgram(vertexShader, fragmentShader) {

    val attMVPMatrixHandle by lazy { glGetUniformLocation(id, "u_MVPMatrix") }
    val attMVMatrixHandle by lazy { glGetUniformLocation(id, "u_MVMatrix") }
    val attLightPosHandle by lazy { glGetUniformLocation(id, "u_LightPos") }
    val attPositionHandle by lazy { glGetAttribLocation(id, "a_Position") }
    val attNormalHandle by lazy { glGetAttribLocation(id, "a_Normal") }

    // add new
    val uTexture by lazy { glGetUniformLocation(id, "u_Texture") }
    val aTexCoordinate by lazy { glGetAttribLocation(id, "a_TexCoordinate") }

    // note the vec4 position instead of vec3 as before!

    constructor() : this("""
        uniform mat4 u_MVPMatrix;		// A constant representing the combined model/view/projection matrix.
        uniform mat4 u_MVMatrix;		// A constant representing the combined model/view matrix.

        attribute vec4 a_Position;		// Per-vertex position information we will pass in.
        attribute vec3 a_Normal;		// Per-vertex normal information we will pass in.
        attribute vec2 a_TexCoordinate; // Per-vertex texture coordinate information we will pass in.

        varying vec3 v_Position;		// This will be passed into the fragment shader.
        varying vec3 v_Normal;			// This will be passed into the fragment shader.
        varying vec2 v_TexCoordinate;   // This will be passed into the fragment shader.

        // The entry point for our vertex shader.
        void main()
        {
            // Transform the vertex into eye space.
            v_Position = vec3(u_MVMatrix * a_Position);

            // Pass through the texture coordinate.
            v_TexCoordinate = a_TexCoordinate;

            // Transform the normal's orientation into eye space.
            v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));

            // gl_Position is a special variable used to store the final position.
            // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
            gl_Position = u_MVPMatrix * a_Position;
        }
    """.trimIndent(), """
        precision mediump float;       	// Set the default precision to medium. We don't need as high of a
                                        // precision in the fragment shader.
        uniform vec3 u_LightPos;       	// The position of the light in eye space.
        uniform sampler2D u_Texture;    // The input texture.

        varying vec3 v_Position;		// Interpolated position for this fragment.
        varying vec3 v_Normal;         	// Interpolated normal for this fragment.
        varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.

        // The entry point for our fragment shader.
        void main()
        {
            // Will be used for attenuation.
            float distance = length(u_LightPos - v_Position);

            // Get a lighting direction vector from the light to the vertex.
            vec3 lightVector = normalize(u_LightPos - v_Position);

            // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
            // pointing in the same direction then it will get max illumination.
            float diffuse = max(dot(v_Normal, lightVector), 0.0);

            // Add attenuation.
            diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance)));

            // Add ambient lighting
            diffuse = diffuse + 0.7;

            // Multiply the color by the diffuse illumination level and texture value to get final output color.
            gl_FragColor = (diffuse * texture2D(u_Texture, v_TexCoordinate));
        }

    """.trimIndent())
}

