package me.whereareiam.toolkit.shipshape.rule.types

import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of typed values: no `Object`, no choice by text, no state as text, enums that know their word. */
object TypeRules {
	/** Registers the rules with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.shape(EnumWordRule())
		shipshape.shape(ObjectTypeRule())
		shipshape.shape(StringChoiceRule())
		shipshape.shape(StringStateRule())
	}
}
