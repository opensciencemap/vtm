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
package org.mapsforge.android.maps.rendertheme;

import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.rendertheme.renderinstruction.Line;
import org.mapsforge.android.maps.rendertheme.renderinstruction.RenderInstruction;
import org.mapsforge.core.LRUCache;
import org.mapsforge.core.Tag;
import org.xml.sax.Attributes;

import android.graphics.Color;

/**
 * A RenderTheme defines how ways and nodes are drawn.
 */
public class RenderTheme {
	private static final int MATCHING_CACHE_SIZE = 1024;
	private static final int RENDER_THEME_VERSION = 1;

	private static void validate(String elementName, Integer version, float baseStrokeWidth, float baseTextSize) {
		if (version == null) {
			throw new IllegalArgumentException("missing attribute version for element:" + elementName);
		} else if (version.intValue() != RENDER_THEME_VERSION) {
			throw new IllegalArgumentException("invalid render theme version:" + version);
		} else if (baseStrokeWidth < 0) {
			throw new IllegalArgumentException("base-stroke-width must not be negative: " + baseStrokeWidth);
		} else if (baseTextSize < 0) {
			throw new IllegalArgumentException("base-text-size must not be negative: " + baseTextSize);
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

	// private List<RenderInstruction> mMatchingListWay;
	// private List<RenderInstruction> mMatchingListArea;
	// private List<RenderInstruction> mMatchingListNode;

	RenderTheme(int mapBackground, float baseStrokeWidth, float baseTextSize) {
		mMapBackground = mapBackground;
		mBaseStrokeWidth = baseStrokeWidth;
		mBaseTextSize = baseTextSize;
		mRulesList = new ArrayList<Rule>();

		mMatchingCacheNodes = new LRUCache<MatchingCacheKey, RenderInstruction[]>(MATCHING_CACHE_SIZE);
		mMatchingCacheWay = new LRUCache<MatchingCacheKey, RenderInstruction[]>(MATCHING_CACHE_SIZE);
		mMatchingCacheArea = new LRUCache<MatchingCacheKey, RenderInstruction[]>(MATCHING_CACHE_SIZE);
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
	 */
	public void matchNode(RenderCallback renderCallback, Tag[] tags, byte zoomLevel) {
		// List<RenderInstruction> matchingList = matchingListNode;
		// MatchingCacheKey matchingCacheKey = matchingCacheKeyNode;
		//
		// if (!changed) {
		// if (matchingList != null) {
		// for (int i = 0, n = matchingList.size(); i < n; ++i) {
		// matchingList.get(i).renderNode(renderCallback, tags);
		// }
		// }
		// return;
		// }
		// matchingCacheKey = new MatchingCacheKey(tags, zoomLevel);
		// matchingList = matchingCacheNodes.get(matchingCacheKey);
		//
		// if (matchingList != null) {
		// // cache hit
		// for (int i = 0, n = matchingList.size(); i < n; ++i) {
		// matchingList.get(i).renderNode(renderCallback, tags);
		// }
		// } else {
		// // cache miss
		// matchingList = new ArrayList<RenderInstruction>();
		// for (int i = 0, n = mRulesList.size(); i < n; ++i) {
		// mRulesList.get(i).matchNode(renderCallback, tags, zoomLevel, matchingList);
		// }
		// matchingCacheNodes.put(matchingCacheKey, matchingList);
		// }
		//
		// matchingListNode = matchingList;
		// matchingCacheKeyNode = matchingCacheKey;
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

	private RenderInstruction[] mRenderInstructions = null;

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
	 * @param changed
	 *            ...
	 */
	public void matchWay(RenderCallback renderCallback, Tag[] tags, byte zoomLevel, boolean closed, boolean changed) {
		RenderInstruction[] renderInstructions = null;

		LRUCache<MatchingCacheKey, RenderInstruction[]> matchingCache;
		MatchingCacheKey matchingCacheKey;

		if (!changed) {
			renderInstructions = mRenderInstructions;

			if (renderInstructions != null) {
				for (int i = 0, n = renderInstructions.length; i < n; i++)
					renderInstructions[i].renderWay(renderCallback, tags);
			}
			return;
		}

		if (closed) {
			matchingCache = mMatchingCacheArea;
		} else {
			matchingCache = mMatchingCacheWay;
		}

		matchingCacheKey = new MatchingCacheKey(tags, zoomLevel);
		boolean found = matchingCache.containsKey(matchingCacheKey);
		if (found) {
			renderInstructions = matchingCache.get(matchingCacheKey);

			if (renderInstructions != null) {
				for (int i = 0, n = renderInstructions.length; i < n; i++)
					renderInstructions[i].renderWay(renderCallback, tags);

			}
		} else {
			// cache miss
			Closed c = (closed ? Closed.YES : Closed.NO);
			List<RenderInstruction> matchingList = new ArrayList<RenderInstruction>(4);
			for (int i = 0, n = mRulesList.size(); i < n; ++i) {
				mRulesList.get(i).matchWay(renderCallback, tags, zoomLevel, c, matchingList);
			}
			int size = matchingList.size();
			if (size > 0) {
				renderInstructions = new RenderInstruction[matchingList.size()];
				for (int i = 0, n = matchingList.size(); i < n; ++i) {
					RenderInstruction renderInstruction = matchingList.get(i);
					renderInstruction.renderWay(renderCallback, tags);
					renderInstructions[i] = renderInstruction;
				}
			}
			matchingCache.put(matchingCacheKey, renderInstructions);
		}

		mRenderInstructions = renderInstructions;

		// if (matchingList != null) {
		// for (int i = 0, n = matchingList.size(); i < n; ++i) {
		// matchingList.get(i).renderWay(renderCallback, tags);
		// }
		// }
		// return renderInstructions;

		// if (closed) {
		// mMatchingListArea = matchingList;
		// } else {
		// mMatchingListWay = matchingList;
		// }

		// if (mCompareKey.set(tags, zoomLevel, closed)) {
		// // Log.d("mapsforge", "SAME AS BAFORE!!!" + tags);
		// for (int i = 0, n = mInstructionList.length; i < n; ++i) {
		// mInstructionList[i].renderWay(renderCallback, tags);
		// }
		// return;
		// }
		//
		// SparseArray<RenderInstruction[]> matchingList = mMatchingCache.get(mCompareKey);
		//
		// if (matchingList != null) {
		// mInstructionList = matchingList.get(zoomLevel);
		// if (mInstructionList != null) {
		// // cache hit
		// // Log.d("mapsforge", "CCACHE HIT !!!" + tags);
		// for (int i = 0, n = mInstructionList.length; i < n; ++i) {
		// mInstructionList[i].renderWay(renderCallback, tags);
		// }
		//
		// return;
		// }
		// }
		// // Log.d("mapsforge", "CACHE MISS !!!" + tags);
		// // cache miss
		// ArrayList<RenderInstruction> instructionList = new ArrayList<RenderInstruction>();
		//
		// for (int i = 0, n = mRulesList.size(); i < n; ++i) {
		// mRulesList.get(i).matchWay(renderCallback, mCompareKey.getTags(), zoomLevel, closed, instructionList);
		// }
		//
		// boolean found = false;
		// int size = instructionList.size();
		//
		// if (matchingList == null) {
		// matchingList = new SparseArray<RenderInstruction[]>(25);
		// MatchingCacheKey matchingCacheKey = new MatchingCacheKey(mCompareKey);
		// mMatchingCache.put(matchingCacheKey, matchingList);
		// } else {
		// // check if another zoomLevel uses the same instructionList
		// for (int i = 0, n = matchingList.size(); i < n; i++) {
		// int key = matchingList.keyAt(i);
		//
		// RenderInstruction[] list2 = matchingList.get(key);
		// if (list2.length != size)
		// continue;
		//
		// int j = 0;
		// while (j < size && (list2[j] == instructionList.get(j)))
		// j++;
		//
		// if (j == size) {
		// instructionList.clear();
		// mInstructionList = list2;
		// found = true;
		// break;
		// }
		// }
		// }
		//
		// if (!found) {
		// mInstructionList = new RenderInstruction[size];
		// for (int i = 0; i < size; i++)
		// mInstructionList[i] = instructionList.get(i);
		// }
		//
		// for (int i = 0, n = mInstructionList.length; i < n; ++i)
		// mInstructionList[i].renderWay(renderCallback, tags);
		//
		// matchingList.put(zoomLevel, mInstructionList);
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

	private final ArrayList<Line> outlineLayers = new ArrayList<Line>();

	void addOutlineLayer(Line line) {
		outlineLayers.add(line);
	}

	/**
	 * @param layer
	 *            ...
	 * @return Line (paint and level) used for outline
	 */
	public Line getOutline(int layer) {
		return outlineLayers.get(layer);
	}

	void setLevels(int levels) {
		mLevels = levels;
	}
}
