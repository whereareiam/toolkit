package me.whereareiam.toolkit.shipshape.rule.models

import com.sun.source.tree.ClassTree
import com.sun.source.tree.MethodTree
import com.sun.source.tree.PrimitiveTypeTree
import com.sun.source.tree.Tree
import com.sun.source.tree.VariableTree
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import me.whereareiam.toolkit.shipshape.api.type.RoleFolder
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeKind

/**
 * A model is immutable. A class below `model/` declares every field of its instances final, has no
 * setter, and carries neither `@Data` nor `@Setter`; a changed value is a new model.
 *
 * A class named `…Builder` nested in a model is not held: a builder collects the values before the model
 * exists, and Lombok writes it (`returning-accessor`). A field is final by Lombok where the class carries `@Value`.
 */
internal class ImmutableModelRule : ShapeRule {
	override val id = "immutable-model"

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main || !parsed.source.isBelow(RoleFolder.MODEL)) return emptyList()

		return parsed.types.filter { it.kind == Tree.Kind.CLASS && it.simpleName.isNotEmpty() && !it.simpleName.endsWith(BUILDER) }
			.flatMap { model -> mutable(model).map { (place, does) -> breach(parsed, place, does) } }
	}

	/** What makes [model] changeable, each as the tree it stands at and what that tree does. */
	private fun mutable(model: ClassTree): List<Pair<Tree, String>> = buildList {
		val name = model.simpleName
		val finalByLombok = ParsedSource.annotated(model.modifiers, FINAL_BY_LOMBOK)

		ParsedSource.names(model.modifiers.annotations).filter(MUTATING::contains)
			.forEach { add(model to "carries @$it on the class $name") }
		ParsedSource.instanceFields(model).forEach { field ->
			val changeable = Modifier.FINAL !in field.modifiers.flags && !finalByLombok

			if (ParsedSource.annotated(field.modifiers, MUTATING)) add(field to "carries @$SETTER on the field ${field.name} of $name")
			if (changeable) add(field to "declares the field ${field.name} of $name without final")
		}
		model.members.filterIsInstance<MethodTree>().filter(::sets).forEach { add(it to "declares the setter ${it.name}() of $name") }
	}

	/** Says whether [method] is a setter: it is called `set…`, takes one value, returns nothing and belongs to an instance. */
	private fun sets(method: MethodTree): Boolean =
		SETTER_NAME.matches(method.name)
			&& method.parameters.size == 1
			&& Modifier.STATIC !in method.modifiers.flags
			&& (method.returnType as? PrimitiveTypeTree)?.primitiveTypeKind == TypeKind.VOID

	private fun breach(parsed: ParsedSource, place: Tree, does: String): Breach =
		Breach(parsed.source.path, parsed.lineOf(place), "$does; $ASKED")

	private companion object {
		const val ASKED = "a model is immutable: its fields are final, and a changed value is a new model"

		const val BUILDER = "Builder"
		const val SETTER = "Setter"

		/** Lombok's annotations that write a setter: `@Data` for every field that is not final, `@Setter` where it stands. */
		val MUTATING = setOf("Data", SETTER)

		/** Lombok's `@Value` makes every field of the class private and final. */
		val FINAL_BY_LOMBOK = setOf("Value")

		val SETTER_NAME = Regex("set[A-Z].*")
	}
}
