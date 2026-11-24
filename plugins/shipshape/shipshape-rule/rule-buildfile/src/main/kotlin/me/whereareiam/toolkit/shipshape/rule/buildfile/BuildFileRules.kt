package me.whereareiam.toolkit.shipshape.rule.buildfile

import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of build files: the order of plugins and of dependencies, and roles apart. */
object BuildFileRules {
	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.build(DependencyOrderRule())
		shipshape.build(PluginOrderRule())
		shipshape.build(RoleSeparationRule())
	}
}
