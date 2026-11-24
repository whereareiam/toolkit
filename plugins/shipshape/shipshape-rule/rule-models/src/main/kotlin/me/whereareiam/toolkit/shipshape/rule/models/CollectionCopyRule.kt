package me.whereareiam.toolkit.shipshape.rule.models

import com.sun.source.tree.AnnotatedTypeTree
import com.sun.source.tree.AssignmentTree
import com.sun.source.tree.ClassTree
import com.sun.source.tree.ConditionalExpressionTree
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.source.tree.MethodTree
import com.sun.source.tree.NewClassTree
import com.sun.source.tree.ParameterizedTypeTree
import com.sun.source.tree.ParenthesizedTree
import com.sun.source.tree.Tree
import com.sun.source.tree.VariableTree
import com.sun.source.util.TreeScanner
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import me.whereareiam.toolkit.shipshape.api.type.RoleFolder
import javax.lang.model.element.Modifier

/**
 * A model is immutable, so it does not keep the collection it is handed: whoever built the model
 * could still change that one. A `List`, a `Set` or a `Map` a class below `model/` holds is copied where it
 * is assigned, or collected by Lombok's builder (`@Singular`, on the field or on the parameter of the
 * constructor the builder calls), which copies as well.
 *
 * What is assigned is a copy when every value it can be is one the model alone holds: `List.copyOf`, a new
 * collection, `List.of`, what a stream collects or `toList()` gives, and `null`. A view of a collection, as
 * `Collections.unmodifiableList` gives one, is a copy when it is the view of a copy, and of the two sides of
 * `a == null ? … : …` each is read. `@Builder.Default` copies nothing: the builder hands over what it was
 * given in place of the default.
 */
internal class CollectionCopyRule : ShapeRule {
	override val id = "collection-copy"
	override val category = ModelRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main || !parsed.source.isBelow(RoleFolder.MODEL)) return emptyList()

		return parsed.types.filter { it.kind == Tree.Kind.CLASS || it.kind == Tree.Kind.RECORD }.flatMap { type ->
			val copied = copied(type) + collected(type)

			type.members.filterIsInstance<VariableTree>().filter { handedIn(it) && it.name.toString() !in copied }.map {
				val kind = collection(it.type)

				Breach(
					parsed.source.path, parsed.lineOf(it),
					"keeps the $kind ${it.name} as it was handed in; copy it where it is assigned (copyOf), or let the builder collect " +
						"it (@Singular)",
				)
			}
		}
	}

	/**
	 * Says whether [field] is a collection an instance is given from outside: no constant, nothing the class
	 * makes itself, which a default of the builder is not, and not collected by the builder.
	 */
	private fun handedIn(field: VariableTree): Boolean =
		collection(field.type) != null
			&& Modifier.STATIC !in field.modifiers.flags
			&& (field.initializer == null || ParsedSource.annotated(field.modifiers, DEFAULTED))
			&& !ParsedSource.annotated(field.modifiers, COLLECTED)

	/** The kind of collection [type] names, `List` for `List<String>`, or `null` when it names none. */
	private fun collection(type: Tree?): String? = when (type) {
		is ParameterizedTypeTree -> collection(type.type)
		is AnnotatedTypeTree -> collection(type.underlyingType)
		else -> ParsedSource.simpleName(type)?.takeIf(COLLECTIONS::contains)
	}

	/** The names of what [type] assigns a copy to somewhere: `this.items = List.copyOf(items)`. */
	private fun copied(type: ClassTree): Set<String> {
		val copied = mutableSetOf<String>()
		object : TreeScanner<Unit?, Unit?>() {
			override fun visitAssignment(node: AssignmentTree, unused: Unit?): Unit? {
				if (copies(node.expression)) ParsedSource.simpleName(node.variable)?.let(copied::add)

				return super.visitAssignment(node, unused)
			}
		}.scan(type, null)

		return copied
	}

	/** The names of what the builder of [type] collects for a constructor: the parameters that carry `@Singular`. */
	private fun collected(type: ClassTree): Set<String> = type.members.filterIsInstance<MethodTree>()
		.flatMap(MethodTree::getParameters)
		.filter { ParsedSource.annotated(it.modifiers, COLLECTED) }
		.map { it.name.toString() }
		.toSet()

	/** Says whether every value [assigned] can be is a collection the model alone holds, or none at all. */
	private fun copies(assigned: Tree?): Boolean = when (assigned) {
		is ParenthesizedTree -> copies(assigned.expression)
		is ConditionalExpressionTree -> copies(assigned.trueExpression) && copies(assigned.falseExpression)
		is NewClassTree -> true
		is LiteralTree -> assigned.value == null
		is MethodInvocationTree -> when (ParsedSource.simpleName(assigned.methodSelect)) {
			in MAKERS -> true
			in VIEWS -> copies(assigned.arguments.firstOrNull())
			else -> false
		}
		else -> false
	}

	private companion object {
		/** The calls that give a collection nobody else holds: a copy, a collection of given values, what a stream ends in. */
		val MAKERS = setOf(
			"copyOf", "of", "ofEntries", "toList", "collect", "emptyList", "emptySet", "emptyMap",
		)

		/** The calls that wrap the collection they are handed and copy nothing. */
		val VIEWS = setOf(
			"unmodifiableList", "unmodifiableSet", "unmodifiableMap", "unmodifiableCollection",
			"unmodifiableSortedSet", "unmodifiableSortedMap",
		)

		val COLLECTIONS = setOf("List", "Set", "Map", "Collection")

		/** Lombok's annotation for a collection its builder gathers and hands over as a copy. */
		val COLLECTED = setOf("Singular")

		/** Lombok's `@Builder.Default`: the field's value is what the builder was given, and the default only where it was given none. */
		val DEFAULTED = setOf("Default")
	}
}
