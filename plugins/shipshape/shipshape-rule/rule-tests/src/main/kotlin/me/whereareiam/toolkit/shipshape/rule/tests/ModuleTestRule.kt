package me.whereareiam.toolkit.shipshape.rule.tests

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaModule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import me.whereareiam.toolkit.shipshape.api.type.RoleFolder

/**
 * A module whose classes do something holds a test. A class does something when it has a body to run and is no
 * exception: an interface, an enum, an annotation and a class Lombok writes whole have nothing to run.
 */
internal class ModuleTestRule : SourceRule {
	override val id = "module-test"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.modules.mapNotNull { module ->
		val files = sources.of(module)
		if (files.any { it.isOf(JavaSource.TEST) }) return@mapNotNull null

		files.filter(JavaSource::main).firstOrNull(::doesSomething)?.let { withoutTest(module, it) }
	}

	/** Says whether the file of [source] holds something a test could prove. */
	private fun doesSomething(source: JavaSource): Boolean =
		!source.isBelow(RoleFolder.EXCEPTION)
			&& Regex("""\bclass\s+${Regex.escape(source.typeName)}\b""").containsMatchIn(source.code)
			&& BODY.containsMatchIn(source.code)

	private fun withoutTest(module: JavaModule, unproven: JavaSource): Breach {
		val folder = listOf(module.directory, "src/${JavaSource.MAIN}/java").filter(String::isNotEmpty).joinToString("/")

		return Breach(
			folder, null,
			"holds ${unproven.typeName}, which does something, and ${module.label} has no test; a module whose classes do something " +
				"holds a test",
		)
	}

	private companion object {
		/** Where a method or a constructor begins its body. */
		val BODY = Regex("""\)\s*(?:throws\s+[\w.,\s]+?)?\s*\{""")
	}
}
