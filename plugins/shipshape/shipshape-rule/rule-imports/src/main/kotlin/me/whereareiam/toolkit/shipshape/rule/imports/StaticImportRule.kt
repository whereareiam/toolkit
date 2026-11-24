package me.whereareiam.toolkit.shipshape.rule.imports

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A static import stands in the code of tests only, where an assertion reads as a sentence. Main code
 * names the class a member comes from.
 */
internal class StaticImportRule : SourceRule {
	override val id = "static-import"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.filter(JavaSource::main).flatMap { source ->
		source.find(STATIC).map {
			Breach(
				source.path, it.line,
				"imports ${it.group(1)} statically; main code names the class, a static import stands in tests only",
			)
		}
	}

	private companion object {
		val STATIC = Regex("""(?m)^[ \t]*import\s+static\s+([\w.]+(?:\.\*)?)\s*;""")
	}
}
