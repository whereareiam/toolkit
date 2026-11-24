package me.whereareiam.toolkit.shipshape.rule.flow

import com.sun.source.tree.IfTree
import com.sun.source.util.TreeScanner
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * Main code writes no `else if`. A choice among several cases is an early return or `continue`, a pattern
 * `switch`, or a table.
 */
internal class ElseIfRule : ShapeRule {
	override val id = "else-if"

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main) return emptyList()

		val found = mutableListOf<Breach>()
		object : TreeScanner<Unit?, Unit?>() {
			override fun visitIf(node: IfTree, unused: Unit?): Unit? {
				(node.elseStatement as? IfTree)?.let {
					found += Breach(parsed.source.path, parsed.lineOf(it), ASKED)
				}

				return super.visitIf(node, unused)
			}
		}.scan(parsed.topLevel, null)

		return found.sortedBy(Breach::line)
	}

	private companion object {
		const val ASKED = "writes else if; return early, or choose by a pattern switch or a table"
	}
}
