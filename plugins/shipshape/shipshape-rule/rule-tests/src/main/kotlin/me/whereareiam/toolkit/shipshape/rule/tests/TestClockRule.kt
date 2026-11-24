package me.whereareiam.toolkit.shipshape.rule.tests

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * The clock a test moves is written once, in the main code of a library of test fixtures, and not again in
 * the tests of a build. The code of tests, its fixtures among them, declares no class that extends `java.time.Clock`.
 */
internal class TestClockRule : SourceRule {
	override val id = "test-clock"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.filterNot { it.main }.flatMap { source ->
		source.find(CLOCK).map {
			Breach(
				source.path, it.line,
				"writes the clock ${it.group(1)} for its tests; take the one of a library of test fixtures, a helper of tests is " +
					"written once",
			)
		}
	}
	private companion object {
		val CLOCK = Regex("""\bclass\s+(\w+)\s+extends\s+(?:java\.time\.)?Clock\b""")
	}
}
