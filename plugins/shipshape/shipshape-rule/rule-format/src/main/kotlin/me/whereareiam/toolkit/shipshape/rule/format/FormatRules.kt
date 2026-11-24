package me.whereareiam.toolkit.shipshape.rule.format

import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of how source is laid out: line width, indentation and blank lines. */
object FormatRules {
	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.shape(BlankLineRule())
		shipshape.source(LineLengthRule())
		shipshape.source(SpaceIndentationRule())
	}
}
