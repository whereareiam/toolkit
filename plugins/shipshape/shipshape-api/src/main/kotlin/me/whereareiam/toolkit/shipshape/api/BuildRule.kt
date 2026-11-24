package me.whereareiam.toolkit.shipshape.api

import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.api.report.Breach

/**
 * A rule that is held on a build file, a script of the build read as text: each `build.gradle.kts`, and each
 * `settings.gradle.kts` where the rule reads those too. Its category's task runs it.
 */
interface BuildRule : Rule {
	/** The scripts the rule reads; a project's build script alone unless the rule says otherwise. */
	val scripts: Set<BuildFile.Script>
		get() = setOf(BuildFile.Script.BUILD)

	/**
	 * Holds [file], one of the [scripts] the rule reads, against the rule.
	 *
	 * @param context what the rule may know of the build beside the file
	 * @return one breach per place that breaks the rule, in the order of the lines; empty when the rule holds
	 */
	fun check(file: BuildFile, context: RuleContext): List<Breach>
}
