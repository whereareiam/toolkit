package me.whereareiam.toolkit.shipshape.rule.size

import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of size: of a class, a method, its parameters and a class's collaborators. */
object SizeRules {
	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.shape(ClassSizeRule())
		shipshape.shape(CollaboratorRule())
		shipshape.shape(MethodSizeRule())
		shipshape.shape(ParameterRule())
	}
}
