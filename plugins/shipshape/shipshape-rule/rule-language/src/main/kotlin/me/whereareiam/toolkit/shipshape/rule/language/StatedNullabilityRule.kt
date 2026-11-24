package me.whereareiam.toolkit.shipshape.rule.language

import com.sun.source.tree.AnnotatedTypeTree
import com.sun.source.tree.AssignmentTree
import com.sun.source.tree.ClassTree
import com.sun.source.tree.MethodTree
import com.sun.source.tree.ModifiersTree
import com.sun.source.tree.PrimitiveTypeTree
import com.sun.source.tree.Tree
import com.sun.source.tree.VariableTree
import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import me.whereareiam.toolkit.shipshape.api.type.RoleFolder
import javax.lang.model.element.Modifier

/**
 * Nullability is stated on every signature a module declares. A public or protected method or
 * constructor of main code, and every method of an interface, carries JetBrains' `@NotNull` or `@Nullable`
 * on each reference it takes and on the reference it returns.
 *
 * A field states it where the field is the signature: in a class below `model/`, and wherever Lombok writes
 * a constructor, an accessor or a builder from the field (`@RequiredArgsConstructor`, `@AllArgsConstructor`,
 * `@Getter`, `@Builder`, `@Value`, `@Data`), because Lombok copies the field's annotation to what it writes.
 * What Lombok writes for the class alone, with `AccessLevel.PRIVATE`, `PACKAGE` or `NONE`, is no signature.
 *
 * A method that overrides another takes its contract from there and is not read, which also leaves out what
 * a foreign type dictates, and so does a wiring method, whose container dictates it. An assembly is not read
 * either: it wires what the modules declare.
 */
internal class StatedNullabilityRule : ShapeRule {
	override val id = "stated-nullability"
	override val category = LanguageRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main || context.kindOf(parsed.source) == ArchitectureKind.ASSEMBLY) return emptyList()

		val ofAModel = parsed.source.isBelow(RoleFolder.MODEL)
		val asked = if (ofAModel) OF_A_MODEL else FROM_THE_FIELD

		return parsed.types.filter { it.simpleName.isNotEmpty() }.flatMap { type ->
			val methods = type.members.filterIsInstance<MethodTree>().filter { declaresAContract(it, type, context) }.mapNotNull { method ->
				val unstated = unstated(method).ifEmpty { return@mapNotNull null }

				breach(parsed, method, "${name(method, type)} without $ASKED on ${unstated.joinToString()}; state what may be null")
			}
			val fields = signatureFields(type, ofAModel).filter { reference(it.type) && !stated(it.modifiers, it.type) }.map {
				breach(parsed, it, "the field ${it.name} of ${type.simpleName} without $ASKED; $asked")
			}

			methods + fields
		}
	}

	/**
	 * The fields of [type] that are a signature other classes are written against: all of a class or a record
	 * of a model, and elsewhere those Lombok writes a constructor, an accessor or a builder from.
	 *
	 * @param ofAModel whether [type] lies below `model/`
	 */
	private fun signatureFields(type: ClassTree, ofAModel: Boolean): List<VariableTree> {
		val fields = ParsedSource.instanceFields(type)
		val onTheClass = signatures(type.modifiers)

		return when {
			type.kind != Tree.Kind.CLASS && type.kind != Tree.Kind.RECORD -> emptyList()
			ofAModel || onTheClass.any(FROM_EVERY_FIELD::contains) -> fields
			else -> fields.filter { field ->
				val required = Modifier.FINAL in field.modifiers.flags && field.initializer == null

				signatures(field.modifiers).any(FROM_EVERY_FIELD::contains) || (REQUIRED in onTheClass && required)
			}
		}
	}

	/**
	 * The names of the annotations among [modifiers] that write what other classes can call: one of Lombok's
	 * that names an access which hides what it writes is none of them.
	 */
	private fun signatures(modifiers: ModifiersTree): Set<String> = ParsedSource.names(
		modifiers.annotations.filter { annotation ->
			annotation.arguments.none { ParsedSource.simpleName((it as? AssignmentTree)?.expression ?: it) in HIDDEN }
		},
	)

	/** Says whether [method] of [type] is a signature of its own that other classes are written against. */
	private fun declaresAContract(method: MethodTree, type: ClassTree, context: RuleContext): Boolean {
		val flags = method.modifiers.flags
		val ofAnInterface = type.kind == Tree.Kind.INTERFACE && Modifier.PRIVATE !in flags
		val visible = Modifier.PUBLIC in flags || Modifier.PROTECTED in flags || ofAnInterface

		return visible && !ParsedSource.annotated(method.modifiers, OVERRIDE + context.wiringAnnotations)
	}

	/** What of [method] is a reference without stated nullability: its parameters by their names, and its result. */
	private fun unstated(method: MethodTree): List<String> = buildList {
		addAll(method.parameters.filter { reference(it.type) && !stated(it.modifiers, it.type) }.map { it.name.toString() })
		if (reference(method.returnType) && !stated(method.modifiers, method.returnType)) add(RESULT)
	}

	/** Says whether [type] is a reference: a constructor has no result, and a primitive and `void` are never null. */
	private fun reference(type: Tree?): Boolean = type != null && type !is PrimitiveTypeTree

	/** Says whether nullability is stated: on the declaration, by [modifiers], or on the [type] itself, as `byte @NotNull []` does. */
	private fun stated(modifiers: ModifiersTree, type: Tree?): Boolean =
		ParsedSource.annotated(modifiers, NULLABILITY)
			|| (type is AnnotatedTypeTree && ParsedSource.names(type.annotations).any(NULLABILITY::contains))

	private fun name(method: MethodTree, type: ClassTree): String =
		if (ParsedSource.isConstructor(method)) "the constructor of ${type.simpleName}" else "${method.name}() of ${type.simpleName}"

	private fun breach(parsed: ParsedSource, declared: Tree, what: String): Breach =
		Breach(parsed.source.path, parsed.lineOf(declared), "declares $what")

	private companion object {
		const val RESULT = "its result"
		const val ASKED = "@NotNull or @Nullable"
		const val OF_A_MODEL = "a model states it on every field"
		const val FROM_THE_FIELD = "Lombok writes a signature from the field, and copies the annotation to it"

		/** Lombok's annotations that write a signature from every field of the class, or from the field they stand on. */
		val FROM_EVERY_FIELD = setOf("Getter", "AllArgsConstructor", "Builder", "Value", "Data")

		/** Lombok's annotation that writes a constructor from the final fields without a value. */
		const val REQUIRED = "RequiredArgsConstructor"

		/** The access levels of Lombok that keep what it writes inside the class or its package. */
		val HIDDEN = setOf("PRIVATE", "PACKAGE", "NONE")

		val NULLABILITY = setOf("NotNull", "Nullable")

		/** A method that carries it has its signature from the type it overrides. */
		val OVERRIDE = setOf("Override")
	}
}
