/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 OpenScienceMap
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
	private final static String TAG = RenderTheme.class.getName();

	private static final int MATCHING_CACHE_SIZE = 512;
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

	private final LRUCache<MatchingCacheKey, RenderInstructionItem> mMatchingCacheNodes;
	private final LRUCache<MatchingCacheKey, RenderInstructionItem> mMatchingCacheWay;
	private final LRUCache<MatchingCacheKey, RenderInstructionItem> mMatchingCacheArea;

	class RenderInstructionItem {
		RenderInstructionItem next;
		int zoom;
		RenderInstruction[] list;
	}

	RenderTheme(int mapBackground, float baseStrokeWidth, float baseTextSize) {
		mMapBackground = mapBackground;
		mBaseStrokeWidth = baseStrokeWidth;
		mBaseTextSize = baseTextSize;
		mRulesList = new ArrayList<Rule>();

		mMatchingCacheNodes = new LRUCache<MatchingCacheKey, RenderInstructionItem>(
				MATCHING_CACHE_SIZE);
		mMatchingCacheWay = new LRUCache<MatchingCacheKey, RenderInstructionItem>(
				MATCHING_CACHE_SIZE);
		mMatchingCacheArea = new LRUCache<MatchingCacheKey, RenderInstructionItem>(
				MATCHING_CACHE_SIZE);
	}

	/**
	 * Must be called when this RenderTheme gets destroyed to clean up and free
	 * resources.
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
	 * @return the number of distinct drawing levels required by this
	 *         RenderTheme.
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

	private static void render(IRenderCallback renderCallback,
			RenderInstruction[] renderInstructions, Tag[] tags) {
		for (int i = 0, n = renderInstructions.length; i < n; i++)
			renderInstructions[i].renderNode(renderCallback, tags);
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

		// list of renderinsctruction items in cache
		RenderInstructionItem ris = null;

		// the item matching tags and zoomlevel
		RenderInstructionItem ri = null;

		MatchingCacheKey matchingCacheKey = new MatchingCacheKey(tags);
		boolean found = mMatchingCacheNodes.containsKey(matchingCacheKey);
		int zoomMask = 1 << zoomLevel;

		if (found) {
			ris = mMatchingCacheNodes.get(matchingCacheKey);

			for (ri = ris; ri != null; ri = ri.next)
				if ((ri.zoom & zoomMask) != 0)
					// cache hit
					break;
		}

		if (ri == null) {
			// cache miss 
			List<RenderInstruction> matches = mMatchingList;
			matches.clear();
			for (int i = 0, n = mRulesList.size(); i < n; ++i)
				mRulesList.get(i).matchNode(renderCallback, tags, zoomLevel, matches);

			int size = matches.size();

			// check if same instructions are used in another level
			for (ri = ris; ri != null; ri = ri.next) {
				if (size == 0) {
					if (ri.list != null)
						continue;

					// both matchinglists are empty
					break;
				}

				if (ri.list == null)
					continue;

				if (ri.list.length != size)
					continue;

				int i = 0;
				for (RenderInstruction r : ri.list) {
					if (r != matches.get(i))
						break;
					i++;
				}
				if (i == size)
					// both matching lists contain the same items
					break;
			}

			if (ri != null) {
				// we found a same matchting list on another zoomlevel
				ri.zoom |= zoomMask;
			} else {

				ri = new RenderInstructionItem();
				ri.zoom = zoomMask;

				if (size > 0) {
					ri.list = new RenderInstruction[size];
					matches.toArray(ri.list);
				}
				// attach this list to the one found for MatchingKey
				if (ris != null)
					ris.next = ri;
				else
					mMatchingCacheNodes.put(matchingCacheKey, ri);
			}
		}

		if (ri.list != null)
			render(renderCallback, ri.list, tags);

		return ri.list;

	}

	private int missCnt = 0;
	private int hitCnt = 0;
	private MatchingCacheKey mCacheKey = new MatchingCacheKey();
	private ArrayList<RenderInstruction> mMatchingList = new ArrayList<RenderInstruction>(4);

	/**
	 * Matches a way with the given parameters against this RenderTheme.
	 * @param renderCallback
	 *            the callback implementation which will be executed on each
	 *            match.
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

		// list of renderinsctruction items in cache
		RenderInstructionItem ris = null;

		// the item matching tags and zoomlevel
		RenderInstructionItem ri = null;

		LRUCache<MatchingCacheKey, RenderInstructionItem> matchingCache;
		MatchingCacheKey matchingCacheKey;

		if (closed) {
			matchingCache = mMatchingCacheArea;
		} else {
			matchingCache = mMatchingCacheWay;
		}
		int zoomMask = 1 << zoomLevel;

		mCacheKey.set(tags);
		ris = matchingCache.get(mCacheKey);

		for (ri = ris; ri != null; ri = ri.next)
			if ((ri.zoom & zoomMask) != 0)
				// cache hit
				break;

		if (ri == null) {
			// cache miss
			//Log.d(TAG, missCnt++ + " / " + hitCnt + " Cache Miss");

			int c = (closed ? Closed.YES : Closed.NO);
			List<RenderInstruction> matches = mMatchingList;
			matches.clear();

			for (int i = 0, n = mRulesList.size(); i < n; ++i)
				mRulesList.get(i).matchWay(renderCallback, tags, zoomLevel, c, matches);

			int size = matches.size();
			// check if same instructions are used in another level
			for (ri = ris; ri != null; ri = ri.next) {
				if (size == 0) {
					if (ri.list != null)
						continue;

					// both matchinglists are empty
					break;
				}

				if (ri.list == null)
					continue;

				if (ri.list.length != size)
					continue;

				int i = 0;
				for (RenderInstruction r : ri.list) {
					if (r != matches.get(i))
						break;
					i++;
				}
				if (i == size)
					// both matching lists contain the same items
					break;
			}

			if (ri != null) {
				// we found a same matchting list on another zoomlevel
				ri.zoom |= zoomMask;
				//Log.d(TAG, " same instructions " + size + " " + Arrays.deepToString(tags));

			} else {
				//Log.d(TAG, " new instructions " + size + " " + Arrays.deepToString(tags));

				ri = new RenderInstructionItem();
				ri.zoom = zoomMask;

				if (size > 0) {
					ri.list = new RenderInstruction[size];
					matches.toArray(ri.list);
				}

				// attach this list to the one found for MatchingKey
				if (ris != null) {
					ris.next = ri;
				}
				else {
					matchingCacheKey = new MatchingCacheKey(mCacheKey);
					matchingCache.put(matchingCacheKey, ri);
				}
			}

		}
		if (render && ri.list != null) {
			for (int i = 0, n = ri.list.length; i < n; i++)
				ri.list[i].renderWay(renderCallback, tags);
		}
		return ri.list;
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
