package me.whereareiam.toolkit.shipshape.rule.flow

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of control flow: cognitive complexity, no `else if` and no ladders. */
object FlowRules {
	/** The category the rules lie in, which `verifyFlow` runs. */
	@JvmField
	val CATEGORY = RuleCategory("flow", "Verifies the control flow: its cognitive complexity, no else if and no ladders.")

	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.shape(ComplexityRule())
		shipshape.shape(ElseIfRule())
		shipshape.shape(LadderRule())
	}
}
