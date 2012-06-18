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
package org.mapsforge.android.mapgenerator;

import org.mapsforge.android.MapRenderer;
import org.mapsforge.android.MapView;
import org.mapsforge.android.utils.PausableThread;

/**
 * A MapWorker uses a {@link MapGenerator} to generate map tiles. It runs in a separate thread to avoid blocking the UI
 * thread.
 */
public class MapWorker extends PausableThread {
	private static final String THREAD_NAME = "MapWorker";

	private final JobQueue mJobQueue;
	private MapGenerator mMapGenerator;
	private MapRenderer mMapRenderer;

	/**
	 * @param mapView
	 *            the MapView for which this MapWorker generates map tiles.
	 */
	public MapWorker(MapView mapView) {
		super();
		mJobQueue = mapView.getJobQueue();
	}

	/**
	 * @param mapGenerator
	 *            the MapGenerator which this MapWorker should use.
	 */
	public void setMapGenerator(MapGenerator mapGenerator) {
		mMapGenerator = mapGenerator;
	}

	/**
	 * @param mapRenderer
	 *            the MapRenderer
	 */
	public void setMapRenderer(MapRenderer mapRenderer) {
		mMapRenderer = mapRenderer;
	}

	@Override
	protected void afterRun() {
		// empty
	}

	@Override
	protected void doWork() {
		MapGeneratorJob mapGeneratorJob = mJobQueue.poll();

		if (mMapGenerator == null || mapGeneratorJob == null)
			return;

		boolean success = mMapGenerator.executeJob(mapGeneratorJob);

		if (!isInterrupted() && success) {
			mMapRenderer.passTile(mapGeneratorJob);
		}
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected int getThreadPriority() {
		return (Thread.NORM_PRIORITY + Thread.MIN_PRIORITY) / 2;
	}

	@Override
	protected boolean hasWork() {
		return !mJobQueue.isEmpty() && mMapRenderer.processedTile();
	}
}
