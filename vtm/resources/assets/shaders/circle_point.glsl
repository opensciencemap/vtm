#ifdef GLES
precision highp float;
#endif
uniform mat4 u_mvp;
uniform float u_scale;
attribute vec2 a_pos;
void main() {
    gl_Position = u_mvp * vec4(a_pos, 0.0, 1.0);
    gl_PointSize = 2.0 * u_scale / gl_Position.z;
}

$$

#ifdef GL_OES_standard_derivatives
#extension GL_OES_standard_derivatives : enable
#endif
#ifdef GLES
precision highp float;
#endif
uniform vec4 u_color;
void main() {
    vec2 cxy = 2.0 * gl_PointCoord - 1.0;
    float r = dot(cxy, cxy);
    float delta = fwidth(r);
    float alpha = 1.0 - smoothstep(1.0 - delta, 1.0 + delta, r);
    gl_FragColor = u_color * alpha;
}
