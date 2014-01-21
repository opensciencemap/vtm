package org.oscim.theme.carto;

import static java.lang.System.out;

import java.util.Map;

import org.jeo.filter.Filter;
import org.jeo.map.Rule;
import org.jeo.map.Selector;

public class RuleDebug {

	static void printRule(Rule r, int level) {

		out.println("> " + level + " >");
		out.println(formatRule(r, level));
	}

	public static String formatRule(Rule r, int indent) {
		StringBuilder sb = new StringBuilder();
		String pad = "";
		for (int i = 0; i < indent; i++) {
			pad += " ";
		};

		sb.append(pad);
		for (Selector s : r.getSelectors()) {
			sb.append(formatSelector(s));
			sb.append(",");
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		sb.append(pad).append(" {").append("\n");

		for (Map.Entry<String, Object> e : r.properties().entrySet()) {
			sb.append(pad).append("  ").append(e.getKey()).append(": ").append(e.getValue())
					.append(";\n");
		}

		for (Rule nested : r.nested()) {
			sb.append(nested.toString(indent + 2)).append("\n");
		}

		sb.append(pad).append("}");
		return sb.toString();
	}

	public static String formatSelector(Selector s) {
		StringBuffer sb = new StringBuffer();

		if (s.getName() != null) {
			sb.append(s.getName());
		}
		if (s.getId() != null) {
			sb.append("#").append(s.getId());
		}
		for (String c : s.getClasses()) {
			sb.append(".").append(c);
		}
		if (s.getFilter() != null && s.getFilter() != Filter.TRUE) {
			sb.append("[").append(s.getFilter()).append("]");
		}
		if (s.getAttachment() != null) {
			sb.append("::").append(s.getAttachment());
		}

		if (s.isWildcard()) {
			sb.append("*");
		}

		return sb.toString();
	}
}
