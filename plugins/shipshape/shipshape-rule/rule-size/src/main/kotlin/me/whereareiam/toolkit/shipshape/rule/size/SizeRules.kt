package me.whereareiam.toolkit.shipshape.rule.size

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of size: of a class, a method, its parameters and a class's collaborators. */
object SizeRules {
	/** The category the rules lie in, which `verifySize` runs. */
	@JvmField
	val CATEGORY = RuleCategory("size", "Verifies the size of a class and of a method, its parameters and a class's collaborators.")

	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.shape(ClassSizeRule())
		shipshape.shape(CollaboratorRule())
		shipshape.shape(MethodSizeRule())
		shipshape.shape(ParameterRule())
	}
}
