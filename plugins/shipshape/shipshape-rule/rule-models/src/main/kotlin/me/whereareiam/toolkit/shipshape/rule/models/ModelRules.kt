package me.whereareiam.toolkit.shipshape.rule.models

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of models and records: immutable values, copied collections and small records. */
object ModelRules {
	/** The category the rules lie in, which `verifyModels` runs. */
	@JvmField
	val CATEGORY = RuleCategory("models", "Verifies that models are immutable values and records small carriers.")

	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.shape(CollectionCopyRule())
		shipshape.shape(ImmutableModelRule())
		shipshape.shape(NestedRecordRule())
		shipshape.shape(TopLevelRecordRule())
	}
}
