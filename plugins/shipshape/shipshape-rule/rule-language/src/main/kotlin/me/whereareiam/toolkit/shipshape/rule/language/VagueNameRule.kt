package me.whereareiam.toolkit.shipshape.rule.language

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A name says what the thing is. No type of main code is named `…Util`, `…Utils`, `…Impl` or
 * `…Helper`: the standard implementation of a contract is `Default<Contract>`, the one behind a technology
 * carries the technology's name, and a class of stateless operations is named after what it works on.
 *
 * `…Manager` is not held: the name is discouraged and stays where it is the precise one, as for a class that
 * implements a `TrustManager`, which a reader decides.
 */
internal class VagueNameRule : SourceRule {
	override val id = "vague-name"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> =
		sources.all.filter(JavaSource::main).mapNotNull { source ->
		val ending = VAGUE.firstOrNull(source.typeName::endsWith) ?: return@mapNotNull null

		Breach(
			source.path, null,
			"is named …$ending, which says nothing of what the type is; name it after what it does or what it is made with",
		)
	}

	private companion object {
		/** The endings of a name that say only that the type helps another. */
		val VAGUE = listOf("Utils", "Util", "Impl", "Helper")
	}
}
