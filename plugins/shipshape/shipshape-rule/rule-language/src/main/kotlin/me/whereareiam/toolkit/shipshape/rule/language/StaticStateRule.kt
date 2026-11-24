package me.whereareiam.toolkit.shipshape.rule.language

import com.sun.source.tree.ClassTree
import com.sun.source.tree.ParameterizedTypeTree
import com.sun.source.tree.Tree
import com.sun.source.tree.VariableTree
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionPlace
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import javax.lang.model.element.Modifier

/**
 * Main code keeps no state in a static field: one that is not `final` is state nobody made and nobody ends, and so is
 * a `static final` `ThreadLocal` or `InheritableThreadLocal`, whose state per thread is static state too. The state
 * belongs to an owner the program makes and hands to whoever needs it.
 *
 * A field of an interface or an annotation is final however it is written, and a constant of an enum is one too.
 * Where a framework forces static state, a static door a game's mixins must reach or a static holder a library
 * requires, the field is declared in the root build file with what forces it: `forcedStatic(field, reason)`, the field
 * by its type's full name and its own, as `com.example.Mod.door`. The rule reads the fields of every named type of a
 * file, a member type among them, and none of a local or an anonymous class.
 */
internal class StaticStateRule : ShapeRule {
	override val id = "static-state"
	override val category = LanguageRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override val exemptions = setOf(FORCED_STATIC)

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		val source = parsed.source
		if (!source.main) return emptyList()

		val packageName = source.qualifiedName.substringBeforeLast('.', "")

		return parsed.topLevel.flatMap { fieldsOf(it, if (packageName.isEmpty()) "" else "$packageName.") }
			.mapNotNull { (owner, field) -> breach(parsed, owner, field) }
			.sortedBy(Breach::line)
	}

	/** The fields [type] and its member types declare, each with the full name of the type that declares it; [outer] ends in a dot. */
	private fun fieldsOf(type: ClassTree, outer: String): List<Pair<String, VariableTree>> {
		val name = "$outer${type.simpleName}"
		val own = if (type.kind in ALWAYS_FINAL) emptyList() else type.members.filterIsInstance<VariableTree>().map { name to it }

		return own + type.members.filterIsInstance<ClassTree>().flatMap { fieldsOf(it, "$name.") }
	}

	private fun breach(parsed: ParsedSource, owner: String, field: VariableTree): Breach? {
		val flags = field.modifiers.flags
		if (Modifier.STATIC !in flags) return null

		val what = when {
			Modifier.FINAL !in flags -> "declares the static field ${field.name}, which is not final"
			ParsedSource.simpleName(raw(field.type)) in PER_THREAD -> "holds state per thread in the static field ${field.name}"
			else -> return null
		}
		val place = "$owner.${field.name}"

		return Breach(
			parsed.source.path, parsed.lineOf(field),
			"$what; static state has no owner: give it an owner the program makes, or declare what forces it: " +
				FORCED_STATIC.declaration(place),
			ExemptionPlace(FORCED_STATIC, place),
		)
	}

	/** The type [type] names without its type arguments: `ThreadLocal` for `ThreadLocal<Session>`. */
	private fun raw(type: Tree?): Tree? = (type as? ParameterizedTypeTree)?.type ?: type

	internal companion object {
		/** A static field a framework forces, by the full name of its type and its own name. */
		val FORCED_STATIC = ExemptionKind("forcedStatic")

		/** The kinds of type whose every field is final however it is written. */
		private val ALWAYS_FINAL = setOf(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE)

		/** The types that hold a value for each thread. */
		private val PER_THREAD = setOf("ThreadLocal", "InheritableThreadLocal")
	}
}
