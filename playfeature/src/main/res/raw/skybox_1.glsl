uniform mat4 projectionMatrix;
uniform mat4 modelViewMatrix;
attribute vec3 position;
varying mediump vec3 texCoord;

void main() {
    texCoord = position;
    // remove translation by conversion mat4->mat3->mat4.
    // skybox has only 8 vertexes so no perf parm done
    //mat4 gWVP = projectionMatrix * mat4(mat3(modelViewMatrix));
    mat4 gWVP = projectionMatrix * modelViewMatrix;
    vec4 WVP_Pos = gWVP * vec4(position, 1.0);
    gl_Position = WVP_Pos.xyww;
}
