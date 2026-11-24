package me.whereareiam.toolkit.shipshape.rule.size

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * The body of a method or a constructor of main code is at most 40 lines between its braces. A longer one
 * does several steps; each becomes a method named after what it does.
 */
internal class MethodSizeRule : ShapeRule {
	override val id = "method-size"
	override val category = SizeRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main) return emptyList()

		return parsed.methods.mapNotNull { (type, method) ->
			val body = method.body ?: return@mapNotNull null
			val lines = parsed.linesOf(body) - 2
			if (lines <= LIMIT) return@mapNotNull null

			val named = if (ParsedSource.isConstructor(method)) "the constructor of ${type.simpleName}" else "${method.name}()"

			Breach(
				parsed.source.path, parsed.lineOf(method),
				"writes $named in $lines lines; a body has at most $LIMIT, each step a method named after what it does",
			)
		}
	}

	internal companion object {
		/** How many lines a body holds between its braces at most. */
		const val LIMIT = 40
	}
}
