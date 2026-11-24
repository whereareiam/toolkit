package me.whereareiam.toolkit.shipshape.rule.lombok

import com.sun.source.tree.AssignmentTree
import com.sun.source.tree.ExpressionStatementTree
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.MethodTree
import com.sun.source.tree.StatementTree
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A constructor of main code that only assigns each of its parameters to the field of the same name
 * is not written by hand; Lombok's `@RequiredArgsConstructor` or `@AllArgsConstructor` writes it. A
 * constructor that checks a value or calls another stays as it is, and so does one that carries an annotation
 * of its own, but for `@Builder`: on the class, Lombok writes the constructor its builder calls as well.
 *
 * Lombok takes the parameters in the order of the fields. Before one replaces a constructor with two
 * parameters of one type, the two orders are compared: a different one swaps the values without a sound.
 */
internal class AssigningConstructorRule : ShapeRule {
	override val id = "assigning-constructor"

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main) return emptyList()

		return parsed.types.flatMap { type ->
			type.members.filterIsInstance<MethodTree>().filter(::onlyAssigns).map {
				Breach(
					parsed.source.path, parsed.lineOf(it),
					"writes the constructor of ${type.simpleName} by hand, and it only assigns its parameters; Lombok writes it, in the " +
						"order of the fields",
				)
			}
		}
	}

	private fun onlyAssigns(method: MethodTree): Boolean {
		val written = ParsedSource.isConstructor(method) && method.parameters.isNotEmpty()
		if (!written || !LOMBOK_WRITES.containsAll(ParsedSource.names(method.modifiers.annotations))) return false

		val parameters = method.parameters.map { it.name.toString() }
		val assigned = method.body?.statements.orEmpty().map(::assigned)

		return assigned.size == parameters.size && assigned.toSet() == parameters.toSet()
	}

	/** The field [statement] gives the parameter of its own name, as `this.name = name;` does, or `null` for any other statement. */
	private fun assigned(statement: StatementTree): String? {
		val assignment = (statement as? ExpressionStatementTree)?.expression as? AssignmentTree ?: return null
		val value = assignment.expression as? IdentifierTree ?: return null

		return ParsedSource.ownField(assignment.variable)?.takeIf(value.name::contentEquals)
	}

	private companion object {
		/** The annotations of a constructor that Lombok's own constructor serves as well, once they stand on the class. */
		val LOMBOK_WRITES = setOf("Builder")
	}
}
