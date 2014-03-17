precision mediump float;
uniform mat4 u_mvp;
attribute vec4 a_pos;
void main() {
	gl_Position = u_mvp * a_pos;
}
§
precision mediump float;
uniform vec4 u_color;
void main() {
	gl_FragColor = u_color;
}

