package me.whereareiam.toolkit.shipshape.rule.flow

import com.sun.source.tree.BinaryTree
import com.sun.source.tree.BlockTree
import com.sun.source.tree.ExpressionStatementTree
import com.sun.source.tree.ExpressionTree
import com.sun.source.tree.IfTree
import com.sun.source.tree.InstanceOfTree
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.source.tree.ParenthesizedTree
import com.sun.source.tree.ReturnTree
import com.sun.source.tree.StatementTree
import com.sun.source.tree.Tree
import com.sun.source.tree.UnaryTree
import com.sun.source.util.TreeScanner
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * Main code writes no ladder, three `if`s or more in a row, without `else`, that test the same thing (the
 * receiver of `equals`, the side of `==`, what `instanceof` reads) or do the same thing (call one method on one
 * receiver, as `can.add(…)`, or return a text). A ladder is a table: an `EnumMap` of rules, behaviour on an enum,
 * or a sealed type with a pattern `switch`.
 */
internal class LadderRule : ShapeRule {
	override val id = "ladder"
	override val category = FlowRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main) return emptyList()

		val found = mutableListOf<Breach>()
		object : TreeScanner<Unit?, Unit?>() {
			override fun visitBlock(node: BlockTree, unused: Unit?): Unit? {
				found += ladders(node.statements).map { (first, does) ->
					Breach(parsed.source.path, parsed.lineOf(first), "begins a ladder of ifs that $does; $ASKED")
				}

				return super.visitBlock(node, unused)
			}
		}.scan(parsed.topLevel, null)

		return found.sortedBy(Breach::line)
	}

	/** The first `if` of each ladder among [statements], with what its `if`s share. */
	private fun ladders(statements: List<StatementTree>): List<Pair<IfTree, String>> {
		val ladders = mutableListOf<Pair<IfTree, String>>()
		var run = mutableListOf<IfTree>()
		for (statement in statements + listOf(null)) {
			if (statement is IfTree && statement.elseStatement == null) {
				run += statement
				continue
			}
			ladders += shared(run)
			run = mutableListOf()
		}

		return ladders
	}

	/** The ladders a run of `if`s holds: each stretch of [LIMIT] or more that test or do one thing. */
	private fun shared(run: List<IfTree>): List<Pair<IfTree, String>> = listOf(::tested, ::done).flatMap { key ->
		stretches(run.map(key)).map { (start, value) -> run[start] to value }
	}

	/** Where in [keys] a stretch of [LIMIT] or more equal keys that are not `null` begins, each with its key. */
	private fun stretches(keys: List<String?>): List<Pair<Int, String>> {
		val found = mutableListOf<Pair<Int, String>>()
		var start = 0
		for (index in 1..keys.size) {
			if (index < keys.size && keys[index] == keys[start]) continue

			keys[start]?.takeIf { index - start >= LIMIT }?.let { found += start to it }
			start = index
		}

		return found
	}

	/** What the condition of [statement] tests, as the rest of a sentence, or `null` where it tests nothing one can name. */
	private fun tested(statement: IfTree): String? = when (val condition = unwrapped(statement.condition)) {
		is MethodInvocationTree -> tested(condition)
		is BinaryTree -> "test ${condition.leftOperand}".takeIf { condition.kind in EQUALITIES }
		is InstanceOfTree -> "test ${condition.expression}"
		is UnaryTree -> (unwrapped(condition.expression) as? MethodInvocationTree)?.let { tested(it) }
		else -> null
	}

	private fun tested(call: MethodInvocationTree): String? = (call.methodSelect as? MemberSelectTree)
		?.takeIf { it.identifier.toString() in COMPARISONS }
		?.let { "test ${it.expression}" }

	/** What [statement] does when its condition holds, as the rest of a sentence, or `null` where it is no one call or return. */
	private fun done(statement: IfTree): String? {
		val then = (statement.thenStatement as? BlockTree)?.statements?.singleOrNull() ?: statement.thenStatement
		if (then is ReturnTree) return "return a text".takeIf { (then.expression as? LiteralTree)?.value is String }

		val call = ((then as? ExpressionStatementTree)?.expression as? MethodInvocationTree)?.methodSelect

		return (call as? MemberSelectTree)?.let { "call ${it.expression}.${it.identifier}" }
	}

	private fun unwrapped(tree: ExpressionTree): Tree = if (tree is ParenthesizedTree) unwrapped(tree.expression) else tree

	internal companion object {
		/** How many `if`s in a row make a ladder. */
		const val LIMIT = 3

		const val ASKED = "a ladder is a table, behaviour on an enum, or a sealed type with a pattern switch"

		/** The calls that test their receiver against what they are handed. */
		val COMPARISONS = setOf("equals", "equalsIgnoreCase", "startsWith", "endsWith", "contains", "matches", "isInstance")

		val EQUALITIES = setOf(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)
	}
}
