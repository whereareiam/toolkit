package me.whereareiam.toolkit.shipshape.rule.types

import com.sun.source.tree.ExpressionTree
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.MemberReferenceTree
import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.source.tree.VariableTree
import com.sun.source.util.TreeScanner
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import me.whereareiam.toolkit.shipshape.api.type.RoleFolder

/**
 * An enum knows its own word. How a constant is spelt outside of Java is a field or a method of the
 * enum, below `type/`, and text becomes the enum once, where it arrives. Main code outside of `type/` does not
 * make the word from the constant's name, find the constant by an upper-cased text, or keep the common
 * beginning of the names as a `…_PREFIX` constant.
 */
internal class EnumWordRule : ShapeRule {
	override val id = "enum-word"
	override val category = TypeRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		val source = parsed.source
		if (!source.main || source.isBelow(RoleFolder.TYPE)) return emptyList()

		val found = mutableListOf<Breach>()
		val reader = object : TreeScanner<Unit?, Unit?>() {
			override fun visitVariable(node: VariableTree, unused: Unit?): Unit? {
				if (keepsABeginning(node)) found += breach(parsed, parsed.lineOf(node), KEEPS)

				return super.visitVariable(node, unused)
			}

			override fun visitMethodInvocation(node: MethodInvocationTree, unused: Unit?): Unit? {
				nameCalled(node)?.let { found += breach(parsed, parsed.lineOfName(it), MAKES) }
				if (findsByUpperCase(node)) found += breach(parsed, parsed.lineOfName(node), FINDS)

				return super.visitMethodInvocation(node, unused)
			}
		}
		parsed.topLevel.forEach { reader.scan(it, null) }

		return found.sortedBy(Breach::line)
	}

	/** Says whether [declared] is a constant named `…_PREFIX` that holds what the names of an enum begin with, as `"SESSION_KIND_"`. */
	private fun keepsABeginning(declared: VariableTree): Boolean =
		declared.name.endsWith(PREFIX) && ((declared.initializer as? LiteralTree)?.value as? String)?.let(BEGINNING::matches) == true

	/**
	 * The call of `name()` on a constant that [call] makes a word of, as `kind.name().toLowerCase()` does, or
	 * `null` when [call] is no such call. An enum that spells its own word calls `name()` on nothing.
	 */
	private fun nameCalled(call: MethodInvocationTree): MethodInvocationTree? {
		val made = call.methodSelect as? MemberSelectTree ?: return null
		val receiver = made.expression as? MethodInvocationTree ?: return null
		val named = (receiver.methodSelect as? MemberSelectTree)?.identifier?.contentEquals(NAME) == true && receiver.arguments.isEmpty()

		return receiver.takeIf { named && made.identifier.toString() in WORD_MAKERS }
	}

	/** Says whether [call] is `valueOf` of a text that is upper-cased among what the call is handed; `String.valueOf` reads no enum. */
	private fun findsByUpperCase(call: MethodInvocationTree): Boolean {
		val selected = call.methodSelect
		val ofText = (selected as? MemberSelectTree)?.let { ParsedSource.simpleName(it.expression) } == TEXT

		return ParsedSource.simpleName(selected) == VALUE_OF && !ofText && call.arguments.any(::upperCases)
	}

	/** Says whether [handed] calls `toUpperCase` anywhere in it. */
	private fun upperCases(handed: ExpressionTree): Boolean {
		var upperCased = false
		object : TreeScanner<Unit?, Unit?>() {
			override fun visitMethodInvocation(node: MethodInvocationTree, unused: Unit?): Unit? {
				if (ParsedSource.simpleName(node.methodSelect) == UPPER_CASE) upperCased = true

				return super.visitMethodInvocation(node, unused)
			}

			override fun visitMemberReference(node: MemberReferenceTree, unused: Unit?): Unit? {
				if (node.name.contentEquals(UPPER_CASE)) upperCased = true

				return super.visitMemberReference(node, unused)
			}
		}.scan(handed, null)

		return upperCased
	}

	private fun breach(parsed: ParsedSource, line: Int, does: String): Breach =
		Breach(parsed.source.path, line, "$does; $ASKED")

	private companion object {
		const val ASKED = "the enum knows its word, as a field or a method of its own"

		const val KEEPS = "keeps the beginning of an enum's names as a constant"
		const val MAKES = "makes an enum's word from the name of its constant"
		const val FINDS = "finds an enum's constant by an upper-cased text"

		const val PREFIX = "_PREFIX"

		/** What the names of an enum's constants begin with: capitals, and an underscore at its end. */
		val BEGINNING = Regex("[A-Z][A-Z0-9_]*_")

		const val NAME = "name"
		const val VALUE_OF = "valueOf"
		const val UPPER_CASE = "toUpperCase"
		const val TEXT = "String"

		/** What a text is turned into a word with, once it was the name of a constant. */
		val WORD_MAKERS = setOf("toLowerCase", "substring", "replace")
	}
}
