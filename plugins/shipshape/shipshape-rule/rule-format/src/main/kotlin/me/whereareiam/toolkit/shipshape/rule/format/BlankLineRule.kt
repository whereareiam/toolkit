package me.whereareiam.toolkit.shipshape.rule.format

import com.sun.source.tree.BlockTree
import com.sun.source.tree.MethodTree
import com.sun.source.tree.ReturnTree
import com.sun.source.tree.StatementTree
import com.sun.source.util.TreeScanner
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A blank line separates the steps of a body. At most six statements of a block stand together without a
 * blank line between them; a longer run is several steps. A method's final `return` has a blank line before it once
 * what comes before it spans more than two lines; a computation of one or two lines may stay beside its return.
 */
internal class BlankLineRule : ShapeRule {
	override val id = "blank-line"

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main) return emptyList()

		val found = mutableListOf<Breach>()
		object : TreeScanner<Unit?, Unit?>() {
			override fun visitBlock(node: BlockTree, unused: Unit?): Unit? {
				found += runs(parsed, node.statements)

				return super.visitBlock(node, unused)
			}

			override fun visitMethod(node: MethodTree, unused: Unit?): Unit? {
				node.body?.let { body -> glued(parsed, body.statements)?.let { found += it } }

				return super.visitMethod(node, unused)
			}
		}.scan(parsed.topLevel, null)

		return found.sortedBy(Breach::line)
	}

	/** One breach for each run of more than [RUN] statements of [statements] that no blank line divides. */
	private fun runs(parsed: ParsedSource, statements: List<StatementTree>): List<Breach> {
		val found = mutableListOf<Breach>()
		var run = 1
		statements.zipWithNext().forEach { (before, after) ->
			run = if (parsed.blankBetween(before, after)) 1 else run + 1
			if (run == RUN + 1) found += Breach(parsed.source.path, parsed.lineOf(after), LONG_RUN)
		}

		return found
	}

	/** The breach of a final `return` that stands right below more than two lines, or `null`. */
	private fun glued(parsed: ParsedSource, statements: List<StatementTree>): Breach? {
		val last = statements.lastOrNull() as? ReturnTree ?: return null
		val before = statements.dropLast(1).ifEmpty { return null }
		if (parsed.blankBetween(before.last(), last)) return null

		val lines = (parsed.lineOf(before.first()) until parsed.lineOf(last)).count { parsed.source.lines[it - 1].isNotBlank() }

		return Breach(parsed.source.path, parsed.lineOf(last), GLUED).takeIf { lines > BESIDE }
	}

	internal companion object {
		/** How many statements stand together at most. */
		const val RUN = 6

		/** How many lines of a computation may stand right above its return. */
		const val BESIDE = 2

		const val LONG_RUN = "runs on as the seventh statement without a blank line; a blank line separates the steps of a body"
		const val GLUED = "returns right below more than two lines; a blank line stands before the final return"
	}
}
