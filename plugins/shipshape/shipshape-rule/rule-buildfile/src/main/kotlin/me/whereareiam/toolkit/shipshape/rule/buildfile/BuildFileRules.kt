package me.whereareiam.toolkit.shipshape.rule.buildfile

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of build files: the order of plugins and of dependencies, roles apart, and libraries from the catalog. */
object BuildFileRules {
	/** The category the rules lie in, which `verifyBuildfile` runs. */
	@JvmField
	val CATEGORY = RuleCategory(
		"buildfile",
		"Verifies the build files: the order of plugins and of dependencies, and libraries from the catalog.",
	)

	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.build(CatalogCoordinateRule())
		shipshape.build(DependencyOrderRule())
		shipshape.build(PluginOrderRule())
		shipshape.build(RoleSeparationRule())
	}
}
