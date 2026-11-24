package me.whereareiam.toolkit.shipshape.rule.flow

import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of control flow: cognitive complexity, no `else if` and no ladders. */
object FlowRules {
	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.shape(ComplexityRule())
		shipshape.shape(ElseIfRule())
		shipshape.shape(LadderRule())
	}
}
