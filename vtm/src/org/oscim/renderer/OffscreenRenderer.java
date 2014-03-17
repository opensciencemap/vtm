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

	private boolean useDepthTexture = false;

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

		GL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, fb);

		// generate color texture
		GL.glBindTexture(GL20.GL_TEXTURE_2D, renderTex);
		GLUtils.checkGlError("0");

		GL.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
		GL.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
		GL.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
		GL.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);

		GL.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA,
		                texW, texH, 0, GL20.GL_RGBA,
		                GL20.GL_UNSIGNED_BYTE, null);

		GL.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER,
		                          GL20.GL_COLOR_ATTACHMENT0,
		                          GL20.GL_TEXTURE_2D,
		                          renderTex, 0);

		GLUtils.checkGlError("00");

		if (useDepthTexture) {
			buf.clear();
			GL.glGenTextures(1, buf);
			renderDepth = buf.get(0);

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
		} else {
			buf.clear();
			GL.glGenRenderbuffers(1, buf);
			int depthRenderbuffer = buf.get(0);

			GL.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthRenderbuffer);

			GL.glRenderbufferStorage(GL20.GL_RENDERBUFFER,
			                         GL20.GL_DEPTH_COMPONENT16,
			                         texW, texH);

			GL.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER,
			                             GL20.GL_DEPTH_ATTACHMENT,
			                             GL20.GL_RENDERBUFFER,
			                             depthRenderbuffer);
		}

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

		//shader = GLShader.createProgram(vShader, fShader);
		//shader = GLShader.createProgram(vShader, fSSAO);
		//shader = GLShader.createProgram(vShader, fShaderFXAA);
		shader = GLShader.loadShader("post_fxaa");

		//hTex = GL.glGetUniformLocation(shader, "u_tex");
		hTexColor = GL.glGetUniformLocation(shader, "u_texColor");
		//hScreen = GL.glGetUniformLocation(shader, "u_screen");
		hPixel = GL.glGetUniformLocation(shader, "u_pixel");
		hPos = GL.glGetAttribLocation(shader, "a_pos");

		return true;
	}

	int shader;
	int hPos;
	//int hTex;
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
		//GL.glActiveTexture(GL20.GL_TEXTURE1);
		//GLUtils.checkGlError("-1");

		//GLState.bindTex2D(renderDepth);
		//GL.glUniform1i(hTex, 1);
		//GLUtils.checkGlError("-2");

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
}
