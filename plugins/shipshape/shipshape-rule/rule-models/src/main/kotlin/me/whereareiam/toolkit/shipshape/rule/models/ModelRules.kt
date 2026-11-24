package me.whereareiam.toolkit.shipshape.rule.models

import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of models and records: immutable values, copied collections and small records. */
object ModelRules {
	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.shape(CollectionCopyRule())
		shipshape.shape(ImmutableModelRule())
		shipshape.shape(NestedRecordRule())
		shipshape.shape(TopLevelRecordRule())
	}
}
