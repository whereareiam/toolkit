package me.whereareiam.toolkit.shipshape.api

import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach

/**
 * A rule that is read from the Java sources of the build as text. It sees every file at once, so it may compare a
 * file with its package, its module or the rest of the build, and `verifySource` runs it.
 */
interface SourceRule : Rule {
	/**
	 * The files beside the Java sources the rule reads, as patterns from the root project's folder (`AGENTS.md`), which
	 * `verifySource` then tracks: a change of one runs the task again.
	 */
	val reads: Set<String>
		get() = emptySet()

	/**
	 * Holds the build's sources against the rule.
	 *
	 * @param sources every Java file of the build, each read once, module by module
	 * @param context what the rule may know of the build beside the files
	 * @return one breach per place that breaks the rule, each with its file, its line and what is asked instead;
	 * empty when the rule holds
	 */
	fun check(sources: JavaSources, context: RuleContext): List<Breach>
}
