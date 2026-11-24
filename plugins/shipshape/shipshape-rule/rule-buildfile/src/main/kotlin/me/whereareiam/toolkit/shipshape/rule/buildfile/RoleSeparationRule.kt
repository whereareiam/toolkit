package me.whereareiam.toolkit.shipshape.rule.buildfile

import me.whereareiam.toolkit.shipshape.api.BuildRule
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * An empty line stands between two roles of a block `dependencies` (java-conventions, "Gradle script style"):
 * where the configuration changes from one declaration to the next, the block shows it.
 */
internal class RoleSeparationRule : BuildRule {
	override val id = "role-separation"
	override val category = BuildFileRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(file: BuildFile, context: RuleContext): List<Breach> = file.dependencies.flatMap { block ->
		block.zipWithNext()
			.filter { (before, declared) -> declared.configuration != before.configuration && !declared.separated }
			.map { (before, declared) ->
				val sentence = "declares ${declared.configuration} directly behind ${before.configuration}; $ASKED"

				Breach(file.path, declared.line, sentence)
			}
	}

	private companion object {
		const val ASKED = "an empty line stands between two roles"
	}
}
