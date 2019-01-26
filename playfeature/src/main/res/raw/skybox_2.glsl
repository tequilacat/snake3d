uniform samplerCube Sampler;
varying mediump vec3 texCoord;

void main() {
    gl_FragColor = textureCube(Sampler, texCoord);
}
