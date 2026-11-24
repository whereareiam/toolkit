package me.whereareiam.toolkit.shipshape.api

import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach

/**
 * A rule that needs the shape of a class: what a constructor's body does, what a method returns, which annotations a
 * field or a parameter carries. It reads one file at a time, as the JDK's parser read it, and its category's task runs it.
 */
interface ShapeRule : Rule {
	/**
	 * Holds one file against the rule.
	 *
	 * @param parsed the file with its syntax tree; nothing is resolved and nothing compiled
	 * @param context what the rule may know of the build beside the file
	 * @return one breach per place that breaks the rule, each with its file, its line and what is asked instead;
	 * empty when the rule holds
	 */
	fun check(parsed: ParsedSource, context: RuleContext): List<Breach>
}
