package org.oscim.event;

import org.oscim.core.MapPosition;

public interface TouchListener {
	boolean onPress(MotionEvent e, MapPosition pos);

	boolean onLongPress(MotionEvent e, MapPosition pos);

	boolean onTap(MotionEvent e, MapPosition pos);
}
