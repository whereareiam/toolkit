package me.whereareiam.toolkit.shipshape.rule.format

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of how source is laid out: line width, indentation, blank lines and guards on one line. */
object FormatRules {
	/** The category the rules lie in, which `verifyFormat` runs. */
	@JvmField
	val CATEGORY = RuleCategory(
		"format",
		"Verifies how the source is laid out: the width of a line, the indentation, the blank lines and guards on one line.",
	)

	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.shape(BlankLineRule())
		shipshape.source(LineLengthRule())
		shipshape.shape(OneLineGuardRule())
		shipshape.source(SpaceIndentationRule())
	}
}
