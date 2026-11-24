package me.whereareiam.toolkit.shipshape.rule.imports

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.java.QualifiedNames
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * No name is written in full where an import works, in main code and in tests. An import works for
 * every name nothing else in the file clashes with; [QualifiedNames] decides that.
 */
internal class ImportableNameRule : SourceRule {
	override val id = "importable-name"
	override val category = ImportRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> {
		val names = QualifiedNames(sources)

		return sources.all.flatMap { source ->
			names.of(source).filter { it.importable }.map {
				Breach(
					source.path, it.line,
					"writes ${it.name} in full; import it, nothing else here is called ${it.simpleName}",
				)
			}
		}
	}
}
