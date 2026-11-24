package me.whereareiam.toolkit.shipshape.rule.documentation

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * An example in a Javadoc stands in `<pre>{@code …}</pre>`. No Javadoc of main code or of tests uses
 * `{@snippet …}`: the tag reads a file beside the source or holds its own markup, and the Javadocs of a build
 * are written one way.
 */
internal class SnippetTagRule : SourceRule {
	override val id = "snippet-tag"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.flatMap { source ->
		source.lines.withIndex().filter { (_, line) -> SNIPPET.containsMatchIn(line) }.map { (index, _) ->
			Breach(source.path, index + 1, "writes an example with @snippet; write it in <pre>{@code …}</pre>")
		}
	}

	private companion object {
		/** The tag on a line of a Javadoc: such a line begins with the slash and the two stars that open the comment, or with a star. */
		val SNIPPET = Regex("""^\s*(?:/\*\*|\*).*\{@snippet\b""")
	}
}
