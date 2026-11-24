package me.whereareiam.toolkit.shipshape.rule.imports

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/** No package and no class is imported whole, in main code and in tests: an import names what it brings. */
internal class WildcardImportRule : SourceRule {
	override val id = "wildcard-import"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.flatMap { source ->
		source.find(WILDCARD).map {
			Breach(source.path, it.line, "imports ${it.group(1)}.* whole; import each name it uses")
		}
	}

	private companion object {
		val WILDCARD = Regex("""(?m)^[ \t]*import\s+(?:static\s+)?([\w.]+)\.\*\s*;""")
	}
}
