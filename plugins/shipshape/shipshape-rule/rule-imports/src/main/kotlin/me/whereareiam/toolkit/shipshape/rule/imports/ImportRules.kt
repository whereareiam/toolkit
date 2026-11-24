package me.whereareiam.toolkit.shipshape.rule.imports

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of imports: what is imported, how, and in which order. */
object ImportRules {
	/** The category the rules lie in, which `verifyImports` runs. */
	@JvmField
	val CATEGORY = RuleCategory("imports", "Verifies what is imported, how, and in which order.")

	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.source(ImportOrderRule())
		shipshape.source(ImportableNameRule())
		shipshape.source(StaticImportRule())
		shipshape.source(WildcardImportRule())
	}
}
