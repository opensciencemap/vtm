package org.oscim.theme.carto;

import static java.lang.System.out;

import java.util.List;
import java.util.Map;

import org.jeo.feature.BasicFeature;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;

//imitate Feature behaviour for tags and zoom-level
class MatcherFeature extends BasicFeature {
	TagSet mTags;
	Integer mZoom;

	void setTags(TagSet tags) {
		mTags = tags;
	}

	void setZoom(int zoom) {
		mZoom = Integer.valueOf(zoom);
	}

	protected MatcherFeature() {
		super("");
	}

	@Override
	public Object get(String key) {
		//out.println("get(" + key + ")");

		if (key.equals("zoom"))
			return mZoom;

		Tag t = mTags.get(key.intern());
		if (t == null)
			return null;

		//out.println("value: " + t.value);

		return t.value;
	}

	@Override
	public void put(String key, Object val) {
		out.println("EEEK put()");
	}

	@Override
	public List<Object> list() {
		out.println("EEEK list()");
		return null;
	}

	@Override
	public Map<String, Object> map() {
		out.println("EEEK map()");
		return null;
	}

	@Override
	public Object get(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set(int arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

};
