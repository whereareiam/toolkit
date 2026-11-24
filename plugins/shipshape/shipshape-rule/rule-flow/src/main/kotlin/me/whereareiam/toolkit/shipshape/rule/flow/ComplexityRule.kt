package me.whereareiam.toolkit.shipshape.rule.flow

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * The cognitive complexity of a method of main code ([Complexity]) is at most 15. A flow that is harder to
 * follow is several decisions; they become early returns, a table, behaviour on an enum, or methods of their own.
 */
internal class ComplexityRule : ShapeRule {
	override val id = "cognitive-complexity"

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main) return emptyList()

		return parsed.methods.mapNotNull { (type, method) ->
			val complexity = Complexity.of(method)
			if (complexity <= LIMIT) return@mapNotNull null

			val named = if (ParsedSource.isConstructor(method)) "the constructor of ${type.simpleName}" else "${method.name}()"

			Breach(
				parsed.source.path, parsed.lineOf(method),
				"writes $named with a cognitive complexity of $complexity; at most $LIMIT, the decisions told apart by early " +
					"returns, a table or methods of their own",
			)
		}
	}

	internal companion object {
		/** The cognitive complexity a method has at most. */
		const val LIMIT = 15
	}
}
