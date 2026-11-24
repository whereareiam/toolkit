package me.whereareiam.toolkit.shipshape.rule.types

import com.sun.source.tree.ClassTree
import com.sun.source.tree.MethodTree
import com.sun.source.tree.Tree
import com.sun.source.tree.VariableTree
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * Main code declares no `Object` as a field, a parameter or a result. A value gets its type once, at the
 * boundary, in one pattern `switch`, and goes on typed. A method that overrides another, as `equals(Object)` does,
 * takes what that one declares, and is held where that one is written.
 */
internal class ObjectTypeRule : ShapeRule {
	override val id = "object-type"

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main) return emptyList()

		val fields = parsed.types.flatMap { type ->
			type.members.filterIsInstance<VariableTree>().filter { isObject(it.type) }
				.map { breach(parsed, it, "the field ${it.name}", type) }
		}
		val methods = parsed.methods.filterNot { (_, method) -> ParsedSource.annotated(method.modifiers, OVERRIDE) }
			.flatMap { (type, method) -> ofMethod(parsed, type, method) }

		return (fields + methods).sortedBy(Breach::line)
	}

	private fun ofMethod(parsed: ParsedSource, type: ClassTree, method: MethodTree): List<Breach> {
		val result = method.returnType?.takeIf(::isObject)?.let { breach(parsed, method, "the result of ${method.name}()", type) }

		return listOfNotNull(result) + method.parameters.filter { isObject(it.type) }
			.map { breach(parsed, it, "the parameter ${it.name} of ${method.name}()", type) }
	}

	private fun isObject(type: Tree?): Boolean = ParsedSource.simpleName(type) == OBJECT && type?.toString() in WRITTEN

	private fun breach(parsed: ParsedSource, tree: Tree, what: String, type: ClassTree): Breach = Breach(
		parsed.source.path, parsed.lineOf(tree),
		"declares $what of ${type.simpleName} as an Object; a value is given its type once, at the boundary",
	)

	private companion object {
		const val OBJECT = "Object"

		/** How a declaration names the type: by its name, or in full. */
		val WRITTEN = setOf(OBJECT, "java.lang.Object")

		val OVERRIDE = setOf("Override")
	}
}
