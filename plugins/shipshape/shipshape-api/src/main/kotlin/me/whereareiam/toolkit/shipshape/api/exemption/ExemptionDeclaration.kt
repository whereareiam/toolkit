package me.whereareiam.toolkit.shipshape.api.exemption

import java.io.Serializable

/**
 * One place a build file excuses from the rules that read [kind], with the reason it gives: `catchesEverything("…", why)`
 * written in the block [block]. A declaration that excuses nothing is a breach, and it names the declaration as it was
 * written, in its block.
 */
data class ExemptionDeclaration(
	val kind: ExemptionKind,
	/** The place exactly as the declaration writes it, `com.example.DeliveryWorker`. */
	val place: String,
	/** Why the rule does not hold there; what a reader of the build file is told. */
	val reason: String,
	/** The block of the build file the declaration is written in, `shipshape`, or the block of a plugin that hands it on. */
	val block: String,
) : Serializable {
	/** The place a breach is marked with when this declaration could excuse it. */
	val exemption: ExemptionPlace
		get() = ExemptionPlace(kind, place)

	/** How the build file writes the declaration, as a report names it: `catchesEverything("…") in shipshape`. */
	val written: String
		get() = "${kind.method}(\"$place\") in $block"

	override fun toString(): String = written

	private companion object {
		private const val serialVersionUID = 1L
	}
}
