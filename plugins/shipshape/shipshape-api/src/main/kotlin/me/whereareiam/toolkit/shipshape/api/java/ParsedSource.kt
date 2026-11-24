package me.whereareiam.toolkit.shipshape.api.java

import com.sun.source.tree.AnnotationTree
import com.sun.source.tree.ClassTree
import com.sun.source.tree.CompilationUnitTree
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.source.tree.MethodTree
import com.sun.source.tree.ModifiersTree
import com.sun.source.tree.Tree
import com.sun.source.tree.VariableTree
import com.sun.source.util.SourcePositions
import com.sun.source.util.TreeScanner
import javax.lang.model.element.Modifier

/** One Java file as the JDK's parser read it: its syntax tree, with no name resolved and nothing compiled. */
class ParsedSource(
	/** The file, with its place in the build. */
	val source: JavaSource,
	private val unit: CompilationUnitTree,
	private val positions: SourcePositions,
) {
	/** The types the file declares at its top level, the one it is named after among them. */
	val topLevel: List<ClassTree> by lazy { unit.typeDecls.filterIsInstance<ClassTree>() }

	/** Every class, interface, enum and record the file declares, the nested ones behind the type that holds them. */
	val types: List<ClassTree> by lazy {
		val found = mutableListOf<ClassTree>()
		object : TreeScanner<Unit?, Unit?>() {
			override fun visitClass(node: ClassTree, unused: Unit?): Unit? {
				found += node

				return super.visitClass(node, unused)
			}
		}.scan(unit, null)

		found
	}

	/** Every method and constructor the file declares, each with the type that declares it, a nested or an anonymous one among them. */
	val methods: List<Pair<ClassTree, MethodTree>> by lazy {
		types.flatMap { type -> type.members.filterIsInstance<MethodTree>().map { type to it } }
	}

	/** The line [tree] begins on, counted from 1. */
	fun lineOf(tree: Tree): Int = unit.lineMap.getLineNumber(positions.getStartPosition(unit, tree)).toInt()

	/** The line [tree] ends on, counted from 1: the line of its last character. */
	fun endLineOf(tree: Tree): Int = unit.lineMap.getLineNumber(positions.getEndPosition(unit, tree) - 1).toInt()

	/** How many lines [tree] spans, the first and the last counted. */
	fun linesOf(tree: Tree): Int = endLineOf(tree) - lineOf(tree) + 1

	/** Says whether a blank line stands between the end of [before] and the start of [after], two trees of the file in that order. */
	fun blankBetween(before: Tree, after: Tree): Boolean =
		(endLineOf(before) + 1 until lineOf(after)).any { source.lines.getOrElse(it - 1) { "" }.isBlank() }

	/**
	 * The line the name of the method [call] calls is written on, counted from 1. A chain of calls begins
	 * where its first receiver stands, which may be lines above the call a rule speaks of.
	 */
	fun lineOfName(call: MethodInvocationTree): Int {
		val selected = call.methodSelect
		val named = positions.getEndPosition(unit, selected).takeIf { it > 0 }?.minus(1) ?: positions.getStartPosition(unit, selected)

		return unit.lineMap.getLineNumber(named).toInt()
	}

	companion object {
		/** The name [tree] ends in when it names something: `List` for `java.util.List`, `name` for `this.name`. */
		fun simpleName(tree: Tree?): String? = when (tree) {
			is IdentifierTree -> tree.name.toString()
			is MemberSelectTree -> tree.identifier.toString()
			else -> null
		}

		/** The field [tree] names through `this`, `name` for `this.name`, or `null` when it is no such tree. */
		fun ownField(tree: Tree?): String? = (tree as? MemberSelectTree)
			?.takeIf { (it.expression as? IdentifierTree)?.name?.contentEquals(THIS) == true }
			?.identifier?.toString()

		/** Says whether [method] is a constructor. */
		fun isConstructor(method: MethodTree): Boolean = method.name.contentEquals(CONSTRUCTOR)

		/** Says whether [modifiers] carry an annotation whose name is one of [names], however it is qualified. */
		fun annotated(modifiers: ModifiersTree, names: Set<String>): Boolean =
			modifiers.annotations.any { simpleName(it.annotationType) in names }

		/** The fields [type] declares for each of its instances; of a record, those are its components. */
		fun instanceFields(type: ClassTree): List<VariableTree> =
			type.members.filterIsInstance<VariableTree>().filter { Modifier.STATIC !in it.modifiers.flags }

		/** The names of the annotations [annotations] are, each without its package. */
		fun names(annotations: List<AnnotationTree>): Set<String> = annotations.mapNotNull { simpleName(it.annotationType) }.toSet()

		private const val THIS = "this"

		/** The name the parser gives a constructor. */
		private const val CONSTRUCTOR = "<init>"
	}
}
