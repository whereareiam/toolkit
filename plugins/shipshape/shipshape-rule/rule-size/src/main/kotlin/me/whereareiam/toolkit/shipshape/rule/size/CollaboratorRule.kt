package me.whereareiam.toolkit.shipshape.rule.size

import com.sun.source.tree.ClassTree
import com.sun.source.tree.MethodTree
import com.sun.source.tree.Tree
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import me.whereareiam.toolkit.shipshape.api.type.RoleFolder

/**
 * A class of main code outside `model/` and `config/` holds at most eight collaborators: the fields of its
 * instances, however they are set (a constructor, `@RequiredArgsConstructor`, a builder), or the parameters of a
 * constructor written by hand where it takes more. A wiring method, one that carries an annotation the build names in
 * `shipshape { wiringAnnotations }` (Spring's `Bean`), takes at most eight. A class with more holds more than one job,
 * and is split by use case.
 */
internal class CollaboratorRule : ShapeRule {
	override val id = "collaborators"

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		val source = parsed.source
		if (!source.main || HELD_APART.any(source::isBelow)) return emptyList()

		val classes = parsed.types.filter { it.kind == Tree.Kind.CLASS && it.simpleName.isNotEmpty() }.mapNotNull { type ->
			val held = maxOf(ParsedSource.instanceFields(type).size, constructors(type).maxOfOrNull { it.parameters.size } ?: 0)

			breach(parsed, type, "${type.simpleName} holds $held collaborators").takeIf { held > LIMIT }
		}
		val wiring = parsed.methods.filter { (_, method) -> ParsedSource.annotated(method.modifiers, context.wiringAnnotations) }
			.mapNotNull { (type, method) ->
				breach(parsed, method, "the wiring method ${method.name} of ${type.simpleName} takes ${method.parameters.size}")
					.takeIf { method.parameters.size > LIMIT }
			}

		return classes + wiring
	}

	private fun constructors(type: ClassTree): List<MethodTree> =
		type.members.filterIsInstance<MethodTree>().filter(ParsedSource::isConstructor)

	private fun breach(parsed: ParsedSource, tree: Tree, holds: String): Breach = Breach(
		parsed.source.path, parsed.lineOf(tree),
		"$holds; a class has at most $LIMIT, split it by use case, each part with what it needs",
	)

	internal companion object {
		/** How many collaborators a class holds at most. */
		const val LIMIT = 8

		/** The folders of values, whose fields are what they are and no collaborators. */
		val HELD_APART = listOf(RoleFolder.MODEL, RoleFolder.CONFIG)
	}
}
