/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.renderer;

import static org.oscim.renderer.bucket.RenderBucket.BITMAP;
import static org.oscim.renderer.bucket.RenderBucket.LINE;
import static org.oscim.renderer.bucket.RenderBucket.MESH;
import static org.oscim.renderer.bucket.RenderBucket.POLYGON;
import static org.oscim.renderer.bucket.RenderBucket.SYMBOL;
import static org.oscim.renderer.bucket.RenderBucket.TEXLINE;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.bucket.BitmapBucket;
import org.oscim.renderer.bucket.HairLineBucket;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.LineTexBucket;
import org.oscim.renderer.bucket.MeshBucket;
import org.oscim.renderer.bucket.PolygonBucket;
import org.oscim.renderer.bucket.RenderBucket;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.renderer.bucket.TextureBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class to use the renderer.elements for drawing.
 * 
 * All methods that modify 'buckets' MUST be synchronized!
 */
public abstract class BucketRenderer extends LayerRenderer {

	public static final Logger log = LoggerFactory.getLogger(BucketRenderer.class);

	/**
	 * Use mMapPosition.copy(position) to keep the position for which
	 * the Overlay is *compiled*. NOTE: required by setMatrix utility
	 * functions to draw this layer fixed to the map
	 */
	protected MapPosition mMapPosition;

	/** Wrap around dateline */
	protected boolean mFlipOnDateLine = true;

	/** Buckets for rendering */
	public final RenderBuckets buckets;

	public BucketRenderer() {
		buckets = new RenderBuckets();
		mMapPosition = new MapPosition();
	}

	/**
	 * Render all 'buckets'
	 */
	@Override
	protected synchronized void render(GLViewport v) {
		MapPosition layerPos = mMapPosition;

		buckets.bind();

		GLState.test(false, false);
		GLState.blend(true);

		float div = (float) (v.pos.scale / layerPos.scale);

		RenderBucket b = buckets.getBaseBuckets();

		if (b != null)
			setMatrix(v, true);

		while (b != null) {
			if (b.type == POLYGON) {
				b = PolygonBucket.Renderer.draw(b, v, 1, true);
				continue;
			}
			if (b.type == LINE) {
				b = LineBucket.Renderer.draw(b, v, div, buckets);
				continue;
			}
			if (b.type == TEXLINE) {
				b = LineTexBucket.Renderer.draw(b, v, div, buckets);
				// rebind
				buckets.ibo.bind();
				continue;
			}
			if (b.type == MESH) {
				b = MeshBucket.Renderer.draw(b, v);
				continue;
			}
			if (b.type == RenderBucket.HAIRLINE) {
				b = HairLineBucket.Renderer.draw(b, v);
				continue;
			}

			log.error("invalid bucket {}", b.type);
			break;
		}

		b = buckets.getTextureBuckets();
		if (b != null)
			setMatrix(v, false);
		while (b != null) {
			if (b.type == BITMAP) {
				b = BitmapBucket.Renderer.draw(b, v, 1, 1);
				continue;
			}
			if (b.type == SYMBOL) {
				b = TextureBucket.Renderer.draw(buckets, b, v, div);
				continue;
			}
			log.error("invalid bucket {}", b.type);
			break;
		}
	}

	/**
	 * Compile all buckets into one BufferObject. Sets renderer to be ready
	 * when successful. When no data is available (buckets.countVboSize() == 0)
	 * then BufferObject will be released and buckets will not be rendered.
	 */
	protected synchronized void compile() {
		boolean ok = buckets.compile(true);
		setReady(ok);
	}

	/**
	 * Utility: Set matrices.mvp matrix relative to the difference of current
	 * MapPosition and the last updated Overlay MapPosition.
	 * Use this to 'stick' your layer to the map. Note: Vertex coordinates
	 * are assumed to be scaled by MapRenderer.COORD_SCALE (== 8).
	 * 
	 * @param v
	 *            GLViewport
	 * @param project
	 *            if true apply view- and projection, or just view otherwise.
	 */
	protected void setMatrix(GLViewport v, boolean project) {
		MapPosition oPos = mMapPosition;

		double tileScale = Tile.SIZE * v.pos.scale;

		double x = oPos.x - v.pos.x;
		double y = oPos.y - v.pos.y;

		if (mFlipOnDateLine) {
			//wrap around date-line
			while (x < 0.5)
				x += 1.0;
			while (x > 0.5)
				x -= 1.0;
		}

		v.mvp.setTransScale((float) (x * tileScale),
		                    (float) (y * tileScale),
		                    (float) (v.pos.scale / oPos.scale)
		                            / MapRenderer.COORD_SCALE);

		v.mvp.multiplyLhs(project ? v.viewproj : v.view);
	}

	/**
	 * Utility: Set matrices.mvp matrix relative to the difference of current
	 * MapPosition and the last updated Overlay MapPosition and applies
	 * view-projection-matrix.
	 */
	protected void setMatrix(GLViewport v) {
		setMatrix(v, true);
	}
}
