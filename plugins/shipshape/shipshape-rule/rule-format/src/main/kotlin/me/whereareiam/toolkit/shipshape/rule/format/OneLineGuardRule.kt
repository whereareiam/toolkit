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
import me.whereareiam.toolkit.shipshape.api.java.SourceText
import me.whereareiam.toolkit.shipshape.api.measure.LineWidth
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A guard, an `if` without `else` that only leaves (`return`, `throw`, `continue` or `break`), has one form. Where it
 * fits in [LineWidth.LIMIT] columns it stands on one line without braces, `if (closed) return;`, however it is wrapped
 * now. Where it does not, it stands without braces, its statement on the lines below. The one line is read from the
 * guard as written, its lines joined, so the breach shows it. A guard with a comment inside it stays as it is.
 */
internal class OneLineGuardRule : ShapeRule {
	override val id = "one-line-guard"
	override val category = FormatRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		val found = mutableListOf<Breach>()
		object : TreeScanner<Unit?, Unit?>() {
			override fun visitIf(node: IfTree, unused: Unit?): Unit? {
				asked(parsed, node)?.let { found += Breach(parsed.source.path, parsed.lineOf(node), it) }

				return super.visitIf(node, unused)
			}
		}.scan(parsed.topLevel, null)

		return found.sortedBy(Breach::line)
	}

	/** What [guard] is asked to change, or `null` where it is no guard or stands as one should. */
	private fun asked(parsed: ParsedSource, guard: IfTree): String? {
		val statement = left(guard) ?: return null
		val opening = parsed.source.lines[parsed.lineOf(guard) - 1]
		if (!opening.trimStart().startsWith("if") || commented(parsed.writtenOf(guard))) return null

		val braced = guard.thenStatement is BlockTree
		val line = opening.takeWhile(Char::isWhitespace) + "if " + joined(parsed.writtenOf(guard.condition)) + " " +
			joined(parsed.writtenOf(statement))
		if (LineWidth.of(line) <= LineWidth.LIMIT) return "$SPREAD${line.trim()}".takeIf { braced || parsed.linesOf(guard) > 1 }

		return BRACED.takeIf { braced }
	}

	/** The statement [guard] leaves by, where it is a guard: an `if` without `else` that does nothing else. */
	private fun left(guard: IfTree): StatementTree? {
		if (guard.elseStatement != null) return null

		val then = guard.thenStatement
		val statement = if (then is BlockTree) then.statements.singleOrNull() else then

		return statement?.takeIf { it is ReturnTree || it is ThrowTree || it is ContinueTree || it is BreakTree }
	}

	/** Says whether [written] holds a comment, which a guard joined onto one line would lose or swallow. */
	private fun commented(written: String): Boolean =
		written.contains("\"\"\"") || SourceText.strip(written, literals = true) != written

	/**
	 * [written] on one line: each line trimmed and joined by a space, but for none after an opening bracket or
	 * before a closing one or a dot.
	 */
	private fun joined(written: String): String = written.lines().map(String::trim).filter(String::isNotEmpty)
		.reduce { line, next -> if (line.last() in OPENING || next.first() in CLOSING) line + next else "$line $next" }

	internal companion object {
		const val SPREAD = "writes a guard over several lines or in braces, and it fits on one; write it as "
		const val BRACED = "braces a guard too wide for one line; it stands without braces, its statement on the lines below"

		private val OPENING = setOf('(', '[')
		private val CLOSING = setOf(')', ']', '.')
	}
}
