package me.whereareiam.toolkit.shipshape.rule.packages

import com.sun.source.tree.BlockTree
import com.sun.source.tree.ClassTree
import com.sun.source.tree.MethodTree
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
 * A reusable stateless operation that has no type of its own lives in the `util`
 * package of a module, and such a package holds nothing else. Every member of a class there is static, and an
 * assembly, which reaches every module it assembles, has no such package: that would be the build-wide catch-all.
 * An operation of a value stays on its type (`Digest.of`).
 */
internal class UtilPackageRule : ShapeRule {
	override val id = "util-package"
	override val category = PackageRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		val source = parsed.source
		if (!source.main || !source.isBelow(RoleFolder.UTIL)) return emptyList()
		if (context.kindOf(source) == ArchitectureKind.ASSEMBLY) return listOf(breach(parsed, null, buildWide(source.module.label)))

		return parsed.topLevel.flatMap { withState(parsed, it) }
	}

	/** What [type], a type of a `util` package, declares that is no stateless operation. */
	private fun withState(parsed: ParsedSource, type: ClassTree): List<Breach> {
		if (ParsedSource.annotated(type.modifiers, STATIC_BY_LOMBOK)) return emptyList()

		val kind = KINDS[type.kind]
		if (kind != null) return listOf(breach(parsed, null, "declares the $kind ${type.simpleName}; $CLASSES_ONLY"))

		return type.members.mapNotNull { member ->
			val declared = withAnInstance(member) ?: return@mapNotNull null

			breach(parsed, parsed.lineOf(member), "declares $declared; a $UTIL package holds stateless operations, $OWNER")
		}
	}

	/**
	 * What [member] is called when it needs an instance of its class, or `null` when it needs none: it is
	 * static, a nested type, or the private constructor that keeps the class from being made.
	 */
	private fun withAnInstance(member: Tree): String? = when (member) {
		is VariableTree -> "${member.name} without static".takeUnless { Modifier.STATIC in member.modifiers.flags }
		is BlockTree -> "an initializer of its instances".takeUnless { member.isStatic }
		is MethodTree -> when {
			Modifier.STATIC in member.modifiers.flags -> null
			!ParsedSource.isConstructor(member) -> "${member.name} without static"
			Modifier.PRIVATE in member.modifiers.flags -> null
			else -> "a constructor that other classes can call"
		}
		else -> null
	}

	private fun breach(parsed: ParsedSource, line: Int?, asks: String): Breach =
		Breach(parsed.source.path, line, asks)

	private companion object {
		val UTIL = RoleFolder.UTIL.folder

		fun buildWide(assembly: String): String =
			"lies in a $UTIL package of the assembly $assembly, a catch-all for every module it assembles; " +
				"a $UTIL package belongs to one module"

		const val OWNER = "state belongs to the type it is about"
		val CLASSES_ONLY = "a $UTIL package holds classes of static operations only"

		/** Lombok's `@UtilityClass` makes every member of the class static and its constructor private. */
		val STATIC_BY_LOMBOK = setOf("UtilityClass")

		/** The kinds of type that are no class of static operations, each as a breach calls it. */
		val KINDS = mapOf(
			Tree.Kind.INTERFACE to "interface",
			Tree.Kind.ENUM to "enum",
			Tree.Kind.RECORD to "record",
			Tree.Kind.ANNOTATION_TYPE to "annotation",
		)
	}
}
