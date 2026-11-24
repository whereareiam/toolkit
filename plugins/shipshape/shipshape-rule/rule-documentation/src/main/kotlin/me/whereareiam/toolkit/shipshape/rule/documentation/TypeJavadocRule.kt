package me.whereareiam.toolkit.shipshape.rule.documentation

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * Every top-level type of main code has a Javadoc that says what the type is for. The Javadoc stands
 * above the type's annotations, and nothing but those annotations stands between it and the type.
 */
internal class TypeJavadocRule : SourceRule {
	override val id = "type-javadoc"
	override val category = DocumentationRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> =
		sources.all.filter(JavaSource::main).mapNotNull { source ->
		val declaration = Regex("""^(?:$MODIFIER\s+)*$KIND\s+${Regex.escape(source.typeName)}\b""")
		val declared = source.lines.indexOfFirst(declaration::containsMatchIn)
		if (declared < 0 || documented(source.lines, declared)) return@mapNotNull null

		Breach(
			source.path, declared + 1,
			"declares ${source.typeName} without a Javadoc; say what the type is for",
		)
	}

	/** Says whether a Javadoc ends above the line at [declared], with only blank lines and annotations between. */
	private fun documented(lines: List<String>, declared: Int): Boolean {
		val above = (declared - 1 downTo 0).firstOrNull { lines[it].isNotBlank() && lines[it].first() !in ANNOTATION }
		if (above == null || !lines[above].trimEnd().endsWith(COMMENT_END)) return false

		val opened = (above downTo 0).firstOrNull { COMMENT_START in lines[it] } ?: return false
		return lines[opened].trimStart().startsWith(JAVADOC_START)
	}

	private companion object {
		const val MODIFIER = "(?:public|protected|private|abstract|final|sealed|non-sealed|static|strictfp)"
		const val KIND = "(?:@interface|interface|enum|class|record)"

		/** What a line of an annotation begins with: the `@`, or what continues or closes one written over several lines. */
		const val ANNOTATION = "@)}\t"

		const val COMMENT_START = "/*"
		const val JAVADOC_START = "/**"
		const val COMMENT_END = "*/"
	}
}
