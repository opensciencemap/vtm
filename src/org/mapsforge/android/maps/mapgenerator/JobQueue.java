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
package org.mapsforge.android.maps.mapgenerator;

import java.util.ArrayList;
import java.util.PriorityQueue;

import org.mapsforge.android.maps.MapView;

import android.os.SystemClock;

/**
 * A JobQueue keeps the list of pending jobs for a MapView and prioritizes them.
 */
public class JobQueue {
	private static final int INITIAL_CAPACITY = 128;

	private final MapView mMapView;
	private PriorityQueue<MapGeneratorJob> mPriorityQueue;
	private boolean mScheduleNeeded;

	/**
	 * @param mapView
	 *            the MapView whose jobs should be organized.
	 */
	public JobQueue(MapView mapView) {
		mMapView = mapView;
		mPriorityQueue = new PriorityQueue<MapGeneratorJob>(INITIAL_CAPACITY);
	}

	/**
	 * Adds the given job to this queue. Does nothing if the given job is already in this queue.
	 * 
	 * @param mapGeneratorJob
	 *            the job to be added to this queue.
	 */
	public synchronized void addJob(MapGeneratorJob mapGeneratorJob) {
		if (!mPriorityQueue.contains(mapGeneratorJob))
		// priorityQueue.remove(mapGeneratorJob);
		{
			mapGeneratorJob.tile.isLoading = true;
			mPriorityQueue.offer(mapGeneratorJob);
		}
	}

	/**
	 * @param jobs
	 *            the job to be added to this queue.
	 */
	public synchronized void setJobs(ArrayList<MapGeneratorJob> jobs) {
		mPriorityQueue.clear();
		for (MapGeneratorJob job : jobs)
			mPriorityQueue.offer(job);
		// priorityQueue.addAll(jobs);
		mScheduleNeeded = true;
	}

	/**
	 * Removes all jobs from this queue.
	 */
	public synchronized void clear() {
		mPriorityQueue.clear();
	}

	/**
	 * @return true if this queue contains no jobs, false otherwise.
	 */
	public synchronized boolean isEmpty() {
		return mPriorityQueue.isEmpty();
	}

	/**
	 * @return the most important job from this queue or null, if empty.
	 */
	public synchronized MapGeneratorJob poll() {
		if (mScheduleNeeded) {
			mScheduleNeeded = false;
			schedule();
		}
		return mPriorityQueue.poll();
	}

	/**
	 * Request a scheduling of all jobs that are currently in this queue.
	 */
	public synchronized void requestSchedule() {
		mScheduleNeeded = true;
	}

	/**
	 * Schedules all jobs in this queue.
	 */
	private void schedule() {
		PriorityQueue<MapGeneratorJob> tempJobQueue = new PriorityQueue<MapGeneratorJob>(INITIAL_CAPACITY);

		TileScheduler.time = SystemClock.uptimeMillis();
		TileScheduler.mapPosition = mMapView.getMapPosition().getMapPosition();

		while (!mPriorityQueue.isEmpty()) {
			MapGeneratorJob mapGeneratorJob = mPriorityQueue.poll();
			double priority = TileScheduler.getPriority(mapGeneratorJob, mMapView);
			mapGeneratorJob.setPriority(priority);
			tempJobQueue.offer(mapGeneratorJob);
		}

		mPriorityQueue = tempJobQueue;
	}

}
