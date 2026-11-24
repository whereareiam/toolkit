package me.whereareiam.toolkit.shipshape.api

import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * What every rule shipshape runs states of itself. A rule is one of `ShapeRule`, `SourceRule` and `BuildRule`, which
 * say what it reads; it is registered once, built in or through the `shipshape` block, and never twice under one
 * [id].
 *
 * A task keeps its rules with the configuration cache, so a rule holds plain values and no project.
 */
interface Rule {
	/**
	 * The rule's id, its name in lower case with hyphens between its words, `wildcard-import`: a breach is printed and
	 * counted under it, and no two rules a build runs share one. It is not called `name`, so that an enum, whose `name`
	 * is its constant's, can be a set of rules.
	 */
	val id: String

	/** What a breach of the rule costs the build. */
	val enforcement: Enforcement

	/**
	 * The kinds of exemption the rule's breaches may be excused by; each must be registered with the `shipshape` block.
	 * A breach names one through [me.whereareiam.toolkit.shipshape.api.report.Breach.exemption].
	 */
	val exemptions: Set<ExemptionKind>
		get() = emptySet()
}
