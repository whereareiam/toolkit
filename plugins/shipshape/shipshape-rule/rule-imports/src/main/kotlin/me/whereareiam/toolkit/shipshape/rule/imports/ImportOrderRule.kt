package me.whereareiam.toolkit.shipshape.rule.imports

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.measure.ImportOrder
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/** Imports stand in the order [ImportOrder] gives them. Main code and tests are read alike. */
internal class ImportOrderRule : SourceRule {
	override val id = "import-order"
	override val category = ImportRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.mapNotNull { source ->
		val written = imports(source).ifEmpty { return@mapNotNull null }
		val stands = written.map { it.second }
		val wrong = misplaced(stands, ImportOrder.ordered(stands)) ?: return@mapNotNull null

		Breach(source.path, written[wrong].first, "imports out of their order; $ASKED")
	}

	/** The index of the first line of [stands] that is not what [asked] has there, or `null` where the two are one. */
	private fun misplaced(stands: List<String>, asked: List<String>): Int? {
		if (stands == asked) return null

		val first = stands.zip(asked).indexOfFirst { (line, belongs) -> line != belongs }

		return first.takeIf { it >= 0 } ?: minOf(stands.size - 1, asked.size)
	}

	/** The lines from the first import to the last, each with its number; a blank line is an empty text. */
	private fun imports(source: JavaSource): List<Pair<Int, String>> {
		val lines = source.lines.mapIndexed { index, line -> index + 1 to line.trim() }
		val first = lines.indexOfFirst { ImportOrder.isImport(it.second) }.takeIf { it >= 0 } ?: return emptyList()
		val last = lines.indexOfLast { ImportOrder.isImport(it.second) }

		return lines.subList(first, last + 1).filter { it.second.isEmpty() || ImportOrder.isImport(it.second) }
	}

	private companion object {
		const val ASKED = "imports stand alphabetically, java and javax after a blank line, then the static ones after another"
	}
}
