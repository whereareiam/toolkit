package me.whereareiam.toolkit.shipshape.rule.models

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.type.RoleFolder

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

	/**
	 * Says whether [source] is the shape of a configuration file: a class below `model/config/`, which a configuration
	 * library (Configura, Jackson) makes empty and fills from the file field by field. It is no value handed around, so
	 * its fields are set after it is made and keep what the library set.
	 */
	internal fun isFileShape(source: JavaSource): Boolean = source.packageName.split('.').zipWithNext()
		.any { (folder, below) -> folder == RoleFolder.MODEL.folder && below == RoleFolder.CONFIG.folder }
}
