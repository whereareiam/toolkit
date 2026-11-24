package me.whereareiam.toolkit.shipshape.api.report

import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/** A breach with the rule that found it, as a report prints and counts it. */
class Finding(
	/** The id of the rule that found the breach. */
	val rule: String,
	val enforcement: Enforcement,
	val breach: Breach,
) {
	constructor(rule: Rule, breach: Breach) : this(rule.id, rule.enforcement, breach)

	/** The breach in one line: the rule, the place, and what is asked. */
	val sentence: String
		get() = "$rule ${breach.place} ${breach.asks}"

	override fun toString(): String = sentence
}
