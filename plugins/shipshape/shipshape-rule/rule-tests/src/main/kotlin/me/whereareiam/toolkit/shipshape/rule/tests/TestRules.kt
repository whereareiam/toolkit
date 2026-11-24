package me.whereareiam.toolkit.shipshape.rule.tests

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of tests: their names, a test per module, the test clock, no fixed waits and no mocks of own types. */
object TestRules {
	/** The category the rules lie in, which `verifyTests` runs. */
	@JvmField
	val CATEGORY = RuleCategory(
		"tests",
		"Verifies the tests: their names, a test for every module, the test clock and what tests stand in for.",
	)

	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.source(FixedSleepRule())
		shipshape.source(MockedOwnTypeRule())
		shipshape.source(ModuleTestRule())
		shipshape.source(TestClockRule())
		shipshape.source(TestNameRule())
	}
}
