package me.whereareiam.toolkit.shipshape.api.exemption

/**
 * The place a declaration of [kind] names to excuse a breach: a class by its full name, a module by its Gradle path, as
 * the kind says.
 */
data class ExemptionPlace(
	val kind: ExemptionKind,
	/** The place exactly as a declaration writes it, `com.example.DeliveryWorker`. */
	val place: String,
)
