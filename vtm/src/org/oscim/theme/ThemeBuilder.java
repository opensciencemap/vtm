package org.oscim.theme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.theme.rule.Element;
import org.oscim.theme.rule.Rule;
import org.oscim.theme.rule.RuleBuilder;
import org.oscim.theme.rule.Selector;
import org.oscim.theme.rule.SingleKeyMatcher;
import org.oscim.theme.rule.SingleValueMatcher;
import org.oscim.theme.styles.Area;
import org.oscim.theme.styles.Line;
import org.oscim.theme.styles.RenderStyle;

public class ThemeBuilder {
	protected final ArrayList<RuleBuilder> mRulesList = new ArrayList<RuleBuilder>();
	protected final Stack<RuleBuilder> mRuleStack = new Stack<RuleBuilder>();

	protected int mLevels = 0;
	protected int mMapBackground = 0xffffffff;
	protected float mBaseTextSize = 1;

	protected RuleBuilder mCurrentRule;

	protected RenderTheme build() {

		Rule[] rules = new Rule[mRulesList.size()];
		for (int i = 0, n = rules.length; i < n; i++)
			rules[i] = mRulesList.get(i).onComplete();

		RenderTheme theme = new RenderTheme(mMapBackground, mBaseTextSize, rules, mLevels);

		mRulesList.clear();
		mRuleStack.clear();

		return theme;
	}

	public ThemeBuilder pop() {

		mRuleStack.pop();
		if (mRuleStack.empty()) {
			mRulesList.add(mCurrentRule);
		} else {
			mCurrentRule = mRuleStack.peek();
		}
		return this;
	}

	public ThemeBuilder push(RuleBuilder rule) {
		if (!mRuleStack.empty())
			mCurrentRule.addSubRule(rule);

		mCurrentRule = rule;
		mRuleStack.push(mCurrentRule);

		return this;
	}

	public ThemeBuilder push(String key, String value) {
		RuleBuilder b = new RuleBuilder(true, Element.ANY, ~0, 0,
		                                key == null ? null : new SingleKeyMatcher(key),
		                                value == null ? null : new SingleValueMatcher(value));
		push(b);
		return this;
	}

	public RuleBuilder pushParse(String keys, String values) {

		return RuleBuilder.create(mRuleStack, keys, values)
		    .zoom(~0)
		    .element(Element.ANY);
	}

	public ThemeBuilder addStyle(RenderStyle style) {
		mCurrentRule.addStyle(style);
		return this;
	}

	public static void main(String[] args) {

		ThemeBuilder b = new ThemeBuilder();

		//b.pushParse("highway", "residential|primary|motorway")

		b.push(RuleBuilder.get().select(Selector.FIRST))
		    .push("highway", null)
		    .addStyle(new Line(1, 1, 1))
		    .pop()

		    .push(RuleBuilder.get().select(Selector.WHEN_MATCHED))
		    .addStyle(new Area(1, 1))
		    .pop()
		    .pop();

		RenderTheme t = b.build();
		TagSet tags = new TagSet(1);
		RenderStyle[] styles;

		tags.add(new Tag("ahighway", "residential"));
		styles = t.matchElement(GeometryType.LINE, tags, 1);
		System.out.println(Arrays.deepToString(styles));

		//		tags.clear();
		//		tags.add(new Tag("highway", "motorway"));
		//		styles = t.matchElement(GeometryType.LINE, tags, 1);
		//		out.println(styles);
		//
		//		tags.clear();
		//		tags.add(new Tag("sup", "wup"));
		//		styles = t.matchElement(GeometryType.LINE, tags, 1);
		//		out.println(styles);
		//
		//		tags.clear();
		//		tags.add(new Tag("highway", "motorway"));
		//		styles = t.matchElement(GeometryType.LINE, tags, 1);
		//		out.println(styles);

	}
}
