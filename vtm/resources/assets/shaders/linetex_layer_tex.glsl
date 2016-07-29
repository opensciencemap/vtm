#ifdef GLES
precision mediump float;
#endif
uniform mat4 u_mvp;
uniform vec4 u_color;
uniform float u_pscale;
uniform float u_width;
attribute vec4 a_pos0;
attribute vec4 a_pos1;
attribute vec2 a_len0;
attribute vec2 a_len1;
attribute float a_flip;
varying vec2 v_st;
void
main(){
  vec4 pos;
  if (a_flip == 0.0) {
    //    vec2 dir = u_width * a_pos0.zw;
    pos = vec4(a_pos0.xy + (u_width * a_pos0.zw), 0.0, 1.0);
    v_st = vec2(a_len0.x / u_pscale, 1.0);
  }
  else {
    //    vec2 dir = u_width * a_pos1.zw;
    pos = vec4(a_pos1.xy - (u_width * a_pos1.zw), 0.0, 1.0);
    v_st = vec2(a_len1.x / u_pscale, -1.0);
  }
  gl_Position = u_mvp * pos;
}

$$

#extension GL_OES_standard_derivatives : enable
#ifdef GLES
precision mediump float;
#endif
uniform vec4 u_color;
uniform vec4 u_bgcolor;
uniform float u_pwidth;
varying vec2 v_st;
uniform sampler2D tex;
void
main(){
  vec4 c=texture2D(tex,vec2(abs(mod(v_st.s+1.0,2.0)),(v_st.t+1.0)*0.5));
  float fuzz=fwidth(c.a);
  gl_FragColor=(c * u_color) *smoothstep(0.5-fuzz,0.5+fuzz,c.a);
}
