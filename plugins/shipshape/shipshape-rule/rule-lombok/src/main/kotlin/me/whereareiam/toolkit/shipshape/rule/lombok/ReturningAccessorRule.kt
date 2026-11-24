package me.whereareiam.toolkit.shipshape.rule.lombok

import com.sun.source.tree.ClassTree
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.MethodTree
import com.sun.source.tree.ReturnTree
import com.sun.source.tree.VariableTree
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import javax.lang.model.element.Modifier

/**
 * An accessor of main code that only returns the field it is named after is not written by hand;
 * Lombok's `@Getter` writes it. One that carries an annotation Lombok would not copy stays as it is.
 *
 * `@Getter` writes `getName()`, or `isName()` for a truth value. An accessor that overrides a method called
 * as the field itself, `name()`, cannot be Lombok's while the type it overrides names the method so, and is
 * not named here: the method of that type is renamed first, and then this rule names the accessor.
 */
internal class ReturningAccessorRule : ShapeRule {
	override val id = "returning-accessor"

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main) return emptyList()

		return parsed.types.flatMap { type ->
			val fields = type.members.filterIsInstance<VariableTree>().map { it.name.toString() }.toSet()

			type.members.filterIsInstance<MethodTree>().mapNotNull { method ->
				val field = returned(method)?.takeIf { it in fields && method.name.toString() in accessors(it) } ?: return@mapNotNull null
				if (method.name.contentEquals(field) && ParsedSource.annotated(method.modifiers, OVERRIDING)) return@mapNotNull null

				Breach(
					parsed.source.path, parsed.lineOf(method),
					"writes ${method.name}() of ${name(type)} by hand, and it only returns the field $field; @Getter writes it",
				)
			}
		}
	}

	/** The field [method] returns and does nothing else, or `null` when it is no such method. */
	private fun returned(method: MethodTree): String? {
		val modifiers = method.modifiers
		val plain = Modifier.STATIC !in modifiers.flags && COPIED.containsAll(ParsedSource.names(modifiers.annotations))
		if (method.parameters.isNotEmpty() || !plain) return null

		val returned = (method.body?.statements?.singleOrNull() as? ReturnTree)?.expression

		return when (returned) {
			is IdentifierTree -> returned.name.toString()
			else -> ParsedSource.ownField(returned)
		}
	}

	/** The names an accessor of [field] has: Lombok's getter, the one of a truth value, and the field's own name. */
	private fun accessors(field: String): Set<String> {
		val capital = field.replaceFirstChar(Char::uppercaseChar)

		return setOf("get$capital", "is$capital", field)
	}

	private fun name(type: ClassTree): String = type.simpleName.toString().ifEmpty { "a class without a name" }

	private companion object {
		/** The annotations a getter of Lombok carries as well: the one of an override, and those of nullability. */
		val COPIED = setOf("Override", "NotNull", "Nullable")

		/** The annotation of a method whose name another type decides. */
		val OVERRIDING = setOf("Override")
	}
}
