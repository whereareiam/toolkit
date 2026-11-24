package me.whereareiam.toolkit.shipshape.rule.language

import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShipshapeExtension

/** The rules of how Java is used: stated nullability, no reflection, no broad catch, no vague names and no static state. */
object LanguageRules {
	/** The category the rules lie in, which `verifyLanguage` runs. */
	@JvmField
	val CATEGORY = RuleCategory(
		"language",
		"Verifies how the language is used: stated nullability, no reflection, no broad catch, no vague names and no static state.",
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

	/**
	 * The exemption of a static field a framework forces, as a static door a game's mixins must reach or a static holder a
	 * library requires (`static-state`), by the full name of its type and its own name:
	 *
	 * ```kotlin
	 * shipshape {
	 * 	exempt(LanguageRules.FORCED_STATIC, "com.example.mod.ModDoor.door", "The mixins reach the mod only statically")
	 * }
	 * ```
	 */
	@JvmField
	val FORCED_STATIC = StaticStateRule.FORCED_STATIC

	/** Registers the rules and the exemption kinds they read with [shipshape]. */
	fun registerWith(shipshape: ShipshapeExtension) {
		shipshape.exemptionKind(CATCHES_EVERYTHING)
		shipshape.exemptionKind(FORCED_STATIC)
		shipshape.source(BroadCatchRule())
		shipshape.source(ReflectionRule())
		shipshape.shape(StatedNullabilityRule())
		shipshape.shape(StaticStateRule())
		shipshape.source(VagueNameRule())
	}
}
