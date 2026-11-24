package me.whereareiam.toolkit.shipshape.rule.types

import com.sun.source.tree.CaseTree
import com.sun.source.tree.ClassTree
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.source.tree.Tree
import com.sun.source.util.TreeScanner
import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * Main code chooses by no text. A `switch` on a string and `equals("…")` of a literal make a closed set
 * of words a type nobody declared: the set is an enum or a sealed type, and the text becomes it once, where it
 * arrives. That place is the one that may compare words: a class named `…Mapper` of an implementation, which turns
 * the words of the wire into the types, and an enum, which finds its constant by its own word.
 */
internal class StringChoiceRule : ShapeRule {
	override val id = "string-choice"
	override val category = TypeRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		val source = parsed.source
		if (!source.main || source.mapper && context.kindOf(source) == ArchitectureKind.IMPLEMENTATION) return emptyList()

		val found = mutableListOf<Breach>()
		val reader = object : TreeScanner<Unit?, Unit?>() {
			override fun visitClass(node: ClassTree, unused: Unit?): Unit? {
				if (node.kind == Tree.Kind.ENUM) return null

				return super.visitClass(node, unused)
			}

			override fun visitCase(node: CaseTree, unused: Unit?): Unit? {
				node.expressions.filterIsInstance<LiteralTree>().filter { it.value is String }.forEach {
					found += breach(parsed, parsed.lineOf(it), "chooses by the text \"${it.value}\" in a switch")
				}

				return super.visitCase(node, unused)
			}

			override fun visitMethodInvocation(node: MethodInvocationTree, unused: Unit?): Unit? {
				compared(node)?.let { found += breach(parsed, parsed.lineOfName(node), "compares a text with \"$it\"") }

				return super.visitMethodInvocation(node, unused)
			}
		}
		parsed.topLevel.forEach { reader.scan(it, null) }

		return found.sortedBy(Breach::line)
	}

	/** The literal [call] compares a text with by `equals`, on either side, or `null` where it is no such call. */
	private fun compared(call: MethodInvocationTree): String? {
		val selected = call.methodSelect as? MemberSelectTree ?: return null
		if (selected.identifier.toString() !in EQUALS) return null

		return (listOf(selected.expression) + call.arguments).filterIsInstance<LiteralTree>().firstNotNullOfOrNull { it.value as? String }
	}

	private fun breach(parsed: ParsedSource, line: Int, does: String): Breach =
		Breach(parsed.source.path, line, "$does; $ASKED")

	private companion object {
		const val ASKED = "a closed set of words is an enum or a sealed type, and the text becomes one in a mapper of the wire"

		val EQUALS = setOf("equals", "equalsIgnoreCase")
	}
}
