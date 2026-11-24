package me.whereareiam.toolkit.shipshape.rule.format

import com.sun.source.tree.BlockTree
import com.sun.source.tree.BreakTree
import com.sun.source.tree.ContinueTree
import com.sun.source.tree.IfTree
import com.sun.source.tree.ReturnTree
import com.sun.source.tree.StatementTree
import com.sun.source.tree.ThrowTree
import com.sun.source.util.TreeScanner
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.measure.LineWidth
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A guard stands on one line: an `if` without `else` that only leaves (`return`, `throw`, `continue` or `break`) is
 * written `if (closed) return;`, without braces, wherever that line is at most [LineWidth.LIMIT] columns wide. One
 * written over several lines, braced or not, is read here when its condition stands on the `if`'s line and its
 * statement on one line of its own; a guard with a comment beside its statement, or one too wide for one line, stays.
 */
internal class OneLineGuardRule : ShapeRule {
	override val id = "one-line-guard"
	override val category = FormatRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		val found = mutableListOf<Breach>()
		object : TreeScanner<Unit?, Unit?>() {
			override fun visitIf(node: IfTree, unused: Unit?): Unit? {
				joined(parsed, node)?.let { found += Breach(parsed.source.path, parsed.lineOf(node), SPREAD) }

				return super.visitIf(node, unused)
			}
		}.scan(parsed.topLevel, null)

		return found.sortedBy(Breach::line)
	}

	/** The guard [guard] written on one line, where it is a guard spread over several that fits on one; else `null`. */
	private fun joined(parsed: ParsedSource, guard: IfTree): String? {
		if (guard.elseStatement != null || parsed.linesOf(guard) == 1) return null

		val braced = guard.thenStatement as? BlockTree
		val statement = braced?.statements?.singleOrNull() ?: guard.thenStatement
		if (!leaves(statement) || parsed.linesOf(statement) != 1) return null

		val first = parsed.lineOf(guard)
		val last = parsed.endLineOf(guard)
		val opening = parsed.source.lines[first - 1].trimEnd()
		val written = parsed.source.lines[parsed.lineOf(statement) - 1].trim()
		val lines = if (braced == null) 2 else 3
		if (parsed.lineOf(statement) != first + 1 || last - first + 1 != lines || written.contains("//")) return null
		if (braced != null && (!opening.endsWith("{") || parsed.source.lines[last - 1].trim() != "}")) return null

		val line = opening.removeSuffix("{").trimEnd() + " " + written

		return line.takeIf { LineWidth.of(it) <= LineWidth.LIMIT }
	}

	private fun leaves(statement: StatementTree): Boolean =
		statement is ReturnTree || statement is ThrowTree || statement is ContinueTree || statement is BreakTree

	internal companion object {
		const val SPREAD = "spreads a guard of one statement over several lines; it fits on one, as if (closed) return;"
	}
}
