package me.whereareiam.toolkit.shipshape.api.exemption

import org.gradle.api.GradleException

/**
 * What the blocks of one build file declare as excused, in the order it was declared. Every declaration names a place
 * and a reason, and a place of one kind once. Shipshape keeps the declarations of its own tasks' kinds in one; a plugin
 * whose own tasks read kinds of their own keeps them in another, and holds what its rules found against them through
 * [DeclaredExemptions].
 */
class ExemptionDeclarations {
	private val declarations = mutableListOf<ExemptionDeclaration>()

	/**
	 * Declares that [place] is excused from the rules that read [kind].
	 *
	 * @param block the block of the build file the declaration is written in, which every message names
	 * @throws GradleException when the place or the reason is blank, or the place of that kind is declared already
	 */
	fun declare(kind: ExemptionKind, place: String, reason: String, block: String) {
		if (place.isBlank()) throw GradleException("$block declares ${kind.method} without the place it excuses")
		if (reason.isBlank()) {
			val incomplete = "$block declares ${kind.method}(\"$place\") without a reason"

			throw GradleException("$incomplete; say why the rule does not hold there")
		}

		val twice = declarations.firstOrNull { it.kind == kind && it.place == place }
		if (twice != null) throw GradleException("$block declares ${kind.method}(\"$place\"), which ${twice.written} declares already")

		declarations += ExemptionDeclaration(kind, place, reason, block)
	}

	/** The declarations of [kinds], by the kind's method and then by the place. */
	fun of(kinds: Set<ExemptionKind>): List<ExemptionDeclaration> =
		declarations.filter { it.kind in kinds }.sortedWith(compareBy({ it.kind.method }, ExemptionDeclaration::place))
}
