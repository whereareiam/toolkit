package me.whereareiam.toolkit.shipshape.rule.tests

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A test never waits a fixed time. It waits for what it expects, up to a deadline, or moves the clock
 * it handed in; a `Thread.sleep` is too short on a slow machine and too long on every other.
 */
internal class FixedSleepRule : SourceRule {
	override val id = "fixed-sleep"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.filterNot { it.main }.flatMap { source ->
		source.find(SLEEP).map {
			Breach(
				source.path, it.line,
				"waits a fixed time with Thread.sleep; wait for what the test expects, up to a deadline, or move the clock",
			)
		}
	}

	private companion object {
		val SLEEP = Regex("""\bThread\s*\.\s*sleep\s*\(""")
	}
}
