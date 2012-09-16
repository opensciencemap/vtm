/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.oscim.theme;

import java.util.ArrayList;
import java.util.List;

import org.oscim.core.Tag;
import org.oscim.theme.renderinstruction.RenderInstruction;
import org.oscim.utils.LRUCache;
import org.xml.sax.Attributes;

import android.graphics.Color;

/**
 * A RenderTheme defines how ways and nodes are drawn.
 */
public class RenderTheme {
	private static final int MATCHING_CACHE_SIZE = 1024;
	private static final int RENDER_THEME_VERSION = 1;

	private static void validate(String elementName, Integer version,
			float baseStrokeWidth, float baseTextSize) {
		if (version == null) {
			throw new IllegalArgumentException("missing attribute version for element:"
					+ elementName);
		} else if (version.intValue() != RENDER_THEME_VERSION) {
			throw new IllegalArgumentException("invalid render theme version:" + version);
		} else if (baseStrokeWidth < 0) {
			throw new IllegalArgumentException("base-stroke-width must not be negative: "
					+ baseStrokeWidth);
		} else if (baseTextSize < 0) {
			throw new IllegalArgumentException("base-text-size must not be negative: "
					+ baseTextSize);
		}
	}

	static RenderTheme create(String elementName, Attributes attributes) {
		Integer version = null;
		int mapBackground = Color.WHITE;
		float baseStrokeWidth = 1;
		float baseTextSize = 1;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("schemaLocation".equals(name)) {
				continue;
			} else if ("version".equals(name)) {
				version = Integer.valueOf(Integer.parseInt(value));
			} else if ("map-background".equals(name)) {
				mapBackground = Color.parseColor(value);
			} else if ("base-stroke-width".equals(name)) {
				baseStrokeWidth = Float.parseFloat(value);
			} else if ("base-text-size".equals(name)) {
				baseTextSize = Float.parseFloat(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		validate(elementName, version, baseStrokeWidth, baseTextSize);
		return new RenderTheme(mapBackground, baseStrokeWidth, baseTextSize);
	}

	private final float mBaseStrokeWidth;
	private final float mBaseTextSize;
	private int mLevels;
	private final int mMapBackground;
	private final ArrayList<Rule> mRulesList;

	private final LRUCache<MatchingCacheKey, RenderInstruction[]> mMatchingCacheNodes;
	private final LRUCache<MatchingCacheKey, RenderInstruction[]> mMatchingCacheWay;
	private final LRUCache<MatchingCacheKey, RenderInstruction[]> mMatchingCacheArea;

	RenderTheme(int mapBackground, float baseStrokeWidth, float baseTextSize) {
		mMapBackground = mapBackground;
		mBaseStrokeWidth = baseStrokeWidth;
		mBaseTextSize = baseTextSize;
		mRulesList = new ArrayList<Rule>();

		mMatchingCacheNodes = new LRUCache<MatchingCacheKey, RenderInstruction[]>(
				MATCHING_CACHE_SIZE);
		mMatchingCacheWay = new LRUCache<MatchingCacheKey, RenderInstruction[]>(
				MATCHING_CACHE_SIZE);
		mMatchingCacheArea = new LRUCache<MatchingCacheKey, RenderInstruction[]>(
				MATCHING_CACHE_SIZE);
	}

	/**
	 * Must be called when this RenderTheme gets destroyed to clean up and free resources.
	 */
	public void destroy() {
		mMatchingCacheNodes.clear();
		mMatchingCacheArea.clear();
		mMatchingCacheWay.clear();

		for (int i = 0, n = mRulesList.size(); i < n; ++i) {
			mRulesList.get(i).onDestroy();
		}
	}

	/**
	 * @return the number of distinct drawing levels required by this RenderTheme.
	 */
	public int getLevels() {
		return mLevels;
	}

	/**
	 * @return the map background color of this RenderTheme.
	 * @see Color
	 */
	public int getMapBackground() {
		return mMapBackground;
	}

	/**
	 * @param renderCallback
	 *            ...
	 * @param tags
	 *            ...
	 * @param zoomLevel
	 *            ...
	 * @return ...
	 */
	public synchronized RenderInstruction[] matchNode(IRenderCallback renderCallback,
			Tag[] tags, byte zoomLevel) {

		RenderInstruction[] renderInstructions = null;

		MatchingCacheKey matchingCacheKey;

		matchingCacheKey = new MatchingCacheKey(tags, zoomLevel);
		boolean found = mMatchingCacheNodes.containsKey(matchingCacheKey);
		if (found) {
			renderInstructions = mMatchingCacheNodes.get(matchingCacheKey);
		} else {
			// cache miss
			List<RenderInstruction> matchingList = new ArrayList<RenderInstruction>(4);
			for (int i = 0, n = mRulesList.size(); i < n; ++i)
				mRulesList.get(i)
						.matchNode(renderCallback, tags, zoomLevel, matchingList);

			int size = matchingList.size();
			if (size > 0) {
				renderInstructions = new RenderInstruction[size];
				matchingList.toArray(renderInstructions);
			}
			mMatchingCacheNodes.put(matchingCacheKey, renderInstructions);
		}

		if (renderInstructions != null) {
			for (int i = 0, n = renderInstructions.length; i < n; i++)
				renderInstructions[i].renderNode(renderCallback, tags);
		}

		return renderInstructions;

	}

	// private int missCnt = 0;
	// private int hitCnt = 0;
	private MatchingCacheKey mCacheKey = new MatchingCacheKey();

	/**
	 * Matches a way with the given parameters against this RenderTheme.
	 * 
	 * @param renderCallback
	 *            the callback implementation which will be executed on each match.
	 * @param tags
	 *            the tags of the way.
	 * @param zoomLevel
	 *            the zoom level at which the way should be matched.
	 * @param closed
	 *            way is Closed
	 * @param render
	 *            ...
	 * @return currently processed render instructions
	 */
	public synchronized RenderInstruction[] matchWay(IRenderCallback renderCallback,
			Tag[] tags, byte zoomLevel, boolean closed, boolean render) {

		RenderInstruction[] renderInstructions = null;

		LRUCache<MatchingCacheKey, RenderInstruction[]> matchingCache;
		MatchingCacheKey matchingCacheKey;

		if (closed) {
			matchingCache = mMatchingCacheArea;
		} else {
			matchingCache = mMatchingCacheWay;
		}

		mCacheKey.set(tags, zoomLevel);
		renderInstructions = matchingCache.get(mCacheKey);

		if (renderInstructions != null) {
			// Log.d("RenderTheme", hitCnt++ + "Cache Hit");
		} else if (!matchingCache.containsKey(mCacheKey)) {
			matchingCacheKey = new MatchingCacheKey(mCacheKey);

			// cache miss
			// Log.d("RenderTheme", missCnt++ + " Cache Miss");

			int c = (closed ? Closed.YES : Closed.NO);
			List<RenderInstruction> matchingList = new ArrayList<RenderInstruction>(4);
			for (int i = 0, n = mRulesList.size(); i < n; ++i) {
				mRulesList.get(i).matchWay(renderCallback, tags, zoomLevel, c,
						matchingList);
			}
			int size = matchingList.size();
			if (size > 0) {
				renderInstructions = new RenderInstruction[size];
				matchingList.toArray(renderInstructions);
			}
			matchingCache.put(matchingCacheKey, renderInstructions);
		}

		if (render && renderInstructions != null) {
			for (int i = 0, n = renderInstructions.length; i < n; i++)
				renderInstructions[i].renderWay(renderCallback, tags);
		}

		return renderInstructions;
	}

	void addRule(Rule rule) {
		mRulesList.add(rule);
	}

	void complete() {
		mRulesList.trimToSize();
		for (int i = 0, n = mRulesList.size(); i < n; ++i) {
			mRulesList.get(i).onComplete();
		}

	}

	/**
	 * Scales the stroke width of this RenderTheme by the given factor.
	 * 
	 * @param scaleFactor
	 *            the factor by which the stroke width should be scaled.
	 */
	public void scaleStrokeWidth(float scaleFactor) {
		for (int i = 0, n = mRulesList.size(); i < n; ++i) {
			mRulesList.get(i).scaleStrokeWidth(scaleFactor * mBaseStrokeWidth);
		}
	}

	/**
	 * Scales the text size of this RenderTheme by the given factor.
	 * 
	 * @param scaleFactor
	 *            the factor by which the text size should be scaled.
	 */
	public void scaleTextSize(float scaleFactor) {
		for (int i = 0, n = mRulesList.size(); i < n; ++i) {
			mRulesList.get(i).scaleTextSize(scaleFactor * mBaseTextSize);
		}
	}

	void setLevels(int levels) {
		mLevels = levels;
	}
}
