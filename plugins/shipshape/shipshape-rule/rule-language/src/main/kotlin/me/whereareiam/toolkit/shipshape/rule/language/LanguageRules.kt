package me.whereareiam.toolkit.shipshape.rule.language

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of how Java is used: stated nullability, no reflection, no broad catch and no vague names. */
object LanguageRules {
	/** The category the rules lie in, which `verifyLanguage` runs. */
	@JvmField
	val CATEGORY = RuleCategory(
		"language",
		"Verifies how the language is used: stated nullability, no reflection, no broad catch and no vague names.",
	)

	/**
	 * The exemption of a class that is a boundary which must answer whatever happens, and so catches `Exception`,
	 * `RuntimeException` or `Throwable` (`broad-catch`):
	 *
	 * ```kotlin
	 * shipshape {
	 * 	exempt(LanguageRules.CATCHES_EVERYTHING, "com.example.delivery.DeliveryWorker", "A delivery that fails must not end the worker")
	 * }
	 * ```
	 */
	@JvmField
	val CATCHES_EVERYTHING = BroadCatchRule.CATCHES_EVERYTHING

	/** Registers the rules and the exemption kinds they read with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.exemptionKind(CATCHES_EVERYTHING)
		shipshape.source(BroadCatchRule())
		shipshape.source(ReflectionRule())
		shipshape.shape(StatedNullabilityRule())
		shipshape.source(VagueNameRule())
	}
}
