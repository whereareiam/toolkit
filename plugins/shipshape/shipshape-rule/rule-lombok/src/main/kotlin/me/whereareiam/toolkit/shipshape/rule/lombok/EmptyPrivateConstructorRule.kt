package me.whereareiam.toolkit.shipshape.rule.lombok

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * The private constructor without parameters and without a body, which only keeps a class of static
 * members from being made, is not written by hand in main code. Lombok writes it, the same way everywhere.
 */
internal class EmptyPrivateConstructorRule : SourceRule {
	override val id = "empty-private-constructor"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.filter(JavaSource::main).flatMap { source ->
		source.find(CONSTRUCTOR).filter { declares(source, it.group(1)) }.map {
			Breach(
				source.path, it.line,
				"writes the empty private constructor of ${it.group(1)} by hand; " +
					"@NoArgsConstructor(access = AccessLevel.PRIVATE) writes it",
			)
		}
	}

	/** Says whether [source] declares a class or an enum named [type]; only then is `private Type() {}` a constructor. */
	private fun declares(source: JavaSource, type: String): Boolean =
		Regex("""\b(?:class|enum)\s+$type\b""").containsMatchIn(source.code)

	private companion object {
		val CONSTRUCTOR = Regex("""\bprivate\s+([A-Z]\w*)\s*\(\s*\)\s*\{\s*}""")
	}
}
