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