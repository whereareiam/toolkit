package me.whereareiam.toolkit.shipshape.rule.imports

import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of imports: what is imported, how, and in which order. */
object ImportRules {
	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.source(ImportOrderRule())
		shipshape.source(ImportableNameRule())
		shipshape.source(StaticImportRule())
		shipshape.source(WildcardImportRule())
	}
}
