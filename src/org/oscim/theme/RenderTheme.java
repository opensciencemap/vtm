/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
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
import java.util.Arrays;
import java.util.List;

import org.oscim.backend.canvas.Color;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.MapElement;
import org.oscim.theme.renderinstruction.RenderInstruction;
import org.oscim.theme.rule.Element;
import org.oscim.theme.rule.Rule;
import org.oscim.utils.LRUCache;
import org.xml.sax.Attributes;

import org.oscim.backend.Log;

/**
 * A RenderTheme defines how map elements are drawn.
 */
public class RenderTheme implements IRenderTheme {
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
	private final int mMapBackground;

	private int mLevels;
	private Rule[] mRules;

	class ElementCache {
		final int matchType;
		final LRUCache<MatchingCacheKey, RenderInstructionItem> cache;
		final MatchingCacheKey cacheKey;

		/* temporary matching instructions list */
		final ArrayList<RenderInstruction> instructionList;

		RenderInstructionItem prevItem;

		public ElementCache(int type) {
			cache = new LRUCache<MatchingCacheKey, RenderInstructionItem>(MATCHING_CACHE_SIZE);
			instructionList = new ArrayList<RenderInstruction>(4);
			cacheKey = new MatchingCacheKey();
			matchType = type;
		}
		RenderInstructionItem getRenderInstructions(){
			return cache.get(cacheKey);
		}
	}

	class RenderInstructionItem {
		RenderInstructionItem next;
		int zoom;
		RenderInstruction[] list;
		MatchingCacheKey key;
	}

	private final ElementCache[] mElementCache;

	RenderTheme(int mapBackground, float baseStrokeWidth, float baseTextSize) {
		mMapBackground = mapBackground;
		mBaseStrokeWidth = baseStrokeWidth;
		mBaseTextSize = baseTextSize;

		mElementCache = new ElementCache[3];
		mElementCache[0] = new ElementCache(Element.NODE);
		mElementCache[1] = new ElementCache(Element.LINE);
		mElementCache[2] = new ElementCache(Element.POLY);
	}

	/*
	 * (non-Javadoc)
	 * @see org.oscim.theme.IRenderTheme#destroy()
	 */
	@Override
	public void destroy() {

		for (int i = 0; i < 3; i++)
			mElementCache[i].cache.clear();

		if (mRules != null) {
			for (int i = 0, n = mRules.length; i < n; i++)
				mRules[i].onDestroy();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.oscim.theme.IRenderTheme#getLevels()
	 */
	@Override
	public int getLevels() {
		return mLevels;
	}

	/*
	 * (non-Javadoc)
	 * @see org.oscim.theme.IRenderTheme#getMapBackground()
	 */
	@Override
	public int getMapBackground() {
		return mMapBackground;
	}

	/*
	 * (non-Javadoc)
	 * @see org.oscim.theme.IRenderTheme#matchWay(org.oscim.core.Tag[], byte,
	 * boolean)
	 */
	@Override
	public RenderInstruction[] matchElement(MapElement element, int zoomLevel) {

		// list of renderinsctruction items in cache
		RenderInstructionItem ris = null;

		// the item matching tags and zoomlevel
		RenderInstructionItem ri = null;

		int type = element.type.nativeInt;
		if (type < 1 || type > 3) {
			Log.d(TAG, "invalid geometry type for RenderTheme " + element.type.name());
			return null;
		}

		ElementCache cache = mElementCache[type - 1];

		// NOTE: maximum zoom level supported is 32
		int zoomMask = 1 << zoomLevel;

		synchronized (cache) {

			if (cache.prevItem == null || (cache.prevItem.zoom & zoomMask) == 0) {
				// previous instructions zoom does not match
				cache.cacheKey.set(element.tags, null);
			} else {
				// compare if tags match previous instructions
				if (cache.cacheKey.set(element.tags, cache.prevItem.key)) {
					//Log.d(TAG, "same as previous " + Arrays.deepToString(tags));
					ri = cache.prevItem;
				}
			}

			if (ri == null) {
				// get instruction for current cacheKey
				ris = cache.getRenderInstructions();

				for (ri = ris; ri != null; ri = ri.next)
					if ((ri.zoom & zoomMask) != 0)
						// cache hit
						break;
			}

			if (ri == null) {
				// cache miss
				//Log.d(TAG, missCnt++ + " / " + hitCnt + " Cache Miss");

				List<RenderInstruction> matches = cache.instructionList;
				matches.clear();

				for (Rule rule : mRules)
					rule.matchElement(cache.matchType, cache.cacheKey.mTags, zoomMask, matches);

				int size = matches.size();
				if (size > 1) {
					for (int i = 0; i < size - 1; i++) {
						RenderInstruction r = matches.get(i);
						for (int j = i + 1; j < size; j++) {
							if (matches.get(j) == r) {
								Log.d(TAG, "fix duplicate instruction! "
										+ Arrays.deepToString(cache.cacheKey.mTags)
										+ ":" + zoomLevel);
								matches.remove(j--);
								size--;
							}
						}
					}
				}
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
					// we found a same matchting list on another zoomlevel add
					// this zoom level to the existing RenderInstructionItem.
					ri.zoom |= zoomMask;

					//Log.d(TAG,
					//		zoomLevel + " same instructions " + size + " "
					//				+ Arrays.deepToString(tags));
				} else {
					//Log.d(TAG,
					//		zoomLevel + " new instructions " + size + " "
					//				+ Arrays.deepToString(tags));

					ri = new RenderInstructionItem();
					ri.zoom = zoomMask;

					if (size > 0) {
						ri.list = new RenderInstruction[size];
						matches.toArray(ri.list);
					}

					// attach this list to the one found for MatchingKey
					if (ris != null) {
						ri.next = ris.next;
						ri.key = ris.key;
						ris.next = ri;
					} else {
						ri.key = new MatchingCacheKey(cache.cacheKey);
						cache.cache.put(ri.key, ri);
					}
				}
			}

			cache.prevItem = ri;
		}

		return ri.list;
	}

	void complete(List<Rule> rulesList, int levels) {
		mLevels = levels;

		mRules = new Rule[rulesList.size()];
		rulesList.toArray(mRules);

		for (int i = 0, n = mRules.length; i < n; i++) {
			mRules[i].onComplete();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.oscim.theme.IRenderTheme#scaleStrokeWidth(float)
	 */
	@Override
	public void scaleStrokeWidth(float scaleFactor) {

		for (int i = 0, n = mRules.length; i < n; i++)
			mRules[i].scaleStrokeWidth(scaleFactor * mBaseStrokeWidth);
	}

	/*
	 * (non-Javadoc)
	 * @see org.oscim.theme.IRenderTheme#scaleTextSize(float)
	 */
	@Override
	public void scaleTextSize(float scaleFactor) {

		for (int i = 0, n = mRules.length; i < n; i++)
			mRules[i].scaleTextSize(scaleFactor * mBaseTextSize);
	}
}
