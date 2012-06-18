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
package org.mapsforge.android.maps.swrenderer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.mapsforge.core.Tile;

import android.graphics.Rect;

/**
 * This class place the labels form POIs, area labels and normal labels. The main target is avoiding collisions of these
 * different labels.
 */
class LabelPlacement {
	/**
	 * This class holds the reference positions for the two and four point greedy algorithms.
	 */
	static class ReferencePosition {
		final float height;
		final int nodeNumber;
		SymbolContainer symbol;
		final float width;
		final float x;
		final float y;

		ReferencePosition(float x, float y, int nodeNumber, float width, float height, SymbolContainer symbol) {
			this.x = x;
			this.y = y;
			this.nodeNumber = nodeNumber;
			this.width = width;
			this.height = height;
			this.symbol = symbol;
		}
	}

	static final class ReferencePositionHeightComparator implements Comparator<ReferencePosition>, Serializable {
		private static final long serialVersionUID = 1L;
		static final ReferencePositionHeightComparator INSTANCE = new ReferencePositionHeightComparator();

		private ReferencePositionHeightComparator() {
			// do nothing
		}

		@Override
		public int compare(ReferencePosition x, ReferencePosition y) {
			if (x.y - x.height < y.y - y.height) {
				return -1;
			}

			if (x.y - x.height > y.y - y.height) {
				return 1;
			}
			return 0;
		}
	}

	static final class ReferencePositionWidthComparator implements Comparator<ReferencePosition>, Serializable {
		private static final long serialVersionUID = 1L;
		static final ReferencePositionWidthComparator INSTANCE = new ReferencePositionWidthComparator();

		private ReferencePositionWidthComparator() {
			// do nothing
		}

		@Override
		public int compare(ReferencePosition x, ReferencePosition y) {
			if (x.x + x.width < y.x + y.width) {
				return -1;
			}

			if (x.x + x.width > y.x + y.width) {
				return 1;
			}

			return 0;
		}
	}

	static final class ReferencePositionXComparator implements Comparator<ReferencePosition>, Serializable {
		private static final long serialVersionUID = 1L;
		static final ReferencePositionXComparator INSTANCE = new ReferencePositionXComparator();

		private ReferencePositionXComparator() {
			// do nothing
		}

		@Override
		public int compare(ReferencePosition x, ReferencePosition y) {
			if (x.x < y.x) {
				return -1;
			}

			if (x.x > y.x) {
				return 1;
			}

			return 0;
		}
	}

	static final class ReferencePositionYComparator implements Comparator<ReferencePosition>, Serializable {
		private static final long serialVersionUID = 1L;
		static final ReferencePositionYComparator INSTANCE = new ReferencePositionYComparator();

		private ReferencePositionYComparator() {
			// do nothing
		}

		@Override
		public int compare(ReferencePosition x, ReferencePosition y) {
			if (x.y < y.y) {
				return -1;
			}

			if (x.y > y.y) {
				return 1;
			}

			return 0;
		}
	}

	private static final int PLACEMENT_MODEL = 1;
	private int mLabelDistanceToLabel = 2;
	private int mLabelDistanceToSymbol = 2;
	// You can choose between 2 Position and 4 Position
	// placement Model 0 - 2-Position 1 - 4 Position
	// distance adjustments
	private int mStartDistanceToSymbols = 4;
	private int mSymbolDistanceToSymbol = 2;

	final DependencyCache mDependencyCache;
	PointTextContainer mLabel;
	Rect mRect1;
	Rect mRect2;
	ReferencePosition mReferencePosition;
	SymbolContainer mSymbolContainer;

	LabelPlacement() {
		mDependencyCache = new DependencyCache();
		mRect1 = new Rect();
		mRect2 = new Rect();
	}

	/**
	 * Centers the labels.
	 * 
	 * @param labels
	 *            labels to center
	 */
	private void centerLabels(List<PointTextContainer> labels) {
		for (int i = 0; i < labels.size(); i++) {
			mLabel = labels.get(i);
			mLabel.x = mLabel.x - mLabel.boundary.width() / 2;
		}
	}

	private void preprocessAreaLabels(List<PointTextContainer> areaLabels) {
		centerLabels(areaLabels);

		removeOutOfTileAreaLabels(areaLabels);

		removeOverlappingAreaLabels(areaLabels);

		if (!areaLabels.isEmpty()) {
			mDependencyCache.removeAreaLabelsInAlreadyDrawnAreas(areaLabels);
		}
	}

	private void preprocessLabels(List<PointTextContainer> labels) {
		removeOutOfTileLabels(labels);
	}

	private void preprocessSymbols(List<SymbolContainer> symbols) {
		removeOutOfTileSymbols(symbols);
		removeOverlappingSymbols(symbols);
		mDependencyCache.removeSymbolsFromDrawnAreas(symbols);
	}

	/**
	 * This method uses an adapted greedy strategy for the fixed four position model, above, under left and right form
	 * the point of interest. It uses no priority search tree, because it will not function with symbols only with
	 * points. Instead it uses two minimum heaps. They work similar to a sweep line algorithm but have not a O(n log n
	 * +k) runtime. To find the rectangle that has the top edge, I use also a minimum Heap. The rectangles are sorted by
	 * their y coordinates.
	 * 
	 * @param labels
	 *            label positions and text
	 * @param symbols
	 *            symbol positions
	 * @param areaLabels
	 *            area label positions and text
	 * @return list of labels without overlaps with symbols and other labels by the four fixed position greedy strategy
	 */
	private List<PointTextContainer> processFourPointGreedy(List<PointTextContainer> labels,
			List<SymbolContainer> symbols, List<PointTextContainer> areaLabels) {
		List<PointTextContainer> resolutionSet = new ArrayList<PointTextContainer>();

		// Array for the generated reference positions around the points of
		// interests
		ReferencePosition[] refPos = new ReferencePosition[(labels.size()) * 4];

		// lists that sorts the reference points after the minimum top edge y
		// position
		PriorityQueue<ReferencePosition> priorUp = new PriorityQueue<ReferencePosition>(labels.size() * 4 * 2
				+ labels.size() / 10 * 2, ReferencePositionYComparator.INSTANCE);
		// lists that sorts the reference points after the minimum bottom edge y
		// position
		PriorityQueue<ReferencePosition> priorDown = new PriorityQueue<ReferencePosition>(labels.size() * 4 * 2
				+ labels.size() / 10 * 2, ReferencePositionHeightComparator.INSTANCE);

		PointTextContainer tmp;
		int dis = mStartDistanceToSymbols;

		// creates the reference positions
		for (int z = 0; z < labels.size(); z++) {
			if (labels.get(z) != null) {
				if (labels.get(z).symbol != null) {
					tmp = labels.get(z);

					// up
					refPos[z * 4] = new ReferencePosition(tmp.x - tmp.boundary.width() / 2, tmp.y
							- tmp.symbol.symbol.getHeight() / 2 - dis, z, tmp.boundary.width(), tmp.boundary.height(),
							tmp.symbol);
					// down
					refPos[z * 4 + 1] = new ReferencePosition(tmp.x - tmp.boundary.width() / 2, tmp.y
							+ tmp.symbol.symbol.getHeight() / 2 + tmp.boundary.height() + dis, z, tmp.boundary.width(),
							tmp.boundary.height(), tmp.symbol);
					// left
					refPos[z * 4 + 2] = new ReferencePosition(tmp.x - tmp.symbol.symbol.getWidth() / 2
							- tmp.boundary.width() - dis, tmp.y + tmp.boundary.height() / 2, z, tmp.boundary.width(),
							tmp.boundary.height(), tmp.symbol);
					// right
					refPos[z * 4 + 3] = new ReferencePosition(tmp.x + tmp.symbol.symbol.getWidth() / 2 + dis, tmp.y
							+ tmp.boundary.height() / 2 - 0.1f, z, tmp.boundary.width(), tmp.boundary.height(),
							tmp.symbol);
				} else {
					refPos[z * 4] = new ReferencePosition(labels.get(z).x - ((labels.get(z).boundary.width()) / 2),
							labels.get(z).y, z, labels.get(z).boundary.width(), labels.get(z).boundary.height(), null);
					refPos[z * 4 + 1] = null;
					refPos[z * 4 + 2] = null;
					refPos[z * 4 + 3] = null;
				}
			}
		}

		removeNonValidateReferencePosition(refPos, symbols, areaLabels);

		// do while it gives reference positions
		for (int i = 0; i < refPos.length; i++) {
			mReferencePosition = refPos[i];
			if (mReferencePosition != null) {
				priorUp.add(mReferencePosition);
				priorDown.add(mReferencePosition);
			}
		}

		while (priorUp.size() != 0) {
			mReferencePosition = priorUp.remove();

			mLabel = labels.get(mReferencePosition.nodeNumber);

			resolutionSet.add(new PointTextContainer(mLabel.text, mReferencePosition.x,
					mReferencePosition.y, mLabel.paintFront, mLabel.paintBack, mLabel.symbol));

			if (priorUp.size() == 0) {
				return resolutionSet;
			}

			priorUp.remove(refPos[mReferencePosition.nodeNumber * 4 + 0]);
			priorUp.remove(refPos[mReferencePosition.nodeNumber * 4 + 1]);
			priorUp.remove(refPos[mReferencePosition.nodeNumber * 4 + 2]);
			priorUp.remove(refPos[mReferencePosition.nodeNumber * 4 + 3]);

			priorDown.remove(refPos[mReferencePosition.nodeNumber * 4 + 0]);
			priorDown.remove(refPos[mReferencePosition.nodeNumber * 4 + 1]);
			priorDown.remove(refPos[mReferencePosition.nodeNumber * 4 + 2]);
			priorDown.remove(refPos[mReferencePosition.nodeNumber * 4 + 3]);

			LinkedList<ReferencePosition> linkedRef = new LinkedList<ReferencePosition>();

			while (priorDown.size() != 0) {
				if (priorDown.peek().x < mReferencePosition.x + mReferencePosition.width) {
					linkedRef.add(priorDown.remove());
				} else {
					break;
				}
			}
			// brute Force collision test (faster then sweep line for a small
			// amount of
			// objects)
			for (int i = 0; i < linkedRef.size(); i++) {
				if ((linkedRef.get(i).x <= mReferencePosition.x + mReferencePosition.width)
						&& (linkedRef.get(i).y >= mReferencePosition.y - linkedRef.get(i).height)
						&& (linkedRef.get(i).y <= mReferencePosition.y + linkedRef.get(i).height)) {
					priorUp.remove(linkedRef.get(i));
					linkedRef.remove(i);
					i--;
				}
			}
			priorDown.addAll(linkedRef);
		}

		return resolutionSet;
	}

	/**
	 * This method uses an adapted greedy strategy for the fixed two position model, above and under. It uses no
	 * priority search tree, because it will not function with symbols only with points. Instead it uses two minimum
	 * heaps. They work similar to a sweep line algorithm but have not a O(n log n +k) runtime. To find the rectangle
	 * that has the leftest edge, I use also a minimum Heap. The rectangles are sorted by their x coordinates.
	 * 
	 * @param labels
	 *            label positions and text
	 * @param symbols
	 *            symbol positions
	 * @param areaLabels
	 *            area label positions and text
	 * @return list of labels without overlaps with symbols and other labels by the two fixed position greedy strategy
	 */
	private List<PointTextContainer> processTwoPointGreedy(List<PointTextContainer> labels,
			List<SymbolContainer> symbols, List<PointTextContainer> areaLabels) {
		List<PointTextContainer> resolutionSet = new ArrayList<PointTextContainer>();
		// Array for the generated reference positions around the points of
		// interests
		ReferencePosition[] refPos = new ReferencePosition[labels.size() * 2];

		// lists that sorts the reference points after the minimum right edge x
		// position
		PriorityQueue<ReferencePosition> priorRight = new PriorityQueue<ReferencePosition>(labels.size() * 2
				+ labels.size() / 10 * 2, ReferencePositionWidthComparator.INSTANCE);
		// lists that sorts the reference points after the minimum left edge x
		// position
		PriorityQueue<ReferencePosition> priorLeft = new PriorityQueue<ReferencePosition>(labels.size() * 2
				+ labels.size() / 10 * 2, ReferencePositionXComparator.INSTANCE);

		// creates the reference positions
		for (int z = 0; z < labels.size(); z++) {
			mLabel = labels.get(z);

			if (mLabel.symbol != null) {
				refPos[z * 2] = new ReferencePosition(mLabel.x - (mLabel.boundary.width() / 2) - 0.1f,
						mLabel.y - mLabel.boundary.height() - mStartDistanceToSymbols, z,
						mLabel.boundary.width(), mLabel.boundary.height(), mLabel.symbol);
				refPos[z * 2 + 1] = new ReferencePosition(mLabel.x - (mLabel.boundary.width() / 2),
						mLabel.y + mLabel.symbol.symbol.getHeight() + mStartDistanceToSymbols, z,
						mLabel.boundary.width(), mLabel.boundary.height(), mLabel.symbol);
			} else {
				refPos[z * 2] = new ReferencePosition(mLabel.x - (mLabel.boundary.width() / 2) - 0.1f,
						mLabel.y, z, mLabel.boundary.width(), mLabel.boundary.height(), null);
				refPos[z * 2 + 1] = null;
			}
		}

		// removes reference positions that overlaps with other symbols or
		// dependency objects
		removeNonValidateReferencePosition(refPos, symbols, areaLabels);

		for (int i = 0; i < refPos.length; i++) {
			mReferencePosition = refPos[i];
			if (mReferencePosition != null) {
				priorLeft.add(mReferencePosition);
				priorRight.add(mReferencePosition);
			}
		}

		while (priorRight.size() != 0) {
			mReferencePosition = priorRight.remove();

			mLabel = labels.get(mReferencePosition.nodeNumber);

			resolutionSet.add(new PointTextContainer(mLabel.text, mReferencePosition.x,
					mReferencePosition.y, mLabel.paintFront, mLabel.paintBack,
					mReferencePosition.symbol));

			// Removes the other position that is a possible position for the label
			// of one point
			// of interest

			priorRight.remove(refPos[mReferencePosition.nodeNumber * 2 + 1]);

			if (priorRight.size() == 0) {
				return resolutionSet;
			}

			priorLeft.remove(mReferencePosition);
			priorLeft.remove(refPos[mReferencePosition.nodeNumber * 2 + 1]);

			// find overlapping labels and deletes the reference points and delete
			// them
			LinkedList<ReferencePosition> linkedRef = new LinkedList<ReferencePosition>();

			while (priorLeft.size() != 0) {
				if (priorLeft.peek().x < mReferencePosition.x + mReferencePosition.width) {
					linkedRef.add(priorLeft.remove());
				} else {
					break;
				}
			}

			// brute Force collision test (faster then sweep line for a small
			// amount of
			// objects)
			for (int i = 0; i < linkedRef.size(); i++) {
				if ((linkedRef.get(i).x <= mReferencePosition.x + mReferencePosition.width)
						&& (linkedRef.get(i).y >= mReferencePosition.y - linkedRef.get(i).height)
						&& (linkedRef.get(i).y <= mReferencePosition.y + linkedRef.get(i).height)) {
					priorRight.remove(linkedRef.get(i));
					linkedRef.remove(i);
					i--;
				}
			}
			priorLeft.addAll(linkedRef);
		}

		return resolutionSet;
	}

	private void removeEmptySymbolReferences(List<PointTextContainer> nodes, List<SymbolContainer> symbols) {
		for (int i = 0; i < nodes.size(); i++) {
			mLabel = nodes.get(i);
			if (!symbols.contains(mLabel.symbol)) {
				mLabel.symbol = null;
			}
		}
	}

	/**
	 * The greedy algorithms need possible label positions, to choose the best among them. This method removes the
	 * reference points, that are not validate. Not validate means, that the Reference overlap with another symbol or
	 * label or is outside of the tile.
	 * 
	 * @param refPos
	 *            list of the potential positions
	 * @param symbols
	 *            actual list of the symbols
	 * @param areaLabels
	 *            actual list of the area labels
	 */
	private void removeNonValidateReferencePosition(ReferencePosition[] refPos, List<SymbolContainer> symbols,
			List<PointTextContainer> areaLabels) {
		int dis = mLabelDistanceToSymbol;

		for (int i = 0; i < symbols.size(); i++) {
			mSymbolContainer = symbols.get(i);
			mRect1.set((int) mSymbolContainer.x - dis, (int) mSymbolContainer.y - dis,
					(int) mSymbolContainer.x + mSymbolContainer.symbol.getWidth() + dis,
					(int) mSymbolContainer.y + mSymbolContainer.symbol.getHeight() + dis);

			for (int y = 0; y < refPos.length; y++) {
				if (refPos[y] != null) {

					mRect2.set((int) refPos[y].x, (int) (refPos[y].y - refPos[y].height),
							(int) (refPos[y].x + refPos[y].width), (int) (refPos[y].y));

					if (android.graphics.Rect.intersects(mRect2, mRect1)) {
						refPos[y] = null;
					}
				}
			}
		}

		dis = mLabelDistanceToLabel;

		for (PointTextContainer areaLabel : areaLabels) {

			mRect1.set((int) areaLabel.x - dis, (int) areaLabel.y - areaLabel.boundary.height() - dis,
					(int) areaLabel.x + areaLabel.boundary.width() + dis, (int) areaLabel.y + dis);

			for (int y = 0; y < refPos.length; y++) {
				if (refPos[y] != null) {

					mRect2.set((int) refPos[y].x, (int) (refPos[y].y - refPos[y].height),
							(int) (refPos[y].x + refPos[y].width), (int) (refPos[y].y));

					if (android.graphics.Rect.intersects(mRect2, mRect1)) {
						refPos[y] = null;
					}
				}
			}
		}

		mDependencyCache.removeReferencePointsFromDependencyCache(refPos);
	}

	/**
	 * This method removes the area labels, that are not visible in the actual tile.
	 * 
	 * @param areaLabels
	 *            area Labels from the actual tile
	 */
	private void removeOutOfTileAreaLabels(List<PointTextContainer> areaLabels) {
		for (int i = 0; i < areaLabels.size(); i++) {
			mLabel = areaLabels.get(i);

			if (mLabel.x > Tile.TILE_SIZE) {
				areaLabels.remove(i);

				i--;
			} else if (mLabel.y - mLabel.boundary.height() > Tile.TILE_SIZE) {
				areaLabels.remove(i);

				i--;
			} else if (mLabel.x + mLabel.boundary.width() < 0.0f) {
				areaLabels.remove(i);

				i--;
			} else if (mLabel.y + mLabel.boundary.height() < 0.0f) {
				areaLabels.remove(i);

				i--;
			}
		}
	}

	/**
	 * This method removes the labels, that are not visible in the actual tile.
	 * 
	 * @param labels
	 *            Labels from the actual tile
	 */
	private void removeOutOfTileLabels(List<PointTextContainer> labels) {
		for (int i = 0; i < labels.size();) {
			mLabel = labels.get(i);

			if (mLabel.x - mLabel.boundary.width() / 2 > Tile.TILE_SIZE) {
				labels.remove(i);
				mLabel = null;
			} else if (mLabel.y - mLabel.boundary.height() > Tile.TILE_SIZE) {
				labels.remove(i);
				mLabel = null;
			} else if ((mLabel.x - mLabel.boundary.width() / 2 + mLabel.boundary.width()) < 0.0f) {
				labels.remove(i);
				mLabel = null;
			} else if (mLabel.y < 0.0f) {
				labels.remove(i);
				mLabel = null;
			} else {
				i++;
			}
		}
	}

	/**
	 * This method removes the Symbols, that are not visible in the actual tile.
	 * 
	 * @param symbols
	 *            Symbols from the actual tile
	 */
	private void removeOutOfTileSymbols(List<SymbolContainer> symbols) {
		for (int i = 0; i < symbols.size();) {
			mSymbolContainer = symbols.get(i);

			if (mSymbolContainer.x > Tile.TILE_SIZE) {
				symbols.remove(i);
			} else if (mSymbolContainer.y > Tile.TILE_SIZE) {
				symbols.remove(i);
			} else if (mSymbolContainer.x + mSymbolContainer.symbol.getWidth() < 0.0f) {
				symbols.remove(i);
			} else if (mSymbolContainer.y + mSymbolContainer.symbol.getHeight() < 0.0f) {
				symbols.remove(i);
			} else {
				i++;
			}
		}
	}

	/**
	 * This method removes all the area labels, that overlap each other. So that the output is collision free
	 * 
	 * @param areaLabels
	 *            area labels from the actual tile
	 */
	private void removeOverlappingAreaLabels(List<PointTextContainer> areaLabels) {
		int dis = mLabelDistanceToLabel;

		for (int x = 0; x < areaLabels.size(); x++) {
			mLabel = areaLabels.get(x);
			mRect1.set((int) mLabel.x - dis, (int) mLabel.y - dis,
					(int) (mLabel.x + mLabel.boundary.width()) + dis,
					(int) (mLabel.y + mLabel.boundary.height() + dis));

			for (int y = x + 1; y < areaLabels.size(); y++) {
				if (y != x) {
					mLabel = areaLabels.get(y);
					mRect2.set((int) mLabel.x, (int) mLabel.y,
							(int) (mLabel.x + mLabel.boundary.width()),
							(int) (mLabel.y + mLabel.boundary.height()));

					if (android.graphics.Rect.intersects(mRect1, mRect2)) {
						areaLabels.remove(y);

						y--;
					}
				}
			}
		}
	}

	/**
	 * Removes the the symbols that overlap with area labels.
	 * 
	 * @param symbols
	 *            list of symbols
	 * @param pTC
	 *            list of labels
	 */
	private void removeOverlappingSymbolsWithAreaLabels(List<SymbolContainer> symbols, List<PointTextContainer> pTC) {
		int dis = mLabelDistanceToSymbol;

		for (int x = 0; x < pTC.size(); x++) {
			mLabel = pTC.get(x);

			mRect1.set((int) mLabel.x - dis, (int) (mLabel.y - mLabel.boundary.height()) - dis,
					(int) (mLabel.x + mLabel.boundary.width() + dis), (int) (mLabel.y + dis));

			for (int y = 0; y < symbols.size(); y++) {
				mSymbolContainer = symbols.get(y);

				mRect2.set((int) mSymbolContainer.x, (int) mSymbolContainer.y,
						(int) (mSymbolContainer.x + mSymbolContainer.symbol.getWidth()),
						(int) (mSymbolContainer.y + mSymbolContainer.symbol.getHeight()));

				if (android.graphics.Rect.intersects(mRect1, mRect2)) {
					symbols.remove(y);
					y--;
				}
			}
		}
	}

	int getLabelDistanceToLabel() {
		return mLabelDistanceToLabel;
	}

	int getLabelDistanceToSymbol() {
		return mLabelDistanceToSymbol;
	}

	int getPlacementOption() {
		return PLACEMENT_MODEL;
	}

	int getStartDistanceToSymbols() {
		return mStartDistanceToSymbols;
	}

	int getSymbolDistanceToSymbol() {
		return mSymbolDistanceToSymbol;
	}

	/**
	 * The inputs are all the label and symbol objects of the current tile. The output is overlap free label and symbol
	 * placement with the greedy strategy. The placement model is either the two fixed point or the four fixed point
	 * model.
	 * 
	 * @param labels
	 *            labels from the current tile.
	 * @param symbols
	 *            symbols of the current tile.
	 * @param areaLabels
	 *            area labels from the current tile.
	 * @param cT
	 *            current tile with the x,y- coordinates and the zoom level.
	 * @return the processed list of labels.
	 */
	List<PointTextContainer> placeLabels(List<PointTextContainer> labels, List<SymbolContainer> symbols,
			List<PointTextContainer> areaLabels, Tile cT) {
		List<PointTextContainer> returnLabels = labels;
		mDependencyCache.generateTileAndDependencyOnTile(cT);

		preprocessAreaLabels(areaLabels);

		preprocessLabels(returnLabels);

		preprocessSymbols(symbols);

		removeEmptySymbolReferences(returnLabels, symbols);

		removeOverlappingSymbolsWithAreaLabels(symbols, areaLabels);

		mDependencyCache.removeOverlappingObjectsWithDependencyOnTile(returnLabels, areaLabels, symbols);

		if (!returnLabels.isEmpty()) {
			switch (PLACEMENT_MODEL) {
				case 0:
					returnLabels = processTwoPointGreedy(returnLabels, symbols, areaLabels);
					break;
				case 1:
					returnLabels = processFourPointGreedy(returnLabels, symbols, areaLabels);
					break;
				default:
					break;
			}
		}

		mDependencyCache.fillDependencyOnTile(returnLabels, symbols, areaLabels);

		return returnLabels;
	}

	/**
	 * This method removes all the Symbols, that overlap each other. So that the output is collision free.
	 * 
	 * @param symbols
	 *            symbols from the actual tile
	 */
	void removeOverlappingSymbols(List<SymbolContainer> symbols) {
		int dis = mSymbolDistanceToSymbol;

		for (int x = 0; x < symbols.size(); x++) {
			mSymbolContainer = symbols.get(x);
			mRect1.set((int) mSymbolContainer.x - dis, (int) mSymbolContainer.y - dis,
					(int) mSymbolContainer.x + mSymbolContainer.symbol.getWidth() + dis,
					(int) mSymbolContainer.y + mSymbolContainer.symbol.getHeight() + dis);

			for (int y = x + 1; y < symbols.size(); y++) {
				if (y != x) {
					mSymbolContainer = symbols.get(y);
					mRect2.set((int) mSymbolContainer.x, (int) mSymbolContainer.y,
							(int) mSymbolContainer.x + mSymbolContainer.symbol.getWidth(),
							(int) mSymbolContainer.y + mSymbolContainer.symbol.getHeight());

					if (android.graphics.Rect.intersects(mRect2, mRect1)) {
						symbols.remove(y);
						y--;
					}
				}
			}
		}
	}

	void setLabelDistanceToLabel(int labelDistanceToLabel) {
		mLabelDistanceToLabel = labelDistanceToLabel;
	}

	void setLabelDistanceToSymbol(int labelDistanceToSymbol) {
		mLabelDistanceToSymbol = labelDistanceToSymbol;
	}

	void setStartDistanceToSymbols(int startDistanceToSymbols) {
		mStartDistanceToSymbols = startDistanceToSymbols;
	}

	void setSymbolDistanceToSymbol(int symbolDistanceToSymbol) {
		mSymbolDistanceToSymbol = symbolDistanceToSymbol;
	}
}
