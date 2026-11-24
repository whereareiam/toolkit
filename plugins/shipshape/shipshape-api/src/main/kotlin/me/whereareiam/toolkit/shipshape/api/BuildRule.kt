package me.whereareiam.toolkit.shipshape.api

import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.api.report.Breach

/** A rule that is held on a build file, `build.gradle.kts`, read as text; its category's task runs it. */
interface BuildRule : Rule {
	/**
	 * Holds [file] against the rule.
	 *
	 * @param context what the rule may know of the build beside the file
	 * @return one breach per place that breaks the rule, in the order of the lines; empty when the rule holds
	 */
	fun check(file: BuildFile, context: RuleContext): List<Breach>
}
