package me.whereareiam.toolkit.shipshape.rule.lombok

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of what Lombok writes: loggers, constructors, accessors, the accessor style and builders. */
object LombokRules {
	/** The category the rules lie in, which `verifyLombok` runs. */
	@JvmField
	val CATEGORY = RuleCategory(
		"lombok",
		"Verifies that Lombok writes what it can: loggers, constructors and accessors, and where builders stand.",
	)

	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.source(AccessorsAnnotationRule())
		shipshape.shape(AssigningConstructorRule())
		shipshape.shape(BuilderPlaceRule())
		shipshape.source(EmptyPrivateConstructorRule())
		shipshape.source(HandWrittenLoggerRule())
		shipshape.shape(ReturningAccessorRule())
	}
}
