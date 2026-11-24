package me.whereareiam.toolkit.shipshape.rule.buildfile

import me.whereareiam.toolkit.shipshape.api.BuildRule
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.api.buildfile.PluginDeclaration
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * The plugins of a build file stand in the order of java-conventions, "Gradle script style": the built-ins,
 * the external ids, then the conventions, and each kind by the length of the name and then by the alphabet.
 */
internal class PluginOrderRule : BuildRule {
	override val id = "plugin-order"

	override val enforcement = Enforcement.FAILS

	override fun check(file: BuildFile, context: RuleContext): List<Breach> = file.plugins.zipWithNext()
		.filter { (before, applied) -> ORDER.compare(applied, before) < 0 }
		.map { (before, applied) -> Breach(file.path, applied.line, "applies ${applied.name} behind ${before.name}; $ASKED") }

	private companion object {
		const val ASKED = "plugins stand as built-ins, external ids, then conventions, " +
			"each kind by the length of the name and then by the alphabet"

		val ORDER: Comparator<PluginDeclaration> = compareBy({ it.kind }, { it.name.length }, { it.name })
	}
}
