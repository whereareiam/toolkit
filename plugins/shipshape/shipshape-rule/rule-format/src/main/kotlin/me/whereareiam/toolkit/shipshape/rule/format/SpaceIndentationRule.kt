package me.whereareiam.toolkit.shipshape.rule.format

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * Code is indented with tabs, in main code and in tests. A space may follow the tabs to align what
 * continues a statement, and never begins a line of code. The lines of a comment and of a text block are
 * not code: the star of a Javadoc's line stands one space in, below the slash that opens the Javadoc, and a
 * text block is indented as its text asks.
 */
internal class SpaceIndentationRule : SourceRule {
	override val id = "space-indentation"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.flatMap { source ->
		val code = source.code.lines()

		source.lines.indices.filter { beginsCodeWithASpace(source, code, it) }.map {
			Breach(
				source.path, it + 1,
				"begins with a space; code is indented with tabs, and a space only aligns behind them",
			)
		}
	}

	/**
	 * Says whether the line at [index] begins with a space and then with code. What a line begins with is code
	 * where the file without its comments and literals, [code], has the same character at the same place.
	 */
	private fun beginsCodeWithASpace(source: JavaSource, code: List<String>, index: Int): Boolean {
		val written = source.lines[index]
		if (!written.startsWith(' ')) return false

		val first = written.indexOfFirst { !it.isWhitespace() }
		val stripped = code.getOrElse(index) { "" }

		return first > 0 && stripped.length > first && stripped.substring(0, first).isBlank() && stripped[first] == written[first]
	}
}
