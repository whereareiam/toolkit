package me.whereareiam.toolkit.shipshape.rule.packages

import com.sun.source.tree.ClassTree
import com.sun.source.tree.Tree
import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import me.whereareiam.toolkit.shipshape.api.type.RoleFolder

/**
 * A type lies in the role folder of its kind. In an API module an enum lies below `type/` and the
 * options, the class named `…Options`, below `config/`; in every module of main code an exception lies below
 * `exception/`, also a single one. The top-level type of a file is read: a type nested in another lies where
 * its owner does.
 *
 * An enum of an implementation belongs to its package and is not held: only an API's closed types have the folder.
 */
internal class RoleFolderRule : ShapeRule {
	override val id = "role-folder"
	override val category = PackageRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		val source = parsed.source
		if (!source.main) return emptyList()

		val ofAnApi = context.kindOf(source) == ArchitectureKind.API

		return parsed.topLevel.mapNotNull { type ->
			val (folder, asked) = placed(type, ofAnApi) ?: return@mapNotNull null
			if (source.isBelow(folder)) return@mapNotNull null

			Breach(
				source.path, parsed.lineOf(type),
				"declares ${type.simpleName} outside of ${folder.folder}/; $asked",
			)
		}
	}

	/**
	 * The role folder [type] belongs below, with what is asked of a type of its kind, or `null` when the kind
	 * of [type] has no folder.
	 *
	 * @param ofAnApi whether [type] is of an API module
	 */
	private fun placed(type: ClassTree, ofAnApi: Boolean): Pair<RoleFolder, String>? = when {
		type.kind == Tree.Kind.CLASS && ParsedSource.simpleName(type.extendsClause)?.let(THROWN::containsMatchIn) == true ->
			RoleFolder.EXCEPTION to "an exception lies below ${RoleFolder.EXCEPTION.folder}/, also a single one"
		ofAnApi && type.kind == Tree.Kind.ENUM -> RoleFolder.TYPE to "an enum of an api lies below ${RoleFolder.TYPE.folder}/"
		ofAnApi && type.kind == Tree.Kind.CLASS && type.simpleName.endsWith(OPTIONS) ->
			RoleFolder.CONFIG to "the options of an api lie below ${RoleFolder.CONFIG.folder}/"
		else -> null
	}

	private companion object {
		const val OPTIONS = "Options"

		/** What the name of a class ends in that is thrown: an exception extends one of these. */
		val THROWN = Regex("(?:Exception|Error|Throwable)$")
	}
}
