package me.whereareiam.toolkit.shipshape.rule.types

import com.sun.source.tree.VariableTree
import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * In an API and in an implementation, a state, a kind, a type, a status, a mode, a role, a level or a
 * category is no `String`: a field or a parameter of that type whose name is one of those words, or ends in it, is an
 * enum or a sealed type in `type/`. Ids stay `String`. A code is not read: a problem code is a typed one, and a
 * confirmation or recovery code is a secret text. A media type is an open set, written by whoever sends it, and stays
 * text. A parameter of a method that overrides another is held where that one is declared. A class named `…Mapper` of
 * an implementation reads the words of the wire, as `string-choice` lets it, and an assembly only wires: neither is read.
 */
internal class StringStateRule : ShapeRule {
	override val id = "string-state"
	override val category = TypeRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		val source = parsed.source
		val kind = context.kindOf(source)
		if (!source.main || kind !in HELD || source.mapper && kind == ArchitectureKind.IMPLEMENTATION) return emptyList()

		val fields = parsed.types.flatMap { type ->
			type.members.filterIsInstance<VariableTree>().filter(::isAStatedText).map { breach(parsed, it, "the field ${it.name}") }
		}
		val parameters = parsed.methods.filterNot { (_, method) -> ParsedSource.annotated(method.modifiers, OVERRIDE) }
			.flatMap { (_, method) ->
				method.parameters.filter(::isAStatedText).map { breach(parsed, it, "the parameter ${it.name} of ${method.name}()") }
			}

		return (fields + parameters).sortedBy(Breach::line)
	}

	private fun isAStatedText(variable: VariableTree): Boolean =
		ParsedSource.simpleName(variable.type) == TEXT && variable.type.toString() in WRITTEN &&
			NAMED.matches(variable.name) && variable.name.toString() !in OPEN

	private fun breach(parsed: ParsedSource, variable: VariableTree, what: String): Breach = Breach(
		parsed.source.path, parsed.lineOf(variable),
		"declares $what as a String; a ${word(variable.name.toString())} is an enum or a sealed type below type/",
	)

	private fun word(name: String): String = WORDS.first { name.endsWith(it) || name.endsWith(it.replaceFirstChar(Char::uppercase)) }

	private companion object {
		val HELD = setOf(ArchitectureKind.API, ArchitectureKind.IMPLEMENTATION)

		const val TEXT = "String"
		val WRITTEN = setOf(TEXT, "java.lang.String")

		/** The words a name of a closed set ends in. */
		val WORDS = listOf("state", "kind", "type", "status", "mode", "role", "level", "category")

		/** Names that end in one of [WORDS] and still hold an open set of texts. */
		val OPEN = setOf("contentType", "mediaType", "mimeType")

		/** A name that is one of [WORDS], or ends in one written with a capital. */
		val NAMED = Regex(
			"(?:${WORDS.joinToString("|")})|[a-z0-9]\\w*(?:${WORDS.joinToString("|") { it.replaceFirstChar(Char::uppercase) }})",
		)

		val OVERRIDE = setOf("Override")
	}
}
