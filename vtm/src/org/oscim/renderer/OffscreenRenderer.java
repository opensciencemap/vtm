package org.oscim.renderer;

import java.nio.IntBuffer;

import org.oscim.backend.GL20;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OffscreenRenderer extends LayerRenderer {
	final static Logger log = LoggerFactory.getLogger(OffscreenRenderer.class);

	int fb;
	int renderTex;
	int renderDepth;

	int texW = -1;
	int texH = -1;

	boolean initialized;
	private float[] mClearColor = { 0, 0, 0, 0 };

	private boolean useDepthTexture = false;

	static class Shader extends GLShader {
		int aPos, uTexDepth, uTexColor, uPixel;

		Shader(String shaderFile) {
			if (!create(shaderFile))
				return;
			aPos = getAttrib("a_pos");
			uTexColor = getUniform("u_texColor");
			uTexDepth = getUniform("u_tex");
			uPixel = getUniform("u_pixel");
		}
	}

	protected boolean setupFBO(GLViewport viewport) {
		IntBuffer buf = MapRenderer.getIntBuffer(1);

		texW = (int) viewport.getWidth();
		texH = (int) viewport.getHeight();

		GL.glGenFramebuffers(1, buf);
		fb = buf.get(0);

		buf.clear();
		GL.glGenTextures(1, buf);
		renderTex = buf.get(0);

		GLUtils.checkGlError("0");

		GL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, fb);

		// generate color texture
		GL.glBindTexture(GL20.GL_TEXTURE_2D, renderTex);

		GLUtils.setTextureParameter(GL20.GL_NEAREST,
		                            GL20.GL_NEAREST,
		                            GL20.GL_CLAMP_TO_EDGE,
		                            GL20.GL_CLAMP_TO_EDGE);

		GL.glTexImage2D(GL20.GL_TEXTURE_2D, 0,
		                GL20.GL_RGBA, texW, texH, 0, GL20.GL_RGBA,
		                GL20.GL_UNSIGNED_BYTE, null);

		GL.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER,
		                          GL20.GL_COLOR_ATTACHMENT0,
		                          GL20.GL_TEXTURE_2D,
		                          renderTex, 0);
		GLUtils.checkGlError("1");

		if (useDepthTexture) {
			buf.clear();
			GL.glGenTextures(1, buf);
			renderDepth = buf.get(0);
			GL.glBindTexture(GL20.GL_TEXTURE_2D, renderDepth);
			GLUtils.setTextureParameter(GL20.GL_NEAREST,
			                            GL20.GL_NEAREST,
			                            GL20.GL_CLAMP_TO_EDGE,
			                            GL20.GL_CLAMP_TO_EDGE);

			GL.glTexImage2D(GL20.GL_TEXTURE_2D, 0,
			                GL20.GL_DEPTH_COMPONENT,
			                texW, texH, 0,
			                GL20.GL_DEPTH_COMPONENT,
			                GL20.GL_UNSIGNED_SHORT, null);

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

		GLUtils.checkGlError("2");

		int status = GL.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);
		GL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0);
		GL.glBindTexture(GL20.GL_TEXTURE_2D, 0);

		if (status != GL20.GL_FRAMEBUFFER_COMPLETE) {
			log.debug("invalid framebuffer! " + status);
			return false;
		}
		return true;
	}

	static void init(GL20 gl20) {
		GL = gl20;
		shaders[0] = new Shader("post_fxaa");
		shaders[1] = new Shader("post_ssao");
		shaders[2] = new Shader("post_combined");
	}

	static Shader[] shaders = new Shader[3];

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
	public void update(GLViewport viewport) {
		if (texW != viewport.getWidth() || texH != viewport.getHeight())
			setupFBO(viewport);

		mRenderer.update(viewport);
		setReady(mRenderer.isReady());
	}

	@Override
	public void render(GLViewport viewport) {
		GL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, fb);
		GL.glViewport(0, 0, texW, texH);
		GL.glDepthMask(true);
		GLState.setClearColor(mClearColor);
		GL.glClear(GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT);

		mRenderer.render(viewport);

		GL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0);

		Shader s = shaders[0];
		s.useProgram();

		/* bind depth texture */
		if (useDepthTexture) {
			GL.glActiveTexture(GL20.GL_TEXTURE1);
			GLState.bindTex2D(renderDepth);
			GL.glUniform1i(s.uTexDepth, 1);
			GL.glActiveTexture(GL20.GL_TEXTURE0);
		}
		/* bind color texture */
		GLState.bindTex2D(renderTex);
		GL.glUniform1i(s.uTexColor, 0);

		MapRenderer.bindQuadVertexVBO(s.aPos, true);

		GL.glUniform2f(s.uPixel,
		               (float) (1.0 / texW * 0.5),
		               (float) (1.0 / texH * 0.5));

		GLState.enableVertexArrays(s.aPos, -1);

		GLState.test(false, false);
		GLState.blend(true);
		GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);
		GLUtils.checkGlError("....");
	}
}
