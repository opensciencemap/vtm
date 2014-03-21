package org.oscim.renderer;

import java.nio.IntBuffer;

import org.oscim.backend.GL20;
import org.oscim.layers.tile.s3db.S3DBLayer.S3DBRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OffscreenRenderer extends LayerRenderer {
	final static Logger log = LoggerFactory.getLogger(OffscreenRenderer.class);

	int fb;
	//int depthRb;
	int renderTex;
	int renderDepth;

	int texW;
	int texH;

	public OffscreenRenderer(int w, int h) {
		texW = w;
		texH = h;
		//texW = nearestPowerOf2(texW);
		//texH = nearestPowerOf2(texH);
	}

	int nearestPowerOf2(int x) {
		--x;
		x |= x >> 1;
		x |= x >> 2;
		x |= x >> 4;
		x |= x >> 8;
		x |= x >> 16;
		return ++x;
	}

	boolean initialized;

	protected boolean setup1(GLViewport viewport) {
		IntBuffer buf = MapRenderer.getIntBuffer(1);

		texW = (int) viewport.getWidth();
		texH = (int) viewport.getHeight();
		//texW = nearestPowerOf2(texW);
		//texH = nearestPowerOf2(texH);

		GL.glGenFramebuffers(1, buf);
		fb = buf.get(0);

		//		buf.clear();
		//		GL.glGenRenderbuffers(1, buf);
		//		depthRb = buf.get(0);

		buf.clear();
		GL.glGenTextures(1, buf);
		renderTex = buf.get(0);

		buf.clear();
		GL.glGenTextures(1, buf);
		renderDepth = buf.get(0);

		GL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, fb);

		// generate color texture
		GL.glBindTexture(GL20.GL_TEXTURE_2D, renderTex);
		GLUtils.checkGlError("0");

		GL.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
		GL.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
		GL.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
		GL.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);

		GL.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA,
		                texW, texH, 0, GL20.GL_RGBA,
		                GL20.GL_UNSIGNED_BYTE, null);

		GL.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER,
		                          GL20.GL_COLOR_ATTACHMENT0,
		                          GL20.GL_TEXTURE_2D,
		                          renderTex, 0);

		GLUtils.checkGlError("00");

		GL.glBindTexture(GL20.GL_TEXTURE_2D, renderDepth);

		GL.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
		GL.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
		GL.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
		GL.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);
		GLUtils.checkGlError("1");

		GL.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_DEPTH_COMPONENT,
		                texW, texH, 0, GL20.GL_DEPTH_COMPONENT,
		                GL20.GL_UNSIGNED_SHORT, null);

		GLUtils.checkGlError("11 " + texW + "  / " + texH);

		GL.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER,
		                          GL20.GL_DEPTH_ATTACHMENT,
		                          GL20.GL_TEXTURE_2D,
		                          renderDepth, 0);

		GLUtils.checkGlError("111");
		// create render buffer and bind 16-bit depth buffer
		//GL.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthRb);

		//GL.glRenderbufferStorage(GL20.GL_RENDERBUFFER,
		//                         GL20.GL_DEPTH_COMPONENT16,
		//                         texW,
		//                         texH);

		GLUtils.checkGlError("2");

		GLUtils.checkGlError("3");

		//GL.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER,
		//                          GL20.GL_COLOR_ATTACHMENT0,
		//                          GL20.GL_TEXTURE_2D,
		//                          renderTex, 0);
		//
		//GL.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER,
		//                             GL20.GL_DEPTH_ATTACHMENT,
		//                             GL20.GL_RENDERBUFFER,
		//                             depthRb);

		int status = GL.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);
		if (status != GL20.GL_FRAMEBUFFER_COMPLETE) {
			log.debug("invalid framebuffer!!! " + status);
			return false;
		}

		GL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0);

		//shader = GLUtils.createProgram(vShader, fShader);
		shader = GLShader.createProgram(vShader, fSSAO);

		hTex = GL.glGetUniformLocation(shader, "u_tex");
		hTexColor = GL.glGetUniformLocation(shader, "u_texColor");
		//hScreen = GL.glGetUniformLocation(shader, "u_screen");
		hPixel = GL.glGetUniformLocation(shader, "u_pixel");
		hPos = GL.glGetAttribLocation(shader, "a_pos");

		return true;
	}

	int shader;
	int hPos;
	int hTex;
	int hTexColor;
	//int hScreen;
	int hPixel;

	public void enable(boolean on) {
		if (on)
			GL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, fb);
		else
			GL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0);
	}

	public void begin() {
		GL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, fb);
		GL.glDepthMask(true);
		GL.glClear(GL20.GL_DEPTH_BUFFER_BIT);
	}

	LayerRenderer mRenderer;

	public void setRenderer(LayerRenderer renderer) {
		mRenderer = renderer;
	}

	@Override
	protected void update(GLViewport viewport) {
		if (!initialized) {
			setup1(viewport);
			initialized = true;
		}
		mRenderer.update(viewport);
		//log.debug(">>> ist ready " + mRenderer.isReady());
		setReady(mRenderer.isReady());
	}

	@Override
	protected void render(GLViewport viewport) {
		//begin();
		GL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, fb);
		GL.glViewport(0, 0, texW, texH);
		GL.glDepthMask(true);
		//GL.glViewport(0, 0, this.texW, this.texH);
		GL.glClearColor(0, 0, 0, 0);
		//GL.glDepthRangef(0, 1);
		//GL.glClearDepthf(1);
		GL.glClear(GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT);
		//GL.glClear(GL20.GL_DEPTH_BUFFER_BIT);

		GLUtils.checkGlError("-----");
		((S3DBRenderer) mRenderer).render2(viewport);
		GLUtils.checkGlError("----");

		GL.glViewport(0, 0, (int) viewport.getWidth(), (int) viewport.getHeight());

		GL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0);

		GLUtils.checkGlError("--");

		GLState.useProgram(shader);
		GLUtils.checkGlError("-0");

		// bind the framebuffer texture
		GL.glActiveTexture(GL20.GL_TEXTURE1);
		GLUtils.checkGlError("-1");

		//GL.glBindTexture(GL20.GL_TEXTURE_2D, renderTex);
		//GL.glBindTexture(GL20.GL_TEXTURE_2D, renderDepth);

		GLState.bindTex2D(renderDepth);
		GL.glUniform1i(hTex, 1);

		GLUtils.checkGlError("-2");

		GL.glActiveTexture(GL20.GL_TEXTURE0);
		GLState.bindTex2D(renderTex);
		GL.glUniform1i(hTexColor, 0);

		MapRenderer.bindQuadVertexVBO(hPos, true);

		//GL.glUniform2f(hScreen, texW, texH);
		GL.glUniform2f(hPixel, (float) (1.0 / texW * 0.5), (float) (1.0 / texH * 0.5));

		GLState.enableVertexArrays(hPos, -1);
		GLUtils.checkGlError("-3");

		GLState.test(false, false);
		GLState.blend(true);
		GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);
		GLUtils.checkGlError("-4");

		//GL.glDepthRangef(1, -1);
		//GL.glClearDepthf(1);

		//GL.glActiveTexture(GL20.GL_TEXTURE1);

	}

	private final static String vShader = ""
	        + "precision highp float;"
	        + "uniform vec2 u_pixel;"
	        + "attribute vec4 a_pos;"
	        + "varying vec2 tex_pos;"

	        + "void main() {"
	        + "  gl_Position = a_pos;"
	        + "  tex_pos = (a_pos.xy + 1.0) * 0.5;"
	        + "}";

	private final static String fSSAO = ""
	        + "precision highp float;"
	        // Depth texture
	        + "uniform sampler2D u_tex;"
	        // Depth texture
	        + "uniform sampler2D u_texColor;"
	        // Random texture
	        //+ "uniform sampler2D rand;"  
	        //+ "uniform vec2 u_screen;"
	        + "uniform vec2 u_pixel;"

	        + "varying vec2 tex_pos;"
	        //
	        // gauss bell center	
	        + "const float gdisplace = 0.2;"
	        //
	        // "vec2 camerarange = vec2(1.0, 8.0);"
	        + "const float nearZ = 1.0;"//camerarange.x;"
	        + "const float farZ = 4.0;" //camerarange.y;"
	        //
	        + "float getDepth(float posZ){"
	        + "	 return (2.0 * nearZ) / (nearZ + farZ - posZ * (farZ - nearZ));"
	        + "}"
	        //
	        + "float compareDepths(in float depth1, in float posZ, inout float far) {"
	        + "  float depth2 = getDepth(posZ);"
	        //   depth difference (0-100)
	        + "	 float diff = (depth1 - depth2) * 100.0;"
	        //   set 'far == 1.0' when 'diff' > 'gdisplace'
	        + "	 far = step(diff, gdisplace);"
	        //   gauss bell width 2, reduce left bell width to avoid self-shadowing
	        + "  float garea = max((1.0 - far) * 2.0, 0.01);"
	        + "	 return pow(2.7182, -2.0 * pow(diff - gdisplace,2.0) / pow(garea, 2.0));"
	        + "}"
	        //
	        + "float addAO(float depth, float x1, float y1, float x2, float y2) {"
	        + "    float ao = 0.0;"

	        + "    float f_11;"
	        + "    float z_11 = texture2D(u_tex, vec2(x1, y1)).x;"
	        + "    float d_11 = compareDepths(depth, z_11, f_11);"

	        + "    float f_12;"
	        + "    float z_12 = texture2D(u_tex, vec2(x1, y2)).x;"
	        + "    float d_12 = compareDepths(depth, z_12, f_12);"

	        + "    float f_21;"
	        + "    float z_21 = texture2D(u_tex, vec2(x2, y1)).x;"
	        + "    float d_21 = compareDepths(depth, z_21, f_21);"

	        + "    float f_22;"
	        + "    float z_22 = texture2D(u_tex, vec2(x2, y2)).x;"
	        + "    float d_22 = compareDepths(depth, z_22, f_22);"

	        + "    ao += (1.0 - step(1.0, x1)) * (1.0 - step(1.0, y1))"
	        + "          * (d_11 + f_11 * (1.0 - d_11) * d_22);"

	        + "    ao += (1.0 - step(1.0, x1)) * step(0.0, y2)"
	        + "          * (d_12 + f_12 * (1.0 - d_12) * d_21);"

	        + "    ao += step(0.0, x2) * (1.0 - step(1.0, y1))"
	        + "          * (d_21 + f_21 * (1.0 - d_21) * d_12);"

	        + "    ao += step(0.0, x2) * step(0.0, y2)"
	        + "          * (d_22 + f_22 * (1.0 - d_22) * d_11);"

	        + "    return ao;"
	        + "}"

	        + "void main(void) {"
	        //   randomization texture:
	        //+ "	 vec2 fres = vec2(20.0, 20.0);"
	        //+ "	vec3 random = texture2D(rand, gl_TexCoord[0].st * fres.xy);"
	        //+ "	random = random * 2.0 - vec3(1.0);"

	        //   initialize stuff:
	        + "  vec4 color = texture2D(u_texColor, tex_pos);"

	        //+ "  vec2 tex_pos = gl_FragCoord.xy / u_screen;"
	        + "	 float depth = getDepth(texture2D(u_tex, tex_pos).x);"
	        +
	        "	 float ao = 0.0;"
	        + "  float x = tex_pos.x;"
	        + "  float y = tex_pos.y;"
	        //   'lookup range in screen pixel'
	        //+ "  float pw = 1.0 / u_screen.x * 0.5;"
	        //+ "  float ph = 1.0 / u_screen.y * 0.5;"

	        + "  float pw = u_pixel.x;"
	        + "  float ph = u_pixel.y;"

	        + "	 for (int i = 0; i < 4; i++) {"
	        + "    float pwByDepth = pw / depth;"
	        + "    float phByDepth = ph / depth;"

	        //      calculate color bleeding and ao:

	        + "    ao += addAO(depth, x + pwByDepth, y + phByDepth,x - pwByDepth, y - phByDepth);"

	        + "    pwByDepth *= 1.2;"
	        + "    phByDepth *= 1.2;"

	        + "    ao += addAO(depth, x + pwByDepth, y, x, y - phByDepth);"

	        //      sample jittering:
	        //+ "		pw += random.x * 0.0007;"
	        //+ "		ph += random.y * 0.0007;"
	        //      increase sampling area:
	        //+ "		pw *= 1.7;"
	        //+ "		ph *= 1.7;"
	        + "		pw *= 1.7;"
	        + "		ph *= 1.7;"

	        + "	 }"
	        //	        //   final values, some adjusting
	        //+ "if ((ao / 32.0) > 1.0){"
	        //+ "	gl_FragColor = vec4(1.0,1.0,ao / 32.0 - 1.0,1.0);"
	        //+ ""
	        //+ "} else {"
	        + "	 vec3 finalAO = vec3(0.25 * pow((ao / 32.0),1.2));"
	        + "	 gl_FragColor = vec4(1.0 - finalAO, 1.0);"
	        //+ "	 gl_FragColor = color - vec4(finalAO, 0.0);"
	        //+ " }"
	        //+ "	 gl_FragColor = vec4(finalAO, 1.0) * texture2D(u_texColor, tex_pos.xy);"
	        //+ "	gl_FragColor = vec4(gl_TexCoord[0].xy,0.0,1.0);"
	        //+ "	gl_FragColor = vec4(tex_pos.xy,0.0,1.0);"
	        //+ "	 gl_FragColor = vec4(gl_FragCoord.xy / u_screen, 0.0, 1.0);"

	        + "}";

	//	private final static String fShader = ""
	//	        + "precision mediump float;"
	//	        + "uniform sampler2D u_tex;"
	//	        + "uniform sampler2D u_texColor;"  // Depth texture 
	//	        + "uniform vec2 u_screen;"
	//	        + "varying vec2 tex_pos;"
	//	        + "void main() {"
	//	        //+ "  vec2 tex_pos = gl_FragCoord.xy / u_screen;"
	//	        + " vec4 c = texture2D(u_tex, tex_pos.xy);"
	//	        + "   gl_FragColor = c * texture2D(u_texColor, tex_pos.xy);"
	//	        + "}";

	//+ "#version 120\n"

}
