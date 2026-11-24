package me.whereareiam.toolkit.shipshape.rule.models

import com.sun.source.tree.ClassTree
import com.sun.source.tree.Tree
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import me.whereareiam.toolkit.shipshape.api.type.RoleFolder
import javax.lang.model.element.Modifier

/**
 * A record stays a small carrier inside its owner. A record that is a member of a class of main code
 * is private to that class, or it is a part of the model it is nested in and has at most three components.
 * A larger part is a class, and so is what other classes share. A record declared inside a method is seen by
 * that method alone and is not read.
 */
internal class NestedRecordRule : ShapeRule {
	override val id = "nested-record"
	override val category = ModelRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main) return emptyList()

		val ofAModel = parsed.source.isBelow(RoleFolder.MODEL)

		return parsed.types.flatMap { owner -> owner.members.filterIsInstance<ClassTree>() }
			.filter { it.kind == Tree.Kind.RECORD && Modifier.PRIVATE !in it.modifiers.flags }
			.mapNotNull { record ->
				val components = ParsedSource.instanceFields(record).size
				val asked = when {
					!ofAModel -> "open to other classes; a record is private to its class, or a part nested in its model"
					components > PART -> "with $components components; a part of a model has at most $PART, a larger one is a class"
					else -> return@mapNotNull null
				}

				val declared = "declares the record ${record.simpleName} $asked"

				Breach(parsed.source.path, parsed.lineOf(record), declared)
			}
	}

	private companion object {
		/** How many components a part of a model has at most. */
		const val PART = 3
	}
}
