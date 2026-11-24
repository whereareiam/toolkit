package me.whereareiam.toolkit.shipshape.rule.lombok

import com.sun.source.tree.MethodTree
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import me.whereareiam.toolkit.shipshape.api.type.RoleFolder

/**
 * Lombok's `@Builder` makes values: a class below `model/` or `config/`, a member type, and what the code of
 * tests builds. A top-level class of main code elsewhere is a service or another class of logic, which takes its
 * collaborators through `@RequiredArgsConstructor`: a builder hides how many it has.
 */
internal class BuilderPlaceRule : ShapeRule {
	override val id = "builder-place"

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		val source = parsed.source
		if (!source.main || VALUES.any(source::isBelow)) return emptyList()

		return parsed.topLevel.flatMap { type ->
			val members = type.members.filterIsInstance<MethodTree>().filter { ParsedSource.annotated(it.modifiers, BUILDERS) }
			val built = listOf(type).filter { ParsedSource.annotated(it.modifiers, BUILDERS) } + members

			built.map {
				Breach(
					source.path, parsed.lineOf(it),
					"builds ${type.simpleName} with @Builder; a builder makes a value below model/ or config/, a class of logic takes " +
						"its collaborators through @RequiredArgsConstructor",
				)
			}
		}
	}

	private companion object {
		val VALUES = listOf(RoleFolder.MODEL, RoleFolder.CONFIG)

		val BUILDERS = setOf("Builder", "SuperBuilder")
	}
}
