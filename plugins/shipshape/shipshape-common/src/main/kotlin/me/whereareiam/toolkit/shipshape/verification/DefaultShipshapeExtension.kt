package me.whereareiam.toolkit.shipshape.verification

import me.whereareiam.toolkit.shipshape.api.BuildRule
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind

/**
 * The `shipshape` block as the plugin creates it: it keeps what the block registers and declares in its [registry],
 * which the plugin's tasks read.
 */
abstract class DefaultShipshapeExtension : ShipshapeExtension {
	/** The rules, exemption kinds and declarations the block holds. */
	val registry = RuleRegistry()

	override fun shape(rule: ShapeRule) = registry.register(rule)

	override fun source(rule: SourceRule) = registry.register(rule)

	override fun build(rule: BuildRule) = registry.register(rule)

	override fun exemptionKind(kind: ExemptionKind) = registry.register(kind)

	override fun exempt(kind: ExemptionKind, place: String, reason: String, block: String) =
		registry.declare(kind, place, reason, block)
}
