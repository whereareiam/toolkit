package me.whereareiam.toolkit.shipshape.rule.lombok

import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of what Lombok writes: loggers, constructors, accessors, the accessor style and builders. */
object LombokRules {
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
