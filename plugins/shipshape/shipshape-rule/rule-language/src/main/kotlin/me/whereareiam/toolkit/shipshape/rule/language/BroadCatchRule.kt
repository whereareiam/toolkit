package me.whereareiam.toolkit.shipshape.rule.language

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionPlace
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * `Exception`, `RuntimeException` and `Throwable` are caught only where a boundary must answer
 * whatever happens. Everywhere else main code catches what the call throws, so a failure nobody foresaw is
 * not taken for one somebody did. A boundary is declared in the root build file, with what would be lost
 * otherwise: `shipshape { catchesEverything(type, reason) }`.
 */
internal class BroadCatchRule : SourceRule {
	override val id = "broad-catch"
	override val category = LanguageRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override val exemptions = setOf(CATCHES_EVERYTHING)

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.filter(JavaSource::main).flatMap { source ->
		source.find(CATCH).mapNotNull { clause ->
			val caught = broadest(clause.group(1)) ?: return@mapNotNull null

			Breach(
				source.path, clause.line,
				"catches $caught; catch what the call throws, or declare the class a boundary: " +
					CATCHES_EVERYTHING.declaration(source.qualifiedName),
				ExemptionPlace(CATCHES_EVERYTHING, source.qualifiedName),
			)
		}
	}

	/** The first of the three broad types among what a `catch` names, [caught], or `null` when it names none. */
	private fun broadest(caught: String): String? = caught.trim().replace(SPACE, " ").substringBeforeLast(' ')
		.split('|')
		.map { it.trim().substringAfterLast(' ').removePrefix("java.lang.") }
		.firstOrNull(BROAD::contains)

	internal companion object {
		/** A class that is a boundary and catches whatever is thrown, by its full name. */
		val CATCHES_EVERYTHING = ExemptionKind("catchesEverything")

		private val BROAD = setOf("Exception", "RuntimeException", "Throwable")
		private val CATCH = Regex("""\bcatch\s*\(([^)]*)\)""")
		private val SPACE = Regex("""\s+""")
	}
}
