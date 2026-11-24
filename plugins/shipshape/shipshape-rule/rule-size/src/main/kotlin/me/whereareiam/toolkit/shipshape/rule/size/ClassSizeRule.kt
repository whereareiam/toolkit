package me.whereareiam.toolkit.shipshape.rule.size

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A top-level type of main code is at most 250 lines, from its first annotation to its closing brace; its
 * Javadoc and the imports above it are not counted. A longer one holds more than one job: it is split by use case,
 * and the tangle is not moved into a helper.
 */
internal class ClassSizeRule : ShapeRule {
	override val id = "class-size"
	override val category = SizeRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main) return emptyList()

		return parsed.topLevel.filter { parsed.linesOf(it) > LIMIT }.map {
			Breach(
				parsed.source.path, parsed.lineOf(it),
				"declares ${it.simpleName} in ${parsed.linesOf(it)} lines; a type has at most $LIMIT, split it by what each part does",
			)
		}
	}

	internal companion object {
		/** How many lines a top-level type spans at most. */
		const val LIMIT = 250
	}
}
