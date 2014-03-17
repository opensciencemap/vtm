precision mediump float;
uniform vec2 u_pixel;
attribute vec4 a_pos;
varying vec2 tex_pos;

void
main()
{
  gl_Position = a_pos;
  tex_pos = (a_pos.xy + 1.0) * 0.5;
  //tex_pos.zw = tex_pos.xz - u_pixel * vec2 (0.5);
}
§
precision mediump float;
//uniform sampler2D m_Texture;
//uniform vec2 g_Resolution;
//uniform vec2 g_Resolution;
uniform sampler2D u_texColor;
uniform vec2 u_pixel;
varying vec2 tex_pos;

//uniform float m_VxOffset;
//uniform float m_SpanMax;
//uniform float m_ReduceMul;

const vec2 m_SpanMax = vec2(2.5, 2.5);
const float m_ReduceMul = 0.15;

//varying vec2 texCoord;
//varying vec4 pos;

//#define FxaaTex(t, p) texture2D(t, p)
//#define OffsetVec(a, b) vec2(a, b)
//#define FxaaTexOff(t, p, o, r) texture2D(t, p + o * r)
// Output of FxaaVertexShader interpolated across screen.
// Input texture.
// Constant {1.0/frameWidth, 1.0/frameHeight}.

#define FXAA_REDUCE_MIN   (1.0/128.0)

//float FxaaLuma(float3 rgb) {
//  return rgb.g * (0.587/0.299) + rgb.b;
//}

vec4
FxaaPixelShader(vec2 pos, sampler2D tex, vec2 pixel){

  //#define FXAA_REDUCE_MUL   (1.0/8.0)
  //#define FXAA_SPAN_MAX     8.0

  vec3 rgbNW = texture2D(tex, pos + vec2(-1.0, -1.0) * pixel).xyz;
//	vec3 rgbNE = FxaaTexOff(tex, pos.zw, OffsetVec(1,0), pixel.xy).xyz;
//	vec3 rgbSW = FxaaTexOff(tex, pos.zw, OffsetVec(0,1), pixel.xy).xyz;
//	vec3 rgbSE = FxaaTexOff(tex, pos.zw, OffsetVec(1,1), pixel.xy).xyz;

  vec3 rgbNE = texture2D(tex, pos + vec2(1.0, -1.0) * pixel).xyz;
  vec3 rgbSW = texture2D(tex, pos + vec2(-1.0, 1.0) * pixel).xyz;
  vec3 rgbSE = texture2D(tex, pos + vec2(1.0, 1.0) * pixel).xyz;

  vec3 rgbM = texture2D(tex, pos).xyz;

  //return vec4(rgbM - (rgbNW + rgbNE)*0.25,1.0);

  vec3 luma = vec3(0.299, 0.587, 0.114);
  float lumaNW = dot(rgbNW, luma);
  float lumaNE = dot(rgbNE, luma);
  float lumaSW = dot(rgbSW, luma);
  float lumaSE = dot(rgbSE, luma);
  float lumaM = dot(rgbM, luma);

  float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
  float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));

  //vec4 rgb = texture2D (tex, pos);
  //return vec4(0.5 + lumaM - lumaMin, lumaM, 0.5 + lumaM - lumaMax, 1.0) * rgb.a;

  vec2 dir;
  dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
  dir.y = ((lumaNW + lumaSW) - (lumaNE + lumaSE));

  //return vec4(dir*0.5, 0.5, 1.0);

  float dirReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * m_ReduceMul), FXAA_REDUCE_MIN);

  float rcpDirMin = 1.0 / (dirReduce + min(abs(dir.x), abs(dir.y)));

  dir = min(m_SpanMax, max(-m_SpanMax, dir * rcpDirMin)) * pixel;

  vec4 rgbA = 0.5 * (texture2D(tex, pos + dir * vec2(1.0 / 3.0 - 0.5))
                   + texture2D(tex, pos + dir * vec2(2.0 / 3.0 - 0.5)));

  vec4 rgbB = rgbA * 0.5 + 0.25 * (texture2D(tex, pos + dir * vec2(0.0 / 3.0 - 0.5))
                                 + texture2D(tex, pos + dir * vec2(3.0 / 3.0 - 0.5)));

  float lumaB = dot(rgbB.xyz, luma.xyz);

  float d = step(lumaB, lumaMin) + step(lumaMax, lumaB);

  return (1.0 - d) * rgbB + d * rgbA;

  //  if ((lumaB < lumaMin) || (lumaB > lumaMax))
  //    { return rgbA; } else { return rgbB; }
}

void
main(){
  //vec2 rcpFrame = vec2(1.0) / g_Resolution;
  //gl_FragColor = vec4(FxaaPixelShader(pos, m_Texture, rcpFrame), 1.0);
  gl_FragColor = FxaaPixelShader(tex_pos, u_texColor, u_pixel * 3.15);
  //gl_FragColor = FxaaPixelShader (tex_pos, u_texColor, u_pixel * 2.35);

  //vec4 c = texture2D (u_texColor, tex_pos);
  //gl_FragColor = vec4 (c.rgb * 0.5, 1.0) + vec4(FxaaPixelShader (tex_pos, u_texColor, u_pixel * 3.15).rgb - c.rgb, 1.0) * 2.0;

  //gl_FragColor = 0.2*c + (FxaaPixelShader (tex_pos, u_texColor, u_pixel * 1.2)) * 0.8;
}
